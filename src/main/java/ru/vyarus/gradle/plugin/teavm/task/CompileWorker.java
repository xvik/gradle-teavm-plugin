package ru.vyarus.gradle.plugin.teavm.task;

import org.gradle.workers.WorkAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.builder.BuildResult;
import org.teavm.tooling.builder.BuildStrategy;
import org.teavm.tooling.builder.InProcessBuildStrategy;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

import java.net.URLClassLoader;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * @author Vyacheslav Rusakov
 * @since 06.01.2023
 */
public abstract class CompileWorker implements WorkAction<CompileParameters> {

    // no way currently to use project logger inside worker
    private final Logger logger = LoggerFactory.getLogger(CompileWorker.class);

    @Override
    public void execute() {
        // order follows org/teavm/maven/TeaVMCompileMojo.java

        final BuildStrategy build = new InProcessBuildStrategy(URLClassLoader::new);
        configure(build);

        build.setProgressListener(new LogListener(logger));
        build.setLog(new LogDelegate(logger));
        try {
            run(build);
        } catch (Exception ex) {
            logger.error("Unexpected compilation error", ex);
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

    private void run(BuildStrategy build) throws Exception {
        BuildResult result = build.build();

        if(result.getProblems() != null) {
            final ErrorsInterceptor collector = new ErrorsInterceptor(logger);
            TeaVMProblemRenderer.describeProblems(result.getCallGraph(), result.getProblems(), collector);

            if (!result.getProblems().getSevereProblems().isEmpty()) {
                // todo write file describing error (task would decide to stop execution or not)
                System.out.println("ERROR: " +collector.getErrors());
            }
        } else {
            final StringBuilder res = new StringBuilder("\n\n");
            res.append(String.format("\t%s-30: %s%n", "classes used", result.getClasses()));
            res.append(String.format("\t%s-30: %s%n", "generated files", result.getGeneratedFiles()));
            res.append(String.format("\t%s-30: %s%n", "used resources", result.getUsedResources()));
            logger.info("TeaVM compilation stats: {}", res.toString());
        }
    }

    public static class LogListener implements TeaVMProgressListener {
        private final Logger logger;
        private double target = 1.0;
        private TeaVMPhase currentPhase;

        public LogListener(Logger logger) {
            this.logger = logger;
        }

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int maxSteps) {
            logger.info("TeaVM: Progress, phase: {} started, targeted steps: {}", phase, (int) maxSteps);
            target = maxSteps;
            currentPhase = phase;
            return TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public TeaVMProgressFeedback progressReached(int stepsReached) {
            logger.info("TeaVM: {}; progress reached: {} of {} -- {}%", currentPhase, stepsReached, (int) target,
                    (int) (Math.round(stepsReached / target * 100.0)));
            return TeaVMProgressFeedback.CONTINUE;
        }
    }

    public static class LogDelegate implements TeaVMToolLog {

        private final Logger logger;

        public LogDelegate(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public void info(final String s) {
            logger.info(s);
        }

        @Override
        public void debug(String s) {
            logger.debug(s);
        }

        @Override
        public void warning(String s) {
            logger.warn(s);
        }

        @Override
        public void error(String s) {
            logger.error(s);
        }

        @Override
        public void info(String s, Throwable throwable) {
            logger.info(s, throwable);
        }

        @Override
        public void debug(String s, Throwable throwable) {
            logger.debug(s, throwable);
        }

        @Override
        public void warning(String s, Throwable throwable) {
            logger.warn(s, throwable);
        }

        @Override
        public void error(String s, Throwable throwable) {
            logger.error(s, throwable);
        }
    }

    public static class ErrorsInterceptor extends LogDelegate {
        private StringJoiner errors = new StringJoiner("\n");

        public ErrorsInterceptor(Logger logger) {
            super(logger);
        }

        @Override
        public void error(String s) {
            super.error(s);
            errors.add(s);
        }

        public String getErrors() {
            return errors.toString();
        }
    }
}
