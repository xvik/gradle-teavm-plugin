package ru.vyarus.gradle.plugin.teavm.util;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Source jars and directories extractor. Source dirs extracted from source sets and some dirs might be manually
 * configured. Source jars resolved from jars in classpath configuration. Also, source jars might be
 * found in manually configured directories.
 *
 * @author Vyacheslav Rusakov
 * @since 08.01.2023
 */
@SuppressWarnings("PMD.SystemPrintln")
public class SourcesBuilder {

    private final Project project;
    private final boolean debug;
    private final List<String> sourceSets;
    private final List<String> configurations;
    private final Set<String> extraSourceDirs;

    private final List<Directory> sourceDirs = new ArrayList<>();
    private final List<File> sourceJars = new ArrayList<>();

    public SourcesBuilder(final Project project,
                          final boolean debug,
                          final List<String> sourceSets,
                          final List<String> configurations,
                          final Set<String> extraSourceDirs) {
        this.project = project;
        this.debug = debug;
        this.sourceSets = sourceSets;
        this.configurations = configurations;
        this.extraSourceDirs = extraSourceDirs;
    }

    public void resolveSources() {
        // source sets
        if (!sourceSets.isEmpty()) {
            resolveSourceSetSources();
        }

        // extra dirs
        if (debug && !extraSourceDirs.isEmpty()) {
            System.out.println("Extra source directories: \n" + extraSourceDirs.stream()
                    .map(s -> "\t" + project.file(s).getAbsolutePath()
                            .replace(project.getRootDir().getAbsolutePath() + File.separator, ""))
                    .sorted()
                    .collect(Collectors.joining("\n")));
        }
        for (String dir : extraSourceDirs) {
            sourceDirs.add(project.getLayout().getProjectDirectory().dir(dir));
            // look only for root level jars
            final File dirFile = project.file(dir);
            final File[] jars = dirFile.listFiles(file -> file.getName().toLowerCase().endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                if (debug) {
                    System.out.println("Source jars from extra directory '" + dirFile.getAbsolutePath()
                            .replace(project.getRootDir().getAbsolutePath() + File.separator, "")
                            + "': \n" + Arrays.stream(jars)
                            .map(s -> "\t" + String.format("%-50s  %s", s.getName(), s
                                    .getAbsolutePath().replace(dirFile.getAbsolutePath() + File.separator, "")))
                            .sorted()
                            .collect(Collectors.joining("\n")));
                }
                Collections.addAll(this.sourceJars, jars);
            }
        }

        // resolve sources for dependencies in configurations
        resolveDependencySources();
    }

    public List<Directory> getSourceDirs() {
        return sourceDirs;
    }

    public void dependencies(final ConfigurableFileCollection files) {
        files.from(sourceJars);
    }

    private void resolveSourceSetSources() {
        project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
            if (sourceSets.contains(sourceSet.getName())) {
                final List<Directory> sources = new ArrayList<>();
                for (File file : sourceSet.getAllSource().getSourceDirectories().getFiles()) {
                    sources.add(project.getLayout().getProjectDirectory().dir(file.getAbsolutePath()));
                }
                if (!sources.isEmpty()) {
                    if (debug) {
                        System.out.println("'" + sourceSet.getName() + "' source set sources: \n" + sources.stream()
                                .map(s -> "\t" + s.getAsFile().getAbsolutePath()
                                        .replace(project.getProjectDir().getAbsolutePath() + File.separator, ""))
                                .sorted()
                                .collect(Collectors.joining("\n")));
                    }
                    sourceDirs.addAll(sources);
                }
            }
        });
    }

    private void resolveDependencySources() {
        for (String config : configurations) {
            final Set<ResolvedArtifact> allDeps = project.getConfigurations().getByName(config)
                    .getResolvedConfiguration().getResolvedArtifacts();
            // load sources
            final ArtifactResolutionResult result = project.getDependencies().createArtifactResolutionQuery()
                    .forComponents(allDeps.stream()
                            .map(dep -> dep.getId().getComponentIdentifier()).collect(Collectors.toSet()))
                    .withArtifacts(JvmLibrary.class, SourcesArtifact.class)
                    .execute();

            final List<File> sourceArtifacts = new ArrayList<>();
            result.getResolvedComponents().forEach(component -> {
                component.getArtifacts(SourcesArtifact.class).forEach(artifactResult -> {
                    if (artifactResult instanceof ResolvedArtifactResult) {
                        sourceArtifacts.add(((ResolvedArtifactResult) artifactResult).getFile());
                    }
                });
            });
            if (debug && !sourceArtifacts.isEmpty()) {
                System.out.println("Resolved source artifacts for configuration'" + config + "': \n"
                        + sourceArtifacts.stream().map(s -> "\t" + String.format("%-50s  %s",
                                s.getName(), s.getAbsolutePath()))
                        .sorted()
                        .collect(Collectors.joining("\n")));
            }
            sourceJars.addAll(sourceArtifacts);
        }
    }
}
