package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 27.12.2022
 */
class TeavmPluginKitTest extends AbstractKitTest {

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
        BuildResult result = run('compileTeavm')

        then: "task successful"
        result.task(':compileTeavm').outcome == TaskOutcome.SUCCESS
//        result.output.contains('fooo: 1')
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
                // https://teavm.org/maven/repository/org/teavm/teavm-cli/
                toolVersion = '0.7.0-dev-1209'
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
//        result.output.contains('fooo: 1')
    }

    def "Check compilation error"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.teavm'
            }
            
            repositories { mavenCentral() }
            
            dependencies {
                implementation "org.teavm:teavm-core:0.7.0"
                implementation "org.teavm:teavm-classlib:0.7.0"
                implementation "org.teavm:teavm-metaprogramming-impl:0.7.0"
            }

            teavm {
                mainClass = 'example.Main'
            }

        """
        file('src/main/java/example/Main.java')  << """
package example;

import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;

public class Main {
    public static void main(String[] args) {
        doSmth(Integer.class);
    }
    
    @Meta
    private static native void doSmth(Class<?> type);
    private static void doSmthImpl(ReflectClass cls) {
        Metaprogramming.emit(() -> cls.getName());
    }
}
"""

        when: "run task"
        debug()
        BuildResult result = runFailed('compileTeavm')

        then: "task successful"
        result.task(':compileTeavm').outcome == TaskOutcome.FAILED
//        result.output.contains('fooo: 1')
    }
}