package UnitTests.Business;

import Application.ICustomLogger;
import Business.GitRepoCleanerLogic;
import Business.INotificationLogic;
import Business.Models.*;
import Provider.IGitProvider;
import TestUtils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GitRepoCleanerLogicTests {

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
    void Constructor_NullRepoList_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                null, mockGitProvider, mockNotificationLogic, mockLogger, 0));
    }

    @Test
    void Constructor_NullGitProvider_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                repoCleaningInfoList, null, mockNotificationLogic, mockLogger, 0));
    }

    @Test
    void Constructor_NullNotificationLogic_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                repoCleaningInfoList, mockGitProvider, null, mockLogger, 0));
    }

    @Test
    void Constructor_NullLogger_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                repoCleaningInfoList, mockGitProvider, mockNotificationLogic, null, 0));
    }

    @Test
    void Constructor_InvalidExecutionTime_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitRepoCleanerLogic(
                repoCleaningInfoList, mockGitProvider, mockNotificationLogic, mockLogger, -1));
    }

    @Test
    void Constructor_ValidParams_CreatesInstance() {
        // arrange

        // act
        GitRepoCleanerLogic result = new GitRepoCleanerLogic(repoCleaningInfoList, mockGitProvider,
                mockNotificationLogic, mockLogger, 0);

        // assert
        assertInstanceOf(GitRepoCleanerLogic.class, result);
    }


    @Test
    void CleanRepos_GivenTwoRepos_CallsMethodsToCleanBoth() {
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
    void SelectRepo_LocalRepoNotExist_ProviderCalledToSetupLocal() {
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
    void SelectRepo_LocalRepoExist_ProviderCalledToUpdateLocal() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void SelectRepo_ProviderThrowsExceptionOnCloneRepo_ThrowsException() {
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
    void SelectRepo_ProviderThrowsExceptionOnSetupRepo_ThrowsException() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void SelectRepo_ProviderThrowsExceptionOnUpdateRepo_ThrowsException() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanRepo_ProviderThrowsExceptionOnGetBranches_SkipsCleaningRepo() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanRepo_ProviderThrowsExceptionOnGetTags_SkipsCleaningRepo() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanRepo_NoIssues_CorrectListOfBranchesPassedToCleanBranches() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanRepo_NoIssues_CorrectListOfTagsPassedToCleanTags() {
        // arrange
        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void ArchiveBranch_ValidParameters_CorrectTagCreatedAndPassedToProvider() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag expectedTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void ArchiveBranch_CreateTagThrowsException_StopsCleaningBranch() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void ArchiveBranch_DeleteBranchThrowsException_StopsCleaningBranch() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void ArchiveBranch_FailedDeletionOfExtraArchiveTag_StopsCleaningBranch() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void ArchiveBranch_DeleteBranchThrowsException_ProviderCalledToDeleteExtraArchiveTag() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void ArchiveBranch_NoIssues_BranchSuccessfullyArchived() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void ArchiveBranch_NotificationLogicThrowsException_WarningLogged() {
        // arrange
        Branch testBranch = new Branch("branch", Collections.emptyList());
        Tag testTag = new Tag("zArchiveBranch_20230601_branch", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanBranches_EmptyBranchesParameter_ReturnsEmptyList() {
        // arrange
        List<Branch> branches = new ArrayList<>();

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanBranches_AllBranchesExcluded_ReturnsEmptyList() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("main", Collections.emptyList()));
        branches.add(new Branch("excluded", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanBranches_BranchesAreNotStale_ReturnsEmptyList() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("main", Collections.emptyList()));
        branches.add(new Branch("a1", Collections.singletonList(new Commit("id", 1684602000, "email"))));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanBranches_GivenAStaleBranch_StaleBranchReturnedAndArchiveBranchCalled() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("main", Collections.emptyList()));
        branches.add(new Branch("a1", Collections.singletonList(new Commit("id", 1672596000, "email"))));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanBranches_BranchPendingArchival_NotificationSentAndEmptyListReturned() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("a1", Collections.singletonList(new Commit("id", 1681023601, "email"))));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanBranches_NotificationLogicThrowsExceptionForPendingArchival_WarningLoggedAndEmptyListReturned() {
        // arrange
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch("a1", Collections.singletonList(new Commit("id", 1681023601, "email"))));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void DeleteArchiveTag_ProviderThrowsExceptionOnDeleteTag_LoggedFailedTagDeletion() {
        // arrange
        Tag testTag = new Tag("archiveTag", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void DeleteArchiveTag_TagSuccessfullyDeleted_NotificationLogicCalled() {
        // arrange
        Tag testTag = new Tag("archiveTag", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void DeleteArchiveTag_NotificationLogicThrowsExceptionOnTagDeletion_LoggedFailureToNotify() {
        // arrange
        Tag testTag = new Tag("archiveTag", Collections.emptyList());

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanTags_TagParameterEmpty_ReturnsEmptyList() {
        // arrange
        List<Tag> tags = new ArrayList<>();

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Tag> result = logic.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    void CleanTags_TagsAreNotArchiveTags_ReturnsEmptyList() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("not_an_archive_tag", Collections.emptyList()));
        tags.add(new Tag("anotherTag", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
                "remote", new ArrayList<>(), new TakeActionCountsDays(0, 0, 0));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Tag> result = logic.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    void CleanTags_NonStaleArchiveTag_ReturnsEmptyList() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("not_an_archive_tag", Collections.emptyList()));
        tags.add(new Tag("zArchiveBranch_20230528_branch", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
                "remote", new ArrayList<>(), new TakeActionCountsDays(60, 30, 7));
        repoCleaningInfoList.add(info);

        GitRepoCleanerLogic logic = createDefaultTestLogic();

        // act
        List<Tag> result = logic.cleanTags(tags, info.repoId(), info.takeActionCountsDays(), 1);

        // assert
        assertIterableEquals(Collections.emptyList(), result);
    }

    @Test
    void CleanTags_StaleArchiveTag_DeletedTagsReturnedAndDeleteTagCalled() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("not_an_archive_tag", Collections.emptyList()));
        tags.add(new Tag("zArchiveBranch_20230101_branch", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanTags_TagPendingArchival_NotificationSentAndEmptyListReturned() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("zArchiveBranch_20230509_branch", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
    void CleanTags_NotificationLogicThrowsExceptionOnPendingTagDeletion_WarningLoggedAndEmptyListReturned() {
        // arrange
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("zArchiveBranch_20230509_branch", Collections.emptyList()));

        RepoCleaningInfo info = new RepoCleaningInfo("1", TestUtils.getProjectRepoDir(),
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
