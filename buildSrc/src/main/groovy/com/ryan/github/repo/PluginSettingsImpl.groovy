package com.ryan.github.repo

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings

class PluginSettingsImpl implements Plugin<Settings> {

    private static String SSH_USER_NAME
    private static final def CONFIG_FILE = 'local.properties'
    private static final def MODULES_DIR = []
    private static final def CMD_PROPERTIES_SYNC = 'sync'
    private static final def CMD_PROPERTIES_RELEASE = 'release'
    private static final def CMD_PROPERTIES_DEV_BRANCH = 'devBranch'
    private static List<Repository> repositories = new ArrayList<>()
    private static final Set<String> sourceCompileModules = new HashSet<>()
    private static final String LINE_SEPARATOR = System.lineSeparator()
    private static final String SUBMODULES_DIR = 'repos'
    private static boolean RELEASE_MODE = false
    private static boolean DEV_BRANCH_MODE = true
    private static File WORK_DIR
    private static File ROOT_DIR

    @Override
    void apply(Settings settings) {
        ROOT_DIR = settings.getRootDir()
        repositories = RepoUtils.readRepoConfig(settings.getRootDir())
        if (settings.hasProperty(CMD_PROPERTIES_RELEASE)) {
            def startShell = settings[CMD_PROPERTIES_RELEASE]
            RELEASE_MODE = startShell
        }
        if (settings.hasProperty(CMD_PROPERTIES_DEV_BRANCH)) {
            def startShell = settings[CMD_PROPERTIES_DEV_BRANCH]
            DEV_BRANCH_MODE = startShell
        }
        WORK_DIR = new File(settings.getRootDir(), SUBMODULES_DIR)
        if (!WORK_DIR.exists()) {
            WORK_DIR.mkdirs()
        }
        // sync all submodules
        if (settings.hasProperty(CMD_PROPERTIES_SYNC)) {
            def startShell = settings[CMD_PROPERTIES_SYNC]
            if (startShell != null && startShell.toBoolean()) {
                loadSSHName(settings)
                cloneModules()
            }
        }
        loadSourceModules(settings)
        includeSourceModules(settings)
    }

    private static void includeSourceModules(Settings settings) {
        WORK_DIR.listFiles().each { dir ->
            dir.listFiles().each { moduleFile ->
                if (moduleFile.isDirectory()) {
                    String moduleName = moduleFile.getName().trim()
                    if (sourceCompileModules.contains(moduleName)) {
                        include(settings, moduleFile)
                    }
                }
            }
        }
    }

