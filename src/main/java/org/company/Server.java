/**
 * The Server class orchestrates the main operations for the chat server, including accepting client connections,
 * managing client threads, and facilitating communication between clients. It ensures messages are correctly
 * broadcasted or sent privately as needed. This class acts as the central hub for all server-side chat functionalities.
 */

package org.company;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Server {
    private int port;
    private List<ClientHandler> clientHandlers;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private ExecutorService threadPool; // For handling client threads
    private JTextArea serverLogTextArea;
    private ClientHandler coordinator;
    private List<ClientHandler> connectedClients = new ArrayList<>();


    // Setter method for serverLogTextArea
    public void setServerLogTextArea(JTextArea logTextArea) {
        this.serverLogTextArea = logTextArea;
    }

    // Method to append a message to the server log
    public void appendToServerLog(String message) {
        if (serverLogTextArea != null) {
            SwingUtilities.invokeLater(() -> {
                serverLogTextArea.append(message + "\n");
            });
        } else {
            System.out.println("Server log text area not set or GUI not initialized.");
        }
    }

    // Initializes the server on the specified port and prepares resources for handling client connections.
    public Server(int port) {
        this.port = port;
        this.clientHandlers = new ArrayList<>();
        this.isRunning = false;
        this.threadPool = Executors.newCachedThreadPool();
    }

    // Starts the server, accepting client connections and delegating them to ClientHandlers via a thread pool.
    public void start() {
        isRunning = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept(); // Accept incoming connections
                    appendToServerLog("Accepted connection from " + clientSocket.getInetAddress().getHostAddress()); // Log the connection

                    // Perform any necessary checks here. For example:
                    if (shouldRejectClient(clientSocket)) {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("Error in connecting to the server: The server is full or you are connecting to the wrong instance.");
                        clientSocket.close();
                        continue; // Skip further processing for this client
                    }

                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    threadPool.execute(clientHandler); // Handle client in a separate thread
                    clientHandlers.add(clientHandler);
                    updateActiveUsers(); // call this method to update all clients with the new user list
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start the server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop(); // Ensure the server is stopped if it exits the loop
        }
    }

    // Determines whether an incoming client connection should be rejected based on custom logic.
    private boolean shouldRejectClient(Socket clientSocket) {
        // Implement logic to determine if the client should be rejected
        // For example, check if the server is full or if there's another reason the client shouldn't connect
        return false; // Replace with actual logic
    }

    // Stops the server, closing the server socket and terminating active client handler threads.
    public void stop() {
        isRunning = false;
        try {
            serverSocket.close(); // Ensure the server socket is closed
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadPool.shutdown(); // Shutdown the thread pool
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow(); // Force shutdown if tasks did not finish
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Broadcasts a message to all connected clients except the sender.
    public synchronized void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            // Do not send the message to the sender
            if (!client.equals(sender)) {
                client.sendMessage(message);
            }
        }
    }

    // Checks if the given username is already taken by another connected client.
    public synchronized boolean isUsernameTaken(String username) {
        return clientHandlers.stream().anyMatch(client -> username.equalsIgnoreCase(client.getClientName()));
    }

    // Removes a client from the server's list of active connections and updates the user list.
    public void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        System.out.println("Client disconnected: " + clientHandler.getClientName());
        // If the leaving client is the coordinator, reassign the role
        if (coordinator != null && coordinator.equals(clientHandler)) {
            reassignCoordinator();
        }
        updateActiveUsers();
    }

    // Attempts to reassign the coordinator role when the current coordinator disconnects.
    private void reassignCoordinator() {
        if (!clientHandlers.isEmpty()) {
            // Assign the coordinator role to the next user in the list
            coordinator = clientHandlers.get(0);
            setCoordinator(coordinator);
        } else {
            // If there are no users left, the coordinator is set to null
            coordinator = null;
        }
    }

    // Processes and executes commands received from clients, such as /kick and /requestuserinfo.
    public void executeCommand(String command, ClientHandler sender) {
        String[] tokens = command.split("\\s+", 2);
        String baseCommand = tokens[0].toLowerCase();
        switch (baseCommand) {
            case "/kick":
                if (tokens.length > 1) {
                    String usernameToKick = tokens[1];
                    kickUser(usernameToKick, sender);
                    break;
                } else {
                    sender.sendMessage("Usage: /kick <username>");
                }
                break;
            case "/activeusers":
                // This case to handles a similar functionality as the requested "/requestuserlist"
                String activeUsers = clientHandlers.stream()
                        .map(ClientHandler::getClientName)
                        .collect(Collectors.joining(", "));
                sender.sendMessage("/updateusers " + activeUsers); // Ensure this format is consistent with the client's expectation
                break;
            case "/requestuserlist":
                // Responds to a request for a list of currently active users
                String users = clientHandlers.stream()
                        .map(ClientHandler::getClientName)
                        .collect(Collectors.joining(","));
                sender.sendMessage("/updateusers " + users);
                break;

            case "/requestuserinfo":
                if (tokens.length > 1) {
                    String requestedUsername = tokens[1];
                    sendUserInfoToRequester(requestedUsername, sender);
                } else {
                    sender.sendMessage("Usage: /requestuserinfo <username>");
                }
                break;

        }
    }

    // Sends information about a specific user to the requester if the user is found.
    private void sendUserInfoToRequester(String username, ClientHandler requester) {
        for (ClientHandler client : clientHandlers) {
            if (client.getClientName().equalsIgnoreCase(username)) {
                String userInfo = String.format("Name: %s, IP Address: %s, Port: %d",
                        client.getClientName(), client.getIpAddress(), client.getPort());
                requester.sendMessage(userInfo);
                return;
            }
        }
        requester.sendMessage("Error: User not found.");
    }

    // Allows the coordinator to kick a user from the chat, removing them from the server and notifying clients.
    private void kickUser(String username, ClientHandler initiator) {
        if (initiator != coordinator) {
            initiator.sendMessage("Error: You are not authorized to kick users.");
            return;
        }

        ClientHandler userToKick = null;
        for (ClientHandler client : clientHandlers) {
            if (client.getClientName().equalsIgnoreCase(username)) {
                userToKick = client;
                break;
            }
        }

        if (userToKick != null) {
            broadcastMessage("User " + username + " has been kicked out by the coordinator.", initiator);
            userToKick.disconnect(); // This should remove the user and close the socket
            removeClient(userToKick); // Remove from the list
            updateActiveUsers(); // Update the active user list
        } else {
            initiator.sendMessage("Error: User " + username + " not found.");
        }
    }


    // Entry point of the server application, initializes and starts the server on a specified port.
    public static void main(String[] args) {
        int port = 8080; // or read from args or a configuration
        Server server = new Server(port);
        server.start();
    }

    // Returns the current running state of the server.
    public boolean isRunning() {
        return isRunning;
    }

    // Updates all connected clients with the latest list of active usernames.
    public synchronized void updateActiveUsers() {
        String activeUsers = clientHandlers.stream()
                .map(ClientHandler::getClientName)
                .collect(Collectors.joining(","));

        String updateMessage = "/updateusers " + activeUsers;
        for (ClientHandler client : clientHandlers) {
            client.sendMessage(updateMessage);
        }
    }

    // Assigns the coordinator role to a specified client and notifies all clients of the new coordinator.
    public synchronized void setCoordinator(ClientHandler clientHandler) {
        // Inform the new coordinator
        clientHandler.sendMessage("You are now the coordinator.");

        // Inform all other clients about the new coordinator
        String coordinatorMessage = clientHandler.getClientName() + " is now the coordinator";
        for (ClientHandler client : clientHandlers) {
            if (client != clientHandler) { // Don't send this message to the new coordinator
                client.sendMessage(coordinatorMessage);
            }
        }

        coordinator = clientHandler; // Update the coordinator reference
        // Broadcast a message if needed or update server log
    }

    // Notifies the server of a new client connection and potentially assigns them as the new coordinator.
    public void notifyNewClientConnection(String clientName) {
        // Updates the server log to show the new connection
        SwingUtilities.invokeLater(() -> serverLogTextArea.append(clientName + " has connected.\n"));

        synchronized (this) {
            // If there's no coordinator yet, set the newly connected client as the coordinator
            if (coordinator == null) {
                for (ClientHandler client : clientHandlers) {
                    if (client.getClientName().equals(clientName)) {
                        setCoordinator(client);
                        break;
                    }
                }
            }
        }
    }

    // Sends a private message from one client to another identified by the recipient's name.
    public void sendPrivateMessage(String message, String recipientName, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            if (client.getClientName().equals(recipientName)) {
                System.out.println("Debug: Sending private message from " + sender.getClientName() + " to " + recipientName); // Debugging line
                client.sendMessage(sender.getClientName() + " (private): " + message);
                return; // message sent to the intended recipient; no need to continue the loop
            }
        }
        // If we reach here, the recipient was not found
        System.out.println("Debug: Private recipient not found: " + recipientName); // Debugging line
        sender.sendMessage("User " + recipientName + " not found or not connected.");
    }


    // Broadcasts a shutdown signal to all connected clients, prompting them to disconnect.
    public void broadcastShutdownSignal() {
        for (ClientHandler handler : clientHandlers) {
            handler.sendShutdownSignal();
        }
    }

    // Disconnects all clients from the server by closing their connections.
    public void disconnectAllClients() {
        for (ClientHandler handler : clientHandlers) {
            handler.disconnectClient();
        }
    }

    // Waits for all client handling threads to finish execution before proceeding.
    public void waitForClientThreadsToFinish() {
        for (ClientHandler handler : clientHandlers) {
            try {
                handler.join(); // Waits for this thread to die
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // preserve interruption status
                // Handle exception, perhaps log it
            }
        }
    }

    // Method to add a client handler to the list
    public void addClient(ClientHandler clientHandler) {
        clientHandlers.add(clientHandler);
    }

    // Getter for clientHandlers
    public synchronized List<ClientHandler> getClientHandlers() {
        return new ArrayList<>(clientHandlers);
    }

    // Retrieves a client handler by username, returning null if no matching client is found.
    public ClientHandler getClientHandlerByUsername(String username) {
        for (ClientHandler client : this.connectedClients) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null; // or throw an exception if preferred
    }

}

