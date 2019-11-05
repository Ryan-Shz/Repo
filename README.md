# Repo

Repo是一个项目开发效率工具，用来优化模块化/组件化分仓后带来的编译和调试痛点，提升日常开发效率。

提供以下几个功能：

1. 自动拉取所有的子代码仓库，并切换到对应的开发分支
2. 自动引用子代码仓库下的module
3. 自动处理源码和maven引用混合时的代码重复冲突
4. 子仓下的模块使用源码编译或maven引用两种方式支持一键切换
5. 支持在多个子仓库中快速执行统一命令，比如git命令

## Quick Start

### 基础配置

在项目根目录下创建配置文件**repositories.xml**，并按要求填写子仓信息：

```
<?xml version="1.0" encoding="utf-8" ?>
<repositories>
    <!-- 表示这个是一个仓库 -->
    <repository> 
    	<!-- 填写仓库地址 -->
        <repo>仓库地址</repo> 
        <!-- 填写仓库当前开发分支 -->
        <devbranch>开发分支</devbranch>
        <!-- 表示这个是仓库里的所有模块 -->
        <modules> 
            <!-- 表示这是仓库里的一个模块，并填写模块的maven仓库信息 -->
            <module> 
                <groupid>${mavenGroupId1}</groupid>
                <artifactid>${mavenArtifactId1}</artifactid>
                <version>${moduleVersion1}</version>
            </module>
            <module>
                <groupid>${mavenGroupId2}</groupid>
                <artifactid>${mavenArtifactId2}</artifactid>
                <version>${moduleVersion2}</version>
            </module>
            <!-- 多个模块就写多个module -->
       	    ...
    </repository>
</repositories>
```

在项目级的setting.gradle中应用settings插件:

```
apply plugin: 'com.ryan.repo.tools.settings'
```

在主工程的build.gradle中应用tools插件：

```
apply plugin: 'com.ryan.repo.tools'
```

### 命令执行

#### 查看帮助

```
./repo
./repo -h
./repo -help
```

帮助命令输出：

```
usage: git [-help] [-branch] [-status] [-push] [-pull] [-diff] [foreach <command>] [sync]
-----------------------------------------------------------------------------------------
git-based commands, easy to use
  -branch:             show the current branch of all submodules.
  -status:             show git status of all submodules.
  -pull:               pull and rebase the latest code of all submodules.
  -diff:               show local changes for all submodules.

use custom command of all submodules
  foreach <command>:   execute the specified command of all submodules.

source and publish command
  sync                 sync all submodules to specified devbranch.
for more help, please see: https://github.com/Ryan-Shz/Repo
```

#### 同步子仓库代码

```
./repo sync
```

sync命令会根据配置文件**repositories.xml**配置来拉取所有子仓库的代码，并切换到对应的**devbranch**分支。子仓代码存放在项目根目录下的repos文件夹中。

拉取完所有的子仓代码后，在根目录下创建**local.properties**文件，按以下格式手动添加编译开关：

```
# 源码编译配置
# 格式: 模块名=0或1
# 0表示以maven引用方式编译该模块
# 1表示以源码引用方式编译该模块
fastwebview=1
xxx=0
xxx=0
...
```

比如fastwebview=1表示fastwebview这个module以源码的方式编译，这样我们在修改fastwebview中的代码后，可以立即生效。

## 场景

项目将功能按业务形式分为多个不同的modul，每个module有独立的代码仓库，主仓和多个子仓之间使用maven依赖引用。

完全隔离的仓库，使每个仓库之间的协作更加的独立，但子仓集成至主仓调试时，操作非常麻烦，需要不断的发布snapshot，主仓更新snapshot，很影响开发效率。若在开发阶段以主仓引用子仓源码的方式，在子仓修改后就可以直接测试，开发效率会有很大的提升。

但源码调试存在以下几个痛点：
1. 需要手动拉取多个子模块的代码仓并切换到指定的子仓分支
2. 主仓对应子仓的分支没有对应关系，排查问题时很难回溯
3. 需要在settting.gradle中手动include对应子仓模块
4. 部分子模块使用源码编译、部分使用maven编译时很容易出现代码重复冲突
5. 多个子仓之间操作起来非常麻烦，比如更新所有子仓最新代码
