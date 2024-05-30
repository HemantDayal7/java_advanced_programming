/**
 * This class is the user interface component of the chat application. It provides
 * a graphical interface for users to interact with the chat service. The class is responsible
 * for setting up the layout and visual elements through which users can send and receive messages,
 * view the status of their connection, and see the list of online users. It integrates with the
 * ClientNetworkManager to handle network operations and ClientUserManager for user-related functionalities.
 */

package org.company;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChatClientUI {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JList<String> userList;
    private DefaultListModel<String> userModel;
    private JButton sendButton;
    private JButton quitButton;


    private ClientNetworkManager networkManager;
    private ClientUserManager userManager;
    private String username;

    public ChatClientUI(ClientNetworkManager networkManager, ClientUserManager userManager, String username) {
        this.networkManager = networkManager;
        this.userManager = userManager;
        this.username = username; // set the username passed from the constructor
        initializeUI(); // initialize the user interface
    }

    // Sets up the chat client's GUI components, including message display, user list, and interaction buttons.
    private void initializeUI() {
        frame = new JFrame(username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        messageArea = new JTextArea(20, 40);
        messageArea.setEditable(false);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);

        inputField = new JTextField(40);
        userModel = new DefaultListModel<>();
        userList = new JList<>(userModel);
        JScrollPane userScrollPane = new JScrollPane(userList);

        setUpUserListContextMenu(); // Setup context menu for user list

        sendButton = new JButton("Send");
        quitButton = new JButton("Quit");


        quitButton.addActionListener(e -> {
            // Logic to handle client shutdown
            networkManager.disconnect();
            frame.dispose(); // Closes the GUI assuming chatClientUI is JFrame or holds your JFrame
            System.exit(0); // Terminate the application
        });
        sendButton.addActionListener(this::sendMessageAction);

        // Layout components
        frame.add(messageScrollPane, BorderLayout.CENTER);
        frame.add(inputField, BorderLayout.SOUTH);

        // Panel for user list and buttons
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(userScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);
        buttonPanel.add(quitButton);
        eastPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(eastPanel, BorderLayout.EAST);

        frame.setSize(new Dimension(300, 400));
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Creates a context menu for the user list, allowing additional options like viewing user information.
    private void setUpUserListContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem viewInfoItem = new JMenuItem("User Information");
        viewInfoItem.addActionListener(e -> requestUserInfo());
        contextMenu.add(viewInfoItem);

        userList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && !userList.isSelectionEmpty() && userList.locationToIndex(e.getPoint()) == userList.getSelectedIndex()) {
                    contextMenu.show(userList, e.getX(), e.getY());
                }
            }
        });
    }

    // Requests detailed information about the selected user from the server.
    private void requestUserInfo() {
        String selectedUser = userList.getSelectedValue();
        if (selectedUser != null && !selectedUser.isEmpty()) {
            networkManager.sendMessage("/requestuserinfo " + selectedUser);
        }
    }

    // Sends the message written in the input field to the server and displays it in the chat area.
    private void sendMessageAction(ActionEvent e) {
        if (networkManager.isConnected()) {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                networkManager.sendMessage(message);
                displayMessage("You: " + message); // This will append the message to the chat area
                inputField.setText("");
            }
        } else {
            displayMessage("Not connected to server. Please connect first.");
        }
    }


    // Refreshes the display of the user list with the most current usernames.
    public void refreshUserList() {
        userModel.clear();
        userModel.addAll(userManager.readUsernames());
    }

    // Appends a given message to the chat area and handles special UI cases for coordinator messages.
    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
            // Check if the message is about coordinator status
            if (message.endsWith("is now the coordinator") || message.startsWith("You are now the coordinator")) {
                // Display the message differently or trigger any additional UI changes
                JOptionPane.showMessageDialog(frame, message);
            }
        });
    }

    // Sets the network manager for the client and updates UI components based on the connection status.
    public void setNetworkManager(ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
        // Assuming the network manager must be connected before enabling the send button.
        sendButton.setEnabled(networkManager.isConnected());
    }

    // Updates the user list UI with the provided array of usernames.
    public void updateUserList(String[] usernames) {
        SwingUtilities.invokeLater(() -> {
            userModel.clear();
            for (String user : usernames) {
                userModel.addElement(user);
            }
        });
    }

    // Retrieves the main frame of the client's user interface.
    public JFrame getFrame() {
        return frame;
    }
}
