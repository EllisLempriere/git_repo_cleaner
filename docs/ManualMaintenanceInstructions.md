# Manual Git Maintenace Instructions

## Parameters
### Definitions
**N**: Number of days without commits before a branch is stale.  
**M**: Number of days without commits before an archive tag is stale.  
**K**: Number of days without commits before a branch or archive tag is stale a developer is notified of pending archival or deletion. 0 <= K < N & M  
**X**: Number of previous commits to read for authors to send notifications to.  
**Excluded Branches**: The list of branch names to exclude in archival.  
**Execution Date**: The date at which the program is being run.

### We will assume
Unless otherwise stated, parameters are 
- N = 60
- M = 30
- K = 7
- X = 3
- Excluded Branches = "main"
- Execution Date = 06/01/2023

**All instructions to be done in a git bash command shell**

## Phase 1 - Aquire up-to-date repo
- TBD


## Phase 2 - Clean Repo

### Step 1
- Set up environment variables - change values to parameters
```
export N=60
export M=30
export K=7
export X=3
export EXE_DATE=$(date --date="2023-06-01 22:00:00")
```

### Step 2
- Change directory to root of repo to be maintained
```
cd <repo>
```

### Step 3
- Get list of branches using the following command:
    ```
    git branch
    ```
- If the list is empty jump to step 5

### Step 4
- For each branch in the list from step 3
    - If the branch is in excluded branches list, ignore it and go to the next branch
    - Else establish branch name variable
        ```
        branch_name=<branch name>
        ```
    - Check the date of the last commit and compare to current date
        ```
        export COMMIT_DATE=$(git show -s --pretty=format:'%cD' $branch_name)
        start_date=$(date --date="$COMMIT_DATE" '+%s')
        end_date=$(date --date="$EXE_DATE" '+%s')
        num_days=$(((end_date-start_date)/86400))
        echo "$num_days days since commit"
        ```
    - If number of days since last commit = 53 (N - K)
        - Get last 3 (X) commits developers emails
            ```
            git log -n $X --format='%ae' $branch_name
            ```
        - Add to list of notification emails to send
        (TODO: Information to go in email and formatting)
    - If number of days since last commit >= 60 (N)
        - Get last 3 (X) commits developers emails
            ```
            git log -n $X --format='%ae' $branch_name
            ```
        - Add to list of notification emails to send
        (TODO: Information to go in email and formatting)
        - Create new archive tag and delete stale branch
            ```
            exe_date=$(date --date="$EXE_DATE" +%Y%m%d%H%M%S)
            tag_name="${exe_date}_${branch_name}"
            git tag $tag_name $branch_name
            git branch $branch_name -D
            ```
***TODO: Edit archive tag name formatting: zArchiveBranch_YYYYMMDD_a1***

### Step 5
- Get list of tags using the following command:
```
git tag
```
- If the list is empty move to phase 3

### Step 6
- For each tag in the list from step 5
    - If the tag is not of the form YYYYMMDDHHMMSS_branch-name, it is not a archive tag, ignore it and go to the next tag
```
tag_name=<tag name>
```
- Check the date of the last commit and compare to current date
```
export COMMIT_DATE=$(git show -s --pretty=format:'%cD' $tag_name)
start_date=$(date --date="$COMMIT_DATE" '+%s')
end_date=$(date --date="$EXE_DATE" '+%s')
num_days=$(((end_date-start_date)/86400))
echo "$num_days days since commit"
```
- If N + M - K days ago
    - Get last X commits developers emails
    ```
    git log -n $X --format='%ae' $tag_name
    ```
    - Add to list of notification emails to send
    (TODO: Information to go in email and formatting)
- If N + M days ago
    - Get last X commits developers emails
    ```
    git log -n $X --format='%ae' $tag_name
    ```
    - Add to list of notification emails to send
    (TODO: Information to go in email and formatting)
    - Delete archive tag
    ```
    git tag $tag_name -d
    ```
    



## Phase 3 - Publish Cleaning Changes
- TBD


***GENERAL TODOS***
- Fix code block tabbing
- Send notifications as you go
- Research into who created a branch/when/who made first commit
- Use just most recent comitter for emails (for now)
- Replicate changes made to step 4 to step 6
- Any other todos in rest of doc