#!/usr/bin/env bash
#!/bin/bash

showAllHelp(){
	echo 'usage: git [-help] [-branch] [-status] [-push] [-pull] [-diff] [foreach <command>] [sync]'
	echo '-----------------------------------------------------------------------------------------'
	echo  "git-based commands, easy to use"
	printf  "  %-20s %-10s \n" '-branch:' 'show the current branch of all submodules.'
	printf  "  %-20s %-10s \n" '-status:' 'show git status of all submodules.'
	printf  "  %-20s %-10s \n" '-pull:' 'pull and rebase the latest code of all submodules.'
	printf  "  %-20s %-10s \n" '-diff:' 'show local changes for all submodules.'
	echo  ""
	echo  "use custom command of all submodules"
	printf  "  %-20s %-10s \n" 'foreach <command>:' 'execute the specified command of all submodules.'
	echo  ""
	echo  "source and publish command"
	printf  "  %-20s %-10s \n" 'sync' 'sync all submodules to specified devbranch.'
	echo "for more help, please see: https://github.com/Ryan-Shz/Repo"
}

if [ $# -eq 0 ];
then
	showAllHelp
	exit 1	
fi

args=$1
execCmd=${@:2}
subArgs=$2
argsList=$@
currPath=""

refreshCurrPath(){
	if [ "$(uname)"=="Darwin" ]
	then
		currPath=$(pwd)
	else 
		currPath=$(dirname $(readlink -f $0))
	fi
}

printBranch(){
	cd submodules
	refreshCurrPath
	path=$currPath 
	files=$(ls $path)
	for fileName in $files
	do
		if [ ! -d $fileName ]
		then
			continue
		fi
		cd $fileName
		branch=$(git rev-parse --abbrev-ref HEAD)
		echo $fileName': '$branch
		cd ..
	done
}

printAllStatus(){
	cd submodules
	refreshCurrPath
	path=$currPath 
	files=$(ls $path)
	for fileName in $files
	do
		if [ ! -d $fileName ]
		then
			continue
		fi
		cd $fileName
		echo -e "\033[33mEntering $fileName\033[0m"
		git status
		cd ..
	done
}

printAllDiff(){
	cd submodules
	refreshCurrPath
	path=$currPath 
	files=$(ls $path)
	for fileName in $files
	do
		if [ ! -d $fileName ]
		then
			continue
		fi
		cd $fileName
		echo -e "\033[33mEntering $fileName\033[0m"
		git diff
		cd ..
	done
}

mergeAll(){
	cd submodules
	refreshCurrPath
	path=$currPath
	files=$currPath
	for fileName in $files
	do
		if [ ! -d $fileName ]
		then
			continue
		fi
		cd $fileName
		echo -e "\033[33mEntering $fileName\033[0m"
		git pull --rebase
		cd ..
	done
}

pullRebase(){
	cd submodules
	refreshCurrPath
	path=$currPath 
	files=$(ls $path)
	for fileName in $files
	do
		if [ ! -d $fileName ]
		then
			continue
		fi
		cd $fileName
		echo -e "\033[33mEntering $fileName\033[0m"
		git pull --rebase
		cd ..
	done
}

foreach(){
	if [ ! -d "submodules" ]
	then
		return
	fi
	cd submodules
	refreshCurrPath
	path=$currPath 
	files=$(ls $path)
	for fileName in $files
	do
		if [ ! -d $fileName ]
		then
			continue
		fi
		cd $fileName
		echo -e "\033[33mEntering $fileName\033[0m"
		$execCmd
		cd ..
	done
}

syncModules(){
	chmod +x ./gradlew
	if [ '-c' = $subArgs ]
	then
		./gradlew -Psync=true -PnoBuild=true -PdevBranch=false cmd
	else
		./gradlew -Psync=true -PnoBuild=true -PdevBranch=true cmd
	fi
}

args_contains () {
    seeking=$1
	inArray=$argsList
	for element in $inArray
	do
		if [ $element == $seeking ];
		then
			return 1
		fi
	done
	return 0
}

string_contains(){
	seeking=$1
	inString=$2
	result=$(echo $inString | grep "${seeking}")
	if [[ "$result" != "" ]]
	then
		return 1
	fi
	return 0
}

case $args in 
	"-help") showAllHelp
	;;
	"-h") showAllHelp
	;;
	"-branch") printBranch
	;;
	"foreach") foreach 
	;;
	"-pull") pullRebase
	;;
	"-status") printAllStatus
	;;
	"-diff") printAllDiff
	;;
	"-merge") mergeAll
	;;
	"sync") syncModules
	;;
	*)
		echo "submodules: '$args' is not a submodules command. See ./submodules.sh -help."
		exit 1
	;;
esac
