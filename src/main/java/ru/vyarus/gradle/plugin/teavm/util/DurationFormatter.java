package ru.vyarus.gradle.plugin.teavm.util;

import org.codehaus.groovy.runtime.StringGroovyMethods;

import java.math.BigDecimal;

/**
 * Copy of gradle's internal {@link org.gradle.internal.time.TimeFormatting} class, which become internal in
 * gradle 4.2 and broke compatibility.
 * <p>
 * Used to pretty print elapsed tile in human-readable form.
 *
 * @author Vyacheslav Rusakov
 * @since 21.09.2017
 */
public final class DurationFormatter {

    private static final long MILLIS_PER_SECOND = 1000;
    private static final long MILLIS_PER_MINUTE = 60_000;
    private static final long MILLIS_PER_HOUR = 3_600_000;
    private static final long MILLIS_PER_DAY = 86_400_000;

    private DurationFormatter() {
    }

    /**
     * @param duration duration in milliseconds
     * @return human-readable (short) duration
     */
    public static String format(final long duration) {
        long remain = duration;
        if (remain == 0L) {
            return "0ms";
        }

        final StringBuilder result = new StringBuilder();
        final long days = remain / MILLIS_PER_DAY;
        remain %= MILLIS_PER_DAY;
        if (days > 0L) {
            append(result, days, "d");
        }

        final long hours = remain / MILLIS_PER_HOUR;
        remain %= MILLIS_PER_HOUR;
        if (hours > 0L) {
            append(result, hours, "h");
        }

        final long minutes = remain / MILLIS_PER_MINUTE;
        remain %= MILLIS_PER_MINUTE;
        if (minutes > 0L) {
            append(result, minutes, "m");
        }

        boolean secs = false;
        if (remain >= MILLIS_PER_SECOND) {
            // if only secs, show rounded value, otherwise get rid of ms
            final int secondsScale = result.length() == 0 ? 2 : 0;
            append(result, BigDecimal.valueOf(remain).divide(BigDecimal.valueOf(MILLIS_PER_SECOND))
                    .setScale(secondsScale, 4).stripTrailingZeros().toPlainString(), "s");
            secs = true;
            remain %= MILLIS_PER_SECOND;
        }

        if (!secs && remain > 0) {
            result.append(StringGroovyMethods.plus(remain, "ms"));
        }

        return result.toString();
    }

    private static void append(final StringBuilder builder, final Object num, final String what) {
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(num).append(what);
    }
}
