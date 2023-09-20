package UnitTests.Application;

import Application.Models.ConfigSecrets;
import Application.Models.ConfigsSetupException;
import TestUtils.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigSecretsTest {

    @Test
    @DisplayName("Pass in null, throws exception")
    void constructorTest1() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigSecrets(null));
    }

    @Test
    @DisplayName("Valid file, expected object created")
    void constructorTest2() {
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
    @DisplayName("Pass invalid file, throws an exception")
    void constructorTest3() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigSecrets("invalid.file"));
    }

    @Test
    @DisplayName("Pass file missing username property, throws an exception")
    void constructorTest4() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigSecrets(
                TestUtils.getFullFilePath("missing-user-test-secrets.properties")));
    }

    @Test
    @DisplayName("Pass file missing password property, throws an exception")
    void constructorTest5() {
        // arrange

        // act/assert
        assertThrows(ConfigsSetupException.class, () -> new ConfigSecrets(
                TestUtils.getFullFilePath("missing-pass-test-secrets.properties")));
    }
}
