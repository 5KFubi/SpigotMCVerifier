package me.fivekfubi.Discord;

import me.fivekfubi.DataBase.DbManager;
import me.fivekfubi.DataBase.UserData;
import me.fivekfubi.Main;
import me.fivekfubi.PayPal.PayPalData;
import me.fivekfubi.PayPal.PayPalUtils;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;


import org.javacord.api.interaction.*;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.*;

public class DiscordUtils {

    private List<String> CUSTOMER_ROLE_IDS;

    // OTHER - MESSAGES
    private String UNKNOWN_COMMAND;
    private String HELP_MESSAGE;
    private String SEARCH_LOADING_MESSAGE;
    private String ALREADY_SEARCHING_MESSAGE;

    // SUCCESS - MESSAGES
    private String SEARCH_SUCCESS_MESSAGE;
    private String SEARCH_UPDATED_SUCCESS_MESSAGE;

    // FAILURE - MESSAGES
    private String ALREADY_LINKED_TO_AN_EMAIL;
    private String SEARCH_FAIL_NOT_FOUND_MESSAGE;
    private String SEARCH_FAIL_EMAIL_ALREADY_USED_MESSAGE;
    private String SEARCH_FAIL_NO_OTHER_RESOURCES_FOUND_MESSAGE;
    private String SEARCH_OTHER_ERROR;

    // OTHER VALUES
    private int cacheMonthsToCheck;
    private int cacheMonthsToSkip;

    private final Main main;
    private final PayPalUtils payPalUtils;
    private final DbManager dbManager;

    // CACHE
    private List<PayPalData> cachedPayPalData;

