/**
 * The ClientHandler class is responsible for managing communication between a single client and the server in a chat application.
 * Each instance of this class is dedicated to handling the input and output streams for one client socket connection,
 * facilitating the exchange of messages between the client it serves and other clients via the server.
 *
 */

package org.company;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final BufferedReader input;
    private final PrintWriter output;
    private final Server server;
    private String name; // Name cannot be final because it's assigned later
    private volatile boolean running = true;
    private String ipAddress;
    private int port;
    private String username;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        this.ipAddress = socket.getInetAddress().getHostAddress();
        this.port = socket.getPort();
    }

    //getters for ipAddress and port
    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
    // used for testing purposes
    public String getUsername() {
        return this.username;
    }


    @Override
    public void run() {
        try {
            // Read the username from the input stream
            String attemptedName = input.readLine().trim();

            if (attemptedName == null || attemptedName.isEmpty()) {
                System.out.println("Username is null or empty");
                output.println("Error: Username cannot be empty.");
                disconnect(); // Disconnect if no valid name is provided
                return; // Exit the run method
            } else if (server.isUsernameTaken(attemptedName)) {
                System.out.println("Username is already taken: " + attemptedName);
                output.println("Error: Username is already taken.");
                socket.close(); // Disconnect if username is taken
                return; // Exit the run method
            }

            // If username is valid and not taken, proceed
            this.name = attemptedName; // Set the username
            server.notifyNewClientConnection(this.name);
            sendWelcomeMessage(); // sends a welcome message
            notifyJoin(); // notifies when a new client joins
            server.updateActiveUsers(); // Updates the active user list

            String inputLine;
            while ((inputLine = input.readLine()) != null) {
                if (inputLine.startsWith("/")) {
                    // If the input line starts with "/", it's a command. Execute it.
                    server.executeCommand(inputLine, this);
                } else {
                    // If it's not a command, process it as a regular message.
                    processInput(inputLine, this);
                }
            }
        } catch (IOException e) {
            System.out.println(name + " encountered an error: " + e.getMessage());
            sendMessage("An error occurred: " + e.getMessage()); // Informs the user about the error if the connection is still alive.
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    // sends a welcome message
    private void sendWelcomeMessage() {
        sendMessage("Welcome to the Chat Client, " + name);
    }

    private void notifyJoin() {
        // Notify other users that a new user has joined
        String joinMessage = this.name + " has joined the chat!";
        server.broadcastMessage(joinMessage, this); // Pass 'true' to exclude the sender
    }


    private void processInput(String inputLine, ClientHandler client) {
        // If the message starts with "@", it's a private message
        if (inputLine.startsWith("@")) {
            String[] parts = inputLine.split(" ", 2);
            if (parts.length < 2) {
                sendMessage("Invalid private message format. Usage: @username message");
            } else {
                String recipientName = parts[0].substring(1); // remove "@" and get the username
                String message = parts[1];
                server.sendPrivateMessage(message, recipientName, client);
            }
        } else if (!inputLine.startsWith("/")) {
            // If it's not a command, prepend the sender's name and broadcast it as a chat message
            String formattedMessage = this.getClientName() + ": " + inputLine;
            server.broadcastMessage(formattedMessage, this); // 'false' or 'true' depending on whether you want to include the sender

        } else {
            // Here, it handle the command - but does not send the command itself to all clients.
            handleCommand(inputLine, client);
        }
    }


    private void handleCommand(String command, ClientHandler client) {
        // for further implementation
    }

    //Method disconnects the user from the server
    public void disconnect() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Log or handle the exception
        } finally {
            // Notify the server that this client is disconnecting
            if (server != null) {
                server.removeClient(this);
            }
        }
    }

    //for sending messages
    public void sendMessage(String message) {
        output.println(message);
    }

    //gets the client's name
    public String getClientName() {
        return name;
    }

    public void sendShutdownSignal() {
        // sends a specific message that the client understands as a shutdown signal
        if (output != null) {
            output.println("SERVER_SHUTDOWN"); // The client would recognize this command
            output.flush();
        }
    }
    // Implement this method to disconnect a client cleanly
    public void disconnectClient() {
        running = false; // Setted running to false so the client handler stops its operations
        try {
            if (socket != null) {
                socket.close();
            }
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
            // Handle exceptions, perhaps log it
            e.printStackTrace();
        }
    }

}
