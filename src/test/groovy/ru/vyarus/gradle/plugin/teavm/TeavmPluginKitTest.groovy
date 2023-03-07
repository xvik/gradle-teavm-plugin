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
        result.output.contains('Output file successfully built')
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
                implementation "org.teavm:teavm-classlib:\${teavm.version}"
                implementation "org.teavm:teavm-metaprogramming-api:\${teavm.version}"
                implementation "org.teavm:teavm-metaprogramming-impl:\${teavm.version}"
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
        result.output.contains('Output file built with errors')

        and: "errors shown inside exception"
        result.output.contains("""> Teavm compilation failed:
  
  \tCorresponding meta method was not found
  \t    at example.Main.doSmth
  \t    at example.Main.main(Main.java:10)""")
    }


    def "Check no stop on error"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.teavm'
            }
            
            repositories { mavenCentral() }
            
            dependencies {
                implementation "org.teavm:teavm-classlib:\${teavm.version}"
                implementation "org.teavm:teavm-metaprogramming-api:\${teavm.version}"
                implementation "org.teavm:teavm-metaprogramming-impl:\${teavm.version}"
            }

            teavm {
                stopOnErrors = false
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
        BuildResult result = run('compileTeavm')

        then: "task successful"
        result.task(':compileTeavm').outcome == TaskOutcome.SUCCESS
        result.output.contains('Output file built with errors')

        and: "errors shown in log"
        result.output.contains("""ERROR: Corresponding meta method was not found
    at example.Main.doSmth
    at example.Main.main(Main.java:10)""")
    }
}