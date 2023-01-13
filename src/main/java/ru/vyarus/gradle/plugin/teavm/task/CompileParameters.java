package ru.vyarus.gradle.plugin.teavm.task;

import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;

/**
 * @author Vyacheslav Rusakov
 * @since 06.01.2023
 */
public interface CompileParameters extends WorkParameters {

    Property<Boolean> getDebug();
    /**
     * Worker process can't directly return anything
     * @return
     */
    RegularFileProperty getErrorFile();
    ListProperty<String> getClassPathEntries();
    ListProperty<Directory> getSourceDirectories();
    ListProperty<File> getSourceJars();
    DirectoryProperty getTargetDirectory();
    DirectoryProperty getCacheDirectory();

    Property<String> getMainClass();
    Property<String> getEntryPointName();
    Property<String> getTargetFileName();
    Property<TeaVMTargetType> getTargetType();
    Property<WasmBinaryVersion> getWasmVersion();

    Property<Boolean> getObfuscated();
    Property<Boolean> getStrict();
    Property<Boolean> getSourceFilesCopied();
    Property<Boolean> getIncremental();
    Property<Boolean> getDebugInformationGenerated();
    Property<Boolean> getSourceMapsFileGenerated();
    Property<Boolean> getShortFileNames();
    Property<Boolean> getLongjmpSupported();
    Property<Boolean> getHeapDump();
    Property<Boolean> getFastDependencyAnalysis();

    Property<Integer> getMaxTopLevelNames();
    Property<Integer> getMinHeapSize();
    Property<Integer> getMaxHeapSize();
    Property<TeaVMOptimizationLevel> getOptimizationLevel();
    ListProperty<String> getTransformers();
    MapProperty<String, String> getProperties();
    ListProperty<String> getClassesToPreserve();


}
