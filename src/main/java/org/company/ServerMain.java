/**
 * The ServerMain class serves as the entry point for the chat server application. It initializes
 * and starts the Server instance on a specified port, which can be passed as a command-line argument.
 * This class also sets up a shutdown hook to ensure the server is gracefully stopped upon termination.
 */

package org.company;

public class ServerMain {
    public static void main(String[] args) {
        // Determine the port number, can be passed as a command line argument
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        try {
            Server server = new Server(port);
            System.out.println("Starting the server on port " + port);
            server.start();

            // Shutdown hook to gracefully stop the server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Stopping the server.");
                server.stop();
            }));
        } catch (Exception e) {
            System.err.println("Error starting the server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
