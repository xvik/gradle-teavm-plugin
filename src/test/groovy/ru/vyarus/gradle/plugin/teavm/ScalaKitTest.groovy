package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 23.01.2023
 */
class ScalaKitTest extends AbstractKitTest {

    def "Check scala compilation"() {
        setup:
        build """
            plugins {
                id 'scala'
                id 'java-library'
                id 'ru.vyarus.teavm'
            }
            
            repositories { mavenCentral() }
            dependencies {
                implementation 'org.scala-lang:scala-library:2.13.11'
            
                implementation "org.teavm:teavm-classlib:\${teavm.version}"
                implementation "org.teavm:teavm-jso:\${teavm.version}"
            }

            teavm {
                mainClass = 'example.Client'
            }

        """
        file('src/main/scala/example/Client.scala')  << """
package example

import org.teavm.jso.browser.Window
import org.teavm.jso.dom.events._
import org.teavm.jso.dom.html._

object Client {
  def main(args: Array[String]): Unit = {
      val doc = HTMLDocument.current

      doc.getElementById("hello-scala").listenClick((e: MouseEvent) => { Window.alert("Hello, developer!") })
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
