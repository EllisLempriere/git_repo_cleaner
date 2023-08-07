package Application;

import Application.Models.Configs;
import Application.Models.ConfigsSetupException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class ConfigGetter {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String configFileName;

    public ConfigGetter(String configFileName) throws ConfigsSetupException {
        if (configFileName == null)
            throw new ConfigsSetupException("Config file name cannot be null");

        this.configFileName = configFileName;
    }

    public Configs getConfigs() throws ConfigsSetupException {
        Configs configs;

        try {
            configs = mapper.readValue(new File(configFileName), new TypeReference<>() {});
        } catch (IOException e) {
            throw new ConfigsSetupException("Failed to read config from file", e);
        }

        if (configs.retries() == null)
            throw new ConfigsSetupException("Retries not provided in config file");
        else if (configs.config_secrets() == null)
            throw new ConfigsSetupException("Config secrets not provided in config file");
        else if (configs.repos() == null)
            throw new ConfigsSetupException("Repos not provided in config file");

        return configs;
    }
}
