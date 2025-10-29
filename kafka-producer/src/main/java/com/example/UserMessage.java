package com.example;

public class UserMessage {
    private String message;
    private String username;

    public UserMessage() {
    }

    public UserMessage(String message, String username) {
        this.message = message;
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    @Override
    public String toString() {
        return "UserMessage{" + "message='" + message + '\'' + ", username='" + username + '}';
    }
}
