# Manual Test Cases

The following are test cases to be executed manually using git shell and the associted ManualMaintenanceInstructions.md



## Test Case 1 - New Repo

**Given** a new repo that does not contain an initial commit  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-1"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-1
cd test-case-1
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-1.git
```

Confirm output like:
```
Initialized empty Git repository in .../repos/test-case-1/.git/
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Branch list is empty
    ```
    git branch
    ```
- Tag list is empty
    ```
    git tag
    ```
- No notifications are pending to be sent

### Test Case Reset Instructions
- Once the remote repo has been created in GitLab it should not need to be reset as the repo should always stay in the "new" state
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -r test-case-1
    ```
- When re-running the test case, if the remote repo still exists, just do confirmation in the arrange section



## Test Case 2 - New Empty Branch on Stale Main

**Given** a repo where the most recent commit on main was made more than 60 days ago and a new branch is on the tip of main  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And the new branch will be archived  
And no pending archival tag notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-2"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-2
cd test-case-2
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-2.git
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-02 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "initial commit"
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-02 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I am a stale main"
git push -u origin main
git checkout -b a1
git push -u origin a1
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows both branches on local and remote, pointing to the commit "I am a stale main":
```
* ... - (2023-04-02) I am a stale main - Ellis Lempriere  (HEAD -> a1, origin/main, origin/a1, main)
* ... - (2023-04-02) initial commit - Ellis Lempriere
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Branch "a1" has been removed and replaced with archive tag "zArchiveTag_20230601_a1" on both remote and local
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-04-02) I am a stale main - Ellis Lempriere  (HEAD -> main, tag: zArchiveBranch_20230601_a1, origin/main)
        * ... - (2023-04-02) initial commit - Ellis Lempriere
        ```
- Notification of archival of branch "a1" as "zArchiveBranch_20230601_a1" sent to Ellis Lempriere

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-2
    ```
- Delete remote repo on GitLab



## Test Case 3 - Test Excluded Branches Parameter

**Given** a repo with branch "main" and branch "a1" with the most recent commit to "a1" being more than 60 days old  
**When** the program runs with default parameters and excluded branches = "main" and "a1"  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-3"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-3
cd test-case-3
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-3.git
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-01-01 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I am a stale main"
git push -u origin main
git checkout -b a1
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-01-01 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I am the stale excluded branch a1"
git push -u origin a1
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows both branches on local and remote, "main" pointing to the commit "I am a stale main" and "a1" pointing to the commit "I am the stale excluded branch a1":
```
* ... - (2023-01-01) I am the stale excluded branch a1 - Ellis Lempriere  (HEAD -> a1, origin/a1)
* ... - (2023-01-01) I am a stale main - Ellis Lempriere  (origin/main, main)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md  
with new parameters Excluded Branches = "main", "a1"

### Assert
Confirm that:
- No changes have been made to local or remote repo
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-01-01) I am the stale excluded branch a1 - Ellis Lempriere  (HEAD -> a1, origin/a1)
        * ... - (2023-01-01) I am a stale main - Ellis Lempriere  (origin/main, main)
        ```
- No notifications were sent

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-3
    ```
- Delete remote repo on GitLab



## Test Case 4 - Test Branch Archival Warning

**Given** a repo where the branch "a1" has had no commits made in 53 days  
**When** the program runs with default parameters  
**Then** a pending branch archival notification will be sent for "a1"  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-4"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-4
cd test-case-4
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-4.git
echo "hello Ellis" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-09 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "ellis commited me to main"
git push -u origin main
git checkout -b a1
echo "hello John" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-09 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="John Doe <john@mail>" -m "I need to notify my dev about pending archival"
git push -u origin a1
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows both branches on local and remote, "main" pointing to the commit "ellis committed me to main" and "a1" pointing to the commit "I need to notify my dev about pending archival":
```
* ... - (2023-04-09) I need to notify my dev about pending archival - John Doe  (HEAD -> a1, origin/a1)
* ... - (2023-04-09) ellis commited me to main - Ellis Lempriere  (origin/main, main)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- No changes have been made to local or remote repo
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-04-09) I need to notify my dev about pending archival - John Doe  (HEAD -> a1, origin/a1)
        * ... - (2023-04-09) ellis commited me to main - Ellis Lempriere  (origin/main, main)
        ```
- Notification of pending archival of branch "a1" sent to John Doe

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-4
    ```
