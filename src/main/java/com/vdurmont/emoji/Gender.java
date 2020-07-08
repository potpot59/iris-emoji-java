package com.vdurmont.emoji;

/**
 * Создано 07.07.2020
 *  @author Improver: Ivan Ivanov [https://vk.com/irisism]
 */
public enum Gender {
	MALE("♂️") , FEMALE("♀️");

	public final String unicode;

	Gender(String unicode) {
		this.unicode = unicode;
	}

	public static Gender genderFromUnicode(String unicode) {
		for (Gender v : values()) {
			if (v.unicode.equals(unicode)) {
				return v;
			}
		}
		return null;
	}

	public static Gender genderFromType(String type) {
		try {
			return Gender.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static Gender find(char[] chars, int startPos) {
		if (startPos >= chars.length)
			return null;
		var ch = chars[startPos];
		switch (ch) {
			case '♂' : return MALE;
			case '♀' : return FEMALE;
		}
		return null;
	}
}
