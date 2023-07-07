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
Execute the following in a bash shell:
```
mkdir test-case-1
cd test-case-1
git init
```

Confirm output something like:
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



## Test Case 2 - New Empty Branch on Stale Main

**Given** a repo where the most recent commit on main was made
more than 60 days ago and a new branch is on the tip of main  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And the new branch will be archived  
And no pending archival tag notifications will be sent  
And no archive tags will be deleted

### Arrange
Execute the following in a bash shell:
```
mkdir test-case-2
cd test-case-2
git init
git branch -M main
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-02 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "last commit on main"
git checkout -b a1
```

Confirm output something like:
```
Initialized empty Git repository in .../repos/test-case-2/.git/

[main (root-commit) ...] last commit on main
 1 file changed, 1 insertion(+)
 create mode 100644 foo.txt

Switched to a new branch 'a1'
```
***TODO: Make sure both branches exist and that they both point to the same commit - commit tree command for character pictorial representation***

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Branch list contains "main"
```
git branch
```
- Tag "20230601220000_a1" exists
```
git tag
```
- Notification of archival of branch "a1" as "20230601220000_a1" pending to be sent to correct devs



## Test Case 3 - Test Excluded Branches Parameter

**Given** a repo with a "main" branch and a branch called "a1" with the most recent commit being more than 60 days old  
**When** the program runs with default parameters and excluded branches = "main" and "a1"  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Execute the following in a bash shell:
```
mkdir test-case-3
cd test-case-3
git init
git branch -M main
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-01-01 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "main1"
git checkout -b a1
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-02-01 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "a1-1"
```

Confirm output something like:
```
Initialized empty Git repository in .../repos/test-case-3/.git/

[main (root-commit) ...] main1
 1 file changed, 1 insertion(+)
 create mode 100644 foo.txt

Switched to a new branch 'a1'

[main ...] a1-1
 1 file changed, 1 insertion(+)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md  
with new parameters Excluded Branches = "main", "a1"

### Assert
Confirm that:
- Branch list contains "main" and "a1"
```
git branch
```
- No tags exist
```
git tag
```
- No notifications are pending being sent



## Test Case 4 - Test Branch Archival Warning

**Given** a repo where the branch "a1" has had no commits made in 53 days  
**When** the program runs with default parameters  
**Then** a pending branch archival notification will be sent for "a1"  
And no branches will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Execute the following in a bash shell:
```
mkdir test-case-4
cd test-case-4
git init
git branch -M main
echo "hello Ellis" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-06 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "ellis commit"
echo "hello Frank" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-07 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="Frank Joe <frank@mail>" -m "frank commit"
git checkout -b a1
echo "hello John" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-08 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="John Doe <john@mail>" -m "john commit"
echo "hello Jane" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-04-09 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="Jane Doe <jane@mail>" -m "jane commit"
```

Confirm output something like:
```
Initialized empty Git repository in .../repos/test-case-4/.git/

[main (root-commit) ...] ellis commit
 1 file changed, 1 insertion(+)
 create mode 100644 foo.txt

[main ...] frank commit
 Author: Frank Joe <frank@mail>
 1 file changed, 1 insertion(+)

Switched to a new branch 'a1'

[a1 ...] john commit
 Author: John Doe <john@mail>
 1 file changed, 1 insertion(+)

[a1 ...] jane commit
 Author: Jane Doe <jane@mail>
 1 file changed, 1 insertion(+)
```

### Act
Execute the steps in ManualMaintenanceInstructions.md

### Assert
Confirm that:
- Branch list contains "main" and "a1"
```
git branch
```
- No tags exist
```
git tag
```
- Notification of archival emails pending to send to Frank, John, and Jane



## Test Case 5 - Test Branch Archival

**Given** a repo where the branch "a1" has had no commits made in 60 days  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And the branch "a1" will be archived  
And no pending archival tag deletion notifications will be sent  
And no archive tags will be deleted

### Arrange
Execute the following in a bash shell:
```
# !! Run test case 4 creation commands !!
cd ..
mkdir test-case-5
git clone -l "<test-case-4 path>" "<test-case-5 path>"
cd test-case-5
```

Confirm output something like:
```
Test case 4 output

Cloning into '...\test-case-5'...
done.
```

### Act
Execute the steps in ManualMaintenanceInstructions.md  
with new parameters Execution Date = 2023-06-08

### Assert
Confirm that:
- Branch list contains "main"
```
git branch
```
- Tag "20230608220000_a1" exists
```
git tag
```
- Notification of archival of branch "a1" as "20230608220000_a1" pending to be sent to Frank, John, and Jane



## Test Case 6 - Test Archive Tag Deletion Warning

**Given** a repo where the archive tag "20230608220000_a1" has not had any changes made in 23 days  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And pending archive tag deletion notifications will be sent to the developers of the last 3 commits  
And no archive tags will be deleted

### Arrange
Execute the following in a bash shell:
```
# !! Run test case 4 creation commands !!
cd ..
mkdir test-case-6
git clone -l "<test-case-4 path>" "<test-case-6 path>"
cd test-case-6
git checkout main
git tag 20230608220000_a1 a1
git branch a1 -D
```

Confirm output something like:
```
Test case 4 output

Cloning into '...\test-case-6'...
done.

Switched to a new branch 'main'
branch 'main' set up to track 'origin/main'.

Deleted branch a1 (was ...).
```

### Act
Execute the steps in ManualMaintenanceInstructions.md  
with new parameters Execution Date = 2023-07-01

### Assert
Confirm that:
- Branch list contains "main"
```
git branch
```
- Tag "20230608220000_a1" exists
```
git tag
```
- Notification of pending deletion of archive tag "20230608220000_a1" pending to be sent to Frank, John, and Jane



## Test Case 7 - Test Archive Tag Deletion

**Given** a repo where the archive tag "20230608220000_a1" has not had any changes made in 30 days  
**When** the program runs with default parameters  
**Then** no pending branch archival notifications will be sent  
And no branches will be archived  
And no pending archive tag deletion notifications will be sent  
And the archive tag "20230608220000_a1" will be deleted

### Arrange
Execute the following in a bash shell:
```
# !! Run test case 4 creation commands !!
cd ..
mkdir test-case-7
git clone -l "<test-case-4 path>" "<test-case-6 path>"
cd test-case-7
git checkout main
git tag 20230608220000_a1 a1
git branch a1 -D
```

### Act
Execute the steps in ManualMaintenanceInstructions.md  
with new parameters Execution Date = 2023-07-08

### Assert
Confirm that:
- Branch list contains "main"
```
git branch
```
- No tags exist
```
git tag
```
- Notification of deletion of archive tag "20230608220000_a1" pending to be sent to Frank, John, and Jane

***GENERAL TODOS***
- Fix code block tabbing
- Make commit messages more informative
- Split up test cases being dependent on each other
- Any other todos in rest of doc