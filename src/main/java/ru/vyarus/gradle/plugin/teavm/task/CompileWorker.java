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
 * TeaVM compilation worker. Worker used to execute teavm inside custom classpath (dynamic teavm version selection).
 * Worker might be executed in different jvm (gradle daemon) and so there are no direct communication between
 * worker and plugin (only parameters could be passed into worker). Special file used to indicate compilation fail:
 * if worker creates file, task would throw an exception.
 * <p>
 * Logging is not supported inside worker so everything is logged into system out (user will see it).
 *
 * @author Vyacheslav Rusakov
 * @since 06.01.2023
 */
@SuppressWarnings({"PMD.SystemPrintln", "PMD.AvoidPrintStackTrace"})
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
            // no way to show exception otherwise
            System.err.println("Unexpected compilation error");
            ex.printStackTrace();
            indicateFail();
        }
    }

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    private void configure(final BuildStrategy build) {
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
                    build.addSourcesJar(jar.getAbsolutePath()));
        }

        if (getParameters().getProperties().isPresent()) {
            final Properties res = new Properties();
            res.putAll(getParameters().getProperties().get());
            build.setProperties(res);
        }
        build.setIncremental(getParameters().getIncremental().get());
        build.setDebugInformationGenerated(getParameters().getDebugInformationGenerated().get());
        build.setSourceMapsFileGenerated(getParameters().getSourceMapsFileGenerated().get());
        build.setMinHeapSize(getParameters().getMinHeapSize().get() * 1024 * 1024);
        build.setMaxHeapSize(getParameters().getMaxHeapSize().get() * 1024 * 1024);
        build.setShortFileNames(getParameters().getShortFileNames().get());
        //build.setAssertionsRemoved(assertionsRemoved);


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
        final long watch = System.currentTimeMillis();
        final BuildResult result = build.build();
        final long time = System.currentTimeMillis() - watch;

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
                        .map(s -> "\t" + s).sorted().collect(Collectors.joining("\n")));

                System.out.println("Generated files: " + result.getGeneratedFiles().size());
                System.out.println(result.getGeneratedFiles().stream()
                        .map(s -> "\t" + s.replace(getParameters().getTargetDirectory().get().getAsFile()
                                .getAbsolutePath() + File.separator, "") + " ("
                                + FileUtils.byteCountToDisplaySize(new File(s).length()) + ")")
                        .sorted()
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

    /**
     * TeaVM progress indicator.
     */
    public static class LogListener implements TeaVMProgressListener {
        private double target = 1.0;
        private TeaVMPhase currentPhase;

        @Override
        public TeaVMProgressFeedback phaseStarted(final TeaVMPhase phase, final int maxSteps) {
            // \r for overriding previous line
            System.out.printf("\rTeaVM: Progress, phase: %s started, targeted steps: %s", phase, maxSteps);
            target = maxSteps;
            currentPhase = phase;
            return TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public TeaVMProgressFeedback progressReached(final int stepsReached) {
            System.out.printf("\rTeaVM: %s; progress reached: %s of %s -- %s%%", currentPhase, stepsReached,
                    (int) target, (int) (Math.round(stepsReached / target * 100.0)));
            return TeaVMProgressFeedback.CONTINUE;
        }
    }

    /**
     * TeaVM logs delegate. Worker does not support loggers so system out used instead.
     */
    public static class LogDelegate implements TeaVMToolLog {

        // \r required to remove last LogListener line from output (after progress listener)

        @Override
        public void info(final String s) {
            System.out.println("\r" + s);
        }

        @Override
        public void info(final String s, final Throwable throwable) {
            System.out.println("\r" + s);
            throwable.printStackTrace();
        }

        @Override
        public void debug(final String s) {
            System.out.println("\r" + s);
        }

        @Override
        public void debug(final String s, final Throwable throwable) {
            System.out.println("\r" + s);
            throwable.printStackTrace();
        }

        @Override
        public void warning(final String s) {
            System.out.println("\rWARNING: " + s);
        }

        @Override
        public void warning(final String s, final Throwable throwable) {
            System.out.println("\rWARNING: " + s);
            throwable.printStackTrace();
        }

        @Override
        public void error(final String s) {
            System.out.println("\rERROR: " + s);
        }

        @Override
        public void error(final String s, final Throwable throwable) {
            System.err.println("\rERROR: " + s);
            throwable.printStackTrace();
        }
    }
}
