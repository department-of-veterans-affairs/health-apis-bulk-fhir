package gov.va.api.health.bulkfhir.anonymizer;

import com.fasterxml.uuid.Generators;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class SaltedType5UuidGenerator implements AnonymizedIdGenerator {

  private final String saltKey;

  private final String resource;

  private final String seed;

  @Override
  public String generateIdFrom(String identifier) {
    String combinedString = saltKey + ":" + resource + ":" + identifier;
    return Generators.nameBasedGenerator(UUID.fromString(seed)).generate(combinedString).toString();
  }
}
