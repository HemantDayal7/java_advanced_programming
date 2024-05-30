/*
This class is only implemented for testing purpose
 */


package org.company;

import java.util.ArrayList;
import java.util.List;

public class ChatHistory {
    private List<String> messages = new ArrayList<>();

    public void addMessage(String message) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }
}
