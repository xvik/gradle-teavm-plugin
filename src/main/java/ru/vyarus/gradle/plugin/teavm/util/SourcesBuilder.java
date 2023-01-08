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
    private final List<String> sourceSets;
    private final Set<String> configurations;
    private final Set<Directory> extraSourceDirs;

    private final List<Directory> sourceDirs = new ArrayList<>();
    private final List<RegularFile> sourceJars = new ArrayList<>();

    public SourcesBuilder(final Project project,
                          final List<String> sourceSets,
                          final Set<String> configurations,
                          final Set<Directory> extraSourceDirs) {
        this.project = project;
        this.sourceSets = sourceSets;
        this.configurations = configurations;
        this.extraSourceDirs = extraSourceDirs;
    }

    public void resolveSources() {
        // source sets
        if (!sourceSets.isEmpty()) {
            project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
                if (sourceSets.contains(sourceSet.getName())) {
                    for (File file : sourceSet.getAllSource().getFiles()) {
                        sourceDirs.add(project.getLayout().getProjectDirectory().dir(file.getAbsolutePath()));
                    }
                }
            });
        }
        // extra dirs
        for (Directory dir : extraSourceDirs) {
            sourceDirs.add(dir);
            // look only for root level jars
            for (File jar : dir.getAsFile().listFiles(file -> file.getName().toLowerCase().endsWith(".jar"))) {
                sourceJars.add(project.getLayout().getProjectDirectory().file(jar.getAbsolutePath()));
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

            result.getResolvedComponents().forEach(component -> {
                component.getArtifacts(SourcesArtifact.class).forEach(artifactResult -> {
                    if (artifactResult instanceof ResolvedArtifactResult) {
                        final String filePath = ((ResolvedArtifactResult) artifactResult).getFile().getAbsolutePath();
                        sourceJars.add(project.getLayout().getProjectDirectory().file(filePath));
                    }
                });
            });
        }
    }

    public List<Directory> getSourceDirs() {
        return sourceDirs;
    }

    public List<RegularFile> getSourceJars() {
        return sourceJars;
    }
}