- Delete remote repo on GitLab



## Test Case 5 - Test Branch Archival

**Given** a repo where the branch "a1" has had no commits made in 60 days  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And the branch "a1" will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-5"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-5
cd test-case-5
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-5.git
echo "hello Ellis" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-02 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "ellis commited me to main"
git push -u origin main
git checkout -b a1
echo "hello John" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-02 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="John Doe <john@mail>" -m "I need to be archived"
git push -u origin a1
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows both branches on local and remote, "main" pointing to the commit "ellis committed me to main" and "a1" pointing to the commit "I need to be archived":
```
* ... - (2023-04-02) I need to be archived - John Doe  (HEAD -> a1, origin/a1)
* ... - (2023-04-02) ellis commited me to main - Ellis Lempriere  (origin/main, main)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Branch "a1" has been removed and replaced with archive tag "zArchiveTag_20230601_a1" on both remote and local
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-04-02) I need to be archived - John Doe  (tag: zArchiveBranch_20230601_a1)
        * ... - (2023-04-02) ellis commited me to main - Ellis Lempriere  (HEAD -> main, origin/main)
        ```
- Notification of archival of branch "a1" sent to John Doe

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-5
    ```
- Delete remote repo on GitLab



## Test Case 6 - Test Archive Tag Deletion Warning

**Given** a repo where the archive tag "zArchiveBranch_20230509_a1" has not had any changes made in 23 days  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And pending archive tag deletion notifications will be sent to the developer of the last commit  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-6"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-6
cd test-case-6
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-6.git
echo "hello Ellis" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-03-10 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "ellis commited me to main"
git push -u origin main
git checkout -b a1
echo "hello John" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-03-10 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="John Doe <john@mail>" -m "I need to notify my dev of pending archive tag deletion"
git push -u origin a1
git checkout main
git tag zArchiveBranch_20230509_a1 a1
git branch a1 -D
git push --prune --all
git push --tags
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows "main" on local and remote pointing to the commit "ellis committed me to main" and the archive tag "zArchiveBranch_20230509_a1" pointing to the commit "I need to notify my dev of pending archive tag deletion":
```
* ... - (2023-03-10) I need to notify my dev of pending archive tag deletion - John Doe  (tag: zArchiveBranch_20230509_a1)
* ... - (2023-03-10) ellis commited me to main - Ellis Lempriere  (HEAD -> main, origin/main)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- No changes have been made
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-03-10) I need to notify my dev of pending archive tag deletion - John Doe  (tag: zArchiveBranch_20230509_a1)
        * ... - (2023-03-10) ellis commited me to main - Ellis Lempriere  (HEAD -> main, origin/main)
        ```
- Notification of pending deletetion of archive tag "zArchiveBranch_20230509_a1" sent to John Doe

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-6
    ```
- Delete remote repo on GitLab



## Test Case 7 - Test Archive Tag Deletion

**Given** a repo where the archive tag "zArchiveBranch_20230502_a1" has not had any changes made in 30 days  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archive tag deletion notifications will be sent  
And the archive tag "zArchiveBranch_20230502_a1" will be deleted

### Arrange
Create remote repo on GitLab named "test-case-7"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-7
cd test-case-7
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-7.git
echo "hello Ellis" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-03-03 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "ellis commited me to main"
git push -u origin main
git checkout -b a1
echo "hello John" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-03-03 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="John Doe <john@mail>" -m "I need to be deleted"
git push -u origin a1
git checkout main
git tag zArchiveBranch_20230502_a1 a1
git branch a1 -D
git push --prune --all
git push --tags
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows the archive tag "zArchiveBranch_20230502_a1" one commit ahead of "main":
```
* ... - (2023-03-03) I need to be deleted - John Doe  (tag: zArchiveBranch_20230502_a1)
* ... - (2023-03-03) ellis commited me to main - Ellis Lempriere  (HEAD -> main, origin/main)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Archive tag "zArchiveBranch_20230502_a1" has been deleted on local and remote
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-03-03) ellis commited me to main - Ellis Lempriere  (HEAD -> main, origin/main)
        ```
