package ru.vyarus.gradle.plugin.teavm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.vm.TeaVMOptimizationLevel;
import ru.vyarus.gradle.plugin.teavm.task.TeavmCompileTask;
import ru.vyarus.gradle.plugin.teavm.util.ClasspathBuilder;
import ru.vyarus.gradle.plugin.teavm.util.FsUtils;
import ru.vyarus.gradle.plugin.teavm.util.SourcesBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.vyarus.gradle.plugin.teavm.util.FsUtils.dir;

/**
 * TeaVM plugin. Compiles java/kotlin/scala sources into javascript.
 * <p>
 * Plugin registers 'teavm' extension. Extension declares source sets to use (by default, main, kotlin and scala).
 * Additional classes and source directories could be configured with special properties (also, default source sets
 * could be flushed to rely only on custom locations).
 * <p>
 * Extension configures production compilation. For development, some options are duplicate in 'devOptions':
 * when `dev = true` these options override default values (dev mode).
 * <p>
 * Plugin could use any teavm version: by default, version would be auto-detected from user classpath
 * (by teavm-classlib jar). If jar not found, then `version` setting would be used. Auto-detection could be
 * disabled with 'autoVersion = false'.
 * <p>
 * IMPORTANT: plugin is compiled with exact teavm version and so only compatible teavm version could be used instead.
 * Also, new options appeared in new teavm would be impossible to use (until plugin would be compiled with new version).
 * <p>
 * NOTE: dev and debug servers are only available in IDEA plugin
 * <p>
 * Debug extension option could be used to print all paths resolved by plugin (plugin debugging).
 * <p>
 * Special "mixedResources" mode allows using static resources like html files (for flavour) inside source directories.
 *
 * @author Vyacheslav Rusakov
 * @since 27.12.2022
 */
@SuppressWarnings("PMD.SystemPrintln")
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
public class TeavmPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        final TeavmExtension extension = project.getExtensions().create("teavm", TeavmExtension.class, project);
        registerConfiguration(project, extension);
        registerShortcuts(project);
        configureTask(project, extension);
        configureResourcesMix(project, extension);
    }

    /**
     * "teavm" configuration used for compiler classpath resolution. Version could be detected from user classpath
     * or configured in extension.
     *
     * @param project   project
     * @param extension extension
     */
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
                String version = extension.getVersion();
                boolean detected = false;
                if (extension.isAutoVersion()) {
                    final String auto = autoDetectVersion(project, extension.getConfigurations());
                    if (auto == null) {
                        project.getLogger().warn("Failed to auto-detect TeaVM version from classpath");
                    } else {
                        version = auto;
                        detected = true;
                    }
                }
                project.getLogger().lifecycle("TeaVM compiler version: {}{}",
                        version, (detected ? " (auto-detected)" : ""));
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

    /**
     * Configures "compileTeavm" task with prod or dev options. Task would depend on "classes" task to compile
     * and process resources before teavm execution.
     *
     * @param project   project
     * @param extension extension
     */
    private void configureTask(final Project project, final TeavmExtension extension) {
        project.getTasks().register("compileTeavm", TeavmCompileTask.class);

        project.getTasks().withType(TeavmCompileTask.class).configureEach(task -> {
            task.getDebug().set(extension.isDebug());
            final DevOptions options = extension.isDev() ? extension.getDevOptions() : extension;

            final ClasspathBuilder cp = new ClasspathBuilder(project,
                    extension.isDebug(),
                    extension.getSourceSets(),
                    extension.getConfigurations(),
                    extension.getExtraClassDirs());
            task.getClassPath().convention(cp.getDirectories());
            cp.dependencies(task.getDependencies());

            if (options.isSourceFilesCopied()) {
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
            configureDevOptions(task, options);

            task.getMaxTopLevelNames().convention(extension.getMaxTopLevelNames());
            task.getMinHeapSize().convention(extension.getMinHeapSize());
            task.getMaxHeapSize().convention(extension.getMaxHeapSize());
            task.getTransformers().convention(extension.getTransformers());
            task.getProperties().convention(extension.getProperties());
            task.getClassesToPreserve().convention(extension.getClassesToPreserve());

            final Task compileJava = project.getTasks().findByPath("classes");
            if (compileJava != null) {
                task.dependsOn(compileJava);
            }
        });
    }

    private void configureDevOptions(final TeavmCompileTask task, final DevOptions options) {
        task.getObfuscated().convention(options.isObfuscated());
        task.getStrict().convention(options.isStrict());
        task.getSourceFilesCopied().convention(options.isSourceFilesCopied());
        task.getIncremental().convention(options.isIncremental());
        task.getDebugInformationGenerated().convention(options.isDebugInformationGenerated());
        task.getSourceMapsGenerated().convention(options.isSourceMapsGenerated());
        task.getShortFileNames().convention(options.isShortFileNames());
        task.getLongjmpSupported().convention(options.isLongjmpSupported());
        task.getHeapDump().convention(options.isHeapDump());
        task.getFastDependencyAnalysis().convention(options.isFastDependencyAnalysis());
        task.getOptimizationLevel().convention(options.getOptimizationLevel());
    }

    /**
     * As configuration performed after project evaluation, it is too late to re-configure source sets. Instead,
     * processResources task is directly configured (to copy resources from source directories ignoring java, kotlin
     * and scala source files).
     *
     * @param project   project
     * @param extension extension
     */
    private void configureResourcesMix(final Project project, final TeavmExtension extension) {
        project.afterEvaluate(p -> {
            if (extension.isMixedResources()) {
                project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
                    if (extension.getSourceSets().contains(sourceSet.getName())) {
                        // source dirs become resource dirs (for prepareResources task)
                        final Set<File> files = sourceSet.getAllJava().getSourceDirectories().getFiles();
                        // source set modification is useless here, instead modifying resources task directly
                        project.getTasks().withType(ProcessResources.class).configureEach(task ->
                                task.from(files, copySpec -> copySpec
                                        .exclude("**/*.java")
                                        .exclude("**/*.kt")
                                        .exclude("**/*.scala")));
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

    private String autoDetectVersion(final Project project, final List<String> configurations) {
        String version = null;
        for (String cf : configurations) {
            final Optional<ResolvedArtifact> tvm = project.getConfigurations().getByName(cf)
                    .getResolvedConfiguration().getResolvedArtifacts()
                    .stream().filter(art -> "teavm-classlib".equals(art.getName()))
                    .findFirst();
            if (tvm.isPresent()) {
                final Properties props = FsUtils.readMavenProperties(tvm.get().getFile(),
                        "META-INF/maven/org.teavm/teavm-classlib/pom.properties");
                if (props != null) {
                    version = props.getProperty("version");
                }
                break;
            }
        }
        return version;
    }
}
