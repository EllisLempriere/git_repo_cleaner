# Manual Program Running Instructions

## Setting up to test program from a zipped repo(s)
- Unzip repo(s) to known and accessible directory
- Create remote project(s)
- Link local repo(s) to remote project(s) and push repo to remote
- Fork remote projects - use initially created project as static backup
- Create config and secrets file (See *Example Config* and *Example Secrets* sections)
- Run program from command line or within IDE
    - Parameters:
        - Execution time: Specify execution time in epoch seconds (negative number for current time)
        - Config file path: Specify the location of the config file
- Log file will output in project folder
- Expected changes should be reflected in repo

## Example Config - File: test_config.json
```
{
  "retries": 3,
  "config_secrets": "example_secrets.properties",

  "repos": [
    {
      "directory": "<directory of repo to be cleaned>",
      "remote_uri": "<clone uri for remote>",
      "excluded_branches": ["main"],
      "stale_branch_inactivity_days": 60,
      "stale_tag_days": 30,
      "notification_before_action_days": 7,
      "recipients": [john-doe@example.com]
    }
  ]
}
```
Config pieces:
- retries - Number of times to retry connection based operations
- config_secrets - Location of secrets.properties file
    - Must be a valid file with *both* username and password
    - Must be a '.properties' typed file
- repos - List of all repos to be cleaned
    - directory - The local location of the repo to be cleaned
        - Does not need to already contain the repo, program will create one if not found
    - remote_uri - The uri obtained from Git host 'clone' instructions
    - excluded_branches - List of branches to be excluded from cleaning process
        - Strongly recommended to always have trunk branch contained here
    - stale_branch_inactivity_days - Number of days before a branch is declared stale and archived
    - stale_tag_days - Number of days before an archive tag is declared stale and deleted
    - notification_before_action_days - Number of days before a given action developers are notified about the pending action
    - recipients - List of emails to always send cleaning notifications to

## Example Secrets - File: example_secrets.properties
```
# Example user secrets
username=user
password=pass
```