package Business;

import Application.ICustomLogger;
import Business.Models.*;
import Provider.IGitProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GitRepoCleanerLogicTest {

    @Test
    @DisplayName("Null value for repo list, throws exception")
    void constructorTest1() {
        // arrange
        IGitProvider mockGitProvider = mock(IGitProvider.class);
        INotificationLogic mockNotificationLogic = mock(INotificationLogic.class);
        ICustomLogger mockLogger = mock(ICustomLogger.class);

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                null, mockGitProvider, mockNotificationLogic, mockLogger, 0));
    }

    @Test
    @DisplayName("Null value for git provider, throws exception")
    void constructorTest2() {
        // arrange
        List<RepoCleaningInfo> mockRepoInfoList = new ArrayList<>();
        INotificationLogic mockNotificationLogic = mock(INotificationLogic.class);
        ICustomLogger mockLogger = mock(ICustomLogger.class);

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                mockRepoInfoList, null, mockNotificationLogic, mockLogger, 0));
    }

    @Test
    @DisplayName("Null value for notification logic, throws exception")
    void constructorTest3() {
        // arrange
        List<RepoCleaningInfo> mockRepoInfoList = new ArrayList<>();
        IGitProvider mockGitProvider = mock(IGitProvider.class);
        ICustomLogger mockLogger = mock(ICustomLogger.class);

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                mockRepoInfoList, mockGitProvider, null, mockLogger, 0));
    }

    @Test
    @DisplayName("Null value for logger, throws exception")
    void constructorTest4() {
        // arrange
        List<RepoCleaningInfo> mockRepoInfoList = new ArrayList<>();
        IGitProvider mockGitProvider = mock(IGitProvider.class);
        INotificationLogic mockNotificationLogic = mock(INotificationLogic.class);

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                mockRepoInfoList, mockGitProvider, mockNotificationLogic, null, 0));
    }

    @Test
    @DisplayName("Illegal value for execution time, throws exception")
    void constructorTest5() {
        // arrange
        List<RepoCleaningInfo> mockRepoInfoList = new ArrayList<>();
        IGitProvider mockGitProvider = mock(IGitProvider.class);
        INotificationLogic mockNotificationLogic = mock(INotificationLogic.class);
        ICustomLogger mockLogger = mock(ICustomLogger.class);

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                mockRepoInfoList, mockGitProvider, mockNotificationLogic, mockLogger, -1));
    }

    @Test
    @DisplayName("All valid values, creates instance")
    void constructorTest6() {
        // arrange
        List<RepoCleaningInfo> mockRepoInfoList = new ArrayList<>();
        IGitProvider mockGitProvider = mock(IGitProvider.class);
        INotificationLogic mockNotificationLogic = mock(INotificationLogic.class);
        ICustomLogger mockLogger = mock(ICustomLogger.class);

        // act
        GitRepoCleanerLogic result = new GitRepoCleanerLogic(mockRepoInfoList, mockGitProvider, mockNotificationLogic,
                mockLogger, 0);

        // assert
        assertInstanceOf(GitRepoCleanerLogic.class, result);
    }


    @Test
    @DisplayName("Two repos to clean, correct methods get called twice")
    void cleanReposTest1() {
        // arrange
        List<RepoCleaningInfo> repos = new ArrayList<>();
        RepoCleaningInfo info1 = new RepoCleaningInfo("1", "dir1", "remote1", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        RepoCleaningInfo info2 = new RepoCleaningInfo("2", "dir2", "remote2", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repos.add(info1);
        repos.add(info2);

        IGitProvider mockGitProvider = mock(IGitProvider.class);
        INotificationLogic mockNotificationLogic = mock(INotificationLogic.class);
        ICustomLogger mockLogger = mock(ICustomLogger.class);

        GitRepoCleanerLogic logic = new GitRepoCleanerLogic(repos, mockGitProvider, mockNotificationLogic, mockLogger, 0);
        GitRepoCleanerLogic cleanerSpy = spy(logic);

        // act
        cleanerSpy.cleanRepos();

        // assert
        try {
            verify(cleanerSpy, times(1)).selectRepo(info1.repoDir(), info1.remoteUri());
            verify(cleanerSpy, times(1)).cleanRepo(info1);
            verify(cleanerSpy, times(1)).selectRepo(info2.repoDir(), info2.remoteUri());
            verify(cleanerSpy, times(1)).cleanRepo(info2);

        } catch (GitCloningException | GitUpdateException | GitStartupException e) {
            throw new RuntimeException(e);
        }
    }
}
