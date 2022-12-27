package ru.vyarus.gradle.plugin.teavm

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.GradleException

/**
 * teavm plugin.
 *
 * @author Vyacheslav Rusakov
 * @since 27.12.2022
 */
@CompileStatic
class TeavmPlugin implements Plugin<Project> {

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void apply(Project project) {
        TeavmExtension extension = project.extensions.create('teavm', TeavmExtension)

        project.afterEvaluate {
            if (extension.bar == null) {
                throw new GradleException('teavm.bar configuration required')
            }
        }
    }
}
