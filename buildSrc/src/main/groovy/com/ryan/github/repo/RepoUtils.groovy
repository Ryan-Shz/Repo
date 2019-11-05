package com.ryan.github.repo

class RepoUtils {

    static List<Repository> readRepoConfig(File rootDir) {
        List<Repository> repositories = new ArrayList<>()
        File configFile = new File(rootDir, 'repositories.xml')
        Node configNode = new XmlParser().parse(configFile)
        configNode.children().each {
            Repository repository = new Repository()
            List<Repository.Module> modules = new ArrayList<>()
            it.each {
                String tagName = it.name()
                def value = it.value()[0]
                switch (tagName) {
                    case Repository.XMl_TAG_REPO:
                        repository.setRepo(value)
                        break
                    case Repository.XMl_TAG_DEV_BRANCH:
                        repository.setDevBranch(value)
                        break
                    case Repository.XMl_TAG_MODULES:
                        it.each {
                            Repository.Module module = new Repository.Module()
                            it.each { info ->
                                String infoName = info.name()
                                def infoValue = info.value()[0]
                                if (infoName == Repository.XMl_TAG_ARTIFACT_ID) {
                                    module.setArtifactId(infoValue)
                                } else if (infoName == Repository.XMl_TAG_VERSION) {
                                    module.setVersion(infoValue)
                                } else if (infoName == Repository.XMl_TAG_GROUP) {
                                    module.setGroupId(infoValue)
                                }
                            }
                            modules.add(module)
                        }
                        break
                }
            }
            repository.setModules(modules)
            repositories.add(repository)
        }
        return repositories
    }

}