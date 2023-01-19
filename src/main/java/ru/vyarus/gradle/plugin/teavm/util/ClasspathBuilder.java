package ru.vyarus.gradle.plugin.teavm.util;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classpath jars and directories extractor.
 *
 * @author Vyacheslav Rusakov
 * @since 08.01.2023
 */
@SuppressWarnings("PMD.SystemPrintln")
public class ClasspathBuilder {

    private final Project project;
    private final boolean debug;
    private final List<String> sourceSets;
    private final Set<String> extraClassDirs;
    private final List<String> configurations;

    public ClasspathBuilder(final Project project,
                            final boolean debug,
                            final List<String> sourceSets,
                            final List<String> configurations,
                            final Set<String> extraClassDirs) {
        this.project = project;
        this.debug = debug;
        this.sourceSets = sourceSets;
        this.configurations = configurations;
        this.extraClassDirs = extraClassDirs;
    }

    public List<Directory> getDirectories() {
        final List<Directory> res = new ArrayList<>();
        // compiled sources
        if (!sourceSets.isEmpty()) {
            project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
                if (sourceSets.contains(sourceSet.getName())) {
                    final List<Directory> collect = sourceSet.getOutput().getFiles().stream()
                            .map(s -> project.getLayout().getProjectDirectory().dir(s.getAbsolutePath()))
                            .collect(Collectors.toList());
                    if (debug) {
                        System.out.println("'" + sourceSet.getName() + "' source set classes: \n" + collect.stream()
                                .map(s -> "\t" + s.getAsFile().getAbsolutePath()
                                        .replace(project.getProjectDir().getAbsolutePath() + "/", ""))
                                .collect(Collectors.joining("\n")));
                    }
                    res.addAll(collect);
                }
            });
        }
        // extra locations
        final List<Directory> extras = new ArrayList<>();
        for (String dir : extraClassDirs) {
            extras.add(project.getLayout().getProjectDirectory().dir(dir));
        }
        if (!extras.isEmpty()) {
            if (debug) {
                System.out.println("Extra class directories: \n" + extras.stream()
                        .map(s -> "\t" + s.getAsFile().getAbsolutePath()
                                .replace(project.getProjectDir().getAbsolutePath() + "/", ""))
                        .collect(Collectors.joining("\n")));
            }
            res.addAll(extras);
        }
        return res;
    }

    public void dependencies(final ConfigurableFileCollection files) {
        // jars
        for (String config : configurations) {
            final List<File> jars = new ArrayList<>(project.getConfigurations().getByName(config).getFiles());
            if (!jars.isEmpty()) {
                if (debug) {
                    System.out.println("'" + config + "' configuration jars: \n" + jars.stream()
                            .map(s -> "\t" + String.format("%-50s  %s", s.getName(), s.getAbsolutePath()))
                            .collect(Collectors.joining("\n")));
                }
                files.from(jars);
            }
        }
    }
}
