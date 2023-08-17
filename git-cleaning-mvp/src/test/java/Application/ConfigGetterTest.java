package Application;

import Application.Models.ConfigSecrets;
import Application.Models.Configs;
import Application.Models.ConfigsSetupException;
import Application.Models.RepoConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigGetterTest {

    @Test
    @DisplayName("Test for null config file name on construction")
    void constructorTest1() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigGetter(null));
    }

    @Test
    @DisplayName("Test with a file that does not exist")
    void getConfigTest1() {
        try {
            // arrange
            ConfigGetter getter = new ConfigGetter("invalid.file");

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Valid basic config, ensure expected object is created")
    void getConfigTest2() {
        try {
            // arrange
            ConfigGetter getter = new ConfigGetter("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner" +
                    "\\git-cleaning-mvp\\src\\test\\resources\\Application\\valid-test-config.json");

            ConfigSecrets expectedSecrets = new ConfigSecrets("user", "pass");
            List<RepoConfig> expectedRepoConfigs = buildTestRepoConfigs();
            Configs expected = new Configs(3, expectedSecrets, expectedRepoConfigs);

            // act
            Configs actual = getter.getConfigs();

            // assert
            assertEquals(expected, actual);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Config file missing retries value, throws exception")
    void getConfigTest3() {
        try {
            // arrange
            ConfigGetter getter = new ConfigGetter("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner" +
                    "\\git-cleaning-mvp\\src\\test\\resources\\Application\\missing-retries-test-config.json");

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Empty config file, throws exception")
    void getConfigTest4() {
        try {
            // arrange
            ConfigGetter getter = new ConfigGetter("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner" +
                    "\\git-cleaning-mvp\\src\\test\\resources\\Application\\empty-test-config.json");

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Config file missing config secrets value, throws exception")
    void getConfigTest5() {
        try {
            // arrange
            ConfigGetter getter = new ConfigGetter("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner" +
                    "\\git-cleaning-mvp\\src\\test\\resources\\Application\\missing-config-secrets-test-config.json");

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Config file missing repos value, throws exception")
    void getConfigTest6() {
        try {
            // arrange
            ConfigGetter getter = new ConfigGetter("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner" +
                    "\\git-cleaning-mvp\\src\\test\\resources\\Application\\missing-repos-test-config.json");

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Repos list in config file is empty, returns configs with empty repos list")
    void getConfigTest7() {
        try {
            // arrange
            ConfigGetter getter = new ConfigGetter("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner" +
                    "\\git-cleaning-mvp\\src\\test\\resources\\Application\\empty-repos-test-config.json");

            // act
            Configs configs = getter.getConfigs();

            // assert
            assertTrue(configs.repos().isEmpty());

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }


    private List<RepoConfig> buildTestRepoConfigs() {
        List<RepoConfig> expectedRepoConfigs = new ArrayList<>();

        String directory1 = "C:\\Users\\ellis\\Documents\\repos\\gitlab_test-case-1";
        String remoteUri1 = "https://gitlab.com/EllisLempriere/test-case-1.git";
        List<String> excludedBranches1 = Collections.singletonList("main");
        int staleBranchInactivityDays1 = 60;
        int staleTagDays1 = 30;
        int notificationBeforeActionDays1 = 7;
        List<String> recipients1 = Collections.emptyList();
        expectedRepoConfigs.add(new RepoConfig(directory1, remoteUri1, excludedBranches1, staleBranchInactivityDays1,
                staleTagDays1, notificationBeforeActionDays1, recipients1));

        String directory2 = "C:\\Users\\ellis\\Documents\\repos\\github_test-case-1";
        String remoteUri2 = "https://github.com/EllisLempriere/test-case-1.git";
        List<String> excludedBranches2 = Arrays.asList("main", "excluded");
        int staleBranchInactivityDays2 = 60;
        int staleTagDays2 = 30;
        int notificationBeforeActionDays2 = 7;
        List<String> recipients2 = Collections.singletonList("peter@pslfamily.org");
        expectedRepoConfigs.add(new RepoConfig(directory2, remoteUri2, excludedBranches2, staleBranchInactivityDays2,
                staleTagDays2, notificationBeforeActionDays2, recipients2));

        return expectedRepoConfigs;
    }
}
