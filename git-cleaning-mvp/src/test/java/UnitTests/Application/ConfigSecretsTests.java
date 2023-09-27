package UnitTests.Application;

import Application.Models.ConfigSecrets;
import Application.Models.ConfigsSetupException;
import TestUtils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSecretsTests {

    @Test
    void Constructor_NullParam_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigSecrets(null));
    }

    @Test
    void Constructor_ValidFile_CreatesExpectedObject() {
        try {
            // arrange
            ConfigSecrets expected = new ConfigSecrets("user", "pass");

            // act
            ConfigSecrets actual = new ConfigSecrets(TestUtils.getFullFilePath("valid-test-secrets.properties"));

            // assert
            assertEquals(expected, actual);

        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void Constructor_InvalidFile_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigSecrets("invalid.file"));
    }

    @Test
    void Constructor_MissingUsername_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigSecrets(
                TestUtils.getFullFilePath("missing-user-test-secrets.properties")));
    }

    @Test
    void Constructor_MissingPassword_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigSecrets(
                TestUtils.getFullFilePath("missing-pass-test-secrets.properties")));
    }
}
