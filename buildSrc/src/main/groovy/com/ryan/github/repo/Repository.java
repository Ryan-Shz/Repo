package com.ryan.github.repo;

import java.util.List;

/**
 * Created by Ryan
 * at 2019/8/21
 */
public class Repository {

    static final String XMl_TAG_GROUP = "groupid";
    static final String XMl_TAG_ARTIFACT_ID = "artifactid";
    static final String XMl_TAG_VERSION = "version";
    static final String XMl_TAG_TAG_VERSION = "tagversion";
    static final String XMl_TAG_COMMIT_ID = "commitid";
    static final String XMl_TAG_DEV_BRANCH = "devbranch";
    static final String XMl_TAG_REPO = "repo";
    static final String XMl_TAG_MODULES = "modules";

    private String repo;
    private List<Module> modules;
    private String devBranch;

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getDevBranch() {
        return devBranch;
    }

    public void setDevBranch(String devBranch) {
        this.devBranch = devBranch;
    }

    public static class Module {

        private String groupId;
        private String artifactId;
        private String version;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String group) {
            this.groupId = group;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