    private static void loadSourceModules(Settings settings) {
        Properties localProperties = new Properties()
        File localPropertiesFile = new File(settings.rootDir, CONFIG_FILE)
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.newDataInputStream())
        }
        sourceCompileModules.clear()
        repositories.each { repository ->
            repository.getModules().each { module ->
                String artifactId = module.getArtifactId()
                String needInclude = localProperties.get(artifactId)
                println("${artifactId} needInclude: ${needInclude}")
                if (needInclude != null && needInclude.toInteger() > 0) {
                    sourceCompileModules << artifactId
                }
            }
        }
        println(sourceCompileModules.toListString())
    }

    private static void include(Settings settings, File moduleFile) {
        String moduleName = moduleFile.getName()
        settings.include(":${moduleName}")
        ProjectDescriptor PD = settings.project(":${moduleName}")
        PD.projectDir = moduleFile
        MODULES_DIR << PD.projectDir
    }

    private static void loadSSHName(Settings settings) {
        String repoCmd = "git remote -v"
        String result = repoCmd.execute(null, settings.getRootDir()).text
        if (!isTextEmpty(result)) {
            String keyword = "ssh://"
            int start = result.indexOf(keyword)
            int end = result.indexOf("@")
            String findSSHUserName = result.substring(start + keyword.length(), end).trim()
            if (!isTextEmpty(findSSHUserName)) {
                SSH_USER_NAME = findSSHUserName
                return
            }
        }
        Properties properties = new Properties()
        File localFile = new File(settings.getRootDir(), 'local.properties')
        if (localFile.exists()) {
            properties.load(localFile.newDataInputStream())
            String userName = properties.get('SSH_USER_NAME')
            if (userName != 'YOUR_SSH_USER_NAME') {
                SSH_USER_NAME = userName
            }
        }
        if (SSH_USER_NAME == null || SSH_USER_NAME == '') {
            String cmd = 'git config user.name'
            SSH_USER_NAME = cmd.execute(null, settings.getRootDir()).text.trim()
        }
    }

    private static void cloneModules() {
        boolean firstClone = false
        repositories.each { repository ->
            String repo = repository.getRepo()
            if (!isTextEmpty(repo)) {
                String repoDirName = getRepoDirName(repo)
                String sshUserName = SSH_USER_NAME
                String repoUrl = repo.replace('$user', sshUserName)
                def gitFile = new File(WORK_DIR, "${repoDirName}/.git")
                boolean alreadyClone = gitFile.exists()
                if (alreadyClone) {
                    print "${repoDirName} already exist."
                    syncBranch(new File(WORK_DIR, repoDirName), repository)
                    return
                }
                print "cloning ${repoDirName}..."
                firstClone = true
                String cmd = "git clone ${repoUrl}"
                Process process = cmd.execute(null, WORK_DIR)
                StringBuffer errorBuffer = new StringBuffer()
                process.consumeProcessOutput(null, errorBuffer)
                int result = process.waitFor()
                if (result == 0) {
                    print "success!"
                    syncBranch(new File(WORK_DIR, repoDirName), repository)
                } else {
                    println "failed!"
                    println "--------------- failed error message ----------------"
                    println errorBuffer.toString().trim()
                    println "-----------------------------------------------------"
                }
            }
        }
    }

    private static void syncBranch(File workDir, Repository repository) {
        if (!workDir.exists()) {
            return
        }
        String devBranch = repository.getDevBranch()
        if (DEV_BRANCH_MODE) {
            if (devBranch != null && devBranch != '') {
                checkoutBranch(workDir, devBranch)
                printBranch(workDir)
            } else {
                String repoName = getRepoDirName(repository.getRepo())
                throw new GradleException("${repoName} devbranch is invalid.")
            }
        } else {
            String commitId = repository.getCommitId()
            checkoutCommitId(workDir, commitId)
            printCommitId(workDir)
        }
    }

    private static void printCommitId(File workDir) {
        if (workDir.exists()) {
            String currentCommitCmd = "git rev-parse --short HEAD"
            String currentCommitId = currentCommitCmd.execute(null, workDir).text.trim()
            println " current commitid: ${currentCommitId}"
        }
    }

    private static void printBranch(File workDir) {
        if (workDir.exists()) {
            String currentBranchCmd = 'git rev-parse --abbrev-ref HEAD'
            String currentBranch = currentBranchCmd.execute(null, workDir).text.trim()
            println " current branch: ${currentBranch}"
        }
    }

    private static String getRepoDirName(String repo) {
        if (isGitLabRepo(repo)) {
            int start = repo.lastIndexOf('/')
            int end = repo.lastIndexOf('.git')
            String repoDirName = repo.substring(start + 1, end)
            return repoDirName
        }
        if (!isTextEmpty(repo)) {
            int start = repo.lastIndexOf('/')
            String repoDirName = repo.substring(start + 1)
            return repoDirName
        }
        return null
    }

    private static void pullAndRebase(File workDir, boolean ignoreError) {
        if (workDir.exists()) {
            String cmd = "git pull --rebase"
            Process process = cmd.execute(null, workDir)
            StringBuffer errorBuffer = new StringBuffer()
            process.consumeProcessOutput(null, errorBuffer)
            int result = process.waitFor()
            if (result != 0 && errorBuffer.size() > 0) {
                String errorMessage = errorBuffer.toString().trim()
                if (ignoreError) {
                    println errorMessage
                } else {
                    throw new GradleException(errorMessage)
                }
            }
        }
    }

    private static void checkoutCommitId(File workDir, String commitId) {
        if (!workDir.exists() || isTextEmpty(commitId)) {
            return
        }
        String currentCmdIdCmd = "git rev-parse HEAD"
        String currentCmdId = currentCmdIdCmd.execute(null, workDir).text.trim()
        if (currentCmdId == commitId) {
            return
        }
        fetchOrigin(workDir)
        String cmd = "git checkout ${commitId}"
        Process process = cmd.execute(null, workDir)
        StringBuffer errorBuffer = new StringBuffer()
        process.consumeProcessOutput(null, errorBuffer)
        int result = process.waitFor()
        if (result != 0 && errorBuffer.size() > 0) {
            throw new GradleException("checkout out commitId: '${commitId}' failed. please check if the configuration in the repositories.xml file is correct.${LINE_SEPARATOR}${errorBuffer.toString().trim()}")
        }
    }

    private static void checkoutBranch(File workDir, String branch) {
        if (!workDir.exists() || isTextEmpty(branch)) {
            return
        }
        String currentBranchCmd = 'git rev-parse --abbrev-ref HEAD'
        String currentBranch = currentBranchCmd.execute(null, workDir).text.trim()
        if (currentBranch.endsWith(branch)) {
            pullAndRebase(workDir, false)
            return
        }
        String cmd = "git rev-parse --verify --quiet refs/heads/${branch}"
        Process process = cmd.execute(null, workDir)
        StringBuffer outputBuffer = new StringBuffer()
        process.consumeProcessOutput(outputBuffer, null)
        int result = process.waitFor()
        if (result == 0 && outputBuffer.size() > 0) {
            cmd = "git checkout ${branch}"
            process = cmd.execute(null, workDir)
            StringBuffer errorBuffer = new StringBuffer()
            process.consumeProcessOutput(null, errorBuffer)
            result = process.waitFor()
            if (result != 0 && errorBuffer.size() > 0) {
                println " sync branch to ${branch} failed."
                println errorBuffer.toString().trim()
                return
            }
            pullAndRebase(workDir, false)
        } else {
            fetchOrigin(workDir)
            cmd = "git checkout -b ${branch} origin/${branch}"
            process = cmd.execute(null, workDir)
            StringBuffer errorBuffer = new StringBuffer()
            process.consumeProcessOutput(null, errorBuffer)
            result = process.waitFor()
            if (result != 0 && errorBuffer.size() > 0) {
                throw new GradleException("checkout branch: ${branch} failed! please check if the configuration in the repositories.xml file is correct.${LINE_SEPARATOR}${errorBuffer.toString().trim()}")
            }
        }
    }

    private static boolean isTextEmpty(String text) {
        return text == null || text == ''
    }

    private static void fetchOrigin(File workDir) {
        String cmd = "git fetch origin"
        Process process = cmd.execute(null, workDir)
        int result = process.waitFor()
        if (result != 0) {
            println "fetch origin failed."
        }
    }

    private static boolean isGitLabRepo(String repo) {
        if (isTextEmpty(repo)) {
            return false
        }
        return repo.startsWith("git@gitlab")
    }
}