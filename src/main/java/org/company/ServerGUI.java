/**
 * The ServerGUI class provides a graphical user interface for the chat server, allowing administrators
 * to monitor server activity, view connected clients, and manage server operations such as start, stop,
 * and broadcasting messages. It serves as a visual tool for overseeing the overall health and status
 * of the chat server environment.
 */

package org.company;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerGUI extends JFrame {

    private JButton startStopButton;
    private JTextArea logTextArea;
    private Server server;
    private boolean isServerRunning = false;
    private JSpinner portSpinner;

    // Constructs the server GUI and initializes its components.
    public ServerGUI() {
        initializeUI();
        setTitle("Chat Server Control Panel");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Sets up the user interface, including the log area and server control panel.
    private void initializeUI() {
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel("Enter port number:"));
        SpinnerNumberModel portModel = new SpinnerNumberModel(8080, 0, 65535, 1);
        portSpinner = new JSpinner(portModel);
        controlPanel.add(portSpinner);

        startStopButton = new JButton("Start Server");
        startStopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isServerRunning) {
                    int port = (int) portSpinner.getValue();
                    startServer(port);
                } else {
                    stopServer();
                }
            }
        });
        controlPanel.add(startStopButton);
        add(controlPanel, BorderLayout.SOUTH);
    }

    // Starts the server on a specified port and updates the UI accordingly.
    private void startServer(int port) {
        if (server == null || !server.isRunning()) {
            server = new Server(port);
            server.setServerLogTextArea(logTextArea); // Pass the log text area to the server
            new Thread(() -> server.start()).start();
            startStopButton.setText("Stop Server");
            logTextArea.append("Server started on port: " + port + "\n");
            isServerRunning = true;
        }
    }

    // Stops the server, gracefully disconnects clients, and updates the UI.
    private void stopServer() {
        if (server != null && isServerRunning) {
            // First, inform all connected clients about server shutdown
            server.broadcastShutdownSignal();

            // Then, attempt to disconnect each client gracefully
            server.disconnectAllClients(); // This method should iterate over all client handlers and disconnect them

            // Finally, stop the server itself
            server.stop(); // This method should close the ServerSocket and interrupt its main loop, if applicable

            // Wait for all client handling threads to finish
            server.waitForClientThreadsToFinish(); // This method should join all threads or ensure they terminate

            // Update UI after all threads have been handled
            SwingUtilities.invokeLater(() -> {
                startStopButton.setText("Start Server");
                logTextArea.append("Server stopped.\n");
            });

            isServerRunning = false;
        }
    }


    // Method to append messages to the log text area but has no usage (for further implementation)
    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> logTextArea.append(message + "\n"));
    }

    // Entry point to run the ServerGUI, creating an instance of the GUI on the Event Dispatch Thread.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerGUI());
    }


}
