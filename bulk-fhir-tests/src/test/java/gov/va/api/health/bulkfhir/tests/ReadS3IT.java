package gov.va.api.health.bulkfhir.tests;

import com.google.common.collect.ImmutableMap;
import gov.va.api.health.bulkfhir.tests.categories.LabBulkFhir;
import gov.va.api.health.bulkfhir.tests.categories.ProdBulkFhir;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class ReadS3IT {

  private final String apiPath() {
    return TestClients.bulkFhir().service().apiPath();
  }

  @Test
  @Category({ProdBulkFhir.class, LabBulkFhir.class})
  public void verifyBucketConnectivity() {
    String path = apiPath() + "bulk/publication/index.json";
    log.info("Verify index.json is readable [{}]", path);
    TestClients.bulkFhir()
        .get(ImmutableMap.of("client-key", System.getProperty("bulk-token", "default-value")), path)
        .expect(200);
  }
}
