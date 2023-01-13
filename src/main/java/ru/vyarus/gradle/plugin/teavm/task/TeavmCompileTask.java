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
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vyacheslav Rusakov
 * @since 06.01.2023
 */
public abstract class TeavmCompileTask extends DefaultTask {

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    // directories with compiled classes and jar files (dependencies)
    @InputFiles
    public abstract SetProperty<Directory> getClassPath();

    @InputFiles
    public abstract ConfigurableFileCollection getDependencies();

    // directories with sources and source jar files
    @Input
    @Optional
    public abstract SetProperty<Directory> getSources();

    @Input
    @Optional
    public abstract ConfigurableFileCollection getSourceDependencies();

    @OutputDirectory
    public abstract DirectoryProperty getTargetDir();

    @Internal
    public abstract DirectoryProperty getCacheDir();

    @Input
    public abstract Property<String> getMainClass();

    @Input
    @Optional
    public abstract Property<String> getEntryPointName();

    @Input
    @Optional
    public abstract Property<String> getTargetFileName();

    @Input
    public abstract Property<TeaVMTargetType> getTargetType();

    @Input
    public abstract Property<WasmBinaryVersion> getWasmVersion();

    @Input
    public abstract Property<Boolean> getStopOnErrors();

    @Input
    public abstract Property<Boolean> getObfuscated();

    @Input
    public abstract Property<Boolean> getStrict();

    @Input
    public abstract Property<Boolean> getSourceFilesCopied();

    @Input
    public abstract Property<Boolean> getIncremental();

    @Input
    public abstract Property<Boolean> getDebugInformationGenerated();

    @Input
    public abstract Property<Boolean> getSourceMapsGenerated();

    @Input
    public abstract Property<Boolean> getShortFileNames();

    @Input
    public abstract Property<Boolean> getLongjmpSupported();

    @Input
    public abstract Property<Boolean> getHeapDump();

    @Input
    public abstract Property<Boolean> getFastDependencyAnalysis();

    @Input
    public abstract Property<Integer> getMaxTopLevelNames();

    @Input
    public abstract Property<Integer> getMinHeapSize();

    @Input
    public abstract Property<Integer> getMaxHeapSize();

    @Input
    public abstract Property<TeaVMOptimizationLevel> getOptimizationLevel();

    @Input
    @Optional
    public abstract ListProperty<String> getTransformers();

    @Input
    @Optional
    public abstract MapProperty<String, String> getProperties();

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
            resultFile.delete();
        }

        workQueue.submit(CompileWorker.class, parameters -> {
            parameters.getDebug().set(getDebug());

            final List<String> classpath = new ArrayList<>();
            classpath.addAll(getClassPath().get().stream()
                    .map(s-> s.getAsFile().getAbsolutePath()).collect(Collectors.toList()));
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

        workQueue.await();

        if (getStopOnErrors().get() && resultFile.exists()) {
            resultFile.delete();
            throw new GradleException("Teavm compilation failed");
        }
    }
}
