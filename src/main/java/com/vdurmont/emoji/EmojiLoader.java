package com.vdurmont.emoji;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

	protected static Emoji buildEmojiFromJSON(JSONObject json) {
		if (!json.has("emoji")) {
			return null;
		}
		var unicode = json.getString("emoji");

		// Lifehach to filter out old emojis map from wrong gender_base records
		if (unicode.startsWith("\uD83D\uDC69\u200D") || unicode.startsWith("\uD83D\uDC68\u200D"))
			return null;
		var emojiChar = json.getString("emojiChar");
		//byte[] bytes = unicode.getBytes(StandardCharsets.UTF_8);

		String description = null;
		if (json.has("description")) {
			description = json.getString("description");
		}
		int sequenceType = 0;
		if (json.has("sequence_type")) {
			sequenceType = json.getInt("sequence_type");
		}
		List<String> aliases = jsonArrayToStringList(json.getJSONArray("aliases"));
		List<String> tags = jsonArrayToStringList(json.getJSONArray("tags"));
		return new Emoji(description, sequenceType, aliases, tags, unicode, !unicode.equals(emojiChar)? emojiChar : unicode);
	}

	private static List<String> jsonArrayToStringList(JSONArray array) {
		List<String> strings = new ArrayList<String>(array.length());
		for (int i = 0; i < array.length(); i++) {
			strings.add(array.getString(i));
		}
		return strings;
	}
}
