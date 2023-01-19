package ru.vyarus.gradle.plugin.teavm.util;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    /**
     * Resolve multiple directories (with {@link #dir(org.gradle.api.Project, String)}.
     *
     * @param project project
     * @param dirs    directories
     * @return list of directory objects
     */
    public static List<Directory> dirs(final Project project, final Iterable<String> dirs) {
        final List<Directory> res = new ArrayList<>();
        for (String dir : dirs) {
            res.add(dir(project, dir));
        }
        return res;
    }

    /**
     * Read maven properties file from jar.
     *
     * @param jar  jar file
     * @param path properties path
     * @return read properties or null if not found
     */
    @SuppressWarnings({"PMD.SystemPrintln", "PMD.ReturnEmptyCollectionRatherThanNull"})
    public static Properties readMavenProperties(final File jar, final String path) {
        try (ZipFile file = new ZipFile(jar)) {
            final ZipEntry entry = file.getEntry(path);
            if (entry == null) {
                System.err.println("Maven properties file not found inside " + jar.getName() + ": " + path);
                return null;
            }
            final Properties props = new Properties();
            props.load(file.getInputStream(entry));
            return props;
        } catch (Exception e) {
            throw new IllegalStateException("Maven properties resolution failed in jar " + jar.getAbsolutePath(), e);
        }
    }
}
