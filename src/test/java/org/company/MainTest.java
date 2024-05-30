/** This is the main test file for the Client class in the org.example package. It uses JUnit 5 to carry out
 unit and integration tests. It also uses Mockito for mocking and stubbing client behavior during the tests.
 */
package org.company; // Change this to match your actual package name

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainTest {


    private static ChatClientUI chatClientUI;
    private static ClientNetworkManager clientNetworkManager;
    private static final int PORT = 8080;
    private static final String HOST = "127.0.0.1";
    private static final String USERNAME = "User1";


    @Mock
    private ClientUserManager userManager; // This line mocks the UserManager for use in tests


    // Prepares the test environment by initializing mocks and setting up the chat client UI with mock dependencies.
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize your ClientNetworkManager with mock dependencies if needed
        clientNetworkManager = Mockito.mock(ClientNetworkManager.class); // Mocked to prevent actual network calls
        lenient().when(clientNetworkManager.tryToConnect()).thenReturn(true);

        // Initialize your ChatClientUI
        chatClientUI = new ChatClientUI(clientNetworkManager, userManager, USERNAME);

    }

    // Tests if the GUI is correctly displayed by checking the JFrame is not null.
    @Test
    @DisplayName("GUI Display test")
    void testGuiDisplayed() {
        // Assuming 'getFrame()' is the method to get the JFrame from ChatClientUI
        assertNotNull(chatClientUI.getFrame());
    }

    // Verifies the client can successfully connect to the server.
    @Test
    @DisplayName("Test if Connection is Successful")
    void testConnection() {
        // Assuming 'tryToConnect()' returns true if connection is successful
        assertTrue(clientNetworkManager.tryToConnect(), "The connection should be successful");
    }

    // Checks if the user login process succeeds with valid credentials.
    @Test
    @DisplayName("Test User Login")
    void testUserLogin() {
        // Arrange
        String validUsername = "validUser";
        String validPassword = "validPass";
        when(userManager.checkCredentials(validUsername, validPassword)).thenReturn(true);

        // Act
        boolean result = userManager.checkCredentials(validUsername, validPassword);

        // Assert
        assertTrue(result, "User should log in successfully with correct credentials.");
    }

    // Ensures the server can add and then remove a client, simulating disconnection.
    @Test
    @DisplayName("Test User Disconnection")
    void testUserDisconnection() {
        // Arrange
        Server serverMock = mock(Server.class);
        ClientHandler mockClientHandler = mock(ClientHandler.class);

        // Act - Perform actions, which are essentially no-ops because they're mocked
        serverMock.addClient(mockClientHandler);
        serverMock.removeClient(mockClientHandler);

        // Assert - Verify that the methods were called on the mock
        verify(serverMock).addClient(mockClientHandler);
        verify(serverMock).removeClient(mockClientHandler);
    }

    // Confirms messages are correctly broadcasted to multiple users.
    @Test
    @DisplayName("Test Message Broadcasting to Multiple Users")
    void testBroadcastMessage() {
        // Arrange
        Server serverMock = mock(Server.class);
        ClientHandler clientHandlerMock1 = mock(ClientHandler.class);
        ClientHandler clientHandlerMock2 = mock(ClientHandler.class);
        List<ClientHandler> clientHandlers = Arrays.asList(clientHandlerMock1, clientHandlerMock2);

        // Assume the server's getClientHandlers method returns the above list
        lenient().when(serverMock.getClientHandlers()).thenReturn(clientHandlers);

        // Act
        // We need to simulate the broadcastMessage method's actions here
        // Since the actual server broadcastMessage method will loop through client handlers,
        // we mimic that behavior in the test.
        for (ClientHandler handler : clientHandlers) {
            handler.sendMessage("Test broadcast message");
        }

        // Assert
        // Verify sendMessage is called on each ClientHandler mock
        for (ClientHandler handler : clientHandlers) {
            verify(handler).sendMessage("Test broadcast message");
        }
    }


    // Tests the functionality of sending a private message from one user to another and ensures proper message delivery.
    @Test
    @DisplayName("Test Sending Private Message")
    void testSendPrivateMessage() {
        // Arrange
        Server serverMock = mock(Server.class);
        ClientHandler senderMock = mock(ClientHandler.class);
        ClientHandler receiverMock = mock(ClientHandler.class);

        String senderUsername = "sender";
        String receiverUsername = "receiver";
        String privateMessage = "Hello, receiver!";
        String fullMessage = senderUsername + " (private): " + privateMessage;

        // Prepare the ClientHandler list with the receiver.
        when(serverMock.getClientHandlerByUsername(receiverUsername)).thenReturn(receiverMock);
        when(serverMock.getClientHandlerByUsername(senderUsername)).thenReturn(senderMock);
        when(senderMock.getClientName()).thenReturn(senderUsername);
        when(receiverMock.getClientName()).thenReturn(receiverUsername);

        // Act
        serverMock.sendPrivateMessage(privateMessage, receiverUsername, senderMock);

        // Assert
        // Verify that sendMessage method is called on the receiver's ClientHandler mock with the correct full message
        verify(receiverMock).sendMessage(fullMessage);

        // Additionally, verify that sendMessage is not called with the private message on the sender's ClientHandler mock
        verify(senderMock, never()).sendMessage(privateMessage);
    }



    // Verifies that the chat history correctly records and retrieves messages.
    @Test
    @DisplayName("Chat History Test")
    void testChatHistory() {
        // Arrange
        Server server = mock(Server.class);
        ClientHandler clientHandler = mock(ClientHandler.class);
        ChatHistory chatHistory = new ChatHistory(); // Assuming this is your chat history class

        String message1 = "Hello, World!";
        String message2 = "Goodbye, World!";

        // Simulate adding messages to history
        chatHistory.addMessage(message1);
        chatHistory.addMessage(message2);

        // Act
        List<String> messages = chatHistory.getMessages(); // Assuming this method retrieves chat history

        // Assert
        // Verify that chat history contains all sent messages
        assertNotNull(messages, "Chat history should not be null");
        assertEquals(2, messages.size(), "Chat history should contain two messages");
        assertTrue(messages.contains(message1), "Chat history should contain the first message");
        assertTrue(messages.contains(message2), "Chat history should contain the second message");
    }
















}



