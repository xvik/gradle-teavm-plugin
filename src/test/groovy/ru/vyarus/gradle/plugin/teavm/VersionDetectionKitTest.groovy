package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Ignore
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 17.01.2023
 */
// appveyor can't load teavm from custom repo for unknown reason
@IgnoreIf({ System.getenv('APPVEYOR') })
class VersionDetectionKitTest extends AbstractKitTest {

    @Ignore // todo no compatible dev versions yet
    def "Check teavm version auto detection from classpath"() {
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
            
            dependencies {
                implementation "org.teavm:teavm-classlib:0.10.0-dev-5"
            }

            teavm {
                autoVersion = true
                // https://teavm.org/maven/repository/org/teavm/teavm-cli/
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
        result.output.contains('TeaVM compiler version: 0.10.0-dev-5 (auto-detected)')
        result.output.contains('Output file successfully built')
    }

    def "Check teavm disable auto version"() {
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
            
            dependencies {
                implementation "org.teavm:teavm-classlib:0.10.0-dev-5"
            }

            teavm {
                autoVersion = false
                // https://teavm.org/maven/repository/org/teavm/teavm-cli/
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
        result.output.contains('TeaVM compiler version: 0.9.1')
        result.output.contains('Output file successfully built')
    }
}
