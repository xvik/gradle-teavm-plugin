package ru.vyarus.gradle.plugin.teavm.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.teavm.apachecommons.io.FileUtils;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TeaVM compile task.
 *
 * @author Vyacheslav Rusakov
 * @since 06.01.2023
 */
@SuppressWarnings("PMD.ExcessiveImports")
public abstract class TeavmCompileTask extends DefaultTask {

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    /**
     * @return true to show teavm compilation debug information
     */
    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    /**
     * @return directories with compiled classes and jar files (dependencies)
     */
    @InputFiles
    public abstract SetProperty<Directory> getClassPath();

    /**
     * @return collection of dependent jar files
     */
    @InputFiles
    public abstract ConfigurableFileCollection getDependencies();

    /**
     * @return directories with sources and source jar files
     */
    @InputFiles
    @Optional
    public abstract SetProperty<Directory> getSources();

    /**
     * @return collection of source jars
     */
    @InputFiles
    @Optional
    public abstract ConfigurableFileCollection getSourceDependencies();

    /**
     * @return target compilation directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getTargetDir();

    /**
     * @return teavm cache directory
     */
    @Internal
    public abstract DirectoryProperty getCacheDir();

    /**
     * @return main class name (entry point)
     */
    @Input
    public abstract Property<String> getMainClass();

    /**
     * @return entry static method name (main by default)
     */
    @Input
    @Optional
    public abstract Property<String> getEntryPointName();

    /**
     * @return target file name (by default classes.js or classes.wasm)
     */
    @Input
    @Optional
    public abstract Property<String> getTargetFileName();

    /**
     * @return compilation target
     */
    @Input
    public abstract Property<TeaVMTargetType> getTargetType();

    /**
     * @return wasm version (if wasm target used)
     */
    @Input
    public abstract Property<WasmBinaryVersion> getWasmVersion();

    /**
     * @return true to stop build on compilation errors
     */
    @Input
    public abstract Property<Boolean> getStopOnErrors();

    /**
     * @return true to minimize js output
     */
    @Input
    public abstract Property<Boolean> getObfuscated();

    /**
     * @return true for strict compilation
     */
    @Input
    public abstract Property<Boolean> getStrict();

    /**
     * @return true to copy source files into target directory (for source maps)
     */
    @Input
    public abstract Property<Boolean> getSourceFilesCopied();

    /**
     * @return true to enable incremental compilation
     */
    @Input
    public abstract Property<Boolean> getIncremental();

    /**
     * @return true to create debug info file (required for debug server)
     */
    @Input
    public abstract Property<Boolean> getDebugInformationGenerated();

    /**
     * @return true to generate source maps (for js)
     */
    @Input
    public abstract Property<Boolean> getSourceMapsGenerated();

    /**
     * @return true for short file name (C only)
     */
    @Input
    public abstract Property<Boolean> getShortFileNames();

    /**
     * @return true for long jmp (C only)
     */
    @Input
    public abstract Property<Boolean> getLongjmpSupported();

    /**
     * @return true for heap dump (C only)
     */
    @Input
    public abstract Property<Boolean> getHeapDump();

    /**
     * ONLY for development because it affects optimization level (set to SIMPLE).
     *
     * @return true for fast dependency analysis
     */
    @Input
    public abstract Property<Boolean> getFastDependencyAnalysis();

    /**
     * @return max top level names (JS target only)
     */
    @Input
    public abstract Property<Integer> getMaxTopLevelNames();

    /**
     * @return min heap size (WASM and C targets)
     */
    @Input
    public abstract Property<Integer> getMinHeapSize();

    /**
     * @return max heap size (WASM and C targets)
     */
    @Input
    public abstract Property<Integer> getMaxHeapSize();

    /**
     * @return optimization level (SIMPLE - minimal, ADVANCED - prod. FULL - for WASM)
     */
    @Input
    public abstract Property<TeaVMOptimizationLevel> getOptimizationLevel();

