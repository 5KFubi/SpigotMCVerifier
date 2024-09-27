package me.fivekfubi.DataBase;

import java.util.List;

public class UserData {

    private String email;
    private String resources;
    private String discordId;

    public UserData(String email, String resources, String discordId) {
        this.email = email;
        this.resources = resources;
        this.discordId = discordId;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getResources() {
        return resources;
    }
    public void setResources(String resources) {
        this.resources = resources;
    }
    public String getDiscordId() {
        return discordId;
    }
    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }
}
