package com.example.levelup.models;

public class Message {
    private String username;
    private String content;
    private String time;

    public Message(String username, String content, String time) {
        this.username = username;
        this.content = content;
        this.time = time;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public String getTime() {
        return time;
    }
}