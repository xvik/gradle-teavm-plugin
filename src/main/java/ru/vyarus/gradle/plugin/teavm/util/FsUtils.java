package ru.vyarus.gradle.plugin.teavm.util;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;

/**
 * FS-related utils.
 *
 * @author Vyacheslav Rusakov
 * @since 08.01.2023
 */
public final class FsUtils {

    private FsUtils() {
    }

    /**
     * Resolve gradle {@link org.gradle.api.file.Directory} object from directory declaration. Relative directory
     * location considered from current project root.
     *
     * @param project project
     * @param dir     directory path
     * @return directory object
     */
    public static Directory dir(final Project project, final String dir) {
        return project.getLayout().getProjectDirectory().dir(dir);
    }
}