- Notification of deletetion of archive tag "zArchiveBranch_20230502_a1" sent to John Doe

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-7
    ```
- Delete remote repo on GitLab



## Test Case 8 - Revived Branch

**Given** a repo where the archive tag "zArchiveTag_20230502_a1" is 30 days old and the branch "a1" is one commit ahead of the archive tag and 5 days old  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archive tag deletion notifications will be sent  
And the archive tag "zArchiveTag_20230502_a1" will be deleted

### Arrange
Create remote repo on GitLab named "test-case-8"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-8
cd test-case-8
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-8.git
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-03-03 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "inital commit"
git push -u origin main
git checkout -b a1
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-03-03 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I will be archived and revived"
git push -u origin a1
git checkout main
git tag zArchiveBranch_20230502_a1 a1
git branch a1 -D
git push --prune --all
git push --tags
git checkout zArchiveBranch_20230502_a1
git checkout -b a1
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-05-27 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I am revivng the branch"
git push -u origin a1
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm the output shows "main" pointing to "initial commit" on both remote and local, the archive tag "zArchiveBranch_20230502_a1" pointing to "I will be archived and revived" on local and remote, and branch "a1" pointing to "I am reviving the branch" on local and remote:
```
* ... - (2023-05-27) I am revivng the branch - Ellis Lempriere  (HEAD -> a1, origin/a1)
* ... - (2023-03-03) I will be archived and revived - Ellis Lempriere  (tag: zArchiveBranch_20230502_a1)
* ... - (2023-03-03) inital commit - Ellis Lempriere  (origin/main, main)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Archive tag "zArchiveBranch_20230502_a1" has been deleted on remote and local and new branch "a1" has been retained with all commits
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-05-27) I am revivng the branch - Ellis Lempriere  (HEAD -> a1, origin/a1)
        * ... - (2023-03-03) I will be archived and revived - Ellis Lempriere
        * ... - (2023-03-03) inital commit - Ellis Lempriere  (origin/main, main)
        ```
- Notification of deletetion of archive tag "zArchiveBranch_20230502_a1" sent to Ellis Lempriere

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-8
    ```
- Delete remote repo on GitLab



## Test Case 9 - Test to ensure no tags collide

**Given** a repo where the archive tag "zArchiveTag_20230428_a1" is 33 days old and the branch "a1" ahead of archive tag by one commit is 30 days old  
**When** the program runs with default parameters and N = 30, M = 60  
**Then** no pending branch archival notifications will be sent  
And the "a1" branch will be archived as "zArchiveTag_20230502_a1"  
And no pending archive tag notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-9"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-9
cd test-case-9
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-9.git
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-03-29 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "inital commit"
git push -u origin main
git checkout -b a1
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-03-29 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I will be archived"
git push -u origin a1
git checkout main
git tag zArchiveBranch_20230428_a1 a1
git branch a1 -D
git push --prune --all
git push --tags
git checkout zArchiveBranch_20230428_a1
git checkout -b a1
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-05-02 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I am revivng the branch and will be archived"
git push -u origin a1
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows the archive tag "zArchiveBranch_20230428_a1" one commit ahead of "main" and one commit behind the new "a1" on both remote and local:
```
* b38ebe5 - (2023-05-02) I am revivng the branch and will be archived - Ellis Lempriere  (HEAD -> a1, origin/a1)
* 53e6d0b - (2023-03-29) I will be archived - Ellis Lempriere  (tag: zArchiveBranch_20230428_a1)
* bf18805 - (2023-03-29) inital commit - Ellis Lempriere  (origin/main, main)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md  
with new parameters N = 30, M = 60

### Assert
Confirm that:
- Branch "a1" has been archived as "zArchiveBranch_20230601_a1" on both remote and local
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-05-02) I am revivng the branch and will be archived - Ellis Lempriere  (tag: zArchiveBranch_20230601_a1)
        * ... - (2023-03-29) I will be archived - Ellis Lempriere  (tag: zArchiveBranch_20230428_a1)
        * ... - (2023-03-29) inital commit - Ellis Lempriere  (HEAD -> main, origin/main)
        ```
