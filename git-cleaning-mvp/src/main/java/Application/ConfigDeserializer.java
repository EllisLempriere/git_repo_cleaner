package Application;

import Business.Models.DaysToActions;
import Business.Models.RepoInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigDeserializer extends StdDeserializer<Configs> {

    public ConfigDeserializer() {
        this(null);
    }

    protected ConfigDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Configs deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        try {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);

            int daysToStaleBranch = (int) node.get("days_to_stale_branch").numberValue();
            int daysToStaleTag = (int) node.get("days_to_stale_tag").numberValue();
            int precedingDaysToWarn = (int) node.get("preceding_days_to_warn").numberValue();
            DaysToActions daysToActions = new DaysToActions(daysToStaleBranch, daysToStaleTag, precedingDaysToWarn);

            int retries = (int) node.get("retries").numberValue();

            String secretsDir = node.get("config_secrets").asText();
            ConfigSecrets configSecrets = new ConfigSecrets(secretsDir);

            List<String> recipients = new ArrayList<>();
            ArrayNode jsonRecipients = (ArrayNode) node.get("recipients");
            for (JsonNode recipient : jsonRecipients)
                recipients.add(recipient.asText());

            List<RepoInfo> repos = new ArrayList<>();
            ArrayNode jsonRepos = (ArrayNode) node.get("repos");
            for (JsonNode repo : jsonRepos) {
                String repoDirectory = repo.get("directory").asText();
                String remoteUri = repo.get("remote_uri").asText();

                List<String> excludedBranches = new ArrayList<>();
                ArrayNode jsonBranches = (ArrayNode) repo.get("excluded_branches");
                for (JsonNode branch : jsonBranches)
                    excludedBranches.add(branch.asText());

                repos.add(new RepoInfo(repoDirectory, remoteUri, excludedBranches));
            }

            return new Configs(daysToActions, retries, configSecrets, recipients, repos);

        } catch (ConfigsSetupException e) {
            // TODO - Handle
            return null;
        }
    }
}
