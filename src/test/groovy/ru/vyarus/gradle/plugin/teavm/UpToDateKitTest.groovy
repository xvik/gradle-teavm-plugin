package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 12.01.2023
 */
class UpToDateKitTest extends AbstractKitTest {

    def "Check source up-to-date check"() {
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
        BuildResult result = run('compileTeavm')

        then: "task successful"
        result.task(':compileTeavm').outcome == TaskOutcome.SUCCESS

        when: "run again"
        result = run('compileTeavm')

        then: "task successful"
        result.task(':compileTeavm').outcome == TaskOutcome.UP_TO_DATE
        
        when: "source changed"
        file('src/main/java/example/Main.java').newWriter().withWriter {
            it << """
package example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Do nothing, but different");
    }
}
"""
        }
        result = run('compileTeavm')

        then:
        result.task(':compileTeavm').outcome == TaskOutcome.SUCCESS
    }
}