- Notification archival of branch "a1" as "zArchiveBranch_20230601_a1" sent to Ellis Lempriere

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-9
    ```
- Delete remote repo on GitLab



## Test Case 10 - Rebase Back to Main

**Given** a repo where the branch "a1" was created on main and commited to then when "a1" is 60 days old it is rebased onto main which has one commit since branching  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-10"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-10
cd test-case-10
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-10.git
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-02 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "inital commit"
git push -u origin main
git checkout -b a1
echo "hello" > foo2.txt
git add foo2.txt
export GIT_AUTHOR_DATE="2023-04-02 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I will be rebased"
git push -u origin a1
git checkout main
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-06-01 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I will be rebased onto"
git push origin main
git rebase --ignore-date main a1
git push -f origin a1
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows the branch "a1" one commit ahead of "main" both locally and on remote:
```
* ... - (2023-06-01) I will be rebased - Ellis Lempriere  (HEAD -> a1, origin/a1)
* ... - (2023-06-01) I will be rebased onto - Ellis Lempriere  (origin/main, main)
* ... - (2023-04-02) inital commit - Ellis Lempriere
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- No changes have been made
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-06-01) I will be rebased - Ellis Lempriere  (HEAD -> a1, origin/a1)
        * ... - (2023-06-01) I will be rebased onto - Ellis Lempriere  (origin/main, main)
        * ... - (2023-04-02) inital commit - Ellis Lempriere
        ```
- No notifications were sent

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-10
    ```
- Delete remote repo on GitLab



## Test Case 11 - Amend to Prevent Archival

**Given** a repo where the branch "a1" is 60 days old and the developer amend committed to it   
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-11"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-11
cd test-case-11
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-11.git
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-02 11:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "inital commit"
git push -u origin main
git checkout -b a1
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-02 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I will be archived"
git push -u origin a1
export GIT_AUTHOR_DATE="2023-06-01 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --amend -m "commit to prevent archival"
git push -f origin a1
```

Run:
```
git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
```
Confirm output shows the branch "a1" one commit ahead of "main" on remote and local:
```
* ... - (2023-06-01) commit to prevent archival - Ellis Lempriere  (HEAD -> a1, origin/a1)
* ... - (2023-04-02) inital commit - Ellis Lempriere  (origin/main, main)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- No changes have been made
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-06-01) commit to prevent archival - Ellis Lempriere  (HEAD -> a1, origin/a1)
        * ... - (2023-04-02) inital commit - Ellis Lempriere  (origin/main, main)
        ```
- No notifications were sent

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-11
    ```
- Delete remote repo on GitLab



## Test Case 12 - New Remote Repo To Download

**Given** a new remote repo, with just the initial commit on "main", the program needs to run on  
**When** the program runs for the first time and with default parameters  
**Then** a new local repo will be created before the scanning of the repo starts
And no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-12"  

Execute the following in a bash shell:
```
cd "C:\Users\ellis\Documents\repos"
mkdir test-case-12
cd test-case-12
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-12.git
echo "hello" > foo.txt
git add foo.txt
git commit -m "initial commit"
git push -u origin main
cd "C:\Users\ellis\Documents\repos"
rm -rf test-case-12
```

- Confirm that
    - Remote repo exists on GitLab with the name "test-case-12" has one commit "initial commit"
    - Local repo does not exist
        ```
        dir
        ```
        - "test-case-12" should not appear in the output

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Local and remote repos exist and are up to date
    ```
    git status
    git pull
    ```
