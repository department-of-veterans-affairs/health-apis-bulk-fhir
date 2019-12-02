package gov.va.api.health.bulkfhir.anonymizer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@AllArgsConstructor
public class SaltedType5UuidGenerator implements AnonymizedIdGenerator {

  private final String saltKey;

  private final String resource;

  /**
   * Create the type 5 UUID for the given byte data.
   *
   * @param data The data to convert to a UUID
   * @return The type 5 UUID
   */
  private UUID constructType5Uuid(byte[] data) {
    long msb = 0;
    long lsb = 0;
    for (int i = 0; i < 8; i++) {
      msb = (msb << 8) | (data[i] & 0xff);
    }
    for (int i = 8; i < 16; i++) {
      lsb = (lsb << 8) | (data[i] & 0xff);
    }
    return new UUID(msb, lsb);
  }

  @Override
  public String generateIdFrom(String identifier) {
    String combinedString = saltKey + ":" + resource + ":" + identifier;
    return generateType5Uuid(combinedString).toString();
  }

  /**
   * Generate a type 5 UUID for the given source string.
   *
   * @param source The string to generate a UUID for
   * @return The type 5 UUID
   */
  private UUID generateType5Uuid(String source) {
    byte[] sourceByteArray = source.getBytes(StandardCharsets.UTF_8);
    return type5UuidFromBytes(sourceByteArray);
  }

  /**
   * Create the type 5 UUID from the byte array.
   *
   * @param sourceByteArray The byte array of the source
   * @return The type 5 UUID
   */
  private UUID type5UuidFromBytes(byte[] sourceByteArray) {
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException nsae) {
      throw new InternalError("SHA-512 not supported", nsae);
    }
    messageDigest.update(saltKey.getBytes(StandardCharsets.UTF_8));
    byte[] bytes = Arrays.copyOfRange(messageDigest.digest(sourceByteArray), 0, 16);
    bytes[6] &= 0x0f;
    /* clear version        */
    bytes[6] |= 0x50;
    /* set to version 5     */
    bytes[8] &= 0x3f;
    /* clear variant        */
    bytes[8] |= 0x80;
    /* set to IETF variant  */
    return constructType5Uuid(bytes);
  }
}