    public DiscordUtils(Main main, PayPalUtils payPalUtils, String token, DbManager dbManager) {
        this.main = main;
        this.payPalUtils = payPalUtils;
        this.dbManager = dbManager;

        JSONObject config = main.getConfig();
        cacheMonthsToCheck = config.getInt("PAYPAL_CACHE_MONTHS_AMOUNT");
        cacheMonthsToSkip = config.getInt("PAYPAL_CACHE_SKIP_MONTH_AMOUNT");

        cachePayPalData(cacheMonthsToCheck, cacheMonthsToSkip);

        DiscordApi api = new DiscordApiBuilder()
                .setToken(token)
                .login()
                .join();

        api.updateActivity(ActivityType.WATCHING, "You.");
        api.updateStatus(UserStatus.IDLE);

        SlashCommand.with("paypal", "Verify purchases from SpigotMC with this command, use your PayPal email to verify.")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.STRING, "email", "PayPal email address from which the purchase was made.", true))
                .setEnabledInDms(false)
                .createGlobal(api)
                .join();

        SlashCommand.with("help", "Help command :)")
                .setEnabledInDms(false)
                .createGlobal(api)
                .join();

        SlashCommand.with("say", "Says something.")
                .setEnabledInDms(false)
                .createGlobal(api)
                .join();

        SlashCommand.with("test", "Check if the bot is running.")
                .setEnabledInDms(false)
                .createGlobal(api)
                .join();

        api.addSlashCommandCreateListener(this::onSlashCommandInteraction);

        loadMessages();
    }

    public void onSlashCommandInteraction(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();

        switch (interaction.getCommandName()) {
            case "paypal":
                handlePaypalCommand(interaction);
                break;
            case "help":
                handleHelpCommand(interaction);
                break;
            case "say":
                handleSayCommand(interaction);
                break;
            case "test":
                handleTestCommand(interaction);
                break;
            default:
                handleUnknownCommand(interaction);
                break;
        }
    }

    private void cachePayPalData(int amountToCheck, int amountToSkip){
        main.print("[DATA] Caching payments for the past 3 years... This may take a while.");
        cachedPayPalData = new ArrayList<>(payPalUtils.getTransactionData(amountToCheck, amountToSkip));
        main.print("[DATA] Cache completed.");
    }

    private void handleSayCommand(SlashCommandInteraction interaction){
        interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent("Something.").update();
        });

    }

    private void handleTestCommand(SlashCommandInteraction interaction){
        interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent("Bot is active :)").update();
        });
    }

    private void handlePaypalCommand(SlashCommandInteraction interaction) {
        String contentEmail = interaction.getArguments().get(0).getStringValue().get();

        interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
            boolean noEndMessageGiven = true;

            // Check cooldown

            User user = interaction.getUser();
            String userId = user.getIdAsString();

            if (waitingUsers.contains(userId)) {
                interactionOriginalResponseUpdater.setContent(ALREADY_SEARCHING_MESSAGE).update();
                return;
            } else {
                interactionOriginalResponseUpdater.setContent(SEARCH_LOADING_MESSAGE).update();
                waitingUsers.add(userId);
                scheduleUserRemoval(userId);
            }

            // Verify user

            boolean newUser = true;

            UserData userData = getUserData(userId);
            if (userData != null){
                newUser = false;
                String userEmail = userData.getEmail();
                if (!contentEmail.equals(userEmail)){
                    interactionOriginalResponseUpdater.setContent(ALREADY_LINKED_TO_AN_EMAIL).update();
                    waitingUsers.add(userId);
                    return;
                }
            }

            if (newUser){
                if (isEmailUsed(contentEmail)){
                    interactionOriginalResponseUpdater.setContent(SEARCH_FAIL_EMAIL_ALREADY_USED_MESSAGE).update();
                    waitingUsers.add(userId);
                    return;
                }
            }

            // Verify purchases

            List<String> purchasedResources = new ArrayList<>();

            List<PayPalData> payPalDataList = payPalUtils.getTransactionData(1, 0);
            for (PayPalData payPalData : payPalDataList) {
                String paypalEmail = payPalData.getEmail();

                if (contentEmail.equalsIgnoreCase(paypalEmail)) {
                    if (!purchasedResources.contains(payPalData.getResourceId())){
                        purchasedResources.add(payPalData.getResourceId());
                    }
                }
            }
            for (PayPalData payPalData : cachedPayPalData) {
                String paypalEmail = payPalData.getEmail();

                if (contentEmail.equalsIgnoreCase(paypalEmail)) {
                    if (!purchasedResources.contains(payPalData.getResourceId())){
                        purchasedResources.add(payPalData.getResourceId());
                    }
                }
            }

            // Handle formatting / received resources

            List<ResourceData> resourceDataList = new ArrayList<>();

            List<String> availableResources = main.getConfig().getJSONArray("RESOURCE_LIST").toList().stream()
                    .map(Object::toString)
                    .toList();

            for (String formattedResource : availableResources) {
                String[] splittedResource = formattedResource.split(":");
                String resourceId = splittedResource[0];
                String roleToGive = splittedResource[1];
                resourceDataList.add(new ResourceData(resourceId, roleToGive));
            }

            // Check user roles

            Server server = interaction.getServer().get();
            List<Role> userRoles = user.getRoles(server);
            List<String> userRolesString = getUserRolesAsString(userRoles);

            List<String> grantedRoles = new ArrayList<>();

            for (String baseCustomerRole : CUSTOMER_ROLE_IDS){
                if (!userRolesString.contains(baseCustomerRole)){
                    grantedRoles.add(baseCustomerRole);
                }
            }

            for (String purchasedResource : purchasedResources) {
                for (ResourceData resourceData : resourceDataList) {
                    if (resourceData.getResourceId().equals(purchasedResource)) {
                        for (String roleId : userRolesString){
                            if (!resourceData.getRoleId().equals(roleId)){
                                grantedRoles.add(resourceData.getRoleId());
                            }
                        }
                    }
                }
            }

            // Add roles

            List<Role> serverRoles = new ArrayList<>(interaction.getServer().get().getRoles());
            Set<String> grantedRolesSet = new HashSet<>();

            for (Role role : serverRoles) {
                String roleId = role.getIdAsString();
                for (String roleToGrantId : grantedRoles) {
                    if (roleToGrantId.equals(roleId)) {
                        boolean hasRole = userRoles.stream().anyMatch(userRole -> userRole.getIdAsString().equals(roleId));
                        if (!hasRole) {
                            server.addRoleToUser(user, role);
                            grantedRolesSet.add(roleToGrantId);
                        }
                    }
                }
            }
            List<String> trueGrantedRoles = new ArrayList<>(grantedRolesSet);

            // Add to database

            if (!purchasedResources.isEmpty()) {
                if (!newUser){
                    String resourceString = String.join(", ", purchasedResources);
                    UserData newUserData = new UserData(
                            contentEmail,
                            resourceString,
                            userId
                    );

                    dbManager.updateUserData(userId, newUserData);

                    interactionOriginalResponseUpdater.setContent(replacePlaceholders(SEARCH_UPDATED_SUCCESS_MESSAGE, trueGrantedRoles)).update();
                }else{
                    String resourceString = String.join(", ", purchasedResources);

                    UserData newUserData = new UserData(
                            contentEmail,
                            resourceString,
                            userId
                    );

                    dbManager.insertUserIntoDatabase(newUserData);
                    interactionOriginalResponseUpdater.setContent(replacePlaceholders(SEARCH_SUCCESS_MESSAGE, trueGrantedRoles)).update();
                }
                noEndMessageGiven = false;
            } else {
                if (!newUser){
                    interactionOriginalResponseUpdater.setContent(SEARCH_FAIL_NO_OTHER_RESOURCES_FOUND_MESSAGE).update();
                }else{
                    interactionOriginalResponseUpdater.setContent(SEARCH_FAIL_NOT_FOUND_MESSAGE).update();
                }
                noEndMessageGiven = false;
            }

            // Reset cooldown
            waitingUsers.remove(userId);
            scheduleUserRemoval(userId);

            // Just in case :)
            if (noEndMessageGiven){
                interactionOriginalResponseUpdater.setContent(SEARCH_OTHER_ERROR).update();
            }
        });
    }

    private void handleHelpCommand(SlashCommandInteraction interaction){
        interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent(HELP_MESSAGE).update();
        });

    }

    private void handleUnknownCommand(SlashCommandInteraction interaction){
        interaction.respondLater(true).thenAccept(interactionOriginalResponseUpdater -> {
            interactionOriginalResponseUpdater.setContent(UNKNOWN_COMMAND).update();
        });
    }

    public void loadMessages(){
        JSONObject config = main.getConfig();

        CUSTOMER_ROLE_IDS = config.getJSONArray("CUSTOMER_ROLE_IDS").toList().stream()
                .map(Object::toString)
                .toList();

        UNKNOWN_COMMAND = String.join("\n", config.getJSONArray("UNKNOWN_COMMAND").toList().stream()
                .map(Object::toString)
                .toList());

        HELP_MESSAGE = String.join("\n", config.getJSONArray("HELP_MESSAGE").toList().stream()
                .map(Object::toString)
                .toList());

        SEARCH_LOADING_MESSAGE = String.join("\n", config.getJSONArray("SEARCH_LOADING_MESSAGE").toList().stream()
                    .map(Object::toString)
                    .toList());
        ALREADY_SEARCHING_MESSAGE = String.join("\n", config.getJSONArray("ALREADY_SEARCHING_MESSAGE").toList().stream()
                    .map(Object::toString)
                    .toList());
        SEARCH_SUCCESS_MESSAGE = String.join("\n", config.getJSONArray("SEARCH_SUCCESS_MESSAGE").toList().stream()
                    .map(Object::toString)
                    .toList());
        SEARCH_UPDATED_SUCCESS_MESSAGE = String.join("\n", config.getJSONArray("SEARCH_UPDATED_SUCCESS_MESSAGE").toList().stream()
                    .map(Object::toString)
                    .toList());
        ALREADY_LINKED_TO_AN_EMAIL = String.join("\n", config.getJSONArray("ALREADY_LINKED_TO_AN_EMAIL").toList().stream()
                    .map(Object::toString)
                    .toList());
        SEARCH_FAIL_NOT_FOUND_MESSAGE = String.join("\n", config.getJSONArray("SEARCH_FAIL_NOT_FOUND_MESSAGE").toList().stream()
                    .map(Object::toString)
                    .toList());
        SEARCH_FAIL_EMAIL_ALREADY_USED_MESSAGE = String.join("\n", config.getJSONArray("SEARCH_FAIL_EMAIL_ALREADY_USED_MESSAGE").toList().stream()
                    .map(Object::toString)
                    .toList());
        SEARCH_FAIL_NO_OTHER_RESOURCES_FOUND_MESSAGE = String.join("\n", config.getJSONArray("SEARCH_FAIL_NO_OTHER_RESOURCES_FOUND_MESSAGE").toList().stream()
                    .map(Object::toString)
                    .toList());
        SEARCH_OTHER_ERROR = String.join("\n", config.getJSONArray("SEARCH_OTHER_ERROR").toList().stream()
                    .map(Object::toString)
                    .toList());
    }

    public boolean isEmailUsed(String email){
        return dbManager.isEmailAlreadyUsed(email);
    }
    public boolean isAccountLinked(String userId){
        return dbManager.discordIdExists(userId);
    }

    public UserData getUserData(String userId){
        return dbManager.getUserDataByDiscordId(userId);
    }

    private List<String> getUserRolesAsString(List<Role> userRoles){
        List<String> userRolesString = new ArrayList<>();
        for (Role role : userRoles){
            userRolesString.add(role.getIdAsString());
        }
        return userRolesString;
    }


    public String replacePlaceholders(String text, List<String> grantedRoles){
        String newText = text;

        if (newText.contains("%given-roles%")){
            List<String> formattedRoles = grantedRoles.stream()
                    .map(roleId -> "<@&" + roleId + ">")
                    .toList();
            String rolesString = "None!";

            if (!formattedRoles.isEmpty()){
                rolesString = String.join(", ", formattedRoles);
            }

            newText = newText.replace("%given-roles%", rolesString);
        }

        return newText;
    }

    private final Set<String> waitingUsers = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<String, ScheduledFuture<?>> removalTasks = new ConcurrentHashMap<>();
    private void scheduleUserRemoval(String userId) {
        ScheduledFuture<?> existingTask = removalTasks.remove(userId);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        ScheduledFuture<?> removalTask = scheduler.schedule(() -> {
            waitingUsers.remove(userId);
            main.print("[SCHEDULER] Timed out user ID: " + userId + " after 10 seconds.");
        }, 260, TimeUnit.SECONDS);

        removalTasks.put(userId, removalTask);
    }

}
