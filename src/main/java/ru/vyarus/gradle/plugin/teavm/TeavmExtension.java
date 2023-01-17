package ru.vyarus.gradle.plugin.teavm;

import org.gradle.api.Project;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Teavm plugin extension. These values would be applied as defaults to all registered teavm compile tasks.
 * It is assumed that these values are production values (for compileTeavm task). Debug task aslo use these values,
 * but explicitly enables all debug options.
 * <p>
 * There is a high duplication of parameters declaration: here, in task
 * ({@link ru.vyarus.gradle.plugin.teavm.task.TeavmCompileTask}) and in worker
 * ({@link ru.vyarus.gradle.plugin.teavm.task.CompileParameters}. It can't be avoided because there must be a way to
 * separately configure different tasks and gradle workers api works only through properties object.
 *
 * @author Vyacheslav Rusakov
 * @since 27.12.2022
 */
public class TeavmExtension {

    // quick enable for teavm debug options (see dev subclosure)
    private boolean dev = false;
    // print plugin-debug information
    private boolean debug = false;
    // configure resources location inside source directory (so IDEA could build correctly)
    private boolean mixedResources = false;

    // would try to guess used teavm version from classpath
    private boolean autoVersion = true;
    // version is ignored if auto version enabled
    private String version = "0.7.0";
    private List<String> sourceSets = new ArrayList<>(Arrays.asList("main", "kotlin", "scala"));
    private Set<String> extraClassDirs = new HashSet<>();
    private List<String> configurations = new ArrayList<>(Collections.singletonList("runtimeClasspath"));
    // dir with sources or with source jars (1st level)
    private Set<String> extraSourceDirs = new HashSet<>();
    private String targetDir;
    private String cacheDir;

    private String mainClass;
    // main (org/teavm/tooling/TeaVMTool.java:448)
    private String entryPointName;
    // classes.js, classes.wasm etc (org.teavm.tooling.TeaVMTool#getResolvedTargetFileName)
    private String targetFileName = "";
    private TeaVMTargetType targetType = TeaVMTargetType.JAVASCRIPT;
    private WasmBinaryVersion wasmVersion = WasmBinaryVersion.V_0x1;

    private boolean stopOnErrors = true;
    private boolean obfuscated = true;
    private boolean strict = false;
    private boolean sourceFilesCopied = false;
    private boolean incremental = false;
    private boolean debugInformationGenerated = false;
    private boolean sourceMapsGenerated = false;
    private boolean shortFileNames = false;
    private boolean longjmpSupported = true;
    private boolean heapDump = false;
    private boolean fastDependencyAnalysis = false;
    //    private boolean assertionsRemoved = false;

    private int maxTopLevelNames = 10000;
    private int minHeapSize = 4;
    private int maxHeapSize = 128;
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.ADVANCED;
    private List<String> transformers = null;
    private Map<String, String> properties = null;
    private List<String> classesToPreserve;

    private Dev devOptions = new Dev();


    public TeavmExtension(final Project project) {
        final String buildDir = project.relativePath(project.getBuildDir());
        targetDir = buildDir + "/teavm";
        cacheDir = buildDir + "/teavm-cache";
    }

    public boolean isDev() {
        return dev;
    }

    public void setDev(boolean dev) {
        this.dev = dev;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isMixedResources() {
        return mixedResources;
    }

    public void setMixedResources(boolean mixedResources) {
        this.mixedResources = mixedResources;
    }

    public boolean isAutoVersion() {
        return autoVersion;
    }

    public void setAutoVersion(boolean autoVersion) {
        this.autoVersion = autoVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setTransformers(List<String> transformers) {
        this.transformers = transformers;
    }

    public void setClassesToPreserve(List<String> classesToPreserve) {
        this.classesToPreserve = classesToPreserve;
    }

    public List<String> getSourceSets() {
        return sourceSets;
    }

    public void setSourceSets(List<String> sourceSets) {
        this.sourceSets = sourceSets;
    }

    public Set<String> getExtraClassDirs() {
        return extraClassDirs;
    }

    public void setExtraClassDirs(Set<String> extraClassDirs) {
        this.extraClassDirs = extraClassDirs;
    }

    public List<String> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<String> configurations) {
        this.configurations = configurations;
    }

    public Set<String> getExtraSourceDirs() {
        return extraSourceDirs;
    }

    public void setExtraSourceDirs(Set<String> extraSourceDirs) {
        this.extraSourceDirs = extraSourceDirs;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getEntryPointName() {
        return entryPointName;
    }

    public void setEntryPointName(String entryPointName) {
        this.entryPointName = entryPointName;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public TeaVMTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TeaVMTargetType targetType) {
        this.targetType = targetType;
    }

    public WasmBinaryVersion getWasmVersion() {
        return wasmVersion;
    }

    public void setWasmVersion(WasmBinaryVersion wasmVersion) {
        this.wasmVersion = wasmVersion;
    }

    public boolean isStopOnErrors() {
        return stopOnErrors;
    }

    public void setStopOnErrors(boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
    }

    public boolean isObfuscated() {
        return obfuscated;
    }

    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public boolean isDebugInformationGenerated() {
        return debugInformationGenerated;
    }

    public void setDebugInformationGenerated(boolean debugInformationGenerated) {
        this.debugInformationGenerated = debugInformationGenerated;
    }

    public boolean isSourceMapsGenerated() {
        return sourceMapsGenerated;
    }

    public void setSourceMapsGenerated(boolean sourceMapsGenerated) {
        this.sourceMapsGenerated = sourceMapsGenerated;
    }

    public boolean isShortFileNames() {
        return shortFileNames;
    }

    public void setShortFileNames(boolean shortFileNames) {
        this.shortFileNames = shortFileNames;
    }

    public boolean isLongjmpSupported() {
        return longjmpSupported;
    }

    public void setLongjmpSupported(boolean longjmpSupported) {
        this.longjmpSupported = longjmpSupported;
    }

    public boolean isHeapDump() {
        return heapDump;
    }

    public void setHeapDump(boolean heapDump) {
        this.heapDump = heapDump;
    }

    public boolean isFastDependencyAnalysis() {
        return fastDependencyAnalysis;
    }

    public void setFastDependencyAnalysis(boolean fastDependencyAnalysis) {
        this.fastDependencyAnalysis = fastDependencyAnalysis;
    }

    public int getMaxTopLevelNames() {
        return maxTopLevelNames;
    }

    public void setMaxTopLevelNames(int maxTopLevelNames) {
        this.maxTopLevelNames = maxTopLevelNames;
    }

    public int getMinHeapSize() {
        return minHeapSize;
    }

    public void setMinHeapSize(int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    public int getMaxHeapSize() {
        return maxHeapSize;
    }

    public void setMaxHeapSize(int maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    public TeaVMOptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }

    public void setOptimizationLevel(TeaVMOptimizationLevel optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
    }

    public List<String> getTransformers() {
        return transformers;
    }

    public void setTransformers(String... transformers) {
        this.transformers = Arrays.asList(transformers);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<String> getClassesToPreserve() {
        return classesToPreserve;
    }

    public void setClassesToPreserve(String... classesToPreserve) {
        this.classesToPreserve = Arrays.asList(classesToPreserve);
    }

    public Dev getDevOptions() {
        return devOptions;
    }

    public void setDevOptions(Dev devOptions) {
        this.devOptions = devOptions;
    }

    public static class Dev {
        private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.SIMPLE;
        private boolean obfuscated = false;
        private boolean strict = false;
        private boolean sourceFilesCopied = true;
        private boolean incremental = false;
        private boolean debugInformationGenerated = true;
        private boolean sourceMapsGenerated = true;
        private boolean fastDependencyAnalysis = false;

        public TeaVMOptimizationLevel getOptimizationLevel() {
            return optimizationLevel;
        }

        public void setOptimizationLevel(TeaVMOptimizationLevel optimizationLevel) {
            this.optimizationLevel = optimizationLevel;
        }

        public boolean isObfuscated() {
            return obfuscated;
        }

        public void setObfuscated(boolean obfuscated) {
            this.obfuscated = obfuscated;
        }

        public boolean isStrict() {
            return strict;
        }

        public void setStrict(boolean strict) {
            this.strict = strict;
        }

        public boolean isSourceFilesCopied() {
            return sourceFilesCopied;
        }

        public void setSourceFilesCopied(boolean sourceFilesCopied) {
            this.sourceFilesCopied = sourceFilesCopied;
        }

        public boolean isIncremental() {
            return incremental;
        }

        public void setIncremental(boolean incremental) {
            this.incremental = incremental;
        }

        public boolean isDebugInformationGenerated() {
            return debugInformationGenerated;
        }

        public void setDebugInformationGenerated(boolean debugInformationGenerated) {
            this.debugInformationGenerated = debugInformationGenerated;
        }

        public boolean isSourceMapsGenerated() {
            return sourceMapsGenerated;
        }

        public void setSourceMapsGenerated(boolean sourceMapsGenerated) {
            this.sourceMapsGenerated = sourceMapsGenerated;
        }

        public boolean isFastDependencyAnalysis() {
            return fastDependencyAnalysis;
        }

        public void setFastDependencyAnalysis(boolean fastDependencyAnalysis) {
            this.fastDependencyAnalysis = fastDependencyAnalysis;
        }
    }
}
