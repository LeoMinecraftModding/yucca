package team.leomcm.yucca.dump;

import com.google.gson.JsonArray;

import java.util.Locale;

final class BbmodelJson {

	private static final float ROUND_FACTOR = 1_000_000.0F;

	static JsonArray vector(float x, float y, float z) {
		JsonArray array = new JsonArray();
		array.add(number(x));
		array.add(number(y));
		array.add(number(z));
		return array;
	}

	static JsonArray vector(int x, int y) {
		JsonArray array = new JsonArray();
		array.add(x);
		array.add(y);
		return array;
	}

	static Number number(float value) {
		float cleaned = round(value);
		return cleaned == (int) cleaned ? (Number) (int) cleaned : (Number) cleaned;
	}

	static float round(float value) {
		if (value == 0.0F) {
			return 0.0F;
		}
		return (float) (Math.round(value * (double) ROUND_FACTOR) / (double) ROUND_FACTOR);
	}

	static float degrees(float value) {
		return Math.round(value * 1000.0F) / 1000.0F;
	}

	static String molang(float value) {
		return text(round(value));
	}

	static String molangDegrees(float value) {
		return text(degrees(value));
	}

	private static String text(float cleaned) {
		if (cleaned == (int) cleaned) {
			return String.valueOf((int) cleaned);
		}
		return String.format(Locale.ROOT, "%.6f", cleaned).replaceAll("0+$", "").replaceAll("\\.$", "");
	}
}