- Repo is in expected state
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-06-01) initial commit - Ellis Lempriere  (HEAD -> main, origin/main, origin/HEAD)
        ```

### Test Case Reset Instructions
- Once the remote repo has been created in GitLab it should not need to be reset as no changes are being made to the repo
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-12
    ```
- When re-running the test case, if the remote repo still exists, just do confirmation in the arrange section



## Test Case 13 - Local And Remote Do Not Exist

**Given** no repo exists locally or remotely  
**When** the program runs with default parameters and for the first time  
**Then** a new empty repo will be created locally and remotely  

### Arrange
- Confirm that
    - Remote repo with the name "test-case-13" does not exist in GitLab
    - Local repo does not exist
        ```
        cd "C:\Users\ellis\Documents\repos"
        dir
        ```
        - "test-case-13" should not appear in the output

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Remote repo exists and is empty
- Local repo exists and is also empty
    ```
    git status
    git pull
    ```

### Test Case Reset Instructions
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-13
    ```
- Delete remote repo on GitLab



## Test Case 14 - Local Directory Is Not A Repo

**Given** a remote repo exists with branch "main" and an initial commit and a local directory exists to be a repo, but is not initialized as one  
**When** the program runs with default parameters  
**Then** the local directory will become a repo for the program to run on  
And no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-14"  

Execute the following in a bash shell:
```
mkdir test-case-14
cd test-case-14
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-14.git
echo "hello" > foo.txt
git add foo.txt
git commit -m "initial commit"
git push -u origin main
cd "C:\Users\ellis\Documents\repos"
rm -rf test-case-14
mkdir test-case-14
```

- Confirm that
    - Remote repo with the name "test-case-14" exists in GitLab with "main" branch pointing to "initial commit"
    - Local folder is not a git repo
        ```
        cd "C:\Users\ellis\Documents\repos\test-case-14"
        ```
        - "test-case-14" should not appear as a git repo

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Local and remote repos exist and are up to date
    ```
    git status
    git pull
    ```
- Repo is in expected state
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-06-01) initial commit - Ellis Lempriere  (HEAD -> main, origin/main)
        ```

### Test Case Reset Instructions
- Once the remote repo has been created in GitLab it should not need to be reset as no changes are being made to the repo
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-14
    mkdir test-case-14
    ```
- When re-running the test case, if the remote repo still exists, just do confirmation in the arrange section



## Test Case 15 - Local Repo Unlinked To Remote

**Given** a remote repo exists with branch "main" and an initial commit and a local repo exists but they are unlinked  
**When** the program runs with default parameters  
**Then** the local repo will be linked before the program runs to clean the repo 
And no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Create remote repo on GitLab named "test-case-15"  

Execute the following in a bash shell:
```
mkdir test-case-15
cd test-case-15
git init --initial-branch=main
git remote add origin https://gitlab.com/EllisLempriere/test-case-15.git
echo "hello" > foo.txt
git add foo.txt
git commit -m "initial commit"
git push -u origin main
cd "C:\Users\ellis\Documents\repos"
rm -rf test-case-15
mkdir test-case-15
cd test-case-15
git init --initial-branch=main
```

- Confirm that
    - Remote repo with the name "test-case-15" exists in GitLab with "main" branch pointing to "initial commit"
    - Local folder is an unlinked git repo
        ```
        git remote
        ```
        - Should be no output

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Local and remote repos exist and are up to date
    ```
    git status
    git pull
    ```
- Repo is in expected state
    ```
    git log --graph --abbrev-commit --decorate --format=format:"%h - (%cs) %s - %an %d" --all
    ```
    - Expected output:
        ```
        * ... - (2023-06-01) initial commit - Ellis Lempriere  (HEAD -> main, origin/main)
        ```

### Test Case Reset Instructions
- Once the remote repo has been created in GitLab it should not need to be reset as no changes are being made to the repo
- Execute following commands to clear local repo
    ```
    cd "C:\Users\ellis\Documents\repos"
    rm -rf test-case-15
    mkdir test-case-15
    cd test-case-15
    git init --initial-branch=main
    ```
- When re-running the test case, if the remote repo still exists, just do confirmation in the arrange section