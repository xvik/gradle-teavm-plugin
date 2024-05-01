package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 23.01.2023
 */
class KotlinKitTest extends AbstractKitTest {

    def "Check kotlin compilation"() {
        setup:
        build """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '1.9.23'
                id 'java-library'
                id 'ru.vyarus.teavm'
            }
            
            repositories { mavenCentral() }
            dependencies {
                implementation platform('org.jetbrains.kotlin:kotlin-bom')
                implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'
            
                implementation "org.teavm:teavm-classlib:\${teavm.version}"
                implementation "org.teavm:teavm-jso:\${teavm.version}"
            }

            teavm {
                mainClass = 'example.ClientKt'
            }

        """
        file('src/main/kotlin/example/Client.kt')  << """
package example

import org.teavm.jso.browser.*

fun main(args : Array<String>) {
    val document = Window.current().document

    document.getElementById("hello-kotlin").addEventListener("click") { Window.alert("Hello, developer!") }
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
