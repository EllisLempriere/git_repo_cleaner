package UnitTests.Business;

import Application.ICustomLogger;
import Business.GitRepoCleanerLogic;
import Business.INotificationLogic;
import Business.Models.*;
import Provider.IGitProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GitRepoCleanerLogicTest {

    private IGitProvider mockGitProvider;
    private INotificationLogic mockNotificationLogic;
    private ICustomLogger mockLogger;
    private List<RepoCleaningInfo> repoCleaningInfoList;

    @BeforeEach
    public void prepareMocks() {
        mockGitProvider = mock(IGitProvider.class);
        mockNotificationLogic = mock(INotificationLogic.class);
        mockLogger = mock(ICustomLogger.class);
    }

    @BeforeEach
    public void prepareRepoInfo() {
        repoCleaningInfoList = new ArrayList<>();
    }

    private GitRepoCleanerLogic createDefaultTestLogic() {
        return new GitRepoCleanerLogic(repoCleaningInfoList, mockGitProvider,
                mockNotificationLogic, mockLogger, 1685602801);
    }

    @Test
    @DisplayName("Null value for repo list, throws exception")
    void constructorTest1() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                null, mockGitProvider, mockNotificationLogic, mockLogger, 0));
    }

    @Test
    @DisplayName("Null value for gitProvider, throws exception")
    void constructorTest2() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                repoCleaningInfoList, null, mockNotificationLogic, mockLogger, 0));
    }

    @Test
    @DisplayName("Null value for notificationLogic, throws exception")
    void constructorTest3() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                repoCleaningInfoList, mockGitProvider, null, mockLogger, 0));
    }

    @Test
    @DisplayName("Null value for logger, throws exception")
    void constructorTest4() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                repoCleaningInfoList, mockGitProvider, mockNotificationLogic, null, 0));
    }

    @Test
    @DisplayName("Illegal value for executionTime, throws exception")
    void constructorTest5() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                repoCleaningInfoList, mockGitProvider, mockNotificationLogic, mockLogger, -1));
    }

    @Test
    @DisplayName("All valid values, creates instance")
    void constructorTest6() {
        // arrange

        // act
        GitRepoCleanerLogic result = new GitRepoCleanerLogic(repoCleaningInfoList, mockGitProvider,
                mockNotificationLogic, mockLogger, 0);

        // assert
        assertInstanceOf(GitRepoCleanerLogic.class, result);
    }


    @Test
    @DisplayName("Two repos to clean, correct methods get called twice")
    void cleanReposTest1() {
        // arrange
        RepoCleaningInfo info1 = new RepoCleaningInfo("1", "dir1", "remote1", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        RepoCleaningInfo info2 = new RepoCleaningInfo("2", "dir2", "remote2", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info1);
        repoCleaningInfoList.add(info2);

        GitRepoCleanerLogic logic = createDefaultTestLogic();
        GitRepoCleanerLogic logicSpy = spy(logic);

        // act
        logicSpy.cleanRepos();

        // assert
        try {
            verify(logicSpy, times(1)).selectRepo(info1.repoDir(), info1.remoteUri(), 1);
            verify(logicSpy, times(1)).cleanRepo(info1, 1);
            verify(logicSpy, times(1)).selectRepo(info2.repoDir(), info2.remoteUri(), 2);
            verify(logicSpy, times(1)).cleanRepo(info2, 2);

        } catch (GitCloningException | GitUpdateException | GitStartupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Local repo does not exist, cloneRepo, setupRepo, and updateRepo called")
    void selectRepoTest1() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "dir1", "remote1", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            // act
            logic.selectRepo(info.repoDir(), info.remoteUri(), 1);

            // assert
            verify(mockGitProvider).cloneRepo(info.repoDir(), info.remoteUri());
            verify(mockGitProvider).setupRepo(info.repoDir());
            verify(mockGitProvider).updateRepo(info.repoDir());

        } catch (GitCloningException | GitUpdateException | GitStartupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Local repo does exist, cloneRepo not called, setupRepo, and updateRepo called")
    void selectRepoTest2() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            // act
            logic.selectRepo(info.repoDir(), info.remoteUri(), 1);

            // assert
            verify(mockGitProvider, never()).cloneRepo(anyString(), anyString());
            verify(mockGitProvider).setupRepo(info.repoDir());
            verify(mockGitProvider).updateRepo(info.repoDir());

        } catch (GitCloningException | GitUpdateException | GitStartupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Local repo does not exist, cloneRepo throws exception, throws exception")
    void selectRepoTest3() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "dir1", "remote1", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitCloningException("error")).when(mockGitProvider).cloneRepo(anyString(), anyString());
        } catch (GitCloningException ignored) {
        }

        // act/assert
        assertThrows(GitCloningException.class, () -> logic.selectRepo(info.repoDir(), info.remoteUri(), 1));
    }

    @Test
    @DisplayName("setupRepo throws exception, throws exception")
    void selectRepoTest4() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitStartupException("error")).when(mockGitProvider).setupRepo(anyString());
        } catch (GitStartupException ignored) {
        }

        // act/assert
        assertThrows(GitStartupException.class, () -> logic.selectRepo(info.repoDir(), info.remoteUri(), 1));
    }

    @Test
    @DisplayName("updateRepo throws exception, throws exception")
    void selectRepoTest5() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitUpdateException("error")).when(mockGitProvider).updateRepo(anyString());
        } catch (GitUpdateException | GitStartupException ignored) {
        }

        // act/assert
        assertThrows(GitUpdateException.class, () -> logic.selectRepo(info.repoDir(), info.remoteUri(), 1));
    }

    @Test
    @DisplayName("getBranches throws an exception, execution halted")
    void cleanRepoTest1() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitBranchFetchException("error")).when(mockGitProvider).getBranches();
        } catch (GitBranchFetchException ignored) {
        }

        // act
        logic.cleanRepo(info, 1);

        // assert
        verify(mockLogger, never()).log(Level.INFO, "Branch list successfully obtained");
    }

    @Test
    @DisplayName("getTags throws an exception, execution halted")
    void cleanRepoTest2() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitTagFetchException("error")).when(mockGitProvider).getTags();
        } catch (GitTagFetchException ignored) {
        }

        // act
        logic.cleanRepo(info, 1);

        // assert
        verify(mockLogger, never()).log(Level.INFO, "Tag list successfully obtained");
    }

    @Test
    @DisplayName("List of branches from getBranches passed to cleanBranches")
    void cleanRepoTest3() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(Collections.singletonList("main")), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();
        GitRepoCleanerLogic cleanerSpy = spy(logic);

        List<Branch> branchList = Collections.singletonList(new Branch("main", Collections.emptyList()));

        try {
            when(mockGitProvider.getBranches()).thenReturn(branchList);
        } catch (GitBranchFetchException ignored) {
        }

        // act
        cleanerSpy.cleanRepo(info, 1);

        // assert
        verify(cleanerSpy, times(1)).cleanBranches(branchList,
                info.repoId(), info.excludedBranches(), info.takeActionCountsDays(), 1);
    }

    @Test
    @DisplayName("List of tags from getTags passed to cleanTags")
    void cleanRepoTest4() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();
        GitRepoCleanerLogic cleanerSpy = spy(logic);

        List<Tag> tagList = Collections.singletonList(new Tag("tag", Collections.emptyList()));

        try {
            when(mockGitProvider.getTags()).thenReturn(tagList);
        } catch (GitTagFetchException ignored) {
        }

        // act
        cleanerSpy.cleanRepo(info, 1);

        // assert
        verify(cleanerSpy, times(1)).cleanTags(tagList, info.repoId(), info.takeActionCountsDays(), 1);
    }

    @Test
    @DisplayName("Correct tag created and passed to createTag")
    void archiveBranchTest1() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag expectedTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        logic.archiveBranch(testBranch, info.repoId(), 1, 1);

        // assert
        try {
            verify(mockGitProvider, times(1)).createTag(expectedTag);
        } catch (GitCreateTagException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("createTag throws exception, halts execution")
    void archiveBranchTest2() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitCreateTagException("error")).when(mockGitProvider).createTag(testTag);

            // act
            logic.archiveBranch(testBranch, info.repoId(), 1, 1);

            // assert
            verify(mockGitProvider, never()).deleteBranch(testBranch);

        } catch (GitCreateTagException | GitBranchDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("deleteBranch throws exception, halts execution")
    void archiveBranchTest3() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitBranchDeletionException("error")).when(mockGitProvider).deleteBranch(testBranch);
        } catch (GitBranchDeletionException ignored) {
        }

        // act
        logic.archiveBranch(testBranch, info.repoId(), 1, 1);

        // assert
        verify(mockLogger, times(1)).logBranchWarn(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("deleteBranch throws exception, tries to delete archive tag, deleteTag throws exception, halts execution")
    void archiveBranchTest4() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitBranchDeletionException("error")).when(mockGitProvider).deleteBranch(testBranch);
            doThrow(new GitTagDeletionException("error")).when(mockGitProvider).deleteTag(testTag);
        } catch (GitBranchDeletionException | GitTagDeletionException ignored) {
        }

        // act
        logic.archiveBranch(testBranch, info.repoId(), 1, 1);

        // assert
        verify(mockLogger, times(2)).logBranchWarn(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("deleteBranch throws exception, successfully deletes extra tag, halts execution")
    void archiveBranchTest5() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitBranchDeletionException("error")).when(mockGitProvider).deleteBranch(testBranch);

            // act
            logic.archiveBranch(testBranch, info.repoId(), 1, 1);

            // assert
            verify(mockGitProvider, times(1)).deleteTag(testTag);

        } catch (GitTagDeletionException | GitBranchDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("No git exceptions, branch archived")
    void archiveBranchTest6() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        logic.archiveBranch(testBranch, info.repoId(), 1, 1);

        // assert
        try {
            verify(mockGitProvider, times(1)).createTag(testTag);
            verify(mockGitProvider, times(1)).deleteBranch(testBranch);
            verify(mockNotificationLogic, times(1)).sendNotificationArchival(testBranch, testTag, info.repoId(), 1, 1);
        } catch (GitCreateTagException | GitBranchDeletionException | SendEmailException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("sendNotificationArchival throws exception, warning logged")
    void archiveBranchTest7() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new SendEmailException("error")).when(mockNotificationLogic).sendNotificationArchival(
                    testBranch, testTag, info.repoId(), 1, 1);
        } catch (SendEmailException ignored) {
        }

        // act
        logic.archiveBranch(testBranch, info.repoId(), 1, 1);

        // assert
        verify(mockLogger, times(1)).logBranchWarn(
                String.format("Failed to notify of archival of branch '%s'",
                testBranch.name()), 1, 1);
    }


    @Test
    @DisplayName("Empty list of branches to clean, returns empty list")
    void cleanBranchesTest1() {
        // arrange
        List<Branch> branches = new ArrayList<>();

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Branch> result = logic.cleanBranches(branches,
                info.repoId(), info.excludedBranches(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("All branches in list to clean are in excluded list, returns empty list")
    void cleanBranchesTest2() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("main", Collections.emptyList()));
        branches.add(new Branch("excluded", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", Arrays.asList("main", "excluded"), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Branch> result = logic.cleanBranches(branches,
                info.repoId(), info.excludedBranches(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("Branches to be cleaned are excluded or are not stale, returns empty list")
    void cleanBranchesTest3() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("main", Collections.emptyList()));
        branches.add(new Branch("a1", Collections.singletonList(new Commit("id", 1684602000, "email"))));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", List.of("main"), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Branch> result = logic.cleanBranches(branches,
                info.repoId(), info.excludedBranches(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("Stale branches are returned and correct call to archiveBranch made")
    void cleanBranchesTest4() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("main", Collections.emptyList()));
        branches.add(new Branch("a1", Collections.singletonList(new Commit("id", 1672596000, "email"))));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", List.of("main"), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();
        GitRepoCleanerLogic cleanerSpy = spy(logic);

        List<Branch> expected = Collections.singletonList(branches.get(1));

        // act
        List<Branch> result = cleanerSpy.cleanBranches(branches,
                info.repoId(), info.excludedBranches(), info.takeActionCountsDays(), 1);

        // assert
        verify(cleanerSpy, times(1)).archiveBranch(branches.get(1), info.repoId(), 1, 2);
        assertIterableEquals(expected, result);
    }

    @Test
    @DisplayName("Branch pending archival, notification sent, empty list returned")
    void cleanBranchesTest5() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("a1", Collections.singletonList(new Commit("id", 1681023601, "email"))));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Branch> result = logic.cleanBranches(branches,
                info.repoId(), info.excludedBranches(), info.takeActionCountsDays(), 1);

        // assert
        try {
            verify(mockNotificationLogic, times(1)).sendNotificationPendingArchival(branches.get(0), info.repoId(), 1, 1);
            assertIterableEquals(Collections.emptyList(), result);
        } catch (SendEmailException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Branch pending archival, sendNotificationPendingArchival throws exception, warning logged, empty list returned")
    void cleanBranchesTest6() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("a1", Collections.singletonList(new Commit("id", 1681023601, "email"))));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new SendEmailException("error")).when(mockNotificationLogic).sendNotificationPendingArchival(
                    branches.get(0), info.repoId(), 1, 1);
        } catch (SendEmailException ignored) {
        }

        // act
        List<Branch> result = logic.cleanBranches(branches,
                info.repoId(), info.excludedBranches(), info.takeActionCountsDays(), 1);

        // assert
        verify(mockLogger, times(1)).logBranchWarn(
                String.format("Failed to notify of pending archival of branch '%s'",
                branches.get(0).name()), 1, 1);
        assertIterableEquals(Collections.emptyList(), result);
    }


    @Test
    @DisplayName("deleteTag throws an exception tag deletion logged as failed")
    void deleteArchiveTagTest1() {
        // arrange
        Tag testTag = new Tag("archiveTag", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new GitTagDeletionException("error")).when(mockGitProvider).deleteTag(testTag);
        } catch (GitTagDeletionException ignored) {
        }

        // act
        logic.deleteArchiveTag(testTag, info.repoId(), 1, 1);

        // assert
        verify(mockLogger, times(1)).logTagWarn(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Tag deleted, sendNotificationTagDeletion called")
    void deleteArchiveTagTest2() {
        // arrange
        Tag testTag = new Tag("archiveTag", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        logic.deleteArchiveTag(testTag, info.repoId(), 1, 1);

        // assert
        try {
            verify(mockNotificationLogic, times(1)).sendNotificationTagDeletion(testTag, info.repoId(), 1, 1);
        } catch (SendEmailException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Tag deleted, sendNotificationTagDeletion throws exception, failure to notify logged")
    void deleteArchiveTagTest3() {
        // arrange
        Tag testTag = new Tag("archiveTag", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new SendEmailException("error")).when(mockNotificationLogic).sendNotificationTagDeletion(testTag, info.repoId(), 1, 1);
        } catch (SendEmailException ignored) {
        }

        // act
        logic.deleteArchiveTag(testTag, info.repoId(), 1, 1);

        // assert
        verify(mockLogger, times(1)).logTagWarn(
                String.format("Failed to notify of deletion of tag '%s'",
                testTag.name()), 1, 1);
    }


    @Test
    @DisplayName("Empty list of tags to clean, returns empty list")
    void cleanTagsTest1() {
        // arrange
        List<Tag> tags = new ArrayList<>();

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Tag> result = logic.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("All tags in list to clean are not archive tags, returns empty list")
    void cleanTagsTest2() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("not_an_archive_tag", Collections.emptyList()));
        tags.add(new Tag("anotherTag", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Tag> result = logic.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("Tags to be cleaned are not archive tags or are not stale, returns empty list")
    void cleanTagsTest3() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("not_an_archive_tag", Collections.emptyList()));
        tags.add(new Tag("zArchiveBranch_20230528_branch", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Tag> result = logic.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    @DisplayName("Deleted tags are returned and correct call to deleteTag made")
    void cleanTagsTest4() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("not_an_archive_tag", Collections.emptyList()));
        tags.add(new Tag("zArchiveBranch_20230101_branch", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", List.of("main"), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();
        GitRepoCleanerLogic cleanerSpy = spy(logic);

        List<Tag> expected = Collections.singletonList(tags.get(1));

        // act
        List<Tag> result = cleanerSpy.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        verify(cleanerSpy).deleteArchiveTag(tags.get(1), info.repoId(), 1, 2);
        assertIterableEquals(expected, result);
    }

    @Test
    @DisplayName("Tag pending archival, notification sent, empty list returned")
    void cleanTagsTest5() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("zArchiveBranch_20230509_branch", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Tag> result = logic.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        try {
            verify(mockNotificationLogic, times(1)).sendNotificationPendingTagDeletion(tags.get(0), info.repoId(), 1, 1);
            assertIterableEquals(Collections.emptyList(), result);
        } catch (SendEmailException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Tag pending deletion, sendNotificationPendingTagDeletion throws exception, warning logged, empty list returned")
    void cleanTagsTest6() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("zArchiveBranch_20230509_branch", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "remote", new ArrayList<>(), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        try {
            doThrow(new SendEmailException("error")).when(mockNotificationLogic).sendNotificationPendingTagDeletion(
                    tags.get(0), info.repoId(), 1, 1);
        } catch (SendEmailException ignored) {
        }

        // act
        List<Tag> result = logic.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        verify(mockLogger, times(1)).logTagWarn(
                String.format("Failed to notify of pending deletion of tag '%s'",
                tags.get(0).name()), 1, 1);
        assertIterableEquals(Collections.emptyList(), result);
    }
}
