package ru.vyarus.gradle.plugin.teavm

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.teavm.backend.javascript.JSModuleType
import org.teavm.backend.wasm.render.WasmBinaryVersion
import org.teavm.tooling.TeaVMTargetType
import org.teavm.vm.TeaVMOptimizationLevel
import ru.vyarus.gradle.plugin.teavm.task.TeavmCompileTask

/**
 * @author Vyacheslav Rusakov
 * @since 27.12.2022
 */
class TeavmPluginTest extends AbstractTest {

    def "Check extension registration"() {

        when: "plugin applied"
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply "java"
        project.plugins.apply "ru.vyarus.teavm"

        then: "extension registered"
        project.extensions.findByType(TeavmExtension)

        then: "task registered"
        with(project.tasks.findByName('compileTeavm')) {
            it.dependsOn.size() == 1
        }

    }

    def "Check task configuration from extension"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: 'java'
            apply plugin: "ru.vyarus.teavm"

            repositories { mavenCentral() }
            dependencies {
                implementation "org.teavm:teavm-classlib:${teavm.version}"
            }

            teavm {
                extraClassDirs = ['build/classes/foo']
                extraSourceDirs = ['src/foo/java']

                mainClass = 'com.foo.Client'
                sourceFilesCopied = true
                sourceFilesCopiedAsLocalLinks = true
                targetFileName = 'classes.js'
                targetType = TeaVMTargetType.WEBASSEMBLY
                jsModuleType = JSModuleType.ES2015
                classesToPreserve = ['com.foo.Some']
                transformers = ['com.bar.Other']
                properties = ['foo': 'bar']
                assertionsRemoved = true
            }
        }

        then: "defaults applied"
        TeavmCompileTask task = project.tasks.findByName('compileTeavm')

        task.getClassPath().get().collect { project.relativePath(it.asFile).replace(File.separator, '/')} as Set == [
                'build/classes/java/main', 'build/resources/main', 'build/classes/foo'] as Set
        task.dependencies.files.collect { it.getName()}.contains('teavm-classlib-0.10.0.jar')

        task.getSources().get().collect { project.relativePath(it.asFile).replace(File.separator, '/')} as Set == [
                'src/main/java', 'src/main/resources', 'src/foo/java'] as Set
        task.sourceDependencies.files.collect { it.getName()}.contains('teavm-classlib-0.10.0-sources.jar')

        project.relativePath(task.getTargetDir().get().asFile).replace(File.separator, '/') == 'build/teavm'
        project.relativePath(task.getCacheDir().get().asFile).replace(File.separator, '/') == 'build/teavm-cache'

        task.mainClass.get() == 'com.foo.Client'
        task.entryPointName.get() == 'main'
        task.targetFileName.get() == 'classes.js'
        task.targetType.get() == TeaVMTargetType.WEBASSEMBLY
        task.jsModuleType.get() == JSModuleType.ES2015
        task.wasmVersion.get() == WasmBinaryVersion.V_0x1
        task.stopOnErrors.get() == true
        task.maxTopLevelNames.get() == 80000
        task.minHeapSize.get() == 4
        task.maxHeapSize.get() == 128
        task.transformers.get() == ['com.bar.Other']
        task.properties.get() == ['foo': 'bar']
        task.classesToPreserve.get() == ['com.foo.Some']


        task.obfuscated.get()
        !task.strict.get()
        !task.incremental.get()
        !task.debugInformationGenerated.get()
        !task.sourceMapsGenerated.get()
        !task.fastDependencyAnalysis.get()
        task.assertionsRemoved.get()
        task.optimizationLevel.get() == TeaVMOptimizationLevel.ADVANCED
        !task.shortFileNames.get()
        !task.heapDump.get()
    }


    def "Check dev mode"() {

        when: "plugin configured"
        Project project = project {
            apply plugin: 'java'
            apply plugin: "ru.vyarus.teavm"

            repositories { mavenCentral() }
            dependencies {
                implementation "org.teavm:teavm-classlib:${teavm.version}"
            }

            teavm {
                dev = true

                obfuscated = true
                strict = true
                sourceFilesCopied = false
                sourceFilesCopiedAsLocalLinks = false
                incremental = false
                debugInformationGenerated = false
                sourceMapsGenerated = false
                fastDependencyAnalysis = false
                assertionsRemoved = true
                optimizationLevel = ADVANCED

                // C target ONLY
                shortFileNames = false
                heapDump = false

                devOptions {
                    obfuscated = false
                    strict = false
                    sourceFilesCopied = true
                    sourceFilesCopiedAsLocalLinks = true
                    incremental = true
                    debugInformationGenerated = true
                    sourceMapsGenerated = true
                    fastDependencyAnalysis = true
                    assertionsRemoved = false
                    optimizationLevel = SIMPLE

                    // C target ONLY
                    shortFileNames = true
                    heapDump = true
                }
            }
        }

        then: "task configured for dev"
        TeavmCompileTask task = project.tasks.findByName('compileTeavm')

        !task.obfuscated.get()
        !task.strict.get()
        task.getSourceFilesCopied().get()
        task.getSourceFilesCopiedAsLocalLinks().get()
        task.incremental.get()
        task.debugInformationGenerated.get()
        task.sourceMapsGenerated.get()
        task.fastDependencyAnalysis.get()
        !task.assertionsRemoved.get()
        task.optimizationLevel.get() == TeaVMOptimizationLevel.SIMPLE
        task.shortFileNames.get()
        task.heapDump.get()
    }
}