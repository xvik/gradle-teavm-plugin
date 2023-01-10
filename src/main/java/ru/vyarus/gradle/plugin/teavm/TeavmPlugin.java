package ru.vyarus.gradle.plugin.teavm;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.attributes.Usage;
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
        registerConfiguration(project, extension);
        registerShortcuts(project);
        registerTasks(project);
        configureTaskDefaults(project, extension);
    }

    private void registerConfiguration(final Project project, final TeavmExtension extension) {
        // internal configuration used for cli dependency resolution
        project.getConfigurations().create("teavm", conf -> {
            conf.attributes(attrs -> {
                attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
            });
            conf.setDescription("TeaVM compiler classpath");
            conf.setTransitive(true);
            conf.setVisible(false);
            conf.setCanBeConsumed(false);
            conf.setCanBeResolved(true);

            conf.defaultDependencies(dependencies -> {
                dependencies.add(project.getDependencies().create("org.teavm:teavm-cli:"+extension.getToolVersion()));
            });
        });
    }

    /**
     * Shortcuts required to simplify configuration in build file: to use task class and teavm enums constants
     * without complete package.
     *
     * @param project project instance
     */
    private void registerShortcuts(final Project project) {
        final ExtraPropertiesExtension extraProps = project.getExtensions().getExtraProperties();
        // task shortcut
        extraProps.set(TeavmCompileTask.class.getSimpleName(), TeavmCompileTask.class);

        // enum shortcuts
        Arrays.asList(TeaVMTargetType.values()).forEach(type -> extraProps.set(type.name(), type));
        Arrays.asList(WasmBinaryVersion.values()).forEach(type -> extraProps.set(type.name(), type));
        Arrays.asList(TeaVMOptimizationLevel.values()).forEach(type -> extraProps.set(type.name(), type));
    }

    private void registerTasks(final Project project) {
        project.getTasks().register("compileTeavm", TeavmCompileTask.class);

        // special task with all debug options enabled
        project.getTasks().register("compileTeavmDev", TeavmCompileTask.class, task -> {
            task.getOptimizationLevel().set(TeaVMOptimizationLevel.SIMPLE);
            task.getSourceMapsGenerated().set(true);
            task.getDebugInformationGenerated().set(true);
            task.getObfuscated().set(false);
            task.getSourceFilesCopied().set(true);
        });
    }

    private void configureTaskDefaults(final Project project, final TeavmExtension extension) {
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
}
