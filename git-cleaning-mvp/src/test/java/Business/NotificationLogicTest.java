package Business;

import Business.Models.*;
import Provider.IEmailProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("Empty committer list and empty recipient list, no notifications sent")
    void sendNotificationPendingArchivalTest4() {
        // arrange
        IEmailProvider mockEmailProvider = mock(IEmailProvider.class);
        List<RepoNotificationInfo> repos = new ArrayList<>();
        repos.add(new RepoNotificationInfo("1", new TakeActionCountsDays(0, 0, 0), new ArrayList<>()));

        NotificationLogic logic = new NotificationLogic(mockEmailProvider, repos);

        Branch noCommitsBranch = new Branch("branch", Collections.emptyList());

        try {
            // act
            logic.sendNotificationPendingArchival(noCommitsBranch, repos.get(0).repoId());

            // assert
            verify(mockEmailProvider, never()).sendEmail(anyString(), anyString(), anyString());

        } catch (SendEmailException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Multiple RepoNotificationInfo given on construction, correct recipient list used")
    void sendNotificationPendingArchivalTest5() {
        // arrange
        IEmailProvider mockEmailProvider = mock(IEmailProvider.class);
        List<String> recipients1 = Arrays.asList("john@email.com", "steve@email.com");
        List<String> recipients2 = Arrays.asList("jane@email.com", "susan@email.com");

        List<RepoNotificationInfo> repos = new ArrayList<>();
        repos.add(new RepoNotificationInfo("1", new TakeActionCountsDays(60, 30, 7), recipients1));
        repos.add(new RepoNotificationInfo("2", new TakeActionCountsDays(90, 60, 14), recipients2));

        NotificationLogic logic = new NotificationLogic(mockEmailProvider, repos);

        Branch noCommitsBranch = new Branch("branch", Collections.emptyList());

        try {
            // act
            logic.sendNotificationPendingArchival(noCommitsBranch, "1");

            // assert
            verify(mockEmailProvider, times(1)).sendEmail("john@email.com", "Pending archival of branch 'branch'",
                    "Branch 'branch' will be archived in 7 days. Commit to it again to prevent archival.");
            verify(mockEmailProvider, times(1)).sendEmail("steve@email.com", "Pending archival of branch 'branch'",
                    "Branch 'branch' will be archived in 7 days. Commit to it again to prevent archival.");

            verify(mockEmailProvider, never()).sendEmail("jane@email.com", "Pending archival of branch 'branch'",
                    "Branch 'branch' will be archived in 7 days. Commit to it again to prevent archival.");
            verify(mockEmailProvider, never()).sendEmail("susan@email.com", "Pending archival of branch 'branch'",
                    "Branch 'branch' will be archived in 7 days. Commit to it again to prevent archival.");

        } catch (SendEmailException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Given recipient list and many commits w/different authors, notifications sent to correct emails")
    void sendNotificationPendingArchivalTest6() {
        // arrange
        IEmailProvider mockEmailProvider = mock(IEmailProvider.class);
        List<RepoNotificationInfo> repos = new ArrayList<>();
        List<String> recipients = Arrays.asList("john@email.com", "steve@email.com");
        repos.add(new RepoNotificationInfo("1", new TakeActionCountsDays(60, 30, 7), recipients));

        NotificationLogic logic = new NotificationLogic(mockEmailProvider, repos);

        List<Commit> branchCommits = new ArrayList<>();
        branchCommits.add(new Commit("1", 0, "sam@email.com"));
        branchCommits.add(new Commit("2", 0, "john@email.com"));
        branchCommits.add(new Commit("3", 0, "sam@email.com"));
        branchCommits.add(new Commit("4", 0, "sam@email.com"));
        branchCommits.add(new Commit("5", 0, "tim@email.com"));
        branchCommits.add(new Commit("6", 0, "evan@email.com"));

        Branch branch = new Branch("branch", branchCommits);

        try {
            // act
            logic.sendNotificationPendingArchival(branch, "1");

            // assert
            verify(mockEmailProvider, times(1)).sendEmail("john@email.com", "Pending archival of branch 'branch'",
                    "Branch 'branch' will be archived in 7 days. Commit to it again to prevent archival.");
            verify(mockEmailProvider, times(1)).sendEmail("sam@email.com", "Pending archival of branch 'branch'",
                    "Branch 'branch' will be archived in 7 days. Commit to it again to prevent archival.");
            verify(mockEmailProvider, times(1)).sendEmail("tim@email.com", "Pending archival of branch 'branch'",
                    "Branch 'branch' will be archived in 7 days. Commit to it again to prevent archival.");

            verify(mockEmailProvider, never()).sendEmail("evan@email.com", "Pending archival of branch 'branch'",
                    "Branch 'branch' will be archived in 7 days. Commit to it again to prevent archival.");

        } catch (SendEmailException e) {
            throw new RuntimeException(e);
        }
    }
}
