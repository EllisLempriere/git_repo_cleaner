package Provider;

import Application.Models.ConfigSecrets;
import Business.Models.GitStartupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class GitProviderTest {

    private static GitProvider provider;

    @BeforeEach
    void setupProvider() {
        ConfigSecrets secrets = new ConfigSecrets("user", "pass");
        provider = new GitProvider(secrets, 0);
    }

    @Test
    @DisplayName("Null ConfigSecrets parameter, throws exception")
    void constructorTest1() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitProvider(null, 0));
    }

    @ParameterizedTest
    @DisplayName("Null values in ConfigSecrets parameter, throws exception")
    @CsvSource({", pass", "user,"})
    void constructorTest2(String username, String password) {
        // arrange
        ConfigSecrets invalidSecrets = new ConfigSecrets(username, password);

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitProvider(invalidSecrets, 0));
    }

    @ParameterizedTest
    @DisplayName("Invalid retries value < 0")
    @ValueSource(ints = {-1, -3})
    void constructorTest3(int retries) {
        // arrange
        ConfigSecrets secrets = new ConfigSecrets("user", "pass");

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitProvider(secrets, retries));
    }

    @Test
    @DisplayName("Valid parameters, creates an instance")
    void constructorTest4() {
        // arrange
        ConfigSecrets secrets = new ConfigSecrets("user", "pass");

        // act
        GitProvider result = new GitProvider(secrets, 0);

        // assert
        assertInstanceOf(GitProvider.class, result);
    }


    @Test
    @DisplayName("Null repo directory, throws exception")
    void setupRepoTest1() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.setupRepo(null));
    }

    @ParameterizedTest
    @DisplayName("Repo directory param does not point to a git repo, throws exception")
    @ValueSource(strings = {"not_a_file_path", "hello", "C:\\Documents"})
    void setupRepoTest2(String invalidPath) {
        // arrange

        // act/assert
        assertThrows(GitStartupException.class, () -> provider.setupRepo(invalidPath));
    }

    @Test
    @DisplayName("Use project repo as valid repo to set up")
    void setupRepoTest3() {
        // arrange

        // act/assert
        assertDoesNotThrow(() -> provider.setupRepo("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner"));
    }


    @Test
    @DisplayName("Null repo directory parameter, throws exception")
    void cloneRepoTest1() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.cloneRepo(
                null, "https://gitlab.com/EllisLempriere/clone-test-repo.git"));
    }

    @Test
    @DisplayName("Null remote uri parameter, throws exception")
    void cloneRepoTest2() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.cloneRepo(
                "C:\\Users\\ellis\\Documents\\repos\\clone-test-repo", null));
    }
}
