package ru.vyarus.gradle.plugin.teavm.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;
import ru.vyarus.gradle.plugin.teavm.util.ClasspathBuilder;
import ru.vyarus.gradle.plugin.teavm.util.FsUtils;
import ru.vyarus.gradle.plugin.teavm.util.SourcesBuilder;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static ru.vyarus.gradle.plugin.teavm.util.FsUtils.dir;

/**
 * @author Vyacheslav Rusakov
 * @since 06.01.2023
 */
public abstract class TeavmCompileTask extends DefaultTask {

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    // source and class dirs extracted from source sets
    public abstract ListProperty<String> getSourceSets();

    // additional directories with compiled classes
    public abstract SetProperty<Directory> getExtraClassDirs();

    // configurations with dependencies
    public abstract SetProperty<String> getConfigurations();

    // extra dependent sources dir
    public abstract SetProperty<Directory> getExtraSourceDirs();

    public abstract DirectoryProperty getTargetDir();

    public abstract DirectoryProperty getCacheDir();

    public abstract Property<String> getMainClass();

    public abstract Property<String> getEntryPointName();

    public abstract Property<String> getTargetFileName();

    public abstract Property<TeaVMTargetType> getTargetType();

    public abstract Property<WasmBinaryVersion> getWasmVersion();

    public abstract Property<Boolean> getStopOnErrors();

    public abstract Property<Boolean> getObfuscated();

    public abstract Property<Boolean> getStrict();

    public abstract Property<Boolean> getSourceFilesCopied();

    public abstract Property<Boolean> getIncremental();

    public abstract Property<Boolean> getDebugInformationGenerated();

    public abstract Property<Boolean> getSourceMapsGenerated();

    public abstract Property<Boolean> getShortFileNames();

    public abstract Property<Boolean> getLongjmpSupported();

    public abstract Property<Boolean> getHeapDump();

    public abstract Property<Boolean> getFastDependencyAnalysis();

    public abstract Property<Integer> getMaxTopLevelNames();

    public abstract Property<Integer> getMinHeapSize();

    public abstract Property<Integer> getMaxHeapSize();

    public abstract Property<TeaVMOptimizationLevel> getOptimizationLevel();

    public abstract ListProperty<String> getTransformers();

    public abstract MapProperty<String, String> getProperties();

    public abstract ListProperty<String> getClassesToPreserve();

    @TaskAction
    public void compile() {
        final WorkQueue workQueue = getWorkerExecutor().noIsolation();

        workQueue.submit(CompileWorker.class, parameters -> {

            final ClasspathBuilder cp = new ClasspathBuilder(getProject(),
                    getSourceSets().get(),
                    getConfigurations().get(),
                    getExtraClassDirs().get());

            final SourcesBuilder src = new SourcesBuilder(getProject(),
                    getSourceSets().get(),
                    // avoid source jars resolution if not required
                    getSourceFilesCopied().get() ? getConfigurations().get() : Collections.emptySet(),
                    getExtraSourceDirs().get());
            src.resolveSources();

//                parameters.getReportDir().set();
            parameters.getClassPathEntries().set(cp.prepareClassPath());
            parameters.getSourceDirectories().set(src.getSourceDirs());
            parameters.getSourceJars().set(src.getSourceJars());
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
        });

        workQueue.await();

        // todo check for error
    }

    public void setSourceSets(String... sourceSets) {
        getSourceSets().set(Arrays.asList(sourceSets));
    }

    public void setExtraClassDirs(String... dirs) {
        getExtraClassDirs().set(FsUtils.dirs(getProject(), Arrays.asList(dirs)));
    }

    public void setTargetDir(String dir) {
        getTargetDir().set(dir(getProject(), dir));
    }

    public void setConfigurations(String... configs) {
        getConfigurations().set(Arrays.asList(configs));
    }

    public void setExtraSourceDirs(String... dirs) {
        getExtraSourceDirs().set(FsUtils.dirs(getProject(), Arrays.asList(dirs)));
    }

    public void setCacheDir(String dir) {
        getCacheDir().set(dir(getProject(), dir));
    }

    public void setMainClass(String main) {
        getMainClass().set(main);
    }

    public void setEntryPointName(String name) {
        getEntryPointName().set(name);
    }

    public void setTargetFileName(String name) {
        getTargetFileName().set(name);
    }

    public void setTargetType(TeaVMTargetType type) {
        getTargetType().set(type);
    }

    public void setWasmVersion(WasmBinaryVersion version) {
        getWasmVersion().set(version);
    }

    public void setStopOnErrors(boolean enable) {
        getStopOnErrors().set(enable);
    }

    public void setObfuscated(boolean enable) {
        getObfuscated().set(enable);
    }

    public void setStrict(boolean enable) {
        getStrict().set(enable);
    }

    public void setSourceFilesCopied(boolean enable) {
        getSourceFilesCopied().set(enable);
    }

    public void setIncremental(boolean enable) {
        getIncremental().set(enable);
    }

    public void setDebugInformationGenerated(boolean enable) {
        getDebugInformationGenerated().set(enable);
    }

    public void setSourceMapsGenerated(boolean enable) {
        getSourceMapsGenerated().set(enable);
    }

    public void setShortFileNames(boolean enable) {
        getShortFileNames().set(enable);
    }

    public void setLongjmpSupported(boolean enable) {
        getLongjmpSupported().set(enable);
    }

    public void setHeapDump(boolean enable) {
        getHeapDump().set(enable);
    }

    public void setFastDependencyAnalysis(boolean enable) {
        getFastDependencyAnalysis().set(enable);
    }

    public void setMaxTopLevelNames(int max) {
        getMaxTopLevelNames().set(max);
    }

    public void setMinHeapSize(int size) {
        getMinHeapSize().set(size);
    }

    public void setMaxHeapSize(int size) {
        getMaxHeapSize().set(size);
    }

    public void setOptimizationLevel(TeaVMOptimizationLevel level) {
        getOptimizationLevel().set(level);
    }

    public void setTransformers(String... transformers) {
        getTransformers().set(Arrays.asList(transformers));
    }

    public void setProperties(Map<String, String> props) {
        getProperties().set(props);
    }

    public void setClassesToPreserve(String... classes) {
        getClassesToPreserve().set(Arrays.asList(classes));
    }
}
