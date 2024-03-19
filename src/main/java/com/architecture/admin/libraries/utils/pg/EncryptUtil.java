package com.architecture.admin.libraries.utils.pg;

import com.architecture.admin.config.SettleConfig;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/*
 * EncryptUtil
 */
public class EncryptUtil {

	// AES256 암호화 -> Base64 인코딩
	public static String encryptParam(String aesKey, String plainText) throws Exception {
		String cipherText = "";
		if (!("".equals(plainText))) {
			byte[] cipherRaw = aes256EncryptEcb(aesKey, plainText);
			cipherText = encodeBase64(cipherRaw);
		}
		return cipherText;
	}

	// Base64 디코딩 -> AES256 복호화
	public static String decryptParam(String aesKey, String cipherText) throws Exception {
		String plainText = "";
		if (!("".equals(cipherText))) {
			byte[] cipherRaw = decodeBase64(cipherText);
			plainText = new String(aes256DecryptEcb(aesKey, cipherRaw), "UTF-8");
		}
		return plainText;
	}

	// SHA256
	public static String digestSHA256(String plain) throws Exception {
		StringBuffer sb = new StringBuffer();
		MessageDigest sh = MessageDigest.getInstance("SHA-256");
		if (plain != null) {
			sh.update(plain.getBytes("UTF-8"));
			byte[] byteData = sh.digest();
			for (int i = 0; i < byteData.length; i++) {
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
			}
			return sb.toString();
		}
		return null;
	}

	/*
	 * AES256 Encrypt
	 */
	public static byte[] aes256EncryptEcb(String sKey, String sText) throws Exception {
		byte[] key = null;
		byte[] text = null;
		byte[] encrypted = null;
		final int AES_KEY_SIZE_256 = 256;
		// UTF-8
		key = sKey.getBytes("UTF-8");

		// Key size (256bit, 16byte)
		key = Arrays.copyOf(key, AES_KEY_SIZE_256 / 8);

		// UTF-8
		text = sText.getBytes("UTF-8");

		// AES/EBC/PKCS5Padding
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
		encrypted = cipher.doFinal(text);

		return encrypted;
	}

	/*
	 * AES256 Decrypt
	 */
	public static byte[] aes256DecryptEcb(String sKey, byte[] encrypted) throws Exception {
		byte[] key = null;
		byte[] decrypted = null;
		final int AES_KEY_SIZE_256 = 256;

		// UTF-8
		key = sKey.getBytes("UTF-8");

		// Key size (256bit, 16byte)
		key = Arrays.copyOf(key, AES_KEY_SIZE_256 / 8);

		// AES/EBC/PKCS5Padding
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
		decrypted = cipher.doFinal(encrypted);

		return decrypted;
	}

	/*
	 * Base64 Encode
	 */
	public static final String encodeBase64(byte[] s) throws Exception {
		if (s == null) {
			return null;
		} else {
			return new String(encode(s));
		}
	}

	public static final byte[] encode(byte abyte0[]) throws Exception {
		if (abyte0 == null)
			return null;
		byte abyte1[] = new byte[((abyte0.length + 2) / 3) * 4];
		int i = 0;
		int j = 0;
		for (; i < abyte0.length - 2; i += 3) {
			abyte1[j++] = Base64EncMap[abyte0[i] >>> 2 & 0x3f];
			abyte1[j++] = Base64EncMap[abyte0[i + 1] >>> 4 & 0xf | abyte0[i] << 4 & 0x3f];
			abyte1[j++] = Base64EncMap[abyte0[i + 2] >>> 6 & 0x3 | abyte0[i + 1] << 2 & 0x3f];
			abyte1[j++] = Base64EncMap[abyte0[i + 2] & 0x3f];
		}

		if (i < abyte0.length) {
			abyte1[j++] = Base64EncMap[abyte0[i] >>> 2 & 0x3f];
			if (i < abyte0.length - 1) {
				abyte1[j++] = Base64EncMap[abyte0[i + 1] >>> 4 & 0xf | abyte0[i] << 4 & 0x3f];
				abyte1[j++] = Base64EncMap[abyte0[i + 1] << 2 & 0x3f];
			} else {
				abyte1[j++] = Base64EncMap[abyte0[i] << 4 & 0x3f];
			}
		}
		for (; j < abyte1.length; j++)
			abyte1[j] = 61;

		return abyte1;
	}

	private static byte Base64EncMap[];
	private static byte Base64DecMap[];

	static {
		byte abyte0[] = { 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87,
				88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115,
				116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47 };
		Base64EncMap = abyte0;
		Base64DecMap = new byte[128];
		for (int i = 0; i < Base64EncMap.length; i++)
			Base64DecMap[Base64EncMap[i]] = (byte) i;
	}

	/*
	 * Base64 Decode : String s
	 */
	public static final byte[] decodeBase64(String s) throws Exception {
		if (s == null) {
			return null;
		} else {
			byte abyte0[] = s.getBytes("UTF-8");
			return decodeBase64(abyte0);
		}
	}

	/*
	 * Base64 Decode : byte
	 */
	public static final byte[] decodeBase64(byte abyte0[]) throws Exception {
		if (abyte0 == null)
			return null;
		int i;
		for (i = abyte0.length; abyte0[i - 1] == 61; i--)
			;
		byte abyte1[] = new byte[i - abyte0.length / 4];
		for (int j = 0; j < abyte0.length; j++)
			abyte0[j] = Base64DecMap[abyte0[j]];

		int k = 0;
		int l;
		for (l = 0; l < abyte1.length - 2; l += 3) {
			abyte1[l] = (byte) (abyte0[k] << 2 & 0xff | abyte0[k + 1] >>> 4 & 0x3);
			abyte1[l + 1] = (byte) (abyte0[k + 1] << 4 & 0xff | abyte0[k + 2] >>> 2 & 0xf);
			abyte1[l + 2] = (byte) (abyte0[k + 2] << 6 & 0xff | abyte0[k + 3] & 0x3f);
			k += 4;
		}

		if (l < abyte1.length) {
			abyte1[l] = (byte) (abyte0[k] << 2 & 0xff | abyte0[k + 1] >>> 4 & 0x3);
		}
		if (++l < abyte1.length) {
			abyte1[l] = (byte) (abyte0[k + 1] << 4 & 0xff | abyte0[k + 2] >>> 2 & 0xf);
		}
		return abyte1;
	}

}
