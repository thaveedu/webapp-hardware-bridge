package tigerworkshop.webapphardwarebridge.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.models.Config;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger("ConfigService");

    private static final String CONFIG_FILENAME = "config.json";

    private static final Gson gson = new Gson();
    private static Config config = new Config();

    public static ConfigService getInstance() {
        return new ConfigService();
    }

    public Config getConfig() {
        try {
            JsonReader reader = new JsonReader(new FileReader(CONFIG_FILENAME));
            config = gson.fromJson(reader, Config.class);
            reader.close();
        } catch (Exception e) {
            logger.error("Cannot read config.json, fallback to default config: {}", e.getMessage(), e);

            saveConfig();
        }

        return config;
    }

    public Boolean saveConfig() {
        try {
            Writer writer = new FileWriter(CONFIG_FILENAME);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(config, writer);
            writer.close();

            return true;
        } catch (Exception e) {
            logger.error("Cannot write config.json: {}", e.getMessage(), e);

            return false;
        }
    }
}
