package Application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.File;
import java.io.IOException;

public class ConfigBuilder {

    private final ObjectMapper configMapper;
    private final String configFileName;

    public ConfigBuilder(String configFileName) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule().addDeserializer(Configs.class, new ConfigDeserializer()));

        this.configMapper = mapper;
        this.configFileName = configFileName;
    }

    public Configs build() throws ConfigsSetupException {
        try {
            return configMapper.readValue(new File(configFileName), Configs.class);
        } catch (IOException e) {
            throw new ConfigsSetupException("Failed to read config from file", e);
        }
    }
}
