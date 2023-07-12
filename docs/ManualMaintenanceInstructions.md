# Manual Git Maintenace Instructions

## Parameters
### Definitions
**N**: Number of days without commits before a branch is stale.  
**M**: Number of days without commits before an archive tag is stale.  
**K**: Number of days without commits before a branch or archive tag is stale a developer is notified of pending archival or deletion. 0 <= K < N & M  
**X**: Number of previous commits to read for authors to send notifications to.  
**Excluded Branches**: The list of branch names to exclude in archival.  
**Execution Date**: The date at which the program is being run.
**File Path**: The file path to the local repo on the machine.
**Remote URL**: The URL linking the remote repo.

### We will assume
Unless otherwise stated, parameters are 
- N = 60
- M = 30
- K = 7
- X = 3
- Excluded Branches = "main"
- Execution Date = 2023-06-01
- File Path = C:\Users\ellis\Documents\repos\test-case-(test case num)
- Remote URL = https://gitlab.com/EllisLempriere/test-case-(test case num).git

**All instructions to be done in a git bash command shell**

## Phase 1 - Aquire up-to-date repo

### Step 1
- Set up environment variables - change values to parameters
    ```
    # !! Enter test case number before running rest of commands !!
    export CASE_NUM=

    export REMOTE_URL="https://gitlab.com/EllisLempriere/test-case-$CASE_NUM.git"
    export FILE_PATH="C:\Users\ellis\Documents\repos\test-case-$CASE_NUM"
    export EXE_DATE=$(date --date="2023-06-01 22:00:00")
    ```

### Step 2
- Check if the test repo exists locally
    ```
    cd $FILE_PATH
    ```
    - If directory is not changed into file path
        ```
        cd "C:\Users\ellis\Documents\repos"
        git clone $REMOTE_URL
        cd $FILE_PATH
        ```
        - If the remote repo does not exist
            - Create a new repo on GitLab with naming "test-case-(test number)"
            - Create local repo and link to remote
                ```
                mkdir test-case-$CASE_NUM
                cd $FILE_PATH
                git init --initial-branch=main
                git remote add origin $REMOTE_URL
                ```
    - Else update local repo from remote
        ```
        git pull origin main
        # Currently assuming ALL repos this runs on will have their trunk branch named main.
        # Will this be a requirement of those using the program or can this be flexible?
        ```
        - If the remote does not have a main branch, it is empty, carry on to phase 2
        - Else if the repo is up to date, carry on to phase 2
        - Else If the directory is not a git repo
            ```
            git init --initial-branch=main
            git remote add origin $REMOTE_URL
            git pull origin main
            ```
        - Else if the repo is not linked, link repo and update
            ```
            git remote add origin $REMOTE_URL
            git pull origin main
            ```
            - If remote repo is empty, carry on to phase 2



## Phase 2 - Clean Repo

### Step 1
- Get list of branches using the following command:
    ```
    git branch
    ```
- If the list is empty jump to step 3

### Step 2
- For each branch in the list from step 1
    - If the branch is in excluded branches list, ignore it and go to the next branch
    - Else establish branch name variable
        ```
        # !! Enter branch name before running rest of commands !!
        branch_name=
        ```
    - Check the date of the last commit and compare to current date
        ```
        commit_date=$(git show -s --pretty=format:'%cD' $branch_name)
        start_date=$(date --date="$commit_date" '+%s')
        end_date=$(date --date="$EXE_DATE" '+%s')
        num_days=$(((end_date-start_date)/86400))
        echo "$num_days days since commit"
        ```
    - If number of days since last commit = 53 (N - K)
        - Get last commiter's email (temporarily only last commiter)
            ```
            git log -n 1 --format='%ae' $branch_name
            ```
        - Send notification of pending deletion
    - If number of days since last commit >= 60 (N)
        - Get last commiter's email (temporarily only last commiter)
            ```
            git log -n 1 --format='%ae' $branch_name
            ```
        - Send notification of archival
        - Create new archive tag and delete stale branch
            ```
            exe_date=$(date --date="$EXE_DATE" +%Y%m%d)
            tag_name="zArchiveBranch_${exe_date}_${branch_name}"
            git tag $tag_name $branch_name
            git checkout main
            git branch $branch_name -D
            ```

### Step 3
- Get list of tags using the following command:
    ```
    git tag
    ```
- If the list is empty move to phase 3

### Step 4
- For each tag in the list from step 3
    - If the tag is not formatted like "zArchiveBranch_(YYYYMMDD)_(branchName), it is not a archive tag, ignore it and go to the next tag
    - Else establish branch name variable
        ```
        # !! Enter tag name before running rest of commands !!
        tag_name=
        ```
    - Check the date of the last commit and compare to current date
        ```
        commit_date=$(git show -s --pretty=format:'%cD' $tag_name)
        start_date=$(date --date="$commit_date" '+%s')
        end_date=$(date --date="$EXE_DATE" '+%s')
        num_days=$(((end_date-start_date)/86400))
        echo "$num_days days since commit"
        ```
    - If number of days since last commit = 83 (N + M - K)
        - Get last commiter's email (temporarily only last commiter)
            ```
            git log -n 1 --format='%ae' $tag_name
            ```
        - Send notification of pending archive tag deletion
    - If number of days since last commit >= 90 (N + M)
        - Get last commiter's email (temporarily only last commiter)
            ```
            git log -n 1 --format='%ae' $tag_name
            ```
        - Send notification of archive tag deletion
        - Delete archive tag
            ```
            git tag $tag_name -d
            ```
    


## Phase 3 - Publish Cleaning Changes
- If steps 2 and 4 in phase 2 were skipped, skip this phase
- Push changes made to repo to remote
    ```
    git push --prune --all
    git push --tags
    ```
- For each archive tag deleted
    ```
    git push --delete origin $tag_name
    ```
