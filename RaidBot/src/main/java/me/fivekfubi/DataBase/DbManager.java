package me.fivekfubi.DataBase;

import java.sql.*;
import java.util.List;

public class DbManager {
    private final Connection connection;

    public DbManager(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "email TEXT," +
                            "resources TEXT," +
                            "discord_id TEXT" +
                            ")");
        }
    }

    public void updateUserData(String discordId, UserData userData) {
        String sql = "UPDATE users SET email = ?, resources = ? WHERE discord_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userData.getEmail());
            pstmt.setString(2, userData.getResources());
            pstmt.setString(3, discordId); // Using discordId to find the user

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("User data updated successfully.");
            } else {
                System.out.println("No user found with the specified Discord ID.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertUserIntoDatabase(UserData userData) {
        String sql = "INSERT INTO users (email, resources, discord_id) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userData.getEmail());
            pstmt.setString(2, String.join(",", userData.getResources())); // Assuming resources is a List<String>
            pstmt.setString(3, String.valueOf(userData.getDiscordId())); // Convert long to String

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public UserData getUserDataByDiscordId(String discordId) {
        String sql = "SELECT email, resources FROM users WHERE discord_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                String resources = rs.getString("resources");
                return new UserData(email, resources, discordId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // Return null if no user is found
    }

    public int getNextAvailableID() {
        try {
            String sql = "SELECT MAX(id) FROM users";
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int maxID = resultSet.getInt(1);
                return maxID + 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // Return -1 if no IDs are found
    }

    public boolean isEmailAlreadyUsed(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0; // Return true if the count is greater than 0
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Return false if an error occurs or no rows are found
    }

    public boolean discordIdExists(String discordId) {
        String sql = "SELECT COUNT(*) FROM users WHERE discord_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0; // Return true if the count is greater than 0
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Return false if an error occurs or no rows are found
    }
}
