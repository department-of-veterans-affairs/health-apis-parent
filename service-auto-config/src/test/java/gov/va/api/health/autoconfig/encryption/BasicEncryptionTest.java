package gov.va.api.health.autoconfig.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BasicEncryptionTest {

  @Test
  void encrypterRoundTrip() {
    String og = "The quick brown fox jumped over the lazy dog.";
    BasicEncryption e = BasicEncryption.forKey("foobar");
    String encrypted = e.encrypt(og);
    assertThat(encrypted).isNotEqualTo(og);
    assertThat(e.decrypt(encrypted).equals(og));
  }
}
