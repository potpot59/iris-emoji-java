package com.vdurmont.emoji;

/**
 * Создано 07.07.2020
 *  @author Improver: Ivan Ivanov [https://vk.com/irisism]
 */
public enum Gender {
	MALE() , FEMALE(), PERSON();

	public static Gender find(char[] chars, int startPos) {
		if (startPos >= chars.length)
			return null;
		var ch = chars[startPos];
		return switch (ch) {
			case '♂' -> MALE;
			case '♀' -> FEMALE;
			default -> null;
		};
	}

	public static Gender find2(String emoji) {
		return find2(emoji.toCharArray(), 0);
	}

	public static Gender find2(char[] chars, int startPos) {
		if (startPos + 2 > chars.length)
			return null;
		var ch = chars[startPos];
		return switch (ch) {
			case '\uD83D' -> switch (chars[startPos + 1]) {
				case '\uDC68' -> MALE;
				case '\uDC69' -> FEMALE;
				default -> null;
			};
			case '\uD83E' -> switch (chars[startPos + 1]) {
				case '\uDDD1' -> PERSON;
				default -> null;
			};

			default -> null;
		};
	}
}
