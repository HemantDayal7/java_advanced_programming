/**
 * The ClientUserManager class manages user-related information and actions within the chat client,
 * such as storing and retrieving the user's username and handling user status updates. It serves as
 * a central point for user data management, facilitating interactions with the user list and user details.
 */

package org.company;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClientUserManager {
    private final String filename = "usernames.txt";

    public List<String> readUsernames() {
        List<String> usernames = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                usernames.add(line.split(",")[0].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle read error
        }
        return usernames;
    }

    // Methods for writing and removing usernames as needed but are to be implemented later
    public void addUsername(String username) {
        // Append the username to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write(username + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeUsername(String username) {
        // Read current usernames, filter out the one to remove, and rewrite the file
        List<String> usernames = readUsernames();
        usernames.remove(username);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String user : usernames) {
                writer.write(user + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean createUser(String username) {
        // Simulated logic for user creation. Always returns true for testing.
        if(username == null || username.isEmpty()) {
            // Return false if the username is null or empty
            // to simulate validation failure.
            return false;
        }
        // Assume the username is valid and the user is "created" successfully.
        return true;
    }

    public boolean checkCredentials(String username, String password) {
        // Logic to check credentials
        // This could be checking against a database or a predefined list of users
        // For the purposes of this example, let's just pretend it's a static check
        if("validUser".equals(username) && "validPass".equals(password)) {
            return true; // Valid credentials
        } else {
            return false; // Invalid credentials
        }
    }
}
