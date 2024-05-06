package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 19.01.2023
 */
@IgnoreIf({jvm.java17Compatible}) // only gradle 7.3 supports java 17
class LegacyKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '6.2'

    def "Check plugin execution"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.teavm'
            }
            
            repositories { mavenCentral() }

            teavm {
                mainClass = 'example.Main'
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
        BuildResult result = runVer(GRADLE_VERSION, 'compileTeavm')

        then: "task successful"
        result.task(':compileTeavm').outcome == TaskOutcome.SUCCESS
        result.output.contains('Output file successfully built')
    }
}
