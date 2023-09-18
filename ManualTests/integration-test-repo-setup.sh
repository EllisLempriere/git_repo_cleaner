read -r -p "Enter parent directory for repos (must exist): " parentRepoDir
read -r -p "Enter remote repo uri for 1st repo: " repo1Remote
read -r -p "Enter remote repo uri for 2nd repo: " repo2Remote
#-----------------------------------------------------------------------------------
cd $parentRepoDir
mkdir test-repo-1
cd test-repo-1
git init --initial-branch=main
git remote add origin $repo1Remote
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-01-01 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "initial commit"
git push -u origin main
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-01-01 12:05:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "primary branch commit"
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-01-01 12:10:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "stale main"
git checkout -b emptyStaleBranch
git push -u origin emptyStaleBranch
git tag notAnArchiveTag main
git checkout main^
git branch doNothing
git push -u origin doNothing
git branch notifyPendingArchive
git push -u origin notifyPendingArchive
git branch archive
git push -u origin archive
git branch notifyPendingDelete
git branch delete
git checkout doNothing
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-1 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "branch not stale"
git checkout notifyPendingArchive
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-53 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "dev will be notified of pending archival"
git checkout archive
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-60 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "branch will be archived"
git checkout notifyPendingDelete
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-83 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "dev will be notified of pending deletion"
git checkout delete
git tag zArchiveBranch_$(date --date='-23 day' '+%Y%m%d')_notifyPendingDelete notifyPendingDelete
git branch -D notifyPendingDelete
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-90 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "tag will be deleted"
git checkout main
git tag zArchiveBranch_$(date --date='-30 day' '+%Y%m%d')_delete delete
git branch -D delete
git push --all
git push --tags
#-----------------------------------------------------------------------------------
cd $parentRepoDir
mkdir test-repo-2
cd test-repo-2
git init --initial-branch=main
git remote add origin $repo2Remote
echo "hello" > foo.txt
git add foo.txt
export GIT_AUTHOR_DATE="2023-01-01 12:00:00"
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "initial commit"
git push -u origin main
echo "main1" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-120 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="Fred Doe <fred@mail>" -m "1st commit to main"
git checkout -b exclude
echo "hello" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-100 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit -m "I am stale but excluded"
git checkout main
git push -u origin exclude
git checkout -b merge
echo "merge1" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-90 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="John Doe <john@mail>" -m "1st commit to merge"
echo "merge2" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-80 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="John Doe <john@mail>" -m "2nd commit to merge"
git checkout main
git push -u origin merge
echo "main2" >> foo.txt
git add foo.txt
export GIT_AUTHOR_DATE=$(date --date='-40 day' '+%F %T')
export GIT_COMMITTER_DATE=$GIT_AUTHOR_DATE
git commit --author="Jane Doe <jane@mail>" -m "2nd commit to main"
git merge merge
git add foo.txt
git commit -m "merged merge into main"
git push --all