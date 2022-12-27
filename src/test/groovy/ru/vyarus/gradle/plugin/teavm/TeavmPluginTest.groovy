package ru.vyarus.gradle.plugin.teavm

import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder

/**
 * @author Vyacheslav Rusakov
 * @since 27.12.2022
 */
class TeavmPluginTest extends AbstractTest {

    def "Check extension registration"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply "ru.vyarus.teavm"

        then: "extension registered"
        project.extensions.findByType(TeavmExtension)

    }

    def "Check extension validation"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.teavm"

            teavm {
                foo '1'
                bar '2'
            }
        }

        then: "validation pass"
        def teavm = project.extensions.teavm;
        teavm.foo == '1'
        teavm.bar == '2'
    }


    def "Check extension validation failure"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: "ru.vyarus.teavm"

            teavm {
                foo '1'
            }
        }

        then: "validation failed"
        def ex = thrown(ProjectConfigurationException)
        ex.cause.message == 'teavm.bar configuration required'
    }

}