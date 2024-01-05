package ru.vyarus.gradle.plugin.teavm.task;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.gradle.workers.WorkAction;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMSourceFilePolicy;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        final BuildStrategy build = new InProcessBuildStrategy();
        configure(build);

        build.setProgressListener(new LogListener());
        build.setLog(new LogDelegate());
        try {
            run(build);
        } catch (Exception ex) {
            // no way to show exception otherwise
            System.err.println("Unexpected compilation error");
            ex.printStackTrace();
            indicateFail("Unexpected processing error: \n" + ex.getMessage());
        }
    }

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    private void configure(final BuildStrategy build) {
        // settings applied in the same order as teavm gradle plugin to simplify future comparisons
        build.setClassPathEntries(getParameters().getClassPathEntries().get());
        build.setObfuscated(getParameters().getObfuscated().get());
        build.setStrict(getParameters().getStrict().get());
        build.setTargetDirectory(getParameters().getTargetDirectory().get().getAsFile().getAbsolutePath());

        if (getParameters().getTransformers().isPresent()) {
            build.setTransformers(getParameters().getTransformers().get().toArray(new String[]{}));
        }
        if (getParameters().getSourceFilesCopied().get()) {
            build.setSourceFilePolicy(getParameters().getSourceFilesCopiedAsLocalLinks().get()
                    ? TeaVMSourceFilePolicy.LINK_LOCAL_FILES : TeaVMSourceFilePolicy.COPY);
            getParameters().getSourceDirectories().get().forEach(directory ->
                    build.addSourcesDirectory(directory.getAsFile().getAbsolutePath()));
            getParameters().getSourceJars().get().forEach(jar ->
                    build.addSourcesJar(jar.getAbsolutePath()));
        } else {
            build.setSourceFilePolicy(TeaVMSourceFilePolicy.DO_NOTHING);
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
        build.setAssertionsRemoved(getParameters().getAssertionsRemoved().get());


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
        build.setJsModuleType(getParameters().getJsModuleType().get());
        build.setWasmVersion(getParameters().getWasmVersion().get());
        build.setHeapDump(getParameters().getHeapDump().get());
    }

    private void run(final BuildStrategy build) throws Exception {
        final long watch = System.currentTimeMillis();
        final BuildResult result = build.build();
        final long time = System.currentTimeMillis() - watch;

        if (result.getProblems() != null) {
            final LogDelegate log = new LogDelegate(true);
            TeaVMProblemRenderer.describeProblems(result.getCallGraph(), result.getProblems(), log);

            if (!result.getProblems().getSevereProblems().isEmpty()) {
                // indicate error (double space to separate multi-line errors)
                indicateFail(String.join("\n\n", log.getErrors()));
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
            System.out.println("Overall time: " + DurationFormatter.format(time));
        }

    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void indicateFail(final String message) {
        try {
            final File file = getParameters().getErrorFile().get().getAsFile();
            file.createNewFile();
            Files.writeString(file.toPath(), message);
        } catch (IOException ex) {
            System.err.println("Error creating marker file");
            ex.printStackTrace();
        }
    }

    /**
     * TeaVM progress indicator.
     */
    public static class LogListener implements TeaVMProgressListener {
        private TeaVMPhase currentPhase;
        private int target = 1;
        private long timer;

        @Override
        public TeaVMProgressFeedback phaseStarted(final TeaVMPhase phase, final int maxSteps) {
            if (timer > 0 && currentPhase != null) {
                // teavm may not call listener on 100% so showing previous phase log before changing phase
                // note: time would not be as accurate, but better then nothing
                phaseDone();
            }
            currentPhase = phase;
            target = maxSteps == 0 ? 1 : maxSteps;
            timer = System.currentTimeMillis();
            return TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public TeaVMProgressFeedback progressReached(final int stepsReached) {
            if (stepsReached == target) {
                phaseDone();
                // prevent same log in start phase
                timer = 0;
            } else {
                // log10 from 0 is impossible, so always start with 1
                final int current = stepsReached == 0 ? 1 : stepsReached;
                // use 1000 to show 0% for 1
                final int total = target == 1 ? 1000 : target;
                final StringBuilder string = new StringBuilder(140);
                final int percent = current * 100 / total;
                // \r for overriding previous progress line
                string
                        .append('\r').append(currentPhase).append(' ')
                        .append(String.join("", Collections.nCopies(percent == 0 ? 2
                                : 2 - (int) (Math.log10(percent)), " ")))
                        .append(String.format(" %d%% [", percent))
                        .append(String.join("", Collections.nCopies(percent, "=")))
                        .append('>')
                        .append(String.join("", Collections.nCopies(100 - percent, " ")))
                        .append(']')
                        .append(String.join("", Collections.nCopies((int) (Math.log10(total))
                                - (int) (Math.log10(current)), " ")))
                        .append(String.format(" %d/%d", stepsReached, target));

                System.out.print(string);
            }

            return TeaVMProgressFeedback.CONTINUE;
        }

        private void phaseDone() {
            // overwrite progress with static text
            System.out.printf("\r\t %-40s %s%n", currentPhase,
                    DurationFormatter.format(System.currentTimeMillis() - timer));
        }
    }

    /**
     * TeaVM logs delegate. Worker does not support loggers so system out used instead.
     */
    public static class LogDelegate implements TeaVMToolLog {
        private final boolean collectErrors;
        private final List<String> errors = new ArrayList<>();

        public LogDelegate() {
            this(false);
        }

        public LogDelegate(final boolean collectErrors) {
            this.collectErrors = collectErrors;
        }

        // \r required to remove possible last LogListener line from output (after error)

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
            // only this method used by teavm to report compilation errors
            if (collectErrors) {
                errors.add(s);
            }
        }

        @Override
        public void error(final String s, final Throwable throwable) {
            System.err.println("\rERROR: " + s);
            throwable.printStackTrace();
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
