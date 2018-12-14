package io.skymind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SkilClient {
    private final String username;
    private final String password;

    private String token;

    public SkilClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

    String extractHostAndPort(String endpoint) {
        // http://foo:9008/bar/baz
        //^---------------^ Return this part.
        int thirdSlash = endpoint.indexOf("/", "http://".length());
        return endpoint.substring(0, thirdSlash);
    }

    String extractToken(String tokenJson) {
        // { "token": "......"}
        int colon = tokenJson.indexOf(":");
        int firstQuote = tokenJson.indexOf("\"", colon);
        int lastQuote = tokenJson.indexOf("\"", firstQuote + 1);

        return tokenJson.substring(firstQuote + 1, lastQuote);
    }

    List<Integer> extractResults(String jsonRes) {
        int resultsIndex = jsonRes.indexOf("\"results\":", 0);
        int firstBracket = jsonRes.indexOf("[", resultsIndex);
        int secondBracket = jsonRes.indexOf("]", firstBracket);

        String[] results = jsonRes.substring(firstBracket + 1, secondBracket).split(",");
        int[] res = new int[results.length];
        return Arrays.stream(results).map(String::trim).map(Integer::valueOf).collect(Collectors.toList());
    }

    List<Float> extractProbs(String jsonRes) {
        int probsIndex = jsonRes.indexOf("\"probabilities\":", 0);
        int probsFirstBracket = jsonRes.indexOf("[", probsIndex);
        int probsSecondBracket = jsonRes.indexOf("]", probsFirstBracket);

        String[] probs = jsonRes.substring(probsFirstBracket + 1, probsSecondBracket).split(",");

        return Arrays.stream(probs).map(String::trim).map(Float::valueOf).collect(Collectors.toList());
    }

    public void login(String endpoint) throws IOException {
        String hostAndPort =extractHostAndPort(endpoint);
        String loginUrl = hostAndPort + "/login";
        String json = "{ \"userId\": \"" + username + "\", \"password\": \"" + password + "\"}";
        String tokenRes = sendJson(loginUrl, json);
        if (tokenRes == null) {
            throw new IllegalArgumentException("Check login credentials");
        }

        token = extractToken(tokenRes);
    }

    public ClassifyResult classify(String endpoint, float[] input) throws IOException {
        String url = endpoint + "/classify";
        String reqUUID = UUID.randomUUID().toString();


        String jsonReq =
                "{" +
                    "\"id\": \"" + reqUUID + "\", " +
                    "\"needsPreProcessing\": false, " +
                    "\"prediction\": { " +
                        "\"data\": " + Arrays.toString(input) + ", " +
                        "\"shape\": [1, 784], " +
                        "\"ordering\": \"c\" " +
                    "}" +
                "}";

        if (token == null) {
            login(endpoint);
        }

        String jsonRes = sendJson(url, jsonReq);

        List<Integer> results = extractResults(jsonRes);
        List<Float> probs = extractProbs(jsonRes);

        return new ClassifyResult(results, probs);
    }

    private String sendJson(String endpoint, String json) throws MalformedURLException, IOException {
        if (!endpoint.startsWith("http")) {
            return null;
        }

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);

        conn.setRequestMethod("POST");

        conn.setRequestProperty("Content-Type", "application/json;");
        conn.setRequestProperty("Accept", "application/json");
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        System.out.println("Sending: " + json);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder sb = new StringBuilder();
        int responseCode = conn.getResponseCode();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.out.println("Response Code: " + responseCode);
        }

        System.out.println("Response: " + sb.toString());

        return sb.toString();
    }


}
