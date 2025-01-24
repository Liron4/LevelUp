package com.example.levelup.models;

public class Message {
    private String username;
    private String content;
    private long timestamp;
    private String from;
    private String to;
    private boolean notificationSent;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String username, String content, long timestamp, String from, String to, boolean notificationSent) {
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
        this.from = from;
        this.to = to;
        this.notificationSent = notificationSent;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }
}