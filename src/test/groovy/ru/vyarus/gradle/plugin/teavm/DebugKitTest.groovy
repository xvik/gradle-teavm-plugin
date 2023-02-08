package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 23.01.2023
 */
class DebugKitTest extends AbstractKitTest {

    def "Check debug output"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.teavm'
            }
                       
            repositories { mavenCentral() }
            dependencies {
                implementation "org.teavm:teavm-classlib:\${teavm.version}"
            }

            teavm {
                debug = true
                
                mainClass = 'example.Main'
                extraClassDirs = ['build/classes/foo']
                extraSourceDirs = ['src/foo/java']
                sourceFilesCopied = true
            }

        """
        file('src/main/java/example/Main.java')  << """
package example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Do nothing");
    }
}
"""

        when: "run task"
        debug()
        BuildResult result = run('compileTeavm')

        then: "task successful"
        result.task(':compileTeavm').outcome == TaskOutcome.SUCCESS
        def out = result.output.replace("\r", "").replace(File.separator, '/')
        out.contains('Output file successfully built')

        out.contains("""'main' source set classes: 
\tbuild/classes/java/main
\tbuild/resources/main
Extra class directories: 
\tbuild/classes/foo
""")
        out.contains('\'runtimeClasspath\' configuration jars:')

        out.contains("""'main' source set sources: 
\tsrc/main/java
\tsrc/main/resources
Extra source directories: 
\tsrc/foo/java
""")
        out.contains('Resolved source artifacts for configuration\'runtimeClasspath\':')

        out.contains("""Resources used: 51
\texample/Main.java
""")
        out.contains("""Generated files: 51
\tclasses.js (35 KB)
 \tsrc/example/Main.java (134 bytes)
""")
    }
}
