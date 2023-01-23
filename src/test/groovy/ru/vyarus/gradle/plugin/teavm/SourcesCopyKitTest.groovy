package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 12.01.2023
 */
class SourcesCopyKitTest extends AbstractKitTest {

    def "Check sources copy"() {
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
                debug = false
                
                mainClass = 'example.Main'
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
        result.output.contains('Output file successfully built')

        and: "own source copied"
        file('build/teavm/src/example/Main.java').exists()

        and: "sources from jars copied"
        file('build/teavm/src/org/teavm/classlib/impl/IntegerUtil.java').exists()
    }

}


