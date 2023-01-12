package ru.vyarus.gradle.plugin.teavm.util;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Vyacheslav Rusakov
 * @since 08.01.2023
 */
public class ClasspathBuilder {

    private final Project project;
    private final boolean debug;
    private final List<String> sourceSets;
    private final Set<Directory> extraClassDirs;
    private final Set<String> configurations;

    public ClasspathBuilder(final Project project,
                            final boolean debug,
                            final List<String> sourceSets,
                            final Set<String> configurations,
                            final Set<Directory> extraClassDirs) {
        this.project = project;
        this.debug = debug;
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
                    final List<String> collect = sourceSet.getOutput().getFiles().stream()
                            .map(File::getAbsolutePath).collect(Collectors.toList());
                    if (debug) {
                        System.out.println("'" + sourceSet.getName() + "' source set classes: \n" + collect.stream()
                                .map(s -> "\t" + s.replace(project.getProjectDir().getAbsolutePath() + "/", ""))
                                .collect(Collectors.joining("\n")));
                    }
                    res.addAll(collect);
                }
            });
        }
        // extra locations
        final List<String> extras = new ArrayList<>();
        for (Directory dir : extraClassDirs) {
            extras.add(dir.getAsFile().getAbsolutePath());
        }
        if (!extras.isEmpty()) {
            if (debug) {
                System.out.println("Extra class directories: \n" + extras.stream()
                        .map(s -> "\t" + s).collect(Collectors.joining("\n")));
            }
            res.addAll(extras);
        }

        // jars
        for (String config : configurations) {
            final List<File> jars = new ArrayList<>(project.getConfigurations().getByName(config).getFiles());
            if (!jars.isEmpty()) {
                if (debug) {
                    System.out.println("'" + config + "' configuration jars: \n" + jars.stream()
                            .map(s -> "\t" + String.format("%-50s  %s", s.getName(), s.getAbsolutePath()))
                            .collect(Collectors.joining("\n")));
                }
                res.addAll(jars.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
            }
        }
        return res;
    }
}
