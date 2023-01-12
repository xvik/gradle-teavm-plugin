package ru.vyarus.gradle.plugin.teavm.util;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Vyacheslav Rusakov
 * @since 08.01.2023
 */
public class SourcesBuilder {

    private final Project project;
    private final boolean debug;
    private final List<String> sourceSets;
    private final Set<String> configurations;
    private final Set<Directory> extraSourceDirs;

    private final List<Directory> sourceDirs = new ArrayList<>();
    private final List<RegularFile> sourceJars = new ArrayList<>();

    public SourcesBuilder(final Project project,
                          final boolean debug,
                          final List<String> sourceSets,
                          final Set<String> configurations,
                          final Set<Directory> extraSourceDirs) {
        this.project = project;
        this.debug = debug;
        this.sourceSets = sourceSets;
        this.configurations = configurations;
        this.extraSourceDirs = extraSourceDirs;
    }

    public void resolveSources() {
        // source sets
        if (!sourceSets.isEmpty()) {
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
                                            .replace(project.getProjectDir().getAbsolutePath() + "/", ""))
                                    .collect(Collectors.joining("\n")));
                        }
                        sourceDirs.addAll(sources);
                    }
                }
            });
        }

        // extra dirs
        if (!debug && extraSourceDirs.isEmpty()) {
            System.out.println("Extra source directories: \n" + extraSourceDirs.stream()
                    .map(s -> "\t" + s.getAsFile().getAbsolutePath()
                            .replace(project.getRootDir().getAbsolutePath() + "/", ""))
                    .collect(Collectors.joining("\n")));
        }
        for (Directory dir : extraSourceDirs) {
            sourceDirs.add(dir);
            final List<RegularFile> jars = new ArrayList<>();
            // look only for root level jars
            for (File jar : dir.getAsFile().listFiles(file -> file.getName().toLowerCase().endsWith(".jar"))) {
                jars.add(project.getLayout().getProjectDirectory().file(jar.getAbsolutePath()));
            }
            if (!jars.isEmpty()) {
                if (debug) {
                    System.out.println("Source jars from extra directory '" + dir.getAsFile().getAbsolutePath()
                            .replace(project.getRootDir().getAbsolutePath() + "/", "") + "': \n" + jars.stream()
                            .map(s -> "\t" + String.format("%-50s  %s", s.getAsFile().getName(), s.getAsFile()
                                    .getAbsolutePath().replace(dir.getAsFile().getAbsolutePath() + "/", "")))
                            .collect(Collectors.joining("\n")));
                }
                sourceJars.addAll(jars);
            }
        }

        // resolve sources for dependencies in configurations
        for (String config : configurations) {
            final Set<ResolvedArtifact> allDeps = project.getConfigurations().getByName(config)
                    .getResolvedConfiguration().getResolvedArtifacts();
            // load sources
            final ArtifactResolutionResult result = project.getDependencies().createArtifactResolutionQuery()
                    .forComponents(allDeps.stream()
                            .map(dep -> dep.getId().getComponentIdentifier()).collect(Collectors.toSet()))
                    .withArtifacts(JvmLibrary.class, SourcesArtifact.class)
                    .execute();

            final List<RegularFile> sourceArtifacts = new ArrayList<>();
            result.getResolvedComponents().forEach(component -> {
                component.getArtifacts(SourcesArtifact.class).forEach(artifactResult -> {
                    if (artifactResult instanceof ResolvedArtifactResult) {
                        final String filePath = ((ResolvedArtifactResult) artifactResult).getFile().getAbsolutePath();
                        sourceArtifacts.add(project.getLayout().getProjectDirectory().file(filePath));
                    }
                });
            });
            if (debug && !sourceArtifacts.isEmpty()) {
                System.out.println("Resolved source artifacts for configuration'" + config + "': \n"
                        + sourceArtifacts.stream().map(s -> "\t" + String.format("%-50s  %s",
                                s.getAsFile().getName(), s.getAsFile().getAbsolutePath()))
                        .collect(Collectors.joining("\n")));
            }
            sourceJars.addAll(sourceArtifacts);
        }
    }

    public List<Directory> getSourceDirs() {
        return sourceDirs;
    }

    public List<RegularFile> getSourceJars() {
        return sourceJars;
    }
}
