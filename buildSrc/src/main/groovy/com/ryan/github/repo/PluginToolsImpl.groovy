package com.ryan.github.repo

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class PluginToolsImpl implements Plugin<Project> {

    private static final def CONFIG_FILE = 'local.properties'
    private static final def CMD_PROPERTIES_PRE_BUILD = 'noBuild'
    private static final def SUBMODULES_ROOT_DIR = 'repos'
    private static List<Repository> repositories = new ArrayList<>()
    private static final Set<Repository.Module> sourceCompileModules = new HashSet<>()
    private static final Set<Repository.Module> remoteCompileModules = new HashSet<>()
    private static File WORK_DIR

    @Override
    void apply(Project project) {
        project.afterEvaluate {
            insertCmdTask(project)
            if (project.hasProperty(CMD_PROPERTIES_PRE_BUILD)) {
                def startShell = project[CMD_PROPERTIES_PRE_BUILD]
                if (startShell != null && startShell.toBoolean()) {
                    return
                }
            }
            WORK_DIR = new File(project.getRootDir(), SUBMODULES_ROOT_DIR)
            repositories = RepoUtils.readRepoConfig(project.getRootDir())
            loadSourceModules(project)
            switchRemoteToSourceOfAllModules(project)
            compileAllSources(project)
            compileAllRemotes(project)
            checkDependencies(project)
        }
    }

    private static void compileAllSources(Project project) {
        sourceCompileModules.each { sourceModule ->
            String artifactId = sourceModule.artifactId
            compileSource(project, artifactId)
        }
    }

    private static void compileAllRemotes(Project project) {
        remoteCompileModules.each { remoteModule ->
            String groupId = remoteModule.groupId
            String artifactId = remoteModule.artifactId
            String version = remoteModule.version
            String remoteUrl = "${groupId}:${artifactId}:${version}"
            compileRemote(project, remoteUrl)
        }
    }

    private static void compileSource(Project project, String moduleName) {
        println "local: ${moduleName}"
        project.dependencies.add('implementation', project.project(":${moduleName}")) {
            for (sourceModule in sourceCompileModules) {
                if (sourceModule.artifactId != moduleName) {
                    exclude group: sourceModule.groupId, module: sourceModule.artifactId
                }
            }
        }
    }

    private static void compileRemote(Project project, String url) {
        println "remote: ${url}"
        project.dependencies.add('implementation', url) {
            for (sourceModule in sourceCompileModules) {
                exclude group: sourceModule.groupId, module: sourceModule.artifactId
            }
        }
    }

    private static void switchRemoteToSourceOfAllModules(Project project) {
        project.getRootProject().subprojects.each { subProject ->
            subProject.configurations.all { configuration ->
                configuration.resolutionStrategy.dependencySubstitution { substitution ->
                    for (sourceModule in sourceCompileModules) {
                        String artifactId = sourceModule.artifactId
                        substitution.substitute(substitution.module("${sourceModule.groupId}:${artifactId}"))
                                .with(substitution.project(":${artifactId}"))
                    }
                }
            }
        }
    }

    private static void insertCmdTask(Project project) {
        project.tasks.register('cmd')
    }

    private static void loadSourceModules(Project project) {
        Properties localProperties = new Properties()
        File localPropertiesFile = new File(project.rootDir, CONFIG_FILE)
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.newDataInputStream())
        }
        sourceCompileModules.clear()
        remoteCompileModules.clear()
        repositories.each { repository ->
            repository.getModules().each { module ->
                String artifactId = module.getArtifactId()
                String needInclude = localProperties.get(artifactId)
                if (needInclude != null && needInclude.toInteger() > 0) {
                    sourceCompileModules.add(module)
                } else {
                    remoteCompileModules.add(module)
                }
            }
        }
    }

    private static void checkDependencies(Project project) {
        project.configurations.all { configuration ->
            configuration.resolutionStrategy.eachDependency { details ->
                String group = details.requested.group
                String artifactId = details.requested.name
                String version = details.requested.version
                Repository.Module compileModule = findCompileModule(artifactId)
                if (compileModule != null) {
                    if (group == compileModule.groupId && artifactId == compileModule.artifactId && version > compileModule.version) {
                        throw new GradleException("module: ${artifactId} uses a higher version: ${version} " +
                                "which is larger than the version declared in repositories.xml: ${compileModule.version}, " +
                                "in order to avoid problems, a consistent version must be used.")
                    }
                }
            }
        }
    }

    private static Repository.Module findCompileModule(String artifactId) {
        for (compileModule in remoteCompileModules) {
            if (compileModule.artifactId == artifactId) {
                return compileModule
            }
        }
        return null
    }
}