    /**
     * @return list of transformer classes (transforming ClassHolders)
     */
    @Input
    @Optional
    public abstract ListProperty<String> getTransformers();

    /**
     * @return properties for teavm plugins
     */
    @Input
    @Optional
    public abstract MapProperty<String, String> getProperties();

    /**
     * @return classes to preserve
     */
    @Input
    @Optional
    public abstract ListProperty<String> getClassesToPreserve();

    @TaskAction
    public void compile() {
        // teavm configuration used for worker classpath
        final WorkQueue workQueue = getWorkerExecutor().classLoaderIsolation(workerSpec -> {
            final Configuration teavmConf = getProject().getConfigurations().getByName("teavm");
            workerSpec.getClasspath().from(teavmConf);
        });

        // file indicating compilation error
        final File resultFile = getProject().getLayout()
                .getBuildDirectory().file(getName() + ".error").get().getAsFile();
        if (resultFile.exists()) {
            FileUtils.deleteQuietly(resultFile);
        }

        runCompilation(workQueue, resultFile);

        if (getStopOnErrors().get() && resultFile.exists()) {
            String errors = null;
            try {
                errors = Files.readString(resultFile.toPath());
                // shift
                errors = Arrays.stream(errors.split("\n")).map(s -> "\t" + s).collect(Collectors.joining("\n"));
            } catch (IOException ignored) {
                // ignore
            }
            FileUtils.deleteQuietly(resultFile);
            throw new GradleException("Teavm compilation failed" + (errors == null ? "" : (":\n\n" + errors + "\n")));
        }
    }

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    private void runCompilation(final WorkQueue workQueue, final File resultFile) {
        workQueue.submit(CompileWorker.class, parameters -> {
            parameters.getDebug().set(getDebug());

            final List<String> classpath = new ArrayList<>();
            classpath.addAll(getClassPath().get().stream()
                    .map(s -> s.getAsFile().getAbsolutePath()).collect(Collectors.toList()));
            classpath.addAll(getDependencies().getFiles().stream()
                    .map(File::getAbsolutePath).collect(Collectors.toList()));

            parameters.getClassPathEntries().set(classpath);
            parameters.getSourceDirectories().set(getSources());
            parameters.getSourceJars().set(getSourceDependencies().getFiles());
            parameters.getTargetDirectory().set(getTargetDir());
            parameters.getCacheDirectory().set(getCacheDir());

            parameters.getMainClass().set(getMainClass());
            parameters.getEntryPointName().set(getEntryPointName());
            parameters.getTargetFileName().set(getTargetFileName());
            parameters.getTargetType().set(getTargetType());
            parameters.getWasmVersion().set(getWasmVersion());

            parameters.getObfuscated().set(getObfuscated());
            parameters.getStrict().set(getStrict());
            parameters.getSourceFilesCopied().set(getSourceFilesCopied());
            parameters.getIncremental().set(getIncremental());
            parameters.getDebugInformationGenerated().set(getDebugInformationGenerated());
            parameters.getSourceMapsFileGenerated().set(getSourceMapsGenerated());
            parameters.getShortFileNames().set(getShortFileNames());
            parameters.getLongjmpSupported().set(getLongjmpSupported());
            parameters.getHeapDump().set(getHeapDump());
            parameters.getFastDependencyAnalysis().set(getFastDependencyAnalysis());

            parameters.getMaxTopLevelNames().set(getMaxTopLevelNames());
            parameters.getMinHeapSize().set(getMinHeapSize());
            parameters.getMaxHeapSize().set(getMaxHeapSize());
            parameters.getOptimizationLevel().set(getOptimizationLevel());
            parameters.getTransformers().set(getTransformers());
            parameters.getProperties().set(getProperties());
            parameters.getClassesToPreserve().set(getClassesToPreserve());

            parameters.getErrorFile().set(resultFile);
        });

        // waiting for compilation finish to fail task if errors occur
        workQueue.await();
    }
}
