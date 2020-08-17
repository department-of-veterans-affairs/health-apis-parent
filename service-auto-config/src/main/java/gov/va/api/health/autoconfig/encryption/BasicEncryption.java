package gov.va.api.health.autoconfig.encryption;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;

@AllArgsConstructor(staticName = "forKey")
public class BasicEncryption {
  private final String encryptionKey;

  /** Decrypts a plain-text AES encoded string. */
  @SneakyThrows
  public String decrypt(String cipherText) {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    byte[] allBytes = Base64.getDecoder().decode(cipherText);
    byte[] iv = Arrays.copyOfRange(allBytes, 0, cipher.getBlockSize());
    Key key = generateKey();
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
    byte[] enBytes = Arrays.copyOfRange(allBytes, cipher.getBlockSize(), allBytes.length);
    return new String(cipher.doFinal(enBytes), StandardCharsets.UTF_8);
  }

  /** Encrypts a plain-text string. */
  @SneakyThrows
  public String encrypt(String plainText) {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    Key key = generateKey();
    SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
    byte[] iv = new byte[cipher.getBlockSize()];
    secureRandom.nextBytes(iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
    byte[] enBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    byte[] combined = ArrayUtils.addAll(iv, enBytes);
    return Base64.getEncoder().encodeToString(combined);
  }

  /** Takes the bytes from the encryptionKey and makes sure it is the correct size. */
  @SneakyThrows
  private Key generateKey() {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(encryptionKey.getBytes(StandardCharsets.UTF_8));
    return new SecretKeySpec(digest.digest(), "AES");
  }
}
