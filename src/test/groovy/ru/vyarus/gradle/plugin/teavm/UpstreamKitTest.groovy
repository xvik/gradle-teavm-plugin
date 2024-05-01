package ru.vyarus.gradle.plugin.teavm


import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 21.01.2023
 */
class UpstreamKitTest extends AbstractKitTest {

    String GRADLE_VERSION = '8.7'
    // https://teavm.org/maven/repository/org/teavm/teavm-classlib/
    String TEAVM_RECENT = '0.10.0-dev-12'

    def "Check plugin execution for the latest gradle"() {
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
        file('src/main/java/example/Main.java') << """
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

    def "Check plugin execution with recent compiler"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.teavm'
            }
            
            repositories { 
                mavenCentral()
                maven { url "https://teavm.org/maven/repository" }  
            }

            teavm {                
                version = '$TEAVM_RECENT'
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
        result.output.contains("TeaVM compiler version: $TEAVM_RECENT")
        result.output.contains('Output file successfully built')
    }
}
