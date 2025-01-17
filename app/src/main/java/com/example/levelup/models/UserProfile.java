package com.example.levelup.models;

import java.util.List;

public class UserProfile {
    public String nickname;
    public List<String> favoriteGames;

    public UserProfile() {
        // Default constructor required for calls to DataSnapshot.getValue(UserProfile.class)
    }

    public UserProfile(String nickname, List<String> favoriteGames) {
        this.nickname = nickname;
        this.favoriteGames = favoriteGames;
    }
}