package net.tgc.gamingblock_management.punishments;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern PART_PATTERN =
            Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Duration parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Duration cannot be empty.");
        }

        String value = input.toLowerCase(Locale.ROOT).replace(" ", "");
        Matcher matcher = PART_PATTERN.matcher(value);

        Duration total = Duration.ZERO;
        int position = 0;
        boolean foundPart = false;

        while (matcher.find()) {
            if (matcher.start() != position) {
                throw new IllegalArgumentException(
                        "Invalid duration: " + input
                );
            }

            long amount;

            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid duration amount: " + matcher.group(1)
                );
            }

            if (amount <= 0) {
                throw new IllegalArgumentException(
                        "Duration values must be greater than zero."
                );
            }

            Duration part = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                case "w" -> Duration.ofDays(Math.multiplyExact(amount, 7));
                default -> throw new IllegalArgumentException(
                        "Unknown duration unit: " + matcher.group(2)
                );
            };

            try {
                total = total.plus(part);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "Duration is too large."
                );
            }

            foundPart = true;
            position = matcher.end();
        }

        if (!foundPart || position != value.length()) {
            throw new IllegalArgumentException(
                    "Invalid duration. Use values such as 30m, 2h, 7d, or 1d12h."
            );
        }

        return total;
    }
}