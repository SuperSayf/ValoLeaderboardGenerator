package com.valoleaderboardgenerator;

import java.util.Scanner;

import java.net.HttpURLConnection;
import java.net.URL;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

public class App {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public static void main(String[] args) throws Exception {

        // Take input of a list of names from the user and store it in an array called
        // namesPlaying
        Scanner input = new Scanner(System.in);
        System.out.println(
                "Enter the IGN#tagline of the players you want to generate a leaderboard for, separated by a comma: ");
        String names = input.nextLine();
        String[] namesPlaying = names.split(",");

        input.close();

        // Progress bar
        ProgressBar pb = new ProgressBar("Processing", (namesPlaying.length * 10), ProgressBarStyle.ASCII);

        pb.start();

        // Split the namesPlaying strings into name and tagline and send them into the
        // getBattlePower function
        int[] battlePower = new int[namesPlaying.length];
        for (int i = 0; i < namesPlaying.length; i++) {
            String[] nameAndTagline = namesPlaying[i].split("#");
            pb.setExtraMessage(nameAndTagline[0] + "...");
            battlePower[i] = getBattlePower(nameAndTagline[0], nameAndTagline[1]);
            pb.stepBy(10);
        }

        pb.stop();

        // Leave a line
        System.out.println();

        // Sort the names and battlePower arrays in descending order
        sort(namesPlaying, battlePower);

        // Print out the names and their battle power, with battle power formated to the
        // end of the table
        // Minus the length of the longest name to make the table look nice

        // Find the longest name
        int longestName = 0;
        for (int i = 0; i < namesPlaying.length; i++) {
            if (namesPlaying[i].length() > longestName) {
                longestName = namesPlaying[i].length();
            }
        }

        // Print the title
        System.out.print("Name");
        for (int i = 0; i < longestName - 4; i++) {
            System.out.print(" ");
        }
        System.out.print("Battle Power");
        System.out.println();
        System.out.print("----");
        for (int i = 0; i < longestName - 4; i++) {
            System.out.print(" ");
        }
        System.out.print("------------");
        System.out.println();

        // Print out the names and their battle power
        for (int i = 0; i < namesPlaying.length; i++) {
            // Get length of the name
            int nameLength = namesPlaying[i].length();

            // Print out the name and battle power
            System.out.print(namesPlaying[i]);
            for (int j = 0; j < longestName - nameLength; j++) {
                System.out.print(" ");
            }
            System.out.print("\t" + battlePower[i]);
            System.out.println();
        }
    }

    private static void sort(String[] namesPlaying, int[] battlePower) {
        // Loop through the namesPlaying array
        for (int i = 0; i < namesPlaying.length; i++) {
            // Loop through the battlePower array
            for (int j = 0; j < battlePower.length; j++) {
                // If the battlePower at the current index is greater than the battlePower at
                // the
                // next index, swap the two
                if (battlePower[i] > battlePower[j]) {
                    int temp = battlePower[i];
                    battlePower[i] = battlePower[j];
                    battlePower[j] = temp;

                    String tempName = namesPlaying[i];
                    namesPlaying[i] = namesPlaying[j];
                    namesPlaying[j] = tempName;
                }
            }
        }
    }

    public static int getBattlePower(String name, String tagline) {
        // Use API to get the valorant stats of the player
        StringBuffer response = new StringBuffer();
        int responseCode = 0;

        try {
            // Public API
            // https://api.henrikdev.xyz/valorant/v3/matches/eu/{name}}/{tagline}

            // Create a URL
            URL url = new URL("https://api.henrikdev.xyz/valorant/v3/matches/eu/" + name + "/" + tagline);

            // Create a connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            while (responseCode != 200) {

                // Set the request method
                connection.setRequestMethod("GET");

                // Set the request header
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                // Get the response code
                responseCode = connection.getResponseCode();

                // If the response code is not 200, try again
                if (responseCode != 200) {
                    System.out.println(
                            ANSI_RED + "Response code for " + name + "#" + tagline + ": " + responseCode + ANSI_RESET);
                    System.out.println(ANSI_RED + "Trying again..." + ANSI_RESET);
                    Thread.sleep(1000);
                }
            }

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            } else {
                System.out.println(
                        ANSI_RED + "GET request error for " + name + "#" + tagline + ": " + responseCode + ANSI_RESET);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int battlePower = 0;

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try {
                JSONParser parser = new JSONParser();
                Object object = parser.parse(response.toString());
                JSONObject mainjsonObject = (JSONObject) object;

                JSONArray jsonArrayData = (JSONArray) mainjsonObject.get("data");

                for (int i = 0; i < jsonArrayData.size(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArrayData.get(i);

                    JSONObject jsonObjectMetadata = (JSONObject) jsonObject.get("metadata");

                    long roundsPlayed = (long) jsonObjectMetadata.get("rounds_played");

                    JSONObject jsonObjectPlayers = (JSONObject) jsonObject.get("players");

                    JSONArray jsonArrayAllPlayers = (JSONArray) jsonObjectPlayers.get("all_players");

                    for (int j = 0; j < jsonArrayAllPlayers.size(); j++) {
                        JSONObject jsonObjectAllPlayers = (JSONObject) jsonArrayAllPlayers.get(j);

                        String playerName = (String) jsonObjectAllPlayers.get("name");
                        String playerTagline = (String) jsonObjectAllPlayers.get("tag");

                        if (playerName.equals(name) && playerTagline.equals(tagline)) {
                            JSONObject jsonObjectStats = (JSONObject) jsonObjectAllPlayers.get("stats");

                            long kills = (long) jsonObjectStats.get("kills");
                            long deaths = (long) jsonObjectStats.get("deaths");
                            long assists = (long) jsonObjectStats.get("assists");
                            // long bodyshots = (long) jsonObjectStats.get("bodyshots");
                            // long headshots = (long) jsonObjectStats.get("headshots");
                            // long legshots = (long) jsonObjectStats.get("legshots");
                            // long score = (long) jsonObjectStats.get("score");

                            long maxNumKills = 5 * roundsPlayed;
                            long maxAssists = 5 * roundsPlayed;

                            double killRatio = ((double) kills / (double) maxNumKills) * 60;
                            double depthRatio = (1 - ((double) deaths / (double) roundsPlayed)) * 30;
                            double assistRatio = ((double) assists / (double) maxAssists) * 10;

                            battlePower += (int) (killRatio + depthRatio + assistRatio);

                        }

                    }
                }
            } catch (

            Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(ANSI_RED + "JSON Parse request error for " + name + "#" + tagline + ANSI_RESET);
        }

        return battlePower;
    }
}
