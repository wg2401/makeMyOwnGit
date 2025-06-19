gurt: yo


This is a git replication that handles the following methods: init, add, commit, write-tree, branch, checkout, log, status, and merge.
Implements SHA 256 hashing for reduced chance of file collision
Note: does not support symlinks yet; be careful not to use them when testing!

Instructions for build: 
After cloning, run  ./gradlew clean build
Then add this line to your shell config: alias gurt='java -jar /absolute/path/to/build/libs/gurt.jar'

To use: run <gurt command>
    gurt init
    gurt add <files/directories>
    gurt commit "message"
    gurt write-tree
    gurt branch - lists all branches
    gurt branch <branch name> - creates a new branch
    gurt log
    gurt status
    gurt checkout <branch name> - switches repo to specified branch
    gurt checkout <commit hash> - switches to specified commit state and detaches HEAD
