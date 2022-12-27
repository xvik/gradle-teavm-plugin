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
                id 'ru.vyarus.teavm'
            }

            teavm {
                foo '1'
                bar '2'
            }

            task printFoo() {
                doLast {
                    println "fooo: \$teavm.foo"
                }
            }

        """

        when: "run task"
        BuildResult result = run('printFoo')

        then: "task successful"
        result.task(':printFoo').outcome == TaskOutcome.SUCCESS
        result.output.contains('fooo: 1')
    }
}