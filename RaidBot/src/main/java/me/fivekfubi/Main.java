package me.fivekfubi;

import me.fivekfubi.DataBase.DbManager;
import me.fivekfubi.Discord.DiscordUtils;
import me.fivekfubi.PayPal.PayPalUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {


    // DISCORD
    private String discord_token;
    private List<String> allowed_servers = new ArrayList<>();

    // PAYPAL
    private String client_id;
    private String client_secret;

    // VERIFIER
    private List<String> admin_list = new ArrayList<>();

    // APPEARANCE
    private boolean appear_offline;

    // FILES
    private JSONObject config;

    // INIT
    private PayPalUtils payPalUtils;
    private DiscordUtils discordUtils;
    private DbManager dbManager;

    // CODE
    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        print("Booting up...");

        print("Loading configs...");
        loadConfigs();
        print("Loading config values...");
        loadConfigValues();

        print("Loading Database...");
        try {
            dbManager = new DbManager("users.db");
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        print("Loading PayPal...");
        payPalUtils = new PayPalUtils(this, client_id, client_secret);

        print("Loading Discord...");
        discordUtils = new DiscordUtils(this, payPalUtils, discord_token, dbManager);

        print(" ------------ [ BOT IS READY ] ------------ ");

    }

    public void print(String text){
        System.out.println(text);
    }
    public void printList(List<String> list) {
        for (String string : list) {
            System.out.println(string);
        }
    }
    public void printWarning(String text){
        System.out.println("[WARNING] " + text);
    }
    public void printInfo(String text){
        System.out.println("[INFO] " + text);
    }

    private void loadConfigs() {
        try {
            File configFile = new File("config.json");
            if (!configFile.exists()) {
                try (InputStream resourceStream = Main.class.getResourceAsStream("/config.json")) {
                    if (resourceStream != null) {
                        Files.copy(resourceStream, configFile.toPath());
                        print("Default config.json copied from resources.");
                    } else {
                        print("Resource config.json not found.");
                    }
                }
            }

            FileInputStream fileInputStream = new FileInputStream(configFile);
            String content = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
            config = new JSONObject(content);
            fileInputStream.close();

            print("Config loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfigValues() {
        try {
            // Discord
            discord_token = config.getString("DISCORD_TOKEN");
            allowed_servers = config.getJSONArray("GUILD_IDS").toList().stream()
                    .map(Object::toString)
                    .toList();

            // PayPal
            client_id = config.getString("PAYPAL_CLIENT_ID");
            client_secret = config.getString("PAYPAL_CLIENT_SECRET");

            // Verifier
            admin_list = config.getJSONArray("ADMIN_ID_LIST").toList().stream()
                    .map(Object::toString)
                    .toList();

            // Appearance
            appear_offline = config.getBoolean("APPEAR_OFFLINE");

            print("Config values loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PayPalUtils getPayPalUtils(){
        return payPalUtils;
    }
    public JSONObject getConfig(){
        return config;
    }
    public DbManager getDbManager(){
        return dbManager;
    }

}
