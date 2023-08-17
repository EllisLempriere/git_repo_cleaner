package Provider;

import Application.Models.ConfigSecrets;
import Application.Models.ConfigsSetupException;
import Business.Models.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
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

public class GitProviderTest {

    private static GitProvider provider;

    @BeforeEach
    void setupTestProvider() {
        ConfigSecrets secrets = new ConfigSecrets("user", "pass");
        provider = new GitProvider(secrets, 0);
    }

    private void setupProviderWithValidSecrets() {
        try {
            // REQUIRES VALID "secrets.properties" FILE TO FUNCTION
            provider = new GitProvider(new ConfigSecrets("secrets.properties"), 3);
        } catch (ConfigsSetupException e) {
            throw new RuntimeException(e);
        }
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

    @ParameterizedTest
    @DisplayName("Invalid remote uri, throws exception")
    @ValueSource(strings = {"notAUri", "invalid.com", "https://gitlab.com/EllisLempriere/not-exist.git"})
    void cloneRepoTest3(String invalidUri) {
        // arrange

        // act/assert
        assertThrows(GitCloningException.class, () -> provider.cloneRepo(
                "C:\\Users\\ellis\\Documents\\repos\\failed-clone-repo", invalidUri));
    }

    @Test
    @DisplayName("File path already exists and is populated, throws exception")
    void cloneRepoTest4() {
        // arrange

        // act/assert
        assertThrows(GitCloningException.class, () -> provider.cloneRepo(
                "C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner", "https://gitlab.com/pslfamily/git_repo_cleaner.git"));
    }

    @Test
    @DisplayName("Successfully clones repo")
    void cloneRepoTest5() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\clone-test-repo";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));

            // act
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/clone-test-repo.git");

            // assert
            assertTrue(repoEqual(repoDir, "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\clone-test-repo"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Clone from empty repo, initializes new repo")
    void cloneRepoTest6() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\clone-empty-repo";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));

            // act
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/empty-repo.git");

            // assert
            assertTrue(repoEqual(repoDir, "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\clone-empty-repo"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    @DisplayName("Local repo not set up, calls setupRepo")
    void updateRepoTest1() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\clone-test-repo-update-1";

            GitProvider providerSpy = spy(provider);
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/clone-test-repo.git");

            // act
            providerSpy.updateRepo(repoDir);

            // assert
            verify(providerSpy, times(1)).setupRepo(repoDir);

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Repo not linked to remote, throws exception")
    void updateRepoTest2() {
        // arrange
        setupProviderWithValidSecrets();

        // act/assert
        assertThrows(GitUpdateException.class, () -> provider.updateRepo(
                "C:\\Users\\ellis\\Documents\\repos\\unlinked-repo"));
    }

    @Test
    @DisplayName("Empty repo, throws exception")
    void updateRepoTest3() {
        // arrange
        setupProviderWithValidSecrets();

        // act/assert
        assertThrows(GitStartupException.class, () -> provider.updateRepo(
                "C:\\Users\\ellis\\Documents\\repos\\empty-repo"));
    }

    @Test
    @DisplayName("Clone repo then update, repo is in expected up-to-date state")
    void updateRepoTest4() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\update-test-clone";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));

            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/update-test-clone.git");

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
    @DisplayName("Local repo up-to-date, no errors thrown and repo is in expected state")
    void updateRepoTest5() {
        // arrange
        setupProviderWithValidSecrets();
        String repoDir = "C:\\Users\\ellis\\Documents\\repos\\update-test-up-to-date";

        // act/assert
        assertDoesNotThrow(() -> provider.updateRepo(repoDir));
        assertTrue(repoEqual(repoDir, "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\update-test-up-to-date"));
    }

    @Test
    @DisplayName("Local repo behind remote, update repo brings local to expected state")
    void updateRepoTest6() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\update-test-behind-localversion";
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/update-test-behind-localversion.git");
            provider.updateRepo(repoDir);
            provider.removeRemote("origin");
            provider.addRemote("https://gitlab.com/EllisLempriere/update-test-behind-remoteversion.git");

            // act
            provider.updateRepo(repoDir);

            // assert
            assertTrue(repoEqual(repoDir, "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\update-test-behind"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (IOException | GitCloningException | GitUpdateException | GitStartupException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    @DisplayName("Git not started up, throws exception")
    void getBranchesTest1() {
        // arrange

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.getBranches());
    }

    @Test
    @DisplayName("No branches, returns empty list")
    void getBranchesTest2() {
        try {
            // arrange
            setupProviderWithValidSecrets();
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

        } catch (GitBranchFetchException | GitStartupException | GitCloningException | GitUpdateException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Repo with some branches, correctly set up, correct list of branches returned")
    void getBranchesTest3() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\get-refs-test";
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

        } catch (GitStartupException | GitCloningException | GitUpdateException | GitBranchFetchException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    @DisplayName("Git not started up, throws exception")
    void getTagsTest1() {
        // arrange

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.getTags());
    }

    @Test
    @DisplayName("No tags, returns empty list")
    void getTagsTest2() {
        try {
            // arrange
            setupProviderWithValidSecrets();
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

        } catch (GitTagFetchException | GitStartupException | GitCloningException | GitUpdateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Repo with some tags, correctly set up, correct list of tags returned")
    void getTagsTest3() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\get-refs-test";
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

        } catch (GitStartupException | GitCloningException | GitUpdateException | GitTagFetchException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    @DisplayName("Git not setup, throws exception")
    void createTagTest1() {
        // arrange

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.createTag(new Tag("tag", Collections.emptyList())));
    }

    @ParameterizedTest
    @DisplayName("Tag passed to method has null or empty parts, throws exception")
    @MethodSource("generateInvalidTags")
    void createTagTest2(Tag tag, String testId) {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests" + testId;
            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            // act/assert
            assertThrows(IllegalArgumentException.class, () -> provider.createTag(tag));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitStartupException | IOException | GitCloningException | GitUpdateException e) {
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
    @DisplayName("Tag's commit list contains commits that are not part of the repo, throws exception")
    @MethodSource("generateTagsWithInvalidCommits")
    void createTagTest3(Tag tag) {
        try {
            // arrange
            setupProviderWithValidSecrets();
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

        } catch (GitStartupException | GitCloningException | GitUpdateException | IOException e) {
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
    @DisplayName("Valid tag with valid commits, creates tag")
    void createTagTest4() {
        try {
            // arrange
            setupProviderWithValidSecrets();
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

        } catch (GitStartupException | GitCloningException | GitUpdateException | IOException | GitCreateTagException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    @DisplayName("Null branch passed in as parameter, throws exception")
    void deleteBranchTest1() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.deleteBranch(null));
    }

    @Test
    @DisplayName("Git not started up, throws exception")
    void deleteBranchTest2() {
        // arrange

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.deleteBranch(new Branch("branch", Collections.emptyList())));
    }

    @Test
    @DisplayName("Branch passed in does not exist in repo, does nothing")
    void deleteBranchTest3() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-branch-3";

            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            Branch branchToDelete = new Branch("notInRepo", Collections.emptyList());

            // act/assert
            assertDoesNotThrow(() -> provider.deleteBranch(branchToDelete));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Branch to be deleted is checked out, branch is still successfully deleted")
    void deleteBranchTest4() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-branch-4";

            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            Branch branchToDelete = new Branch("a1", Collections.emptyList());
            provider.checkoutBranch(branchToDelete.name());

            // act
            provider.deleteBranch(branchToDelete);

            // assert
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\tag-branch-manip-tests-delete-branch"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitBranchDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Branch to be deleted is not checked out, successfully deletes branch")
    void deleteBranchTest5() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-branch-5";

            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            Branch branchToDelete = new Branch("a1", Collections.emptyList());
            provider.checkoutBranch("main");

            // act
            provider.deleteBranch(branchToDelete);

            // assert
            assertTrue(repoEqual(repoDir,
                    "C:\\Users\\ellis\\Documents\\git-cleaner-expected-repos\\tag-branch-manip-tests-delete-branch"));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitBranchDeletionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Null tag passed in as parameter, throws exception")
    void deleteTagTest1() {
        // arrange

        // act/assert
        assertThrows(IllegalArgumentException.class, () -> provider.deleteTag(null));
    }

    @Test
    @DisplayName("Git not started up, throws exception")
    void deleteTagTest2() {
        // arrange

        // act/assert
        assertThrows(GitNotSetupException.class, () -> provider.deleteTag(new Tag("tag", Collections.emptyList())));
    }

    @Test
    @DisplayName("Tag passed in does not exist in repo, does nothing")
    void deleteTagTest3() {
        try {
            // arrange
            setupProviderWithValidSecrets();
            String repoDir = "C:\\Users\\ellis\\Documents\\repos\\tag-branch-manip-tests-delete-tag-3";

            if (Files.exists(Paths.get(repoDir)))
                FileUtils.deleteDirectory(new File(repoDir));
            provider.cloneRepo(repoDir, "https://gitlab.com/EllisLempriere/tag-branch-manip-tests.git");
            provider.updateRepo(repoDir);

            Tag tagToDelete = new Tag("notInRepo", Collections.emptyList());

            // act/assert
            assertDoesNotThrow(() -> provider.deleteTag(tagToDelete));

            provider.shutdownRepo();
            FileUtils.deleteDirectory(new File(repoDir));

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Tag is successfully deleted")
    void deleteTagTest4() {
        try {
            // arrange
            setupProviderWithValidSecrets();
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

        } catch (GitCloningException | GitUpdateException | GitStartupException | IOException |
                 GitTagDeletionException e) {
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

    // Expected data builders for getBranches and getTags
    private List<Branch> getExpectedBranchList() {
        List<Branch> expectedBranches = new ArrayList<>();

        List<Commit> commits = getCommitsList();

        expectedBranches.add(new Branch("a1", Arrays.asList(commits.get(4), commits.get(1))));
        expectedBranches.add(new Branch("a2", Arrays.asList(commits.get(5), commits.get(0), commits.get(1))));
        expectedBranches.add(new Branch("b1", Arrays.asList(commits.get(2), commits.get(3), commits.get(4), commits.get(1))));
        expectedBranches.add(new Branch("main", Arrays.asList(commits.get(0), commits.get(1))));

        return expectedBranches;
    }
    private List<Tag> getExpectedTagList() {
        List<Tag> expectedTags = new ArrayList<>();

        List<Commit> commits = getCommitsList();

        expectedTags.add(new Tag("tag1", Arrays.asList(commits.get(5), commits.get(0), commits.get(1))));
        expectedTags.add(new Tag("tag2", Arrays.asList(commits.get(3), commits.get(4), commits.get(1))));

        return expectedTags;
    }
    private static List<Commit> getCommitsList() {
        List<Commit> commits = new ArrayList<>();
        commits.add(new Commit("3c678745d4dc1a1430845ad01bf0e4b5a3f37548", 1692221305, "ellis@pslfamily.org"));
        commits.add(new Commit("9de708e0a2cbd029444e0195a9d538e35f48fcd7", 1692220919, "ellis@pslfamily.org"));
        commits.add(new Commit("ba11b5ef223297bec065df15246af8d83075db6c", 1692221251, "ellis@pslfamily.org"));
        commits.add(new Commit("d20d64e04292f8092dbbfae49e45d609a198167a", 1692221202, "ellis@pslfamily.org"));
        commits.add(new Commit("b0d2f20d8d1618b05933755b07ed514782fae1b5", 1692221125, "ellis@pslfamily.org"));
        commits.add(new Commit("905e7869041187ee4ef3ce2331f383838321372b", 1692221362, "ellis@pslfamily.org"));

        return commits;
    }
}
