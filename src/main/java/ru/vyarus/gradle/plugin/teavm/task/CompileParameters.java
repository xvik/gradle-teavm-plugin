package ru.vyarus.gradle.plugin.teavm.task;

import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;

/**
 * Parameters for teavm compiler worker.
 *
 * @author Vyacheslav Rusakov
 * @since 06.01.2023
 */
public interface CompileParameters extends WorkParameters {

    /**
     * @return true to print teavm compilation details
     */
    Property<Boolean> getDebug();

    /**
     * Worker process can't directly return anything, so special file used as error indicator.
     *
     * @return error indication file
     */
    RegularFileProperty getErrorFile();

    /**
     * @return all directories with compiled classes and classpath jar files
     */
    ListProperty<String> getClassPathEntries();

    /**
     * @return all directories with sources
     */
    ListProperty<Directory> getSourceDirectories();

    /**
     * @return list of source jar files
     */
    ListProperty<File> getSourceJars();

    /**
     * @return target directory
     */

    DirectoryProperty getTargetDirectory();

    /**
     * @return teavm cache directory
     */
    DirectoryProperty getCacheDirectory();

    /**
     * @return main class name (entry point)
     */
    Property<String> getMainClass();

    /**
     * @return entry point name (main by default)
     */
    Property<String> getEntryPointName();

    /**
     * @return target file name (by default depends on target: classes.js, classes.wasm)
     */
    Property<String> getTargetFileName();

    /**
     * @return teavm compilation target (js, wasm)
     */
    Property<TeaVMTargetType> getTargetType();

    /**
     * @return teavm produced js module type
     */
    Property<JSModuleType> getJsModuleType();

    /**
     * @return wasm version
     */
    Property<WasmBinaryVersion> getWasmVersion();

    /**
     * @return true to minimize compiled js
     */
    Property<Boolean> getObfuscated();

    /**
     * @return true for strict mode
     */
    Property<Boolean> getStrict();

    /**
     * @return true to copy source files (required for browser debug with source maps)
     */
    Property<Boolean> getSourceFilesCopied();

    /**
     * @return true to put local file links instead of copying source files (for local development only)
     */
    Property<Boolean> getSourceFilesCopiedAsLocalLinks();

    /**
     * @return tue for incremental compilation
     */
    Property<Boolean> getIncremental();

    /**
     * @return true to generate debug info file (required for debug server)
     */
    Property<Boolean> getDebugInformationGenerated();

    /**
     * @return true for source maps generation
     */
    Property<Boolean> getSourceMapsFileGenerated();

    /**
     * @return true for short file names (C target)
     */
    Property<Boolean> getShortFileNames();

    /**
     * @return true for heap dump (C target)
     */
    Property<Boolean> getHeapDump();

    /**
     * ONLY for development because it affects optimization level (set to SIMPLE).
     *
     * @return true for fast dependencies analysis
     */
    Property<Boolean> getFastDependencyAnalysis();

    /**
     * @return true to remove assertions during compilation
     */
    Property<Boolean> getAssertionsRemoved();

    /**
     * @return min heap size (WASM and C targets)
     */
    Property<Integer> getMinHeapSize();

    /**
     * @return max heap size (WASM and C targets)
     */
    Property<Integer> getMaxHeapSize();

    /**
     * @return optimization level (SIMPLE - minimal, ADVANCED - prod. FULL - for WASM)
     */
    Property<TeaVMOptimizationLevel> getOptimizationLevel();

    /**
     * @return list of transformer classes (transforming ClassHolders)
     */
    ListProperty<String> getTransformers();

    /**
     * @return properties for teavm plugins
     */
    MapProperty<String, String> getProperties();

    /**
     * @return classes to preserve
     */
    ListProperty<String> getClassesToPreserve();


}
