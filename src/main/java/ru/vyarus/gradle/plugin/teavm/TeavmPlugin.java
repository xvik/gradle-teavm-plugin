package ru.vyarus.gradle.plugin.teavm;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;
import ru.vyarus.gradle.plugin.teavm.task.TeavmCompileTask;
import ru.vyarus.gradle.plugin.teavm.util.ClasspathBuilder;
import ru.vyarus.gradle.plugin.teavm.util.SourcesBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.vyarus.gradle.plugin.teavm.util.FsUtils.dir;

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
        configureTask(project, extension);
        configureResourcesMix(project, extension);
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
                dependencies.add(project.getDependencies().create("org.teavm:teavm-cli:" + extension.getVersion()));
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

    private void configureTask(final Project project, final TeavmExtension extension) {
        project.getTasks().register("compileTeavm", TeavmCompileTask.class);

        project.getTasks().withType(TeavmCompileTask.class).configureEach(task -> {
            final TeavmExtension.Dev dev = extension.isDev() ? extension.getDevOptions() : null;

            task.getDebug().set(extension.isDebug());

            final ClasspathBuilder cp = new ClasspathBuilder(project,
                    extension.isDebug(),
                    extension.getSourceSets(),
                    extension.getConfigurations(),
                    extension.getExtraClassDirs());
            task.getClassPath().convention(cp.getDirectories());
            cp.dependencies(task.getDependencies());

            if (extension.isSourceFilesCopied()) {
                final SourcesBuilder src = new SourcesBuilder(project,
                        extension.isDebug(),
                        extension.getSourceSets(),
                        extension.getConfigurations(),
                        extension.getExtraSourceDirs());
                src.resolveSources();
                task.getSources().convention(src.getSourceDirs());
                src.dependencies(task.getSourceDependencies());
            }

            task.getTargetDir().convention(dir(project, extension.getTargetDir()));
            task.getCacheDir().convention(dir(project, extension.getCacheDir()));

            task.getMainClass().convention(extension.getMainClass());
            task.getEntryPointName().convention(extension.getEntryPointName());
            task.getTargetFileName().convention(extension.getTargetFileName());
            task.getTargetType().convention(extension.getTargetType());
            task.getWasmVersion().convention(extension.getWasmVersion());

            task.getStopOnErrors().convention(extension.isStopOnErrors());
            task.getObfuscated().convention(dev == null ? extension.isObfuscated() : dev.isObfuscated());
            task.getStrict().convention(dev == null ? extension.isStrict() : dev.isStrict());
            task.getSourceFilesCopied().convention(
                    dev == null ? extension.isSourceFilesCopied() : dev.isSourceFilesCopied());
            task.getIncremental().convention(dev == null ? extension.isIncremental() : dev.isIncremental());
            task.getDebugInformationGenerated().convention(
                    dev == null ? extension.isDebugInformationGenerated() : dev.isDebugInformationGenerated());
            task.getSourceMapsGenerated().convention(
                    dev == null ? extension.isSourceMapsGenerated() : dev.isSourceMapsGenerated());
            task.getShortFileNames().convention(extension.isShortFileNames());
            task.getLongjmpSupported().convention(extension.isLongjmpSupported());
            task.getHeapDump().convention(extension.isHeapDump());
            task.getFastDependencyAnalysis().convention(
                    dev == null ? extension.isFastDependencyAnalysis() : dev.isFastDependencyAnalysis());

            task.getMaxTopLevelNames().convention(extension.getMaxTopLevelNames());
            task.getMinHeapSize().convention(extension.getMinHeapSize());
            task.getMaxHeapSize().convention(extension.getMaxHeapSize());
            task.getOptimizationLevel().convention(
                    dev == null ? extension.getOptimizationLevel() : dev.getOptimizationLevel());
            task.getTransformers().convention(extension.getTransformers());
            task.getProperties().convention(extension.getProperties());
            task.getClassesToPreserve().convention(extension.getClassesToPreserve());

            Task compileJava = project.getTasks().findByPath("classes");
            if (compileJava != null) {
                task.dependsOn(compileJava);
            }
        });
    }

    private void configureResourcesMix(final Project project, final TeavmExtension extension) {
        project.afterEvaluate(p -> {
            if (extension.isMixedResources()) {
                project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
                    if (extension.getSourceSets().contains(sourceSet.getName())) {
                        // source dirs become resource dirs (for prepareResources task)
                        final Set<File> files = sourceSet.getAllJava().getSourceDirectories().getFiles();
                        // source set modification is useless here, instead modifying resources task directly
                        project.getTasks().withType(ProcessResources.class).configureEach(task -> {
                            task.from(files, copySpec -> copySpec
                                    .exclude("**/*.java")
                                    .exclude("**/*.kt")
                                    .exclude("**/*.scala"));
                        });
                        if (extension.isDebug()) {
                            System.out.println("Mixed resources mode for source set '" + sourceSet.getName() + "': \n"
                                    + files.stream().map(file -> "\t" + file.getAbsolutePath()
                                            .replace(project.getProjectDir().getAbsolutePath() + "/", ""))
                                    .collect(Collectors.joining("\n")));
                        }
                    }
                });
            }
        });
    }
}
