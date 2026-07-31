package win.cntier.tag.internal.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyColors {

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern MINECRAFT_COLOR = Pattern.compile(
        "§x(?:§[0-9a-fA-F]){6}|§[0-9a-fA-Fk-oK-OrR]"
    );
    private static final String LEGACY_CODES = "0123456789abcdefklmnor";

    private LegacyColors() {
    }

    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char character : hex.toCharArray()) {
                replacement.append('§').append(character);
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(output);

        String colored = output.toString();
        StringBuilder translated = new StringBuilder(colored.length());
        for (int index = 0; index < colored.length(); index++) {
            char current = colored.charAt(index);
            if (current == '&' && index + 1 < colored.length()) {
                char next = Character.toLowerCase(colored.charAt(index + 1));
                if (LEGACY_CODES.indexOf(next) >= 0) {
                    translated.append('§').append(next);
                    index++;
                    continue;
                }
            }
            translated.append(current);
        }
        return translated.toString();
    }

    public static String strip(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return MINECRAFT_COLOR.matcher(input).replaceAll("");
    }
}
