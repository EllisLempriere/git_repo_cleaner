package IntegrationTests.Provider;

import Application.Models.ConfigSecrets;
import Application.Models.ConfigsSetupException;
import Business.Models.*;
import Provider.GitNotSetupException;
import Provider.GitProvider;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GitProviderTests {

    private GitProvider getProvider() {
        ConfigSecrets secrets = new ConfigSecrets("user", "pass");
        return new GitProvider(secrets, 0);
    }

    private GitProvider getProviderWithValidSecrets() {
        try {
            // REQUIRES VALID "secrets.properties" FILE TO FUNCTION
            return new GitProvider(new ConfigSecrets("secrets.properties"), 3);
        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void Constructor_NullConfigSecrets_ThrowsException() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitProvider(null, 0));
    }

    @ParameterizedTest
    @CsvSource({", pass", "user,"})
    void Constructor_NullValuesToConfigSecrets_ThrowsException(String username, String password) {
        // arrange
        ConfigSecrets invalidSecrets = new ConfigSecrets(username, password);

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitProvider(invalidSecrets, 0));
    }

    @Test
    void Constructor_InvalidRetries_ThrowsException() {
        // arrange
        ConfigSecrets secrets = new ConfigSecrets("user", "pass");

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> new GitProvider(secrets, -1));
    }

    @Test
    void Constructor_ValidParameters_CreatesInstance() {
        // arrange
        ConfigSecrets secrets = new ConfigSecrets("user", "pass");

        // act
        GitProvider result = new GitProvider(secrets, 0);

        // assert
        assertInstanceOf(GitProvider.class, result);
    }


    @Test
    void SetupRepo_NullRepoDir_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.setupRepo(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not_a_file_path", "C:\\Documents"})
    void SetupRepo_InvalidRepoDir_ThrowsException(String invalidPath) {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitStartupException.class, () -> provider.setupRepo(invalidPath));
    }

    @Test
    void SetupRepo_ValidRepoDir_SuccessfullyExecutes() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertDoesNotThrow(() -> provider.setupRepo("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner"));
    }


    @Test
    void CloneRepo_NullRepoDir_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.cloneRepo(
                null, "https://gitlab.com/EllisLempriere/clone-test-repo.git"));
    }

    @Test
    void CloneRepo_NullRemoteUri_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.cloneRepo(
                "C:\\Users\\ellis\\Documents\\repos\\clone-test-repo", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"notAUri", "invalid.com", "https://gitlab.com/EllisLempriere/not-exist.git"})
    void CloneRepo_InvalidRemoteUri_ThrowsException(String invalidUri) {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitCloningException.class, () -> provider.cloneRepo(
                "C:\\Users\\ellis\\Documents\\repos\\failed-clone-repo", invalidUri));
    }

    @Test
    void CloneRepo_FilePathExistsAndIsPopulated_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitCloningException.class, () -> provider.cloneRepo(
                "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner",
                "https://gitlab.com/pslfamily/git_repo_cleaner.git"));
    }

    @Test
    void CloneRepo_ValidSetup_SuccessfullyExecutes() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\clone-test-repo";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));

            // act
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/clone-test-repo.git");

            // assert
            assertTrue(repoEqual(repoDir, "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\clone-test-repo"));

            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void CloneRepo_EmptyRepo_NewRepoInitializedAtRepoDir() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\clone-empty-repo";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));

            // act
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/empty-repo.git");

            // assert
            assertTrue(repoEqual(repoDir, "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\clone-empty-repo"));

            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void UpdateRepo_RepoNotSetUp_CallsSetupRepo() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\clone-test-repo-update-1";

            GitProvider providerSpy = spy(provider);
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            providerSpy.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/clone-test-repo.git");

            // act
            providerSpy.updateRepo(repoDir);

            // assert
            verify(providerSpy, times(1)).setupRepo(repoDir);

            providerSpy.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void UpdateRepo_RepoHasNoRemote_ThrowsException() {
        // arrange
        GitProvider provider = getProviderWithValidSecrets();

        // act/assert
        assertThrows(GitUpdateException.class, () -> provider.updateRepo(
                "C:\\Users\\ellis\\Documents\\repos\\unlinked-repo"));
    }

    @Test
    void UpdateRepo_EmptyRepo_ThrowsException() {
        // arrange
        GitProvider provider = getProviderWithValidSecrets();

        // act/assert
        assertThrows(GitStartupException.class, () -> provider.updateRepo(
                "C:\\Users\\ellis\\Documents\\repos\\empty-repo"));
    }

    @Test
    void UpdateRepo_LocalRepoSetUp_RepoIsInExpectedState() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\update-test-clone";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/update-test-clone.git");
            provider.setupRepo(repoDir);

            // act
            provider.updateRepo(repoDir);

            // assert
            assertTrue(repoEqual(repoDir, "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\update-test-clone"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (IOException | GitCloningException | GitUpdateException | GitStartupException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void UpdateRepo_LocalRepoAlreadyUpToDate_NoExceptionsAndRepoUpToDate() {
        // arrange
        GitProvider provider = getProviderWithValidSecrets();

        String repoDir = "C:\\Users\\ellis\\Documents\\repos\\update-test-up-to-date";

        // act/assert
        assertAll(
                () -> assertDoesNotThrow(() -> provider.updateRepo(repoDir)),
                () -> assertTrue(repoEqual(repoDir,
                        "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\update-test-up-to-date")));

        provider.shutdownRepo();
    }

    @Test
    void UpdateRepo_LocalBehindRemote_RepoUpToDate() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\update-test-behind-localversion";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/update-test-behind-localversion.git");
            provider.updateRepo(repoDir);

            removeRemote(repoDir, "origin");
            addRemote(repoDir, "https://gitlab.com/EllisLempriere/update-test-behind-remoteversion.git");

            // act
            provider.updateRepo(repoDir);

            // assert
            assertTrue(repoEqual(repoDir, "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\update-test-behind"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitUpdateException | GitStartupException | IOException | GitCloningException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void UpdateRepo_InvalidUserSecrets_ThrowsException() {
        try {
            // arrange
            GitProvider provider = new GitProvider(new ConfigSecrets("invalidUser", "invalidPass"), 3);

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\update-test-up-to-date";
            provider.setupRepo(repoDir);

            // act/assert
            assertThrows(GitUpdateException.class, () -> provider.updateRepo(repoDir));

            provider.shutdownRepo();

        } catch (GitStartupException e) {
            throw new RuntimeException(e);
        }
    }



    @Test
    void GetBranches_NotSetup_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitNotSetupException.class, provider::getBranches);
    }

    @Test
    void GetBranches_NoBranches_ReturnsEmptyList() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\empty-repo-get-branches-2";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/empty-repo.git");
            provider.updateRepo(repoDir);

            // act
            List<Branch> result = provider.getBranches();

            // assert
            assertEquals(Collections.emptyList(), result);

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitBranchFetchException | GitCloningException | GitUpdateException | GitStartupException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void GetBranches_ValidRepo_ReturnsExpectedBranches() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\get-refs-test-get-branches";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/get-refs-test.git");
            provider.updateRepo(repoDir);

            List<Branch> expected = getExpectedBranchList();

            // act
            List<Branch> result = provider.getBranches();

            // assert
            assertIterableEquals(expected, result);

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitBranchFetchException | GitCloningException | GitUpdateException | GitStartupException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }
    private List<Branch> getExpectedBranchList() {
        List<Branch> expectedBranches = new ArrayList<>();

        List<Commit> commits = getCommitsList();

        expectedBranches.add(new Branch("a1", Arrays.asList(commits.get(4), commits.get(1))));
        expectedBranches.add(new Branch("a2", Arrays.asList(commits.get(5), commits.get(0), commits.get(1))));
        expectedBranches.add(new Branch("b1", Arrays.asList(commits.get(2), commits.get(3), commits.get(4), commits.get(1))));
        expectedBranches.add(new Branch("main", Arrays.asList(commits.get(0), commits.get(1))));

        return expectedBranches;
    }


    @Test
    void GetTags_NotSetup_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitNotSetupException.class, provider::getTags);
    }

    @Test
    void GetTags_NoTags_ReturnsEmptyList() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\empty-repo-get-tags-2";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/empty-repo.git");
            provider.updateRepo(repoDir);

            // act
            List<Tag> result = provider.getTags();

            // assert
            assertEquals(Collections.emptyList(), result);

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitTagFetchException | GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void GetTags_ValidRepo_ReturnsExpectedTags() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\get-refs-test-get-tags";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/get-refs-test.git");
            provider.updateRepo(repoDir);

            List<Tag> expected = getExpectedTagList();

            // act
            List<Tag> result = provider.getTags();

            // assert
            assertIterableEquals(expected, result);

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitTagFetchException | GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    private List<Tag> getExpectedTagList() {
        List<Tag> expectedTags = new ArrayList<>();

        List<Commit> commits = getCommitsList();

        expectedTags.add(new Tag("tag1", Arrays.asList(commits.get(5), commits.get(0), commits.get(1))));
        expectedTags.add(new Tag("tag2", Arrays.asList(commits.get(3), commits.get(4), commits.get(1))));

        return expectedTags;
    }


    @Test
    void CreateTag_NotSetup_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.createTag(new Tag("tag", Collections.emptyList())));
    }

    @ParameterizedTest
    @MethodSource("generateInvalidTags")
    void CreateTag_InvalidTagParameter_ThrowsException(Tag tag, String testId) {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests" + testId;
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            // act/assert
            assertThrows(IllegalArgumentException.class, () -> provider.createTag(tag));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    static Stream<Arguments> generateInvalidTags() {
        return Stream.of(
                Arguments.of(null, "1"),
                Arguments.of(new Tag(null, null), "2"),
                Arguments.of(new Tag("tag", null), "3"),
                Arguments.of(new Tag("tag", Collections.emptyList()), "4")
        );
    }

    @ParameterizedTest
    @MethodSource("generateTagsWithInvalidCommits")
    void CreateTag_InvalidCommitHistoryOnTag_ThrowsException(Tag tag) {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-create-tag-3."
                    + tag.name().substring(3);
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            // act/assert
            assertThrows(GitCreateTagException.class, () -> provider.createTag(tag));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    static Stream<Arguments> generateTagsWithInvalidCommits() {
        List<Commit> commitIdNotInRepo = Collections.singletonList(new Commit("id", 0, "mail"));

        List<Commit> commitListNotContainFullHistory = Collections.singletonList(
                new Commit("558108245c811af0be3b402e68fe3325df95a808", 1692224937, "ellis@pslfamily.org"));

        List<Commit> commitListContainsFullHistoryInWrongOrder = Arrays.asList(
                new Commit("89e993aba2e0cc6c7ae500646c8b41c0f89e547d", 1692224945, "ellis@pslfamily.org"),
                new Commit("09d084455a5c0b6ab3621463e7dfdbbb03beed04", 1692224663, "ellis@pslfamily.org"),
                new Commit("558108245c811af0be3b402e68fe3325df95a808", 1692224937, "ellis@pslfamily.org"));

        return Stream.of(
                Arguments.of(new Tag("tag1", commitIdNotInRepo)),
                Arguments.of(new Tag("tag2", commitListNotContainFullHistory)),
                Arguments.of(new Tag("tag3", commitListContainsFullHistoryInWrongOrder))
        );
    }

    @Test
    void CreateTag_ValidTag_TagIsCreated() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-create-tag";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            List<Commit> commits = Arrays.asList(
                    new Commit("89e993aba2e0cc6c7ae500646c8b41c0f89e547d", 1692224945, "ellis@pslfamily.org"),
                    new Commit("558108245c811af0be3b402e68fe3325df95a808", 1692224937, "ellis@pslfamily.org"),
                    new Commit("09d084455a5c0b6ab3621463e7dfdbbb03beed04", 1692224663, "ellis@pslfamily.org"));

            // act
            provider.createTag(new Tag("tagged", commits));

            // assert
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\tag-branch-manip-tests-create-tag"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCreateTagException | GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void DeleteBranch_NullBranchParameter_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.deleteBranch(null));
    }

    @Test
    void DeleteBranch_NotSetup_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.deleteBranch(new Branch("branch", Collections.emptyList())));
    }

    @Test
    void DeleteBranch_BranchNotInRepo_DoesNothing() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-branch-3";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            Branch branchToDelete = new Branch("notInRepo", Collections.emptyList());

            // act
            provider.deleteBranch(branchToDelete);

            // assert
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\tag-branch-manip-tests"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitBranchDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void DeleteBranch_BranchCheckedOut_BranchDeleted() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-branch-4";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            Branch branchToDelete = new Branch("a1", Collections.emptyList());
            checkoutBranch(repoDir, branchToDelete.name());

            // act
            provider.deleteBranch(branchToDelete);

            // assert
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\tag-branch-manip-tests-delete-branch"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitBranchDeletionException | GitCloningException | GitUpdateException | GitStartupException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void DeleteBranch_ValidSetup_BranchDeleted() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-branch-5";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            Branch branchToDelete = new Branch("a1", Collections.emptyList());
            checkoutBranch(repoDir, "main");

            // act
            provider.deleteBranch(branchToDelete);

            // assert
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\tag-branch-manip-tests-delete-branch"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitBranchDeletionException | GitCloningException | GitUpdateException | GitStartupException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void DeleteTag_NullTagParameter_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.deleteTag(null));
    }

    @Test
    void DeleteTag_NotSetup_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.deleteTag(new Tag("tag", Collections.emptyList())));
    }

    @Test
    void DeleteTag_TagNotInRepo_DoesNothing() {
        try {
            // arrange
            GitProvider gp = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-tag-3";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            gp.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            gp.updateRepo(repoDir);

            Tag tagToDelete = new Tag("notInRepo", Collections.emptyList());

            String expectedRepoDir = "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\tag-branch-manip-tests";

            // act
            gp.deleteTag(tagToDelete);

            // assert
            assertTrue(repoEqual(repoDir, expectedRepoDir));

            gp.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitTagDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void DeleteTag_ValidSetup_TagDeleted() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-tag-4";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            Tag tagToDelete = new Tag("imATag", Collections.emptyList());

            // act
            provider.deleteTag(tagToDelete);

            // assert
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\tag-branch-manip-tests-delete-tag"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitTagDeletionException | GitCloningException | GitUpdateException | GitStartupException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void PushDeleteRemoteBranch_ValidSetup_BranchRemovedFromRemote() {
        try {
            // arrange
            GitProvider provider1 = getProviderWithValidSecrets();

            String repoDir1 = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-branch-1";
            if (Files.exists(Paths.get(repoDir1)))
                FileUtils.deleteDirectory(new File(repoDir1));
            provider1.cloneRepo(repoDir1, "https://gitlab.com/EllisLempriere/remote-changes-tests-delete-branch.git");
            provider1.updateRepo(repoDir1);

            Branch branchToDelete = new Branch("delete", Arrays.asList(
                    new Commit("04281cf2a21765aee9060880ee9ee905772dbf8a", 1692637168, "ellis@pslfamily.org"),
                    new Commit("31512ea22eb03c3d077ffe171f4d44615775858d", 1692637129, "ellis@pslfamily.org")));
            provider1.deleteBranch(branchToDelete);

            // act
            provider1.pushDeleteRemoteBranch(branchToDelete);

            // assert
            GitProvider provider2 = getProviderWithValidSecrets();

            String repoDir2 = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-branch-result";
            if (Files.exists(Paths.get(repoDir2)))
                FileUtils.deleteDirectory(new File(repoDir2));
            provider2.cloneRepo(repoDir2, "https://gitlab.com/EllisLempriere/remote-changes-tests-delete-branch.git");
            provider2.updateRepo(repoDir2);

            List<Branch> resultBranchList = provider2.getBranches();
            assertFalse(resultBranchList.contains(branchToDelete));

            // reset
            createRemoteBranch(repoDir1, branchToDelete);

            provider1.shutdownRepo();
            provider2.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir1));
            FileUtils.deleteDirectory(new File(repoDir2));

        } catch (GitBranchDeletionException | GitPushBranchDeletionException | GitBranchFetchException |
                 GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void PushDeleteRemoteBranch_BranchNotInRemote_DoesNothing() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-branch-2";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/remote-changes-tests.git");
            provider.updateRepo(repoDir);

            Branch branchToDelete = new Branch("not_exist", Collections.emptyList());

            // act
            provider.pushDeleteRemoteBranch(branchToDelete);

            // assert
            provider.updateRepo(repoDir);
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\remote-changes-tests"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                GitPushBranchDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void PushDeleteRemoteBranch_NotSetup_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.pushDeleteRemoteBranch(
                new Branch("branch", Collections.emptyList())));
    }

    @Test
    void PushDeleteRemoteBranch_RemoteNotLinked_ThrowsException() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\unlinked-repo";
            provider.setupRepo(repoDir);

            // act/assert
            assertThrows(GitPushBranchDeletionException.class,
                    ()-> provider.pushDeleteRemoteBranch(new Branch("branch", Collections.emptyList())));

        } catch (GitStartupException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void PushNewTags_NotSetup_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitNotSetupException.class, provider::pushNewTags);
    }

    @Test
    void PushNewTags_ValidSetup_TagsAddedToRemote() {
        try {
            // arrange
            GitProvider provider1 = getProviderWithValidSecrets();

            String repoDir1 = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-push-tags-2";
            if (Files.exists(Paths.get(repoDir1)))
                FileUtils.deleteDirectory(new File(repoDir1));
            provider1.cloneRepo(repoDir1, "https://gitlab.com/EllisLempriere/remote-changes-tests-push-tags.git");
            provider1.updateRepo(repoDir1);

            List<Tag> tagsToAdd = new ArrayList<>();
            tagsToAdd.add(new Tag("tag1", Arrays.asList(
                    new Commit("262bc85720ae45973cad63f864c4f540390127a1", 1692637284, "ellis@pslfamily.org"),
                    new Commit("499e74e851ebfa25a81a5ea83cedb595bc0af9ce", 1692637272, "ellis@pslfamily.org"),
                    new Commit("7a5ba15a1f4b53561c9ff119e04f1fbf3eb9ab48", 1692637195, "ellis@pslfamily.org"),
                    new Commit("31512ea22eb03c3d077ffe171f4d44615775858d", 1692637129, "ellis@pslfamily.org"))));
            tagsToAdd.add(new Tag("tag2", Arrays.asList(
                    new Commit("499e74e851ebfa25a81a5ea83cedb595bc0af9ce", 1692637272, "ellis@pslfamily.org"),
                    new Commit("7a5ba15a1f4b53561c9ff119e04f1fbf3eb9ab48", 1692637195, "ellis@pslfamily.org"),
                    new Commit("31512ea22eb03c3d077ffe171f4d44615775858d", 1692637129, "ellis@pslfamily.org"))));
            provider1.createTag(tagsToAdd.get(0));
            provider1.createTag(tagsToAdd.get(1));

            // act
            provider1.pushNewTags();

            // assert
            GitProvider provider2 = getProviderWithValidSecrets();

            String repoDir2 = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-push-tags-2-result";
            provider2.cloneRepo(repoDir2, "https://gitlab.com/EllisLempriere/remote-changes-tests-push-tags.git");
            provider2.updateRepo(repoDir2);

            List<Tag> resultTagList = provider2.getTags();
            assertAll(
                    () -> assertTrue(resultTagList.contains(tagsToAdd.get(0))),
                    () -> assertTrue(resultTagList.contains(tagsToAdd.get(1)))
            );

            // reset
            provider2.pushDeleteRemoteTag(tagsToAdd.get(0));
            provider2.pushDeleteRemoteTag(tagsToAdd.get(1));

            provider1.shutdownRepo();
            provider2.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir1));
            FileUtils.deleteDirectory(new File(repoDir2));

        } catch (GitPushNewTagsException | GitTagFetchException | GitCreateTagException | GitCloningException |
                 GitUpdateException | GitStartupException | IOException | GitPushTagDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void PushNewTags_NoNewTags_DoesNothing() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-push-tags-3";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/remote-changes-tests.git");
            provider.updateRepo(repoDir);

            // act
            provider.pushNewTags();

            // assert
            provider.updateRepo(repoDir);
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\remote-changes-tests"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitPushNewTagsException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void PushNewTags_NoRemoteLinked_ThrowsException() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-push-tags-4";
            provider.setupRepo(repoDir);

            Tag tag = new Tag("tag1", Arrays.asList(
                    new Commit("262bc85720ae45973cad63f864c4f540390127a1", 1692637284, "ellis@pslfamily.org"),
                    new Commit("499e74e851ebfa25a81a5ea83cedb595bc0af9ce", 1692637272, "ellis@pslfamily.org"),
                    new Commit("7a5ba15a1f4b53561c9ff119e04f1fbf3eb9ab48", 1692637195, "ellis@pslfamily.org"),
                    new Commit("31512ea22eb03c3d077ffe171f4d44615775858d", 1692637129, "ellis@pslfamily.org")));

            provider.createTag(tag);

            // act/assert
            assertThrows(GitPushNewTagsException.class, provider::pushNewTags);

            // reset
            provider.deleteTag(tag);
            provider.shutdownRepo();

        } catch (GitStartupException | GitCreateTagException | GitTagDeletionException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void PushDeleteRemoteTag_NotSetup_ThrowsException() {
        // arrange
        GitProvider provider = getProvider();

        // act/assert
        assertThrows(GitNotSetupException.class,
                () -> provider.pushDeleteRemoteTag(new Tag("tag", Collections.emptyList())));
    }

    @Test
    void PushDeleteRemoteTag_NoRemoteLinked_ThrowsException() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-tag-2";
            provider.setupRepo(repoDir);

            Tag tagToDelete = new Tag("deleteMe", Arrays.asList(
                    new Commit("7a5ba15a1f4b53561c9ff119e04f1fbf3eb9ab48", 1692637195, "ellis@pslfamily.org"),
                    new Commit("31512ea22eb03c3d077ffe171f4d44615775858d", 1692637129, "ellis@pslfamily.org")));

            provider.deleteTag(tagToDelete);

            // act/assert
            assertThrows(GitPushTagDeletionException.class, () -> provider.pushDeleteRemoteTag(tagToDelete));

            // reset
            provider.createTag(tagToDelete);
            provider.shutdownRepo();

        } catch (GitStartupException | GitTagDeletionException | GitCreateTagException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void PushDeleteRemoteTag_TagNotInRemote_DoesNothing() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-tag-3";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/remote-changes-tests.git");
            provider.updateRepo(repoDir);

            Tag tagToDelete = new Tag("not_exist", Collections.emptyList());

            // act
            provider.pushDeleteRemoteTag(tagToDelete);

            // assert
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\remote-changes-tests"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitPushTagDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void PushDeleteRemoteTag_ValidSetup_TagRemovedFromRemote() {
        try {
            // arrange
            GitProvider provider = getProviderWithValidSecrets();

            String repoDir1 = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-tag-4";
            if (Files.exists(Paths.get(repoDir1)))
                FileUtils.deleteDirectory(new File(repoDir1));
            provider.cloneRepo(repoDir1, "https://gitlab.com/EllisLempriere/remote-changes-tests-delete-tag.git");
            provider.updateRepo(repoDir1);

            Tag tagToDelete = new Tag("deleteMe", Arrays.asList(
                    new Commit("7a5ba15a1f4b53561c9ff119e04f1fbf3eb9ab48", 1692637195, "ellis@pslfamily.org"),
                    new Commit("31512ea22eb03c3d077ffe171f4d44615775858d", 1692637129, "ellis@pslfamily.org")));

            provider.deleteTag(tagToDelete);

            // act
            provider.pushDeleteRemoteTag(tagToDelete);

            // assert
            String repoDir2 = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-tag-4-result";
            provider.cloneRepo(repoDir2, "https://gitlab.com/EllisLempriere/remote-changes-tests-delete-tag.git");
            provider.updateRepo(repoDir2);

            List<Tag> tags = provider.getTags();
            assertFalse(tags.contains(tagToDelete));

            // reset
            provider.createTag(tagToDelete);
            provider.pushNewTags();

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir1));
            FileUtils.deleteDirectory(new File(repoDir2));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitPushTagDeletionException | GitTagDeletionException | GitTagFetchException | GitCreateTagException |
                 GitPushNewTagsException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void PushDeleteRemoteTag_LocalTagNotRemoved_TagRemovedOnRemote() {
        try {
            // arrange
            GitProvider provider1 = getProviderWithValidSecrets();

            String repoDir1 = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-tag-5";
            if (Files.exists(Paths.get(repoDir1)))
                FileUtils.deleteDirectory(new File(repoDir1));
            provider1.cloneRepo(repoDir1, "https://gitlab.com/EllisLempriere/remote-changes-tests-delete-tag.git");
            provider1.updateRepo(repoDir1);

            Tag tagToDelete = new Tag("deleteMe", Arrays.asList(
                    new Commit("7a5ba15a1f4b53561c9ff119e04f1fbf3eb9ab48", 1692637195, "ellis@pslfamily.org"),
                    new Commit("31512ea22eb03c3d077ffe171f4d44615775858d", 1692637129, "ellis@pslfamily.org")));

            // act
            provider1.pushDeleteRemoteTag(tagToDelete);

            // assert
            GitProvider provider2 = getProviderWithValidSecrets();

            String repoDir2 = "C:\\Users\\ellis\\Documents\\repos\\remote-changes-tests-delete-tag-5-result";
            if (Files.exists(Paths.get(repoDir2)))
                FileUtils.deleteDirectory(new File(repoDir2));
            provider2.cloneRepo(repoDir2, "https://gitlab.com/EllisLempriere/remote-changes-tests-delete-tag.git");
            provider2.updateRepo(repoDir2);

            List<Tag> tags1 = provider1.getTags();
            List<Tag> tags2 = provider2.getTags();
            assertAll(
                    () -> assertTrue(tags1.contains(tagToDelete)),
                    () -> assertFalse(tags2.contains(tagToDelete))
            );

            // Reset test
            provider1.pushNewTags();

            provider1.shutdownRepo();
            provider2.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir1));
            FileUtils.deleteDirectory(new File(repoDir2));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitPushTagDeletionException | GitTagFetchException | GitPushNewTagsException e) {
            throw new RuntimeException(e);
        }
    }


    private boolean repoEqual(String repoDir1, String repoDir2) {
        if (!reposExist(repoDir1, repoDir2))
            return false;

        GitProvider provider1 = new GitProvider(new ConfigSecrets("user", "pass"), 0);
        GitProvider provider2 = new GitProvider(new ConfigSecrets("user", "pass"), 0);

        try {
            provider1.setupRepo(repoDir1);
            provider2.setupRepo(repoDir2);

            List<Branch> branchList1 = provider1.getBranches();
            List<Branch> branchList2 = provider2.getBranches();
            if (!branchList1.equals(branchList2))
                return false;

            List<Tag> tagList1 = provider1.getTags();
            List<Tag> tagList2 = provider2.getTags();
            if (!tagList1.equals(tagList2))
                return false;

            provider1.shutdownRepo();
            provider2.shutdownRepo();

            return true;

        } catch (GitStartupException | GitBranchFetchException | GitTagFetchException e) {
            return false;
        }
    }

    private boolean reposExist(String repoDir1, String repoDir2) {
        Path repoPath1 = Paths.get(repoDir1);
        Path repoPath2 = Paths.get(repoDir2);
        boolean repoPath1Exists = Files.exists(repoPath1) && Files.isDirectory(repoPath1);
        boolean repoPath2Exists = Files.exists(repoPath2) && Files.isDirectory(repoPath2);
        if (!repoPath1Exists || !repoPath2Exists)
            return false;

        Path gitPath1 = Paths.get(repoDir1 + "/.git");
        Path gitPath2 = Paths.get(repoDir2 + "/.git");
        boolean repo1Exists = Files.exists(gitPath1) && Files.isDirectory(gitPath1);
        boolean repo2Exists = Files.exists(gitPath2) && Files.isDirectory(gitPath2);
        if (!repo1Exists || !repo2Exists)
            return false;

        return true;
    }

    // Expected commits for tests for getBranches and getTags
    private List<Commit> getCommitsList() {
        List<Commit> commits = new ArrayList<>();
        commits.add(new Commit("3c678745d4dc1a1430845ad01bf0e4b5a3f37548", 1692221305, "ellis@pslfamily.org"));
        commits.add(new Commit("9de708e0a2cbd029444e0195a9d538e35f48fcd7", 1692220919, "ellis@pslfamily.org"));
        commits.add(new Commit("ba11b5ef223297bec065df15246af8d83075db6c", 1692221251, "ellis@pslfamily.org"));
        commits.add(new Commit("d20d64e04292f8092dbbfae49e45d609a198167a", 1692221202, "ellis@pslfamily.org"));
        commits.add(new Commit("b0d2f20d8d1618b05933755b07ed514782fae1b5", 1692221125, "ellis@pslfamily.org"));
        commits.add(new Commit("905e7869041187ee4ef3ce2331f383838321372b", 1692221362, "ellis@pslfamily.org"));

        return commits;
    }

    private void removeRemote(String repoDir, String remoteName) {
        try {
            String repoDirectory = repoDir + "\\.git";

            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            Repository repo = repoBuilder
                    .setGitDir(new File(repoDirectory))
                    .readEnvironment()
                    .findGitDir()
                    .setMustExist(true)
                    .build();
            Git git = new Git(repo);

            git.remoteRemove()
                    .setRemoteName(remoteName)
                    .call();

            git.getRepository().close();

        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private void addRemote(String repoDir, String remoteUri) {
        try {
            String repoDirectory = repoDir + "\\.git";

            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            Repository repo = repoBuilder
                    .setGitDir(new File(repoDirectory))
                    .readEnvironment()
                    .findGitDir()
                    .setMustExist(true)
                    .build();
            Git git = new Git(repo);

            git.remoteAdd()
                    .setUri(new URIish(remoteUri))
                    .setName("origin")
                    .call();

            git.getRepository().close();

        } catch (IOException | GitAPIException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkoutBranch(String repoDir, String branchName) {
        try {
            String repoDirectory = repoDir + "\\.git";

            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            Repository repo = repoBuilder
                    .setGitDir(new File(repoDirectory))
                    .readEnvironment()
                    .findGitDir()
                    .setMustExist(true)
                    .build();
            Git git = new Git(repo);

            git.checkout().setCreateBranch(false).setName(branchName).call();

            git.getRepository().close();

        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private void createRemoteBranch(String repoDir, Branch branch) {
        try {
            String repoDirectory = repoDir + "\\.git";

            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            Repository repo = repoBuilder
                    .setGitDir(new File(repoDirectory))
                    .readEnvironment()
                    .findGitDir()
                    .setMustExist(true)
                    .build();
            Git git = new Git(repo);

            git.branchCreate()
                    .setName(branch.name())
                    .setStartPoint(branch.commits().get(0).commitId())
                    .setForce(true)
                    .call();

            ConfigSecrets secrets = new ConfigSecrets("secrets.properties");

            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(secrets.USERNAME, secrets.PASSWORD))
                    .add(branch.name())
                    .setForce(true)
                    .call();

            git.getRepository().close();

        } catch (IOException | GitAPIException | ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
    }
}
