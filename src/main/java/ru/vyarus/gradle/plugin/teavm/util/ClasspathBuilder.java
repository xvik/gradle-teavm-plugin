package ru.vyarus.gradle.plugin.teavm.util;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Vyacheslav Rusakov
 * @since 08.01.2023
 */
public class ClasspathBuilder {

    private final Project project;
    private final List<String> sourceSets;
    private final Set<Directory> extraClassDirs;
    private final Set<String> configurations;

    public ClasspathBuilder(final Project project,
                            final List<String> sourceSets,
                            final Set<String> configurations,
                            final Set<Directory> extraClassDirs) {
        this.project = project;
        this.sourceSets = sourceSets;
        this.configurations = configurations;
        this.extraClassDirs = extraClassDirs;
    }

    public List<String> prepareClassPath() {
        final List<String> res = new ArrayList<>();
        // compiled sources
        if (!sourceSets.isEmpty()) {
            project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
                if (sourceSets.contains(sourceSet.getName())) {
                    res.add(sourceSet.getOutput().getSingleFile().getAbsolutePath());
                }
            });
        }
        // extra locations
        for(Directory dir: extraClassDirs) {
            res.add(dir.getAsFile().getAbsolutePath());
        }
        // jars
        for(String config: configurations) {
            for(File file: project.getConfigurations().getByName(config).getFiles()) {
                res.add(file.getAbsolutePath());
            }
        }
        return res;
    }
}
