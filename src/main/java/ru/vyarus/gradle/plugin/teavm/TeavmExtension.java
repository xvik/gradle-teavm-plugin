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
 * It is assumed that these values are production values. Dev-related options are duplicated in devOptions section,
 * which is applied when 'dev = true' flag enabled.
 * <p>
 * Extension configured with sourceSets and configurations, while task configuration limited to exact directories
 * and resolved exact jar files. This way task could correctly handle up-to-date checks.
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

    /**
     * Enables dev mode: use options from {@link #devOptions} configuration.
     */
    private boolean dev = false;
    /**
     * Prints plugin debug information: used paths, dependencies, resolved sources and complete teavm stats.
     */
    private boolean debug = false;
    /**
     * Configures processResources task to load resources from java/kotlin/scala directories (ignoring compiled
     * sources). Useful for flavour when html templates stored near source files.
     */
    private boolean mixedResources = false;
    /**
     * Detect teavm version from classpath ({@link #configurations}) in order to use the same version for compilation.
     * When enabled, {@link #version} option is ignored.
     */
    private boolean autoVersion = true;
    /**
     * Teavm version to use. Ignored when {@link #autoVersion} enabled.
     */
    private String version = "0.7.0";

    /**
     * Source sets to compile js from. By default, java, kotlin and scala supported.
     */
    private List<String> sourceSets = new ArrayList<>(Arrays.asList("main", "kotlin", "scala"));
    /**
     * Configurations with required dependencies (by default, runtimeClasspath). There is no alternative for direct
     * dependency jars specification - local jar files could always be configured in configuration.
     */
    private List<String> configurations = new ArrayList<>(Collections.singletonList("runtimeClasspath"));
    /**
     * Additional directories with compiled classes. Normally, this should not be needed as {@link #sourceSets}
     * already describe required directories. Could be useful only for specific cases.
     */
    private Set<String> extraClassDirs = new HashSet<>();
    /**
     * Additional source directories (used only when {@link #sourceFilesCopied} enabled). Normally, this should not
     * be needed as sources already descibed with {@link #sourceSets} and dependencies sources are resolved
     * from {@link #configurations}.
     * All jars contained in configured directories (1st level) would be also added.
     */
    private Set<String> extraSourceDirs = new HashSet<>();
    /**
     * Target compilation directory. By default, "build/teavm".
     */
    private String targetDir;
    /**
     * Teavm cache directory. By default, "build/teavm-cache".
     */
    private String cacheDir;

    /**
     * Main application class.
     */
    private String mainClass;
    /**
     * Entry point name (entry static method).
     */
    private String entryPointName = "main";
    /**
     * Output file name. By default, empty to let teavm automaticlly select file name by compilation target:
     * classes.js, classes.wasm, etc. ({@link  org.teavm.tooling.TeaVMTool#getResolvedTargetFileName()}).
     */
    private String targetFileName = "";
    /**
     * Compilation target: js by default. Values: JAVASCRIPT, WEBASSEMBLY, C
     */
    private TeaVMTargetType targetType = TeaVMTargetType.JAVASCRIPT;
    /**
     * Target wasm version (only for compilation to WASM). Values: V_0x1
     */
    private WasmBinaryVersion wasmVersion = WasmBinaryVersion.V_0x1;

    /**
     * Fail on compilation error.
     */
    private boolean stopOnErrors = true;
    /**
     * Minify files. Should be enabled for production, but disabled for dev.
     */
    private boolean obfuscated = true;
    /**
     * Strict teavm mode.
     */
    private boolean strict = false;
    /**
     * Copy java sources into generated folder so they could be loaded in browser through source maps (see
     * {@link #sourceMapsGenerated}).
     */
    private boolean sourceFilesCopied = false;
    /**
     * Incremental compilation speeds up compilation, but limits some optimizations and so should be used only
     * in dev mode.
     */
    private boolean incremental = false;
    /**
     * Generate debug information required for debug server (started from IDE).
     */
    private boolean debugInformationGenerated = false;
    /**
     * Generate source maps. In oder to be able to debug sources in browser enable {@link #sourceFilesCopied}.
     */
    private boolean sourceMapsGenerated = false;
    /**
     * Short file names. ONLY for C target.
     */
    private boolean shortFileNames = false;
    /**
     * Long jmp. ONLY for C target.
     */
    private boolean longjmpSupported = true;
    /**
     * Heap dump. ONLY for C target.
     */
    private boolean heapDump = false;
    /**
     * Fast dependency analysis. Probably could speed up compilation. ONLY for development! (option disables
     * {@link #optimizationLevel} setting).
     */
    private boolean fastDependencyAnalysis = false;
    //    private boolean assertionsRemoved = false;

    /**
     * Top-level names limit. ONLY for JS target.
     */
    private int maxTopLevelNames = 10000;
    /**
     * Minimal heap size (in mb). ONLY for WASM and C targets.
     */
    private int minHeapSize = 4;
    /**
     * Maximum heap size (in mb). ONLY for WASM and C targets.
     */
    private int maxHeapSize = 128;
    /**
     * Output optimization level.
     * SIMPLE – perform only basic optimizations, remain friendly to the debugger (recommended for development).
     * ADVANCED – perform more optimizations, sometimes may stuck debugger (recommended for production).
     * FULL – perform aggressive optimizations, increase compilation time, sometimes can make code even slower
     * (recommended for WebAssembly).
     */
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.ADVANCED;
    /**
     * An array of fully qualified class names. Each class must implement
     * {@link org.teavm.model.ClassHolderTransformer} interface and have a public no-argument constructor. These
     * transformers are used to transform ClassHolders, that are SSA-based representation of JVM classes. Transformers
     * run right after parsing JVM classes and producing SSA representation.
     */
    private List<String> transformers = null;
    /**
     * Properties passed to all TeaVM plugins (usage examples unknown).
     */
    private Map<String, String> properties = null;
    /**
     * Fully qualified class names to preserve (probably, to avoid remove by dependency analysis).
     */
    private List<String> classesToPreserve;

    /**
     * Options override for dev mode (enabled with {@link #dev} flag).
     */
    private Dev devOptions = new Dev();


    public TeavmExtension(final Project project) {
        final String buildDir = project.relativePath(project.getBuildDir());
        targetDir = buildDir + "/teavm";
        cacheDir = buildDir + "/teavm-cache";
    }

    public boolean isDev() {
        return dev;
    }

    public void setDev(final boolean dev) {
        this.dev = dev;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public boolean isMixedResources() {
        return mixedResources;
    }

    public void setMixedResources(final boolean mixedResources) {
        this.mixedResources = mixedResources;
    }

    public boolean isAutoVersion() {
        return autoVersion;
    }

    public void setAutoVersion(final boolean autoVersion) {
        this.autoVersion = autoVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setTransformers(final List<String> transformers) {
        this.transformers = transformers;
    }

    public void setClassesToPreserve(final List<String> classesToPreserve) {
        this.classesToPreserve = classesToPreserve;
    }

    public List<String> getSourceSets() {
        return sourceSets;
    }

    public void setSourceSets(final List<String> sourceSets) {
        this.sourceSets = sourceSets;
    }

    public Set<String> getExtraClassDirs() {
        return extraClassDirs;
    }

    public void setExtraClassDirs(final Set<String> extraClassDirs) {
        this.extraClassDirs = extraClassDirs;
    }

    public List<String> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(final List<String> configurations) {
        this.configurations = configurations;
    }

    public Set<String> getExtraSourceDirs() {
        return extraSourceDirs;
    }

    public void setExtraSourceDirs(final Set<String> extraSourceDirs) {
        this.extraSourceDirs = extraSourceDirs;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(final String targetDir) {
        this.targetDir = targetDir;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(final String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(final String mainClass) {
        this.mainClass = mainClass;
    }

    public String getEntryPointName() {
        return entryPointName;
    }

    public void setEntryPointName(final String entryPointName) {
        this.entryPointName = entryPointName;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(final String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public TeaVMTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(final TeaVMTargetType targetType) {
        this.targetType = targetType;
    }

    public WasmBinaryVersion getWasmVersion() {
        return wasmVersion;
    }

    public void setWasmVersion(final WasmBinaryVersion wasmVersion) {
        this.wasmVersion = wasmVersion;
    }

    public boolean isStopOnErrors() {
        return stopOnErrors;
    }

    public void setStopOnErrors(final boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
    }

    public boolean isObfuscated() {
        return obfuscated;
    }

    public void setObfuscated(final boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    public void setSourceFilesCopied(final boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(final boolean incremental) {
        this.incremental = incremental;
    }

    public boolean isDebugInformationGenerated() {
        return debugInformationGenerated;
    }

    public void setDebugInformationGenerated(final boolean debugInformationGenerated) {
        this.debugInformationGenerated = debugInformationGenerated;
    }

    public boolean isSourceMapsGenerated() {
        return sourceMapsGenerated;
    }

    public void setSourceMapsGenerated(final boolean sourceMapsGenerated) {
        this.sourceMapsGenerated = sourceMapsGenerated;
    }

    public boolean isShortFileNames() {
        return shortFileNames;
    }

    public void setShortFileNames(final boolean shortFileNames) {
        this.shortFileNames = shortFileNames;
    }

    public boolean isLongjmpSupported() {
        return longjmpSupported;
    }

    public void setLongjmpSupported(final boolean longjmpSupported) {
        this.longjmpSupported = longjmpSupported;
    }

    public boolean isHeapDump() {
        return heapDump;
    }

    public void setHeapDump(final boolean heapDump) {
        this.heapDump = heapDump;
    }

    public boolean isFastDependencyAnalysis() {
        return fastDependencyAnalysis;
    }

    public void setFastDependencyAnalysis(final boolean fastDependencyAnalysis) {
        this.fastDependencyAnalysis = fastDependencyAnalysis;
    }

    public int getMaxTopLevelNames() {
        return maxTopLevelNames;
    }

    public void setMaxTopLevelNames(final int maxTopLevelNames) {
        this.maxTopLevelNames = maxTopLevelNames;
    }

    public int getMinHeapSize() {
        return minHeapSize;
    }

    public void setMinHeapSize(final int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    public int getMaxHeapSize() {
        return maxHeapSize;
    }

    public void setMaxHeapSize(final int maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    public TeaVMOptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }

    public void setOptimizationLevel(final TeaVMOptimizationLevel optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
    }

    public List<String> getTransformers() {
        return transformers;
    }

    public void setTransformers(final String... transformers) {
        this.transformers = Arrays.asList(transformers);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(final Map<String, String> properties) {
        this.properties = properties;
    }

    public List<String> getClassesToPreserve() {
        return classesToPreserve;
    }

    public void setClassesToPreserve(final String... classesToPreserve) {
        this.classesToPreserve = Arrays.asList(classesToPreserve);
    }

    public Dev getDevOptions() {
        return devOptions;
    }

    public void setDevOptions(final Dev devOptions) {
        this.devOptions = devOptions;
    }

    /**
     * Options override for dev mode.
     */
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

        public void setOptimizationLevel(final TeaVMOptimizationLevel optimizationLevel) {
            this.optimizationLevel = optimizationLevel;
        }

        public boolean isObfuscated() {
            return obfuscated;
        }

        public void setObfuscated(final boolean obfuscated) {
            this.obfuscated = obfuscated;
        }

        public boolean isStrict() {
            return strict;
        }

        public void setStrict(final boolean strict) {
            this.strict = strict;
        }

        public boolean isSourceFilesCopied() {
            return sourceFilesCopied;
        }

        public void setSourceFilesCopied(final boolean sourceFilesCopied) {
            this.sourceFilesCopied = sourceFilesCopied;
        }

        public boolean isIncremental() {
            return incremental;
        }

        public void setIncremental(final boolean incremental) {
            this.incremental = incremental;
        }

        public boolean isDebugInformationGenerated() {
            return debugInformationGenerated;
        }

        public void setDebugInformationGenerated(final boolean debugInformationGenerated) {
            this.debugInformationGenerated = debugInformationGenerated;
        }

        public boolean isSourceMapsGenerated() {
            return sourceMapsGenerated;
        }

        public void setSourceMapsGenerated(final boolean sourceMapsGenerated) {
            this.sourceMapsGenerated = sourceMapsGenerated;
        }

        public boolean isFastDependencyAnalysis() {
            return fastDependencyAnalysis;
        }

        public void setFastDependencyAnalysis(final boolean fastDependencyAnalysis) {
            this.fastDependencyAnalysis = fastDependencyAnalysis;
        }
    }
}
