# Gurt

Gurt is a git replication written in Java with SHA 256 hashing for reduced collision risk. Designed for educational and experimental purposes.
Symlinks are not yet supported; avoid them when testing.

## Features

- `init`
- `add`
- `commit`
- `write-tree`
- `branch`
- `checkout`
- `log`
- `status`
- `merge` 


## Build

```bash
./gradlew clean build
```
Then add the following line to your shell config (e.g., .bashrc, .zshrc, etc.):
alias gurt='java -jar /absolute/path/to/build/libs/gurt.jar'


## Usage
Run commands in the following format:

bash
Copy
Edit
gurt <command>
Available Commands:
gurt init
Initialize a new Gurt repository.

gurt add <files/directories>
Stage files or directories for commit.

gurt commit "message"
Commit staged changes with a message.

gurt write-tree
Serialize the current state of the staging area into a tree object.

gurt branch
List all branches.

gurt branch <branch-name>
Create a new branch.

gurt log
Show commit history.

gurt status
Show current status of working directory.

gurt checkout <branch-name>
Switch to the specified branch.

gurt checkout <commit-hash>
Switch to a specific commit state and detach HEAD.

