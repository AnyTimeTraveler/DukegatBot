package org.simonscode.dukegatbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Config {

    // Configfile name
    private static final String CONFIGFILE = "botconfig.json";
    private static Config instance;
    public String apiEndpoint = "API_ENDPOINT_HERE";
    public String botToken = "BOT_TOKEN_HERE";
    public String channelId = "@DukegatTS";
    public int refreshIntervalInMS = 30_000;
    public List<Integer> adminIds = new ArrayList<>();
    public TSStatus tsStatus = TSStatus.ERROR;

    public String openText = "TS ist offen";
    public String closeText = "TS ist zu";
    public String errorText = "Fehler!";

    static Config getInstance() {
        if (instance == null) {
            load(new File(CONFIGFILE));
        }
        return instance;
    }

    static void load(File file) {
        try {
            Gson gson = new GsonBuilder().create();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            instance = gson.fromJson(reader, Config.class);
        } catch (FileNotFoundException e) {
            System.out.println("Config file not found!\nI created one for you as an example.");
            if (instance == null) {
                instance = new Config();
                instance.save();
                System.exit(1);
            }
        } catch (JsonIOException | JsonSyntaxException e) {
            System.err.println("Config file improperly formatted!");
            e.printStackTrace();
        }
    }

    void save() {
        save(new File(CONFIGFILE));
    }

    void save(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonConfig = gson.toJson(this);
        FileWriter writer;
        try {
            writer = new FileWriter(file);
            writer.write(jsonConfig);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}