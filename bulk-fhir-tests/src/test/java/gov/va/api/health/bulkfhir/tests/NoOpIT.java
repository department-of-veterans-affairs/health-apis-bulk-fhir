package gov.va.api.health.bulkfhir.tests;

import gov.va.api.health.bulkfhir.tests.categories.LabBulkFhir;
import gov.va.api.health.bulkfhir.tests.categories.ProdBulkFhir;
import gov.va.api.health.sentinel.categories.Local;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class NoOpIT {

  @Test
  @Category({Local.class, ProdBulkFhir.class, LabBulkFhir.class})
  public void noOperation() {
    String url = TestClients.bulkFhir().service().url();
    String apiPath = TestClients.bulkFhir().service().apiPath();
    log.info("Integration Tests Running: {}/{}", url, apiPath);
  }
}
