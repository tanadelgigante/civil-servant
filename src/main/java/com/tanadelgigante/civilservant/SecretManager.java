package com.tanadelgigante.civilservant;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretManager {
	private static final Logger logger = LoggerFactory.getLogger(SecretManager.class);
	private static final SecretManager INSTANCE = new SecretManager();
	private static final String KEY = System.getenv().getOrDefault("CIVILSERVANT_SECRET_KEY", "default-key-123456789012345678901234");

	private SecretManager() {
	}

	public static SecretManager getInstance() {
		return INSTANCE;
	}

	public String decrypt(String encrypted) {
		try {
			SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), 0, 16, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec);
			byte[] decoded = Base64.getDecoder().decode(encrypted);
			return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
		} catch (Exception e) {
			logger.error("Errore decrittazione secret");
			throw new RuntimeException("Secret decryption failed", e);
		}
	}

	public String encrypt(String plain) {
		try {
			SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), 0, 16, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
			byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			logger.error("Errore crittazione secret");
			throw new RuntimeException("Secret encryption failed", e);
		}
	}
}
