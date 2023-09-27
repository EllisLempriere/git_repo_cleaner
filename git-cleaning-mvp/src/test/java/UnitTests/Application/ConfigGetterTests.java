package UnitTests.Application;

import Application.ConfigGetter;
import Application.Models.ConfigSecrets;
import Application.Models.Configs;
import Application.Models.ConfigsSetupException;
import Application.Models.RepoConfig;
import TestUtils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigGetterTests {

    @Test
    void Constructor_NullConfigFile_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigGetter(null));
    }


    @Test
    void GetConfigs_FileDoesNotExist_ThrowsException() {
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
    void GetConfigs_ValidConfigFile_CreatesExpectedConfigs() {
        try {
            // arrange
            String filePath = TestUtils.getFullFilePath("valid-test-config.json");
            ConfigGetter getter = new ConfigGetter(filePath);

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
    void GetConfigs_MissingRetries_ThrowsException() {
        try {
            // arrange
            String filePath = TestUtils.getFullFilePath("missing-retries-test-config.json");
            ConfigGetter getter = new ConfigGetter(filePath);

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void GetConfigs_EmptyFile_ThrowsException() {
        try {
            // arrange
            String filePath = TestUtils.getFullFilePath("empty-test-config.json");
            ConfigGetter getter = new ConfigGetter(filePath);

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void GetConfigs_MissingConfigSecrets_ThrowsException() {
        try {
            // arrange
            String filePath = TestUtils.getFullFilePath("missing-config-secrets-test-config.json");
            ConfigGetter getter = new ConfigGetter(filePath);

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void GetConfigs_MissingRepos_ThrowsException() {
        try {
            // arrange
            String filePath = TestUtils.getFullFilePath("missing-repos-test-config.json");
            ConfigGetter getter = new ConfigGetter(filePath);

            // act/assert
            assertThrows(ConfigsSetupException.class, getter::getConfigs);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void GetConfig_EmptyReposList_ReturnsConfigsWithReposListEmpty() {
        try {
            // arrange
            String filePath = TestUtils.getFullFilePath("empty-repos-test-config.json");
            ConfigGetter getter = new ConfigGetter(filePath);

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
