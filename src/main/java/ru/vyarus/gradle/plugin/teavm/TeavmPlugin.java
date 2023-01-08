package ru.vyarus.gradle.plugin.teavm;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;
import ru.vyarus.gradle.plugin.teavm.task.TeavmCompileTask;

import java.util.Arrays;

import static ru.vyarus.gradle.plugin.teavm.util.FsUtils.dir;
import static ru.vyarus.gradle.plugin.teavm.util.FsUtils.dirs;

/**
 * teavm plugin.
 *
 * @author Vyacheslav Rusakov
 * @since 27.12.2022
 */
public class TeavmPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        final TeavmExtension extension = project.getExtensions().create("teavm", TeavmExtension.class, project);
        configureTasks(project);
        configureDefaults(project, extension);
        configureShortcuts(project);
    }

    private void configureTasks(final Project project) {
        project.getTasks().register("compileTeavm", TeavmCompileTask.class);

        // special task with all debug options enabled
        project.getTasks().register("compileTeavmDev", TeavmCompileTask.class, task -> {
            task.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
            task.setSourceMapsGenerated(true);
            task.setDebugInformationGenerated(true);
            task.setObfuscated(false);
            task.setSourceFilesCopied(true);
        });
    }

    private void configureDefaults(final Project project, final TeavmExtension extension) {
        project.getTasks().withType(TeavmCompileTask.class).configureEach(task -> {
            task.getSourceSets().convention(extension.getSourceSets());
            task.getExtraClassDirs().convention(dirs(project, extension.getExtraClassDirs()));
            task.getConfigurations().convention(extension.getConfigurations());
            task.getExtraSourceDirs().convention(dirs(project, extension.getExtraSourceDirs()));
            task.getTargetDir().convention(dir(project, extension.getTargetDir()));
            task.getCacheDir().convention(dir(project, extension.getCacheDir()));

            task.getMainClass().convention(extension.getMainClass());
            task.getEntryPointName().convention(extension.getEntryPointName());
            task.getTargetFileName().convention(extension.getTargetFileName());
            task.getTargetType().convention(extension.getTargetType());
            task.getWasmVersion().convention(extension.getWasmVersion());

            task.getStopOnErrors().convention(extension.isStopOnErrors());
            task.getObfuscated().convention(extension.isObfuscated());
            task.getStrict().convention(extension.isStrict());
            task.getSourceFilesCopied().convention(extension.isSourceFilesCopied());
            task.getIncremental().convention(extension.isIncremental());
            task.getDebugInformationGenerated().convention(extension.isDebugInformationGenerated());
            task.getSourceMapsGenerated().convention(extension.isSourceMapsGenerated());
            task.getShortFileNames().convention(extension.isShortFileNames());
            task.getLongjmpSupported().convention(extension.isLongjmpSupported());
            task.getHeapDump().convention(extension.isHeapDump());
            task.getFastDependencyAnalysis().convention(extension.isFastDependencyAnalysis());

            task.getMaxTopLevelNames().convention(extension.getMaxTopLevelNames());
            task.getMinHeapSize().convention(extension.getMinHeapSize());
            task.getMaxHeapSize().convention(extension.getMaxHeapSize());
            task.getOptimizationLevel().convention(extension.getOptimizationLevel());
            task.getTransformers().convention(extension.getTransformers());
            task.getProperties().convention(extension.getProperties());
            task.getClassesToPreserve().convention(extension.getClassesToPreserve());

            Task compileJava = project.getTasks().findByPath("compileJava");
            if (compileJava != null) {
                task.dependsOn(compileJava);
            }
            // todo kotlin and scala support
        });
    }

    private void configureShortcuts(Project project) {
        final ExtraPropertiesExtension extraProps = project.getExtensions().getExtraProperties();
        // task shortcut
        extraProps.set(TeavmCompileTask.class.getSimpleName(), TeavmCompileTask.class);

        // enum shortcuts
        Arrays.asList(TeaVMTargetType.values()).forEach(type -> extraProps.set(type.name(), type));
        Arrays.asList(WasmBinaryVersion.values()).forEach(type -> extraProps.set(type.name(), type));
        Arrays.asList(TeaVMOptimizationLevel.values()).forEach(type -> extraProps.set(type.name(), type));
    }
}
