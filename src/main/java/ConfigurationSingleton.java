import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class ConfigurationSingleton {
    private static ConfigurationSingleton instance;
    private Configuration config;

    private ConfigurationSingleton() {
        Configurations configurations = new Configurations();
        try {
            config = configurations.properties("config.properties");
        } catch (ConfigurationException e) {
            System.err.println("File config.properties non trovato!");
            System.exit(-1);
        }
    }

    public static ConfigurationSingleton getInstance() {
        if (instance == null) instance = new ConfigurationSingleton();
        return instance;
    }

    public String getProperty(String key) {
        return config.getString(key);
    }
}
