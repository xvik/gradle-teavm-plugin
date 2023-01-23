package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 21.01.2023
 */
class ExtraDirsConfigurationKitTest extends AbstractKitTest {

    def "Check plugin execution"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.teavm'
            }
            
            repositories { mavenCentral() }

            teavm {
                sourceSets = [] // disable source sets
                extraClassDirs = ['build/classes/java/main']
                extraSourceDirs = ['src/main/java']
                mainClass = 'example.Main'
                sourceFilesCopied = true // to use source dirs
                debug = true
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
        result.output.contains('Output file successfully built')
    }

}
