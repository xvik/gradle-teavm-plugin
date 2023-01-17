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

    public static Properties readMavenProperties(final File jar, final String path) {
        try (final ZipFile file = new ZipFile(jar)) {
            final ZipEntry entry = file.getEntry(path);
            if (entry == null) {
                System.err.println("Maven properties file not found inside " + jar.getName() + ": " + path);
                return null;
            }
            final Properties props = new Properties();
            props.load(file.getInputStream(entry));
            return props;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
