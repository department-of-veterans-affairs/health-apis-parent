package gov.va.api.health.autoconfig.encryption;

public class Decrypter {
  /**
   * Tool to decrypt strings encrypted by the BasicEncryption class.
   *
   * @see BasicEncryption
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("Invalid number of arguments. Expected <key> <text>.");
      return;
    }

    String encryptionKey = args[0];
    String cipherText = args[1];

    String decrypted = BasicEncryption.forKey(encryptionKey).decrypt(cipherText);

    System.out.println(decrypted);
  }
}
