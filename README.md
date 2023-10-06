# gradle-teavm-plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![CI](https://github.com/xvik/gradle-teavm-plugin/actions/workflows/CI.yml/badge.svg)](https://github.com/xvik/gradle-teavm-plugin/actions/workflows/CI.yml)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-teavm-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-teavm-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-teavm-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-teavm-plugin)

### About

Gradle [TeaVM](https://teavm.org/) plugin: java/kotlin/scala compilation to JS or WASM.
In contrast to [GWT](https://www.gwtproject.org/), teavm works with compiled bytecode (sources not required for compilation).

**Unofficial** gradle plugin. Differences with [official teavm plugin](https://teavm.org/docs/tooling/gradle.html):
- Single `teavmCompile` task and single configuration (instead of per type (js/wasm/etc.) configurations)
- Dev mode support (easy switching to dev configuration)
- Configurable compiler version (official plugin release together with teavm and so targets exact version, while this plugin detects version from classpath)
- No tests support ([must be configured manually](examples/java/build.gradle))

Features:
* Automatic teavm compiler version selection
* Dev mode support

##### Summary

* Configuration: `teavm`
* Tasks:
    - `compileTeavm` - compile sources       

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-teavm-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-teavm-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru.vyarus/teavm/ru.vyarus.teavm.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.teavm)

```groovy
plugins {
    id 'ru.vyarus.teavm' version '1.1.0'
}
```

OR

```groovy
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-teavm-plugin:1.1.0'
    }
}
apply plugin: 'ru.vyarus.teavm'
``` 

### Compatibility

NOTE: Java 11 or above is required (teavm compiled for java 11).

Gradle | Version
--------|-------
6.2-8   | 1.1.0

### Usage

Example projects:

* [Java](examples/java)
* [Flavour](examples/flavour)
* [Kotlin](examples/kotlin)
* [Scala](examples/scala)

####  TeaVM dependencies

TeaVM often publish fresh [dev versions](https://teavm.org/docs/intro/preview-builds.html) into separate repository. 
If you want to use dev build instead of release version, add custom repository:

```groovy
ext {
  teavm = '0.8.0-dev-2'
}
repositories { 
  mavenCentral()
  maven { url "https://teavm.org/maven/repository" }
}
dependencies {
    implementation "org.teavm:teavm-classlib:$teavm"
    implementation "org.teavm:teavm-jso:$teavm"
}
```

You can check the latest published dev build [directly in repository](https://teavm.org/maven/repository/org/teavm/teavm-core/) 

NOTE: [flavour](https://teavm.org/docs/flavour/templates.html) is not currently maintained and
its dev builds are not published (so you can use only the latest released 0.2.1)

Maintained flavour **fork**: [site](https://flavour.sourceforge.io/), [flavour docs](https://frequal.com/teavm-site/docs/flavour/templates.html) (removed from teavm site), 
[source](https://sourceforge.net/projects/flavour/)

#### Plugin configuration

The only required configuration is entry point main class:

```groovy
teavm {
  dev = false
  mainClass = 'com.foo.Client'
}
```

By default, plugin configured for JS production compilation, but you can set dev option 
to true to enable development options (source maps, faster compilation etc.).

NOTE: if you use flavour and want to put html (or other) resources together with source files
then activate `teavm.mixedResources` option (it would configure `processResources` task to work from source dirs)

#### Output configuration

compileTeavm task simply compiles java/kotlin/scala (compiled) classes into js (or wasm).
With dev options, it will also create debug file (for IDEA), source maps and copy all
sources into target directory (to let you debug code directly in browser with source maps).

Example webapp build task:

```groovy
tasks.register('buildWebapp', Copy) {
    from('src/main/webapp')
    from(compileTeavm) {
        into 'js'
    }
    into 'build/webapp'
}
```

Each time `buildWebapp` would be called, teavm compilation would be called too
preparing webapp inside `build/webapp` directory (you can run index.html from there to test compiled application)


For some cases it might be preferable to compile teavm directly into prepared webapp directory
(e.g. when webapp resources preparation took significant time): 

```groovy
teavm {
  targetDir = 'build/webapp'
}

tasks.register('buildWebapp', Copy) {
    from('src/main/webapp')   
    into 'build/webapp'
}
```

This way, `buildWebapp` copies webapp resources without compiled js and you should call
`compileTeavm` in order to add (or refresh) js.

#### Web

In html file you only need to include compiled js file:

```html
<script type="text/javascript" charset="utf-8" src="js/classes.js"></script>
```

### IDEA

TeaVM IDEA plugin provides [dev and debug servers](https://teavm.org/docs/tooling/idea.html). These are only available in plugin and can't be
implemented as gradle tasks.

IMPORTANT: idea plugin version, published into official repository is stale and you need
to declare custom plugins repository:
  Open Settings -> Plugins -> Browse repositories... -> Manage repositories... 
  Click Add button and enter https://teavm.org/idea/dev/teavmRepository.xml 
  Then get back to Browse repositories and pick TeaVM plugin from list.

### Configuration 

`teavm` options with default values:

```groovy
{
    /**
     * Enables dev mode: use options from {@link #devOptions} configuration.
     */
    dev = false
    /**
     * Prints plugin debug information: used paths, dependencies, resolved sources and complete teavm stats.
     */
    debug = false
    /**
     * Configures processResources task to load resources from java/kotlin/scala directories (ignoring compiled
     * sources). Useful for flavour when html templates stored near source files.
     */
    mixedResources = false
    /**
     * Detect teavm version from classpath ({@link #configurations}) in order to use the same version for compilation.
     * When enabled, {@link #version} option is ignored.
     */
    autoVersion = true
    /**
     * Teavm version to use. Ignored when {@link #autoVersion} enabled.
     */
    version = "0.8.0"

    /**
     * Source sets to compile js from. By default, java, kotlin and scala supported.
     */
    sourceSets = [main", "kotlin", "scala"]
    /**
     * Configurations with required dependencies (by default, runtimeClasspath). There is no alternative for direct
     * dependency jars specification - local jar files could always be configured in configuration.
     */
    configurations = ["runtimeClasspath"]
    /**
     * Additional directories with compiled classes. Normally, this should not be needed as {@link #sourceSets}
     * already describe required directories. Could be useful only for specific cases.
     * Values: strings with absolute or relative directories locations. 
     */
    extraClassDirs = []
    /**
     * Additional source directories (used only when {@link #sourceFilesCopied} enabled). Normally, this should not
     * be needed as sources already descibed with {@link #sourceSets} and dependencies sources are resolved
     * from {@link #configurations}.
     * All jars contained in configured directories (1st level) would be also added.
     * Values: strings with absolute or relative directories locations. 
     */
    extraSourceDirs = []
    /**
     * Target compilation directory. By default, "build/teavm".
     */
    targetDir = 'build/teavm'
    /**
     * Teavm cache directory. By default, "build/teavm-cache".
     */
    cacheDir = 'build/teavm-cache'

    /**
     * Main application class.
     */
    String mainClass
    /**
     * Entry point name (entry static method).
     */
    entryPointName = "main"
    /**
     * Output file name. By default, empty to let teavm automaticlly select file name by compilation target:
     * classes.js, classes.wasm, etc. ({@link  org.teavm.tooling.TeaVMTool#getResolvedTargetFileName()}).
     */
    targetFileName = ""
    /**
     * Compilation target: js by default. Values: JAVASCRIPT, WEBASSEMBLY, C
     */
    targetType = JAVASCRIPT
    /**
     * Target wasm version (only for compilation to WASM). Values: V_0x1
     */
    wasmVersion = V_0x1

    /**
     * Fail on compilation error.
     */
    stopOnErrors = true
    /**
     * Minify files. Should be enabled for production, but disabled for dev.
     */
    obfuscated = true
    /**
     * Strict teavm mode.
     */
    strict = false
    /**
     * Copy java sources into generated folder so they could be loaded in browser through source maps (see
     * {@link #sourceMapsGenerated}).
     */
    sourceFilesCopied = false
    /**
     * Incremental compilation speeds up compilation, but limits some optimizations and so should be used only
     * in dev mode.
     */
    incremental = false
    /**
     * Generate debug information required for debug server (started from IDE).
     */
    debugInformationGenerated = false
    /**
     * Generate source maps. In oder to be able to debug sources in browser enable {@link #sourceFilesCopied}.
     */
    sourceMapsGenerated = false
    /**
     * Short file names. ONLY for C target.
     */
    shortFileNames = false
    /**
     * Long jmp. ONLY for C target.
     */
    longjmpSupported = true
    /**
     * Heap dump. ONLY for C target.
     */
    heapDump = false
    /**
     * Fast dependency analysis. Probably, could speed up compilation. ONLY for development! (option disables
     * {@link #optimizationLevel} setting).
     */
    fastDependencyAnalysis = false
    /**
     * Remove assertions.
     */
    assertionsRemoved = false        

    /**
     * Top-level names limit. ONLY for JS target.
     */
    maxTopLevelNames = 10000
    /**
     * Minimal heap size (in mb). ONLY for WASM and C targets.
     */
    minHeapSize = 4
    /**
     * Maximum heap size (in mb). ONLY for WASM and C targets.
     */
    maxHeapSize = 128
    /**
     * Output optimization level.
     * SIMPLE – perform only basic optimizations, remain friendly to the debugger (recommended for development).
     * ADVANCED – perform more optimizations, sometimes may stuck debugger (recommended for production).
     * FULL – perform aggressive optimizations, increase compilation time, sometimes can make code even slower
     * (recommended for WebAssembly).
     */
    optimizationLevel = ADVANCED
    /**
     * An array of fully qualified class names. Each class must implement
     * {@link org.teavm.model.ClassHolderTransformer} interface and have a public no-argument constructor. These
     * transformers are used to transform ClassHolders, that are SSA-based representation of JVM classes. Transformers
     * run right after parsing JVM classes and producing SSA representation.
     */
    transformers = []
    /**
     * Properties passed to all TeaVM plugins (usage examples unknown).
     * Values: map of strings (key and value) 
     */
    properties = [:]
    /**
     * Fully qualified class names to preserve (probably, to avoid remove by dependency analysis).
     */
    classesToPreserve=[]

```

NOTE: enum values for options `optimizationLevel`, `wasmVersion` and `targetType`
could be used without quotes because all values are pre-configured by plugin as constants


`devOptions` section configures overrides for some values for dev mode (enabled with `dev = true`):

```groovy
teavm {
  devOptions {
    obfuscated = false
    strict = false
    sourceFilesCopied = true
    incremental = false
    debugInformationGenerated = true
    sourceMapsGenerated = true
    fastDependencyAnalysis = false
    assertionsRemoved = false
    optimizationLevel = SIMPLE
    
    // C target ONLY
    shortFileNames = false
    longjmpSupported = true
    heapDump = false
  }
}
```

#### Sources configuration

Extension is configured based on source sets. By default:

```groovy
teavm.sourceSets = ["main", "kotlin", "scala"]
```

For all found source sets (from list) compiled classes and source directories
would be automatically configured for teavm.

If you have additional sources or don't want to rely on source sets use

```groovy
teavm {
  extraClassDirs = ['/path/to/classes']
  extraSourceDirs = ['/path/to/sources']
}
```

(normally, you don't need to declare any additional directories as sourcesets describe everything)

`configurations` option declares teavm dependencies. By default:

`teavm.configurations = ['runtimeClasspath']`

All jars from provided configurations would be passed to teavm.
Also, if sources generation enabled, source jars would be resolved.

In case if you have some custom library with jars, still use configuration to declare them.
Additional source jars might be declared in `extraSourceDirs`: all jars found in extra source
dirs used as source jars.

#### TeaVM version

Plugin is compiled with exact teavm version (and so supports only options available in this version). 
But, at runtime, it could run with different version.
By default, required teavm version is detected from classpath. This behaviour could be
disabled with `teavm.autoVersion=false`.

When auto version disabled or can't be resolved, version from `teavm.version` used.

#### Development

Options related to development are duplicated in the main closure and `devOptions`.
This way, top closure declares production configuration and `devOptions` for 
dev mode (`dev = true`).

#### Debug

```groovy
teavm.debug = true
```

activates debug mode to show all paths resolved from source sets, classpath jars,
loaded source jars and all used teavm files.

### Custom task

You can configure custom teavm task if required:

```groovy
tasks.register('myTeavm', TeavmCompileTask) {
  ...
}
```

Extension values are applied to all compile tasks, so you may need to change only 
different values.

Options list:

```groovy
  debug = false
  classPath = 
  dependencies =
  sources = 
  sourceDependencies =  
  targetDir = 
  cacheDir =
  mainClass = 
  entryPointName =  
  targetFileName =
  targetType =
  wasmVersion =
  stopOnErrors = 
  obfuscated = 
  strict =
  sourceFilesCopied =
  incremental =   
  debugInformationGenerated =   
  sourceMapsGenerated =  
  shortFileNames =
  longjmpSupported = 
  heapDump =
  fastDependencyAnalysis =
  assertionsRemoved =        
  maxTopLevelNames =
  minHeapSize =
  maxHeapSize =
  optimizationLevel =
  transformers =
  properties =  
  classesToPreserve =        
```

Task options are almost the same as in configuration except compiled classes dirs, source dirs and dependencies 
configuration: task does not use sourceSets or configurations, instead it accepts direct 
lists. This is required for proper up-to-date behaviour (so task could correctly detect changes)

So completely custom task configuration would look like:

```groovy
tasks.register('myTeavm', TeavmCompileTask) {
  // list of dirs with compiled classes
  classPath = [layout.buildDirectory.dir('generated-source')]
  // all required jars
  dependencies = configurations.findByName('custom')   
  // sources required ONLY when sourceFilesCopied = true 
  sources = [layout.projectDirectory.dir('src/main/java')]
}
```

For more inof about working with gradle properties see [gradle docs](https://docs.gradle.org/current/userguide/lazy_configuration.html#working_with_task_dependencies_in_lazy_properties)

All teavm tasks would depend on `classes` task to compile java/kotlin/scala and process resources
before teavm start.

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
