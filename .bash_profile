gitCheckout(){
	git checkout -b $1
}

gitCheckoutMerge(){
	BRANCH=`git rev-parse --abbrev-ref HEAD`
	git checkout master
	git pull
	git merge ${BRANCH}
	git branch -d ${BRANCH}
}
gitCommit(){
	git commit -m "$1"
}
alias branch=gitCheckout
alias branch?="git branch"
alias diff="git diff --color"
alias status="git status"
alias add="git add -A"
alias commit=gitCommit
alias merge=gitCheckoutMerge