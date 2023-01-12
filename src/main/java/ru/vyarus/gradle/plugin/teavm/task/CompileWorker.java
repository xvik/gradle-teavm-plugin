package ru.vyarus.gradle.plugin.teavm.task;

import org.gradle.workers.WorkAction;
import org.teavm.apachecommons.io.FileUtils;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.builder.BuildResult;
import org.teavm.tooling.builder.BuildStrategy;
import org.teavm.tooling.builder.InProcessBuildStrategy;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;
import ru.vyarus.gradle.plugin.teavm.util.DurationFormatter;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Vyacheslav Rusakov
 * @since 06.01.2023
 */
public abstract class CompileWorker implements WorkAction<CompileParameters> {

    @Override
    public void execute() {
        // order follows org/teavm/maven/TeaVMCompileMojo.java
        final BuildStrategy build = new InProcessBuildStrategy(URLClassLoader::new);
        configure(build);

        build.setProgressListener(new LogListener());
        build.setLog(new LogDelegate());
        try {
            run(build);
        } catch (Exception ex) {
            System.err.println("Unexpected compilation error");
            ex.printStackTrace();
            indicateFail();
        }
    }

    private void configure(BuildStrategy build) {
        // settings applied in the same order as teavm gradle plugin to simplify future comparisons
        build.setClassPathEntries(getParameters().getClassPathEntries().get());
        build.setObfuscated(getParameters().getObfuscated().get());
        build.setStrict(getParameters().getStrict().get());
        build.setMaxTopLevelNames(getParameters().getMaxTopLevelNames().get());
        build.setTargetDirectory(getParameters().getTargetDirectory().get().getAsFile().getAbsolutePath());

        if (getParameters().getTransformers().isPresent()) {
            build.setTransformers(getParameters().getTransformers().get().toArray(new String[]{}));
        }
        if (getParameters().getSourceFilesCopied().get()) {
            build.setSourceFilesCopied(true);
            getParameters().getSourceDirectories().get().forEach(directory ->
                    build.addSourcesDirectory(directory.getAsFile().getAbsolutePath()));
            getParameters().getSourceJars().get().forEach(jar ->
                    build.addSourcesJar(jar.getAsFile().getAbsolutePath()));
        }

        if (getParameters().getProperties().isPresent()) {
            Properties res = new Properties();
            res.putAll(getParameters().getProperties().get());
            build.setProperties(res);
        }
        build.setIncremental(getParameters().getIncremental().get());
        build.setDebugInformationGenerated(getParameters().getDebugInformationGenerated().get());
        build.setSourceMapsFileGenerated(getParameters().getSourceMapsFileGenerated().get());
        build.setMinHeapSize(getParameters().getMinHeapSize().get() * 1024 * 1024);
        build.setMaxHeapSize(getParameters().getMaxHeapSize().get() * 1024 * 1024);
        build.setShortFileNames(getParameters().getShortFileNames().get());
//        build.setAssertionsRemoved(assertionsRemoved);


        build.setMainClass(getParameters().getMainClass().get());
        build.setEntryPointName(getParameters().getEntryPointName().getOrNull());
        build.setTargetFileName(getParameters().getTargetFileName().getOrNull());
        build.setOptimizationLevel(getParameters().getOptimizationLevel().get());
        build.setFastDependencyAnalysis(getParameters().getFastDependencyAnalysis().get());

        if (getParameters().getClassesToPreserve().isPresent()) {
            build.setClassesToPreserve(getParameters().getClassesToPreserve().get().toArray(new String[]{}));
        }
        build.setCacheDirectory(getParameters().getCacheDirectory().get().getAsFile().getAbsolutePath());
        build.setTargetType(getParameters().getTargetType().get());
        build.setWasmVersion(getParameters().getWasmVersion().get());
        build.setLongjmpSupported(getParameters().getLongjmpSupported().get());
        build.setHeapDump(getParameters().getHeapDump().get());
    }

    private void run(final BuildStrategy build) throws Exception {
        long watch = System.currentTimeMillis();
        final BuildResult result = build.build();
        long time = System.currentTimeMillis() - watch;

        if (result.getProblems() != null) {
            TeaVMProblemRenderer.describeProblems(result.getCallGraph(), result.getProblems(), new LogDelegate());

            if (!result.getProblems().getSevereProblems().isEmpty()) {
                // indicate error
                indicateFail();
            }
        }

        if (result.getProblems() == null || result.getProblems().getSevereProblems().isEmpty()) {
            System.out.println("Resources used: " + result.getUsedResources().size());
            if (getParameters().getDebug().get()) {
                System.out.println(result.getUsedResources().stream()
                        .map(s -> "\t" + s).collect(Collectors.joining("\n")));

                System.out.println("Generated files: \n" + result.getGeneratedFiles().stream()
                        .map(s -> "\t"+ s.replace(getParameters().getTargetDirectory().get().getAsFile()
                                .getAbsolutePath() + File.separator, "") + " ("
                                + FileUtils.byteCountToDisplaySize(new File(s).length()) + ")")
                        .collect(Collectors.joining("\n ")));
            }
            System.out.println("Compiled in " + DurationFormatter.format(time));
        }

    }

    private void indicateFail() {
        try {
            getParameters().getErrorFile().get().getAsFile().createNewFile();
        } catch (IOException ex) {
            System.err.println("Error creating marker file");
            ex.printStackTrace();
        }
    }

    public static class LogListener implements TeaVMProgressListener {
        private double target = 1.0;
        private TeaVMPhase currentPhase;

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int maxSteps) {
            System.out.printf("\rTeaVM: Progress, phase: %s started, targeted steps: %s", phase, maxSteps);
            target = maxSteps;
            currentPhase = phase;
            return TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public TeaVMProgressFeedback progressReached(int stepsReached) {
            System.out.printf("\rTeaVM: %s; progress reached: %s of %s -- %s%%", currentPhase, stepsReached, (int) target,
                    (int) (Math.round(stepsReached / target * 100.0)));
            return TeaVMProgressFeedback.CONTINUE;
        }
    }

    // gradle workers does not support loggers!
    public static class LogDelegate implements TeaVMToolLog {

        // \r required to remove last LogListener line from output

        @Override
        public void info(final String s) {
            System.out.println("\r" + s);
        }

        @Override
        public void debug(String s) {
            System.out.println("\r" + s);
        }

        @Override
        public void warning(String s) {
            System.out.println("\rWARNING: " + s);
        }

        @Override
        public void error(String s) {
            System.out.println("\rERROR: " + s);
        }

        @Override
        public void info(String s, Throwable throwable) {
            System.out.println("\r" + s);
            throwable.printStackTrace();
        }

        @Override
        public void debug(String s, Throwable throwable) {
            System.out.println("\r" + s);
            throwable.printStackTrace();
        }

        @Override
        public void warning(String s, Throwable throwable) {
            System.out.println("\rWARNING: " + s);
            throwable.printStackTrace();
        }

        @Override
        public void error(String s, Throwable throwable) {
            System.err.println("\rERROR: " + s);
            throwable.printStackTrace();
        }
    }
}
