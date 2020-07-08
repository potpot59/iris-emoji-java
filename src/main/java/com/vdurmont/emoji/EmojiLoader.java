package com.vdurmont.emoji;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the emojis from a JSON database.
 *
 * @author Improver: Ivan Ivanov [https://vk.com/irisism]<br>
 * Creator: Vincent DURMONT [vdurmont@gmail.com]
 */
public class EmojiLoader {
	/**
	 * No need for a constructor, all the methods are static.
	 */
	private EmojiLoader() {}

	/**
	 * Loads a JSONArray of emojis from an InputStream, parses it and returns the
   * associated list of {@link com.vdurmont.emoji.Emoji}s
	 *
	 * @param stream the stream of the JSONArray
   *
   * @return the list of {@link com.vdurmont.emoji.Emoji}s
	 * @throws IOException if an error occurs while reading the stream or parsing
	 *                     the JSONArray
	 */
	public static List<Emoji> loadEmojis(InputStream stream) throws IOException {
		JSONArray emojisJSON = new JSONArray(inputStreamToString(stream));
		List<Emoji> emojis = new ArrayList<>(emojisJSON.length());
		for (int i = 0; i < emojisJSON.length(); i++) {
			Emoji emoji = buildEmojiFromJSON(emojisJSON.getJSONObject(i));
			if (emoji != null) {
				emojis.add(emoji);
			}
		}
		return emojis;
	}

	private static String inputStreamToString(
			InputStream stream
	) throws IOException {
		StringBuilder sb = new StringBuilder();
		InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
		BufferedReader br = new BufferedReader(isr);
		String read;
		while ((read = br.readLine()) != null) {
			sb.append(read);
		}
		br.close();
		return sb.toString();
	}
/*
	private static final String[][] gender1 = {
			{"adult", "\uD83E\uDDD1"}
			, {"male", "\uD83D\uDC68"}
			, {"female", "\uD83D\uDC69"}
	};
	private static final String[][] gender2 = {
			{"male", "\u200D♂️"}
			, {"female", "\u200D♀️"}
	};

	private static final String[][] skins = {
			{"white", "\uD83C\uDFFB"}
			, {"cream white", "\uD83C\uDFFC"}
			, {"moderate brown", "\uD83C\uDFFD"}
			, {"dark brown", "\uD83C\uDFFE"}
			, {"black", "\uD83C\uDFFF"}
	};

	protected static List<Emoji> buildEmojiesFromJSON(JSONObject json) throws UnsupportedEncodingException {
		if (!json.has("emoji")) {
			return null;
		}

		String pattern = json.getString("emoji");
		List<String> aliases = jsonArrayToStringList(json.getJSONArray("aliases"));
		EmojiPrepare[] emojies;
		if (pattern.indexOf('{') != -1) {
			boolean hasGender1 = pattern.contains("{person}");
			boolean hasGender2 = pattern.contains("{gender}");
			boolean hasSkin = pattern.contains("{skin}");
			var patterns = new LinkedList<EmojiPrepare>();
			patterns.add(new EmojiPrepare(pattern, aliases));

			if (hasSkin) {
				var tmp = new LinkedList<EmojiPrepare>();
				for (EmojiPrepare i : patterns) {
					tmp.add(new EmojiPrepare(i.pattern.replace("{skin}", ""), aliases));
					for (String[] g : skins) {
						var aa = new LinkedList<String>();
						for (String a : i.aliases)
							aa.add(g[0] + ' ' + a);
						var newPattern = i.pattern.replace("{skin}", g[1]);
						tmp.add(new EmojiPrepare(newPattern, aa));
					}
				}
				patterns = tmp;
			}

			if (hasGender1) {
				var tmp = new LinkedList<EmojiPrepare>();
				for (EmojiPrepare i : patterns)
					for (String[] g : gender1) {
						var aa = new LinkedList<String>();
						for (String a : i.aliases)
							aa.add(g[0] + ' ' + a);
						var newPattern = i.pattern.replace("{person}", g[1]);
						tmp.add(new EmojiPrepare(newPattern, aa));
					}
				patterns = tmp;
			}

			if (hasGender2) {
				var tmp = new LinkedList<EmojiPrepare>();
				for (EmojiPrepare i : patterns)
					for (String[] g : gender2) {
						tmp.add(new EmojiPrepare(i.pattern.replace("{gender}", ""), aliases));
						var aa = new LinkedList<String>();
						for (String a : i.aliases)
							aa.add(g[0] + ' ' + a);
						var newPattern = i.pattern.replace("{gender}", g[1]);
						tmp.add(new EmojiPrepare(newPattern, aa));
					}
				patterns = tmp;
			}



			emojies = patterns.toArray(new EmojiPrepare[0]);

		} else
			emojies = new EmojiPrepare[] {new EmojiPrepare(pattern, aliases)};
		String description = null;
		if (json.has("description")) {
			description = json.getString("description");
		}
		boolean supportsFitzpatrick = false;
		if (json.has("supports_fitzpatrick")) {
			supportsFitzpatrick = json.getBoolean("supports_fitzpatrick");
		}

		List<String> tags = jsonArrayToStringList(json.getJSONArray("tags"));

		ArrayList<Emoji> res = new ArrayList<>();
		for (EmojiPrepare emoji : emojies) {
			byte[] bytes = emoji.pattern.getBytes(StandardCharsets.UTF_8);
			res.add(new Emoji(description, supportsFitzpatrick, emoji.aliases, tags, bytes));
		}
		return res;
		//return new Emoji(description, supportsFitzpatrick, aliases, tags, bytes);
	}

	private static final class EmojiPrepare {
		List<String> aliases;
		String pattern;

		public EmojiPrepare(String patter, List<String> aliases) {
			this.aliases = aliases;
			this.pattern = patter;
		}
	}*/

	protected static Emoji buildEmojiFromJSON(
			JSONObject json
	) throws UnsupportedEncodingException {
		if (!json.has("emoji")) {
			return null;
		}

		byte[] bytes = json.getString("emoji").getBytes(StandardCharsets.UTF_8);
		String description = null;
		if (json.has("description")) {
			description = json.getString("description");
		}
		boolean supportsFitzpatrick = false;
		if (json.has("supports_fitzpatrick")) {
			supportsFitzpatrick = json.getBoolean("supports_fitzpatrick");
		}
		List<String> aliases = jsonArrayToStringList(json.getJSONArray("aliases"));
		List<String> tags = jsonArrayToStringList(json.getJSONArray("tags"));
		return new Emoji(description, supportsFitzpatrick, aliases, tags, bytes);
	}

	private static List<String> jsonArrayToStringList(JSONArray array) {
		List<String> strings = new ArrayList<String>(array.length());
		for (int i = 0; i < array.length(); i++) {
			strings.add(array.getString(i));
		}
		return strings;
	}
}
