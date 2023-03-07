package ru.vyarus.gradle.plugin.teavm;

import org.teavm.vm.TeaVMOptimizationLevel;

/**
 * TeaVM development-related options. Options extracted from main extension to avoid duplication.
 */
@SuppressWarnings({"checkstyle:ExplicitInitialization", "PMD.RedundantFieldInitializer"})
public class DevOptions {
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
     * Fast dependency analysis. Probably, could speed up compilation. ONLY for development! (option disables
     * {@link #optimizationLevel} setting).
     */
    private boolean fastDependencyAnalysis = false;
    /**
     * Remove assertions.
     */
    private boolean assertionsRemoved = false;
    /**
     * Output optimization level.
     * SIMPLE – perform only basic optimizations, remain friendly to the debugger (recommended for development).
     * ADVANCED – perform more optimizations, sometimes may stuck debugger (recommended for production).
     * FULL – perform aggressive optimizations, increase compilation time, sometimes can make code even slower
     * (recommended for WebAssembly).
     */
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.ADVANCED;

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

    public boolean isAssertionsRemoved() {
        return assertionsRemoved;
    }

    public void setAssertionsRemoved(final boolean assertionsRemoved) {
        this.assertionsRemoved = assertionsRemoved;
    }
}
