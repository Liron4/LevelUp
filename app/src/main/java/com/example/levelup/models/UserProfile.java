package com.example.levelup.models;

import java.util.List;

public class UserProfile {
    public String nickname;
    public List<String> favoriteGames;

    public String latestMessage;

    public long timestamp;

    public List<String> favList;
    public List<String> blockedList;

    public UserProfile() {
        // Default constructor required for calls to DataSnapshot.getValue(UserProfile.class)
    }

    public UserProfile(String nickname, List<String> favoriteGames, List<String> favList, List<String> blockedList) {
        this.nickname = nickname;
        this.favoriteGames = favoriteGames;
        this.favList = favList;
        this.blockedList = blockedList;
    }
}