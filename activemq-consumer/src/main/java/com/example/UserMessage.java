package com.example;

import java.time.LocalDateTime;

public class UserMessage {
    private String message;
    private String username;
    private LocalDateTime timestamp = LocalDateTime.now();

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UserMessage{" + "message='" + message + '\'' + ", username='" + username + '\'' + ", timestamp=" + timestamp + '}';
    }
}
