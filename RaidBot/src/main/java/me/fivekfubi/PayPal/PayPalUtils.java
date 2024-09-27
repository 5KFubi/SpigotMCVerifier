package me.fivekfubi.PayPal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;

import me.fivekfubi.Main;
import org.json.JSONArray;
import org.json.JSONObject;

public class PayPalUtils {

    private final String client_id;
    private final String client_secret;
    private final String PAYPAL_ENDPOINT = "https://api.paypal.com";

    private String accessToken = null;

    private final Main main;

    public PayPalUtils(Main main, String client_id, String client_secret){
        this.main = main;
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    public String getAccessToken() throws Exception {
        if (accessToken == null) {
            URL url = new URL(PAYPAL_ENDPOINT + "/v1/oauth2/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String auth = client_id + ":" + client_secret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            String postData = "grant_type=client_credentials";
            conn.getOutputStream().write(postData.getBytes(StandardCharsets.UTF_8));

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            accessToken = jsonResponse.getString("access_token");
        }
        return accessToken;
    }

    public JSONArray getTransactions(String startDate, String endDate) {
        try {
            String accessToken = getAccessToken();
            URL url = new URL(PAYPAL_ENDPOINT + "/v1/reporting/transactions?start_date=" + startDate + "&end_date=" + endDate + "&fields=all");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + url);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getJSONArray("transaction_details");
        }catch (Exception e){
            return null;
        }
    }


    public List<String> getDatesToCheck(int monthsToGoBack, int monthsToSkip) {
        List<String> datesToCheck = new ArrayList<>();

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE; // 'yyyy-MM-dd'

        for (int i = 0; i < monthsToGoBack; i++) {
            if (i < monthsToSkip) {
                continue;
            }

            LocalDate startOfMonth = today.minusMonths(i).with(TemporalAdjusters.firstDayOfMonth());
            LocalDate endOfMonth = today.minusMonths(i).with(TemporalAdjusters.lastDayOfMonth());

            String startDate = startOfMonth.format(formatter) + "T00:00:00Z";
            String endDate = endOfMonth.format(formatter) + "T23:59:59Z";

            String formattedDate = startDate + "|" + endDate;
            datesToCheck.add(formattedDate);
        }

        return datesToCheck;
    }


    public List<PayPalData> getTransactionData(int monthsToCheck, int monthsToSkip){
        List<String> datesToCheck = getDatesToCheck(monthsToCheck, monthsToSkip);

        List<PayPalData> payPalData = new ArrayList<>();

        for (String formattedDate : datesToCheck){
            String[] splittedDate = formattedDate.split("\\|");
            String startDate = splittedDate[0];
            String endDate = splittedDate[1];

            JSONArray transactions = getTransactions(startDate, endDate);

            for (int i = 0; i < transactions.length(); i++) {
                JSONObject transaction = transactions.getJSONObject(i);
                String customField = transaction.optJSONObject("transaction_info").optString("custom_field", null);
                String emailAddress = transaction.optJSONObject("payer_info").optString("email_address", null);
                if (customField != null && emailAddress != null){
                    payPalData.add(new PayPalData(customField, emailAddress));
                }
            }
            //
        }
        //

        return payPalData;
    }


}
