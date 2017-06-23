package boombox.android.util;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author mriley
 */
public final class Hex {

	/**
	 * The digits for every supported radix.
	 */
	private static final char[] DIGITS = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
			'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
			'U', 'V', 'W', 'X', 'Y', 'Z'
	};

	public static String shortToHexString(int i) {
		return shortToHexString(i, new StringBuilder()).toString();
	}

	public static StringBuilder shortToHexString(int i, StringBuilder append) {
		append.append("0000");
		int cursor = append.length();

		while( i != 0 ) {
			append.setCharAt(--cursor, DIGITS[i & 0xf]);
			i >>>= 4;
		}

		return append;
	}

	public static String byteToHexString(int i) {
		return byteToHexString(i, new StringBuilder()).toString();
	}

	public static <T extends Appendable> T byteToHexString(int i, T append) {
		try {
			append.append(DIGITS[(i >> 4) & 0xf]);
			append.append(DIGITS[i & 0xf]);
		} catch (IOException ignored) {}
		return append;
	}

	public static String bytesToHexString(ByteBuffer buf) {
		return bytesToHexString(buf, new StringBuilder()).toString();
	}

	public static String bytesToHexString(byte[] buf) {
		return bytesToHexString(buf, 0, buf.length, new StringBuilder()).toString();
	}

	public static String bytesToHexString(byte[] buf, int off, int len) {
		return bytesToHexString(buf, off, len, new StringBuilder()).toString();
	}

	public static <T extends Appendable> T bytesToHexString(ByteBuffer buf, T append) {
		int len = buf.limit();
		try {
			for (int i = buf.position(); i < len; i++) {
				byteToHexString(buf.get(i), append);
				append.append(' ');
			}
		} catch (IOException ignored) {}
		return append;
	}

	public static <T extends Appendable> T bytesToHexString(byte[] buf, int off, int len, T append) {
		try {
			for(int i = off; i < len; i++ ) {
				byteToHexString(buf[i], append);
				append.append(' ');
			}
		} catch (IOException ignored) {}
		return append;
	}

	private Hex() {}
}