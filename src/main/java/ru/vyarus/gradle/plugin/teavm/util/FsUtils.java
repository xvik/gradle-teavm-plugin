package ru.vyarus.gradle.plugin.teavm.util;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 08.01.2023
 */
public final class FsUtils {

    private FsUtils() {
    }

    public static Directory dir(Project project, String dir) {
        return project.getLayout().getProjectDirectory().dir(dir);
    }

    public static List<Directory> dirs(Project project, Iterable<String> dirs) {
        List<Directory> res = new ArrayList<>();
        for (String dir : dirs) {
            res.add(dir(project, dir));
        }
        return res;
    }
}
