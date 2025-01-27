package com.vdurmont.emoji;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides methods to parse strings with emojis.
 *
 * @author Improver: Ivan Ivanov [https://vk.com/irisism]<br>
 * Creator: Vincent DURMONT [vdurmont@gmail.com]
 */
public class EmojiParser {

    /**
     * See {@link #parseToAliases(String, FitzpatrickAction)} with the action
     * "PARSE"
     *
     * @param input the string to parse
     * @return the string with the emojis replaced by their alias.
     */
    public static String parseToAliases(String input) {
        return parseToAliases(input, FitzpatrickAction.PARSE);
    }

    /**
     * Replaces the emoji's unicode occurrences by one of their alias
     * (between 2 ':').<br>
     * Example: <code>😄</code> will be replaced by <code>:smile:</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a PARSE action, a "|" will be
     * appendend to the alias, with the fitzpatrick type.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>:boy|type_6:</code><br>
     * The fitzpatrick types are: type_1_2, type_3, type_4, type_5, type_6<br>
     * <br>
     * When a fitzpatrick modifier is present with a REMOVE action, the modifier
     * will be deleted.<br>
     * Example: <code>👦🏿</code> will be replaced by <code>:boy:</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a IGNORE action, the modifier
     * will be ignored.<br>
     * Example: <code>👦🏿</code> will be replaced by <code>:boy:🏿</code><br>
     *
     * @param input             the string to parse
     * @param fitzpatrickAction the action to apply for the fitzpatrick modifiers
     * @return the string with the emojis replaced by their alias.
     */
    public static String parseToAliases(String input, final FitzpatrickAction fitzpatrickAction) {
        var emojiTransformer = new EmojiTransformer() {
            public String transform(EmojiResult unicodeCandidate) {
                switch (fitzpatrickAction) {
                    default:
                    case PARSE:
                        if (unicodeCandidate.hasFitzpatrick()) {
                            return ":" +
                                    unicodeCandidate.getEmoji().getAliases().get(0) +
                                    "|" +
                                    unicodeCandidate.getFitzpatrickType() +
                                    ":";
                        }
                    case REMOVE:
                        return ":" +
                                unicodeCandidate.getEmoji().getAliases().get(0) +
                                ":";
                    case IGNORE:
                        return ":" +
                                unicodeCandidate.getEmoji().getAliases().get(0) +
                                ":" +
                                unicodeCandidate.getFitzpatrickUnicode();
                }
            }
        };

        return parseFromUnicode(input, emojiTransformer);
    }

    /**
     * Replace all emojis with character
     *
     * @param str               the string to process
     * @param replacementString replacement the string that will replace all the emojis
     * @return the string with replaced character
     */
    public static String replaceAllEmojis(String str, final String replacementString) {
        var emojiTransformer = new EmojiTransformer() {
            public String transform(EmojiResult unicodeCandidate) {
                return replacementString;
            }
        };

        return parseFromUnicode(str, emojiTransformer);
    }

    /**
     * Replaces the emoji's aliases (between 2 ':') occurrences and the html
     * representations by their unicode.<br>
     * Examples:<br>
     * <code>:smile:</code> will be replaced by <code>😄</code><br>
     * <code>&amp;#128516;</code> will be replaced by <code>😄</code><br>
     * <code>:boy|type_6:</code> will be replaced by <code>👦🏿</code>
     *
     * @param input the string to parse
     * @return the string with the aliases and html representations replaced by
     * their unicode.
     */
    public static String parseToUnicode(String input) {
        var sb = new StringBuilder(input.length());

        for (var last = 0; last < input.length(); last++) {
            var alias = getAliasAt(input, last);
            if (alias == null) {
                alias = getHtmlEncodedEmojiAt(input, last);
            }

            if (alias != null) {
                sb.append(alias.emoji.getUnicode());
                last = alias.endIndex;

                if (alias.fitzpatrick != null) {
                    sb.append(alias.fitzpatrick.unicode);
                }
            } else {
                sb.append(input.charAt(last));
            }
        }

        return sb.toString();
    }

    /**
     * Finds the alias in the given string starting at the given point, null otherwise
     */
    protected static AliasCandidate getAliasAt(String input, int start) {
        if (input.length() < start + 2 || input.charAt(start) != ':') {
            return null; // Aliases start with :
        }
        var aliasEnd = input.indexOf(':', start + 2);  // Alias must be at least 1 char in length
        if (aliasEnd == -1) {
            return null; // No alias end found
        }

        var fitzpatrickStart = input.indexOf('|', start + 2);
        if (fitzpatrickStart != -1 && fitzpatrickStart < aliasEnd) {
            var emoji = EmojiManager.getForAlias(input.substring(start, fitzpatrickStart));
            if (emoji == null) {
                return null; // Not a valid alias
            }
            if (!emoji.supportsFitzpatrick()) {
                return null; // Fitzpatrick was specified, but the emoji does not support it
            }
            var fitzpatrick = Fitzpatrick.fitzpatrickFromType(input.substring(fitzpatrickStart + 1, aliasEnd));
            return new AliasCandidate(emoji, fitzpatrick, start, aliasEnd);
        }

        var emoji = EmojiManager.getForAlias(input.substring(start, aliasEnd));
        if (emoji == null) {
            return null; // Not a valid alias
        }
        return new AliasCandidate(emoji, null, start, aliasEnd);
    }

    /**
     * Finds the HTML encoded emoji in the given string starting at the given point, null otherwise
     */
    protected static AliasCandidate getHtmlEncodedEmojiAt(String input, int start) {
        if (input.length() < start + 4 || input.charAt(start) != '&' || input.charAt(start + 1) != '#') {
            return null;
        }

        Emoji longestEmoji = null;
        var longestCodePointEnd = -1;
        var chars = new char[EmojiManager.EMOJI_TRIE.maxDepth];
        var charsIndex = 0;
        var codePointStart = start;
        do {
            var codePointEnd = input.indexOf(';', codePointStart + 3);  // Code point must be at least 1 char in length
            if (codePointEnd == -1) {
                break;
            }

            try {
                var radix = input.charAt(codePointStart + 2) == 'x' ? 16 : 10;
                var codePoint = Integer.parseInt(input.substring(codePointStart + 2 + radix / 16, codePointEnd), radix);
                charsIndex += Character.toChars(codePoint, chars, charsIndex);
            } catch (IllegalArgumentException e) {
                break;
            }
            var foundEmoji = EmojiManager.EMOJI_TRIE.getEmoji(chars, 0, charsIndex);
            if (foundEmoji != null) {
                longestEmoji = foundEmoji;
                longestCodePointEnd = codePointEnd;
            }
            codePointStart = codePointEnd + 1;
        } while (input.length() > codePointStart + 4 &&
                input.charAt(codePointStart) == '&' &&
                input.charAt(codePointStart + 1) == '#' &&
                charsIndex < chars.length &&
                !EmojiManager.EMOJI_TRIE.isEmoji(chars, 0, charsIndex).impossibleMatch());

        if (longestEmoji == null) {
            return null;
        }
        return new AliasCandidate(longestEmoji, null, start, longestCodePointEnd);
    }

    /**
     * See {@link #parseToHtmlDecimal(String, FitzpatrickAction)} with the action
     * "PARSE"
     *
     * @param input the string to parse
     * @return the string with the emojis replaced by their html decimal
     * representation.
     */
    public static String parseToHtmlDecimal(String input) {
        return parseToHtmlDecimal(input, FitzpatrickAction.PARSE);
    }

    /**
     * Replaces the emoji's unicode occurrences by their html representation.<br>
     * Example: <code>😄</code> will be replaced by <code>&amp;#128516;</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a PARSE or REMOVE action, the
     * modifier will be deleted from the string.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>&amp;#128102;</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a IGNORE action, the modifier
     * will be ignored and will remain in the string.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>&amp;#128102;🏿</code>
     *
     * @param input             the string to parse
     * @param fitzpatrickAction the action to apply for the fitzpatrick modifiers
     * @return the string with the emojis replaced by their html decimal
     * representation.
     */
    public static String parseToHtmlDecimal(
            String input,
            final FitzpatrickAction fitzpatrickAction
    ) {
        var emojiTransformer = new EmojiTransformer() {
            public String transform(EmojiResult unicodeCandidate) {
                return switch (fitzpatrickAction) {
                    case PARSE, REMOVE -> unicodeCandidate.getEmoji().getHtmlDecimal();
                    case IGNORE -> unicodeCandidate.getEmoji().getHtmlDecimal() +
                            unicodeCandidate.getFitzpatrickUnicode();
                };
            }
        };

        return parseFromUnicode(input, emojiTransformer);
    }

    /**
     * See {@link #parseToHtmlHexadecimal(String, FitzpatrickAction)} with the
     * action "PARSE"
     *
     * @param input the string to parse
     * @return the string with the emojis replaced by their html hex
     * representation.
     */
    public static String parseToHtmlHexadecimal(String input) {
        return parseToHtmlHexadecimal(input, FitzpatrickAction.PARSE);
    }

    /**
     * Replaces the emoji's unicode occurrences by their html hex
     * representation.<br>
     * Example: <code>👦</code> will be replaced by <code>&amp;#x1f466;</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a PARSE or REMOVE action, the
     * modifier will be deleted.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>&amp;#x1f466;</code><br>
     * <br>
     * When a fitzpatrick modifier is present with a IGNORE action, the modifier
     * will be ignored and will remain in the string.<br>
     * Example: <code>👦🏿</code> will be replaced by
     * <code>&amp;#x1f466;🏿</code>
     *
     * @param input             the string to parse
     * @param fitzpatrickAction the action to apply for the fitzpatrick modifiers
     * @return the string with the emojis replaced by their html hex
     * representation.
     */
    public static String parseToHtmlHexadecimal(
            String input,
            final FitzpatrickAction fitzpatrickAction
    ) {
        var emojiTransformer = new EmojiTransformer() {
            public String transform(EmojiResult unicodeCandidate) {
                return switch (fitzpatrickAction) {
                    case PARSE, REMOVE -> unicodeCandidate.getEmoji().getHtmlHexadecimal();
                    case IGNORE -> unicodeCandidate.getEmoji().getHtmlHexadecimal() +
                            unicodeCandidate.getFitzpatrickUnicode();
                };
            }
        };

        return parseFromUnicode(input, emojiTransformer);
    }

    /**
     * Removes all emojis from a String
     *
     * @param str the string to process
     * @return the string without any emoji
     */
    public static String removeAllEmojis(String str) {
        var emojiTransformer = new EmojiTransformer() {
            public String transform(EmojiResult unicodeCandidate) {
                return "";
            }
        };

        return parseFromUnicode(str, emojiTransformer);
    }

    /**
     * Removes a set of emojis from a String
     *
     * @param str            the string to process
     * @param emojisToRemove the emojis to remove from this string
     * @return the string without the emojis that were removed
     */
    public static String removeEmojis(
            String str,
            final Collection<Emoji> emojisToRemove
    ) {
        var emojiTransformer = new EmojiTransformer() {
            public String transform(EmojiResult unicodeCandidate) {
                if (!emojisToRemove.contains(unicodeCandidate.getEmoji())) {
                    return unicodeCandidate.getEmoji().getUnicode() +
                            unicodeCandidate.getFitzpatrickUnicode();
                }
                return "";
            }
        };

        return parseFromUnicode(str, emojiTransformer);
    }

    /**
     * Removes all the emojis in a String except a provided set
     *
     * @param str          the string to process
     * @param emojisToKeep the emojis to keep in this string
     * @return the string without the emojis that were removed
     */
    public static String removeAllEmojisExcept(
            String str,
            final Collection<Emoji> emojisToKeep
    ) {
        var emojiTransformer = new EmojiTransformer() {
            public String transform(EmojiResult unicodeCandidate) {
                if (emojisToKeep.contains(unicodeCandidate.getEmoji())) {
                    return unicodeCandidate.getEmoji().getUnicode() +
                            unicodeCandidate.getFitzpatrickUnicode();
                }
                return "";
            }
        };

        return parseFromUnicode(str, emojiTransformer);
    }

    /**
     * Detects all unicode emojis in input string and replaces them with the
     * return value of transformer.transform()
     *
     * @param input       the string to process
     * @param transformer emoji transformer to apply to each emoji
     * @return input string with all emojis transformed
     */
    public static String parseFromUnicode(String input, EmojiTransformer transformer) {
        var prev = 0;
        var sb = new StringBuilder(input.length());
        var replacements = getEmojis(input);
        for (var candidate : replacements) {
            sb.append(input, prev, candidate.startIndex);

            sb.append(' ');
            sb.append(transformer.transform(candidate));
            sb.append(' ');
            prev = candidate.endIndex;
        }

        return sb.append(input.substring(prev)).toString();
    }

    public static List<String> extractEmojiStrings(String input) {
        return extractEmojiStrings(input, 0);
    }

    public static List<String> extractEmojiStrings(String input, int limit) {
        var items = extractEmojis(input, limit);
        List<String> result = new ArrayList<>(items.size());
        for (var i : items) {
            result.add(i.toString());
        }
        return result;
    }

    public static List<EmojiResult> extractEmojis(String input) {
        return getEmojis(input, 0);
    }

    public static List<EmojiResult> extractEmojis(String input, int limit) {
        return getEmojis(input, limit);
    }

    /**
     * Generates a list UnicodeCandidates found in input string. A
     * UnicodeCandidate is created for every unicode emoticon found in input
     * string, additionally if Fitzpatrick modifier follows the emoji, it is
     * included in UnicodeCandidate. Finally, it contains start and end index of
     * unicode emoji itself (WITHOUT Fitzpatrick modifier whether it is there or
     * not!).
     *
     * @param input String to find all unicode emojis in
     * @return List of UnicodeCandidates for each unicode emote in text
     */
    public static List<EmojiResult> getEmojis(String input, int limit) {
        var inputCharArray = input.toCharArray();
        List<EmojiResult> candidates = new ArrayList<>();
        EmojiResult next;
        for (var i = 0; (next = getNextEmoji(inputCharArray, i)) != null; i = next.endIndex) {
            candidates.add(next);
            if (limit != 0) {
                limit--;
                if (limit <= 0) {
                    break;
                }
            }
        }

        return candidates;
    }

    public static List<EmojiResult> getEmojis(String input) {
        return getEmojis(input, 0);
    }

    /**
     * Finds the next UnicodeCandidate after a given starting index
     *
     * @param chars char array to find UnicodeCandidate in
     * @param start starting index for search
     * @return the next UnicodeCandidate or null if no UnicodeCandidate is found after start index
     */
    public static EmojiResult getNextEmoji(char[] chars, int start) {
        for (var i = start; i < chars.length; i++) {
			/*var ch = chars[i];
			if (Character.isAlphabetic(ch))
				continue;*/
            var emoji = getEmojiInPosition(chars, i);
            if (emoji != null) {
                return emoji;
            }
        }

        return null;
    }

    public static EmojiResult getEmojiInPosition(char[] chars, int start) {
        var emoji = getBestBaseEmoji(chars, start);
        if (emoji == null) {
            return null;
        }

        Fitzpatrick fitzpatrick = null;
        Gender gender = null;
        var endPos = start + emoji.unicode.length();
        var sequenceType = emoji.sequenceType;
        if (sequenceType == Emoji.SEQUENCE_BASE_SKIN_GENDER) {
            fitzpatrick = Fitzpatrick.find(chars, endPos);
            if (fitzpatrick != null) {
                endPos += 2;
            }
            var gg = findGender(chars, endPos);
            if (gg != null) {
                endPos = gg.endPos;
                gender = gg.gender;
            }
        } else if (sequenceType == Emoji.SEQUENCE_GENDER_SKIN_BASE) {

            var gg = findGender2(emoji.unicode);
            if (gg != null) {
                //endPos += emoji.unicode.length();
                gender = gg;
            }

            fitzpatrick = Fitzpatrick.find(chars, endPos);
            if (fitzpatrick != null) {
                endPos += 2;
            }

            var baseEmoji = tryGetBestBaseEmoji(chars, endPos);
            if (baseEmoji != null) {
                endPos += baseEmoji.unicode.length() + 1; // +1 because \u200D
                emoji = baseEmoji;
            }
        }

        if (chars.length > endPos) {
            var ch = chars[endPos];
            if (ch == '\uFE0F') {
                endPos++;
            }
        }
        return new EmojiResult(emoji, fitzpatrick, gender, chars, start, endPos);
    }

    private static Emoji tryGetBestBaseEmoji(char[] chars, int startPos) {
        var len = chars.length;
        if (startPos >= len) {
            return null;
        }
        if (chars[startPos] != '\u200D') {
            return null;
        }
        startPos++;
        return getBestBaseEmoji(chars, startPos);
    }

    private static GenderMatch findGender(char[] chars, int startPos) {
        var len = chars.length;
        if (len <= startPos) {
            return null;
        }
        var pos = startPos;
        var ch = chars[pos];
        if (ch != '\u200D') {
            return null;
        }
        pos++;
        var gender = Gender.find(chars, pos);
        if (gender == null) {
            return null;
        }
        return new GenderMatch(gender, pos + 1);
    }

    private static Gender findGender2(String emoji) {
        return Gender.find2(emoji);
    }

    private static class GenderMatch {
        Gender gender;
        int endPos;

        public GenderMatch(Gender gender, int endPos) {
            this.gender = gender;
            this.endPos = endPos;
        }
    }

    /**
     * Returns end index of a unicode emoji if it is found in text starting at
     * index startPos, -1 if not found.
     * This returns the longest matching emoji, for example, in
     * "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC66"
     * it will find alias:family_man_woman_boy, NOT alias:man
     *
     * @param text     the current text where we are looking for an emoji
     * @param startPos the position in the text where we should start looking for
     *                 an emoji end
     * @return the end index of the unicode emoji starting at startPos. -1 if not
     * found
     */
    public static Emoji getBestBaseEmoji(char[] text, int startPos) {
        return EmojiManager.EMOJI_TRIE.getBestEmoji(text, startPos);
    }

    public static class EmojiResult {

        public final Emoji emoji;

        public final Fitzpatrick fitzpatrick;

        public final Gender gender;

        public final char[] source;

        public final int startIndex;

        public final int endIndex;

        public EmojiResult(Emoji emoji, Fitzpatrick fitzpatrick, Gender gender, char[] source, int startIndex, int endIndex) {
            this.emoji = emoji;
            this.fitzpatrick = fitzpatrick;
            this.gender = gender;
            this.source = source;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public Emoji getEmoji() {
            return emoji;
        }

        public boolean hasFitzpatrick() {
            return getFitzpatrick() != null;
        }

        public Fitzpatrick getFitzpatrick() {
            return fitzpatrick;
        }

        public String getFitzpatrickType() {
            return hasFitzpatrick() ? fitzpatrick.name() : "";
        }

        public String getFitzpatrickUnicode() {
            return hasFitzpatrick() ? fitzpatrick.unicode : "";
        }

        public int getEmojiStartIndex() {
            return startIndex;
        }

        public int getEmojiEndIndex() {
            return startIndex + emoji.getUnicode().length();
        }

        public int getFitzpatrickEndIndex() {
            return getEmojiEndIndex() + (fitzpatrick != null ? 2 : 0);
        }

        private String sub = null;

        @Override
        public String toString() {
            if (sub != null) {
                return sub;
            }
            var len = endIndex - startIndex;
            var sub = new char[len];

            System.arraycopy(source, startIndex, sub, 0, len);
            this.sub = new String(sub);
            return this.sub;
        }
    }

    protected static class AliasCandidate {
        public final Emoji emoji;
        public final Fitzpatrick fitzpatrick;
        public final int startIndex;
        public final int endIndex;

        private AliasCandidate(Emoji emoji, Fitzpatrick fitzpatrick, int startIndex, int endIndex) {
            this.emoji = emoji;
            this.fitzpatrick = fitzpatrick;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    /**
     * Enum used to indicate what should be done when a Fitzpatrick modifier is
     * found.
     */
    public enum FitzpatrickAction {
        /**
         * Tries to match the Fitzpatrick modifier with the previous emoji
         */
        PARSE,

        /**
         * Removes the Fitzpatrick modifier from the string
         */
        REMOVE,

        /**
         * Ignores the Fitzpatrick modifier (it will stay in the string)
         */
        IGNORE
    }

    public interface EmojiTransformer {
        String transform(EmojiResult unicodeCandidate);
    }
}
