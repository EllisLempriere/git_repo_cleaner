# Getting Started
Instructions to install and set up to run basic end-to-end test

## Prerequisites
- Java JDK 20
- IntelliJ
- Git client
- Git host account (eg. Gitlab, Github)

## Instructions
### Setup Project
- Clone repo to local
- In Intellij, open project ~/git-cleaning-mvp
- Build project, confirm no errors
- Run tests in ~/src/test/java/UnitTests, confirm all tests pass

### Setup Test
- Create two remote repos on git host
- Run ~/git_repo_cleaner/ManualTests/integration-test-repo-setup.sh
- Fork each remote repo
- Create a secrets.properties file with valid secrets outside the project (see *Example Secrets* section below)
- Edit the following fields in ~/ManualTests/integration-test-config.json
    - "config_secrets" -> file path of secrets.properties file from previous step
    - Under "repos" for both objects change the following fields
        - "directory" -> file path of test directory created by setup script
        - "remote_uri: -> uri of linked remote *fork* on git host
#### Example Secrets
    ```
    # Example user secrets
    username=user
    password=pass
    ```

### Run Test
- In IntelliJ, open ~/src/main/java/Application/Main
- Right-click on run arrow and select Modify Run Configurations
- In 'program arugments' box, enter "-1 ~/git_repo_cleaner/ManualTests/integration-test-config.json" (make sure to fill in full file path)
- Select OK in dialouge box
- Run Main
- Log file will appear in project folders as DDMMYYYYLog-#.log