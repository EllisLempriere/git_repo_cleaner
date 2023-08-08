package Business;

import Business.Models.Branch;
import Business.Models.RepoNotificationInfo;
import Provider.IEmailProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class NotificationLogicTest {

    private static NotificationLogic notificationLogic;

    @BeforeEach
    void setupLogic() {
        IEmailProvider mockEmailProvider = mock(IEmailProvider.class);
        notificationLogic = new NotificationLogic(mockEmailProvider, new ArrayList<>());
    }


    @Test
    @DisplayName("Null email provider parameter, throws exception")
    void constructorTest1() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new NotificationLogic(null, new ArrayList<>()));
    }

    @Test
    @DisplayName("Null repo list parameter, throws exception")
    void constructorTest2() {
        // arrange
        IEmailProvider mockEmailProvider = mock(IEmailProvider.class);

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new NotificationLogic(mockEmailProvider, null));
    }

    @Test
    @DisplayName("All valid parameters, creates instance")
    void constructorTest3() {
        // arrange
        IEmailProvider mockEmailProvider = mock(IEmailProvider.class);

        // act
        NotificationLogic result = new NotificationLogic(mockEmailProvider, new ArrayList<>());

        // assert
        assertInstanceOf(NotificationLogic.class, result);
    }


    @Test
    @DisplayName("Null branch parameter, throws exception")
    void sendNotificationPendingArchivalTest1() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> notificationLogic.sendNotificationPendingArchival(null, ""));
    }

    @Test
    @DisplayName("Null repo id parameter, throws exception")
    void sendNotificationPendingArchivalTest2() {
        // arrange
        Branch mockBranch = new Branch("", new ArrayList<>());

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> notificationLogic.sendNotificationPendingArchival(mockBranch, null));
    }

    @Test
    @DisplayName("Repo id does not exist in given repo list, throws exception")
    void sendNotificationPendingArchivalTest3() {
        // arrange
        IEmailProvider mockEmailProvider = mock(IEmailProvider.class);
        List<RepoNotificationInfo> mockInfo = new ArrayList<>();
        NotificationLogic logic = new NotificationLogic(mockEmailProvider, mockInfo);

        Branch mockBranch = new Branch("", new ArrayList<>());

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> logic.sendNotificationPendingArchival(mockBranch, "not_in_repo_info"));
    }
}
