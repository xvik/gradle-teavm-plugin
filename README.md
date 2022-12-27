# gradle-teavm-plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![CI](https://github.com/xvik/gradle-teavm-plugin/actions/workflows/CI.yml/badge.svg)](https://github.com/xvik/gradle-teavm-plugin/actions/workflows/CI.yml)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-teavm-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-teavm-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-teavm-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-teavm-plugin)

### About

Gradle TeaVM plugin

Features:
* Feature 1
* Feature 2

##### Summary

* Configuration: `teavm`
* Tasks:
    - `task1` - brief task description       

### Setup


[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-teavm-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-teavm-plugin)

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru.vyarus/teavm/ru.vyarus.teavm.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.teavm)

```groovy
plugins {
    id 'ru.vyarus.teavm' version '0.1.0'
}
```

OR

```groovy
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-teavm-plugin:0.1.0'
    }
}
apply plugin: 'ru.vyarus.teavm'
``` 

### Usage

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
