package gov.va.api.health.bulkfhir.service.controller.publication;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.assertj.core.util.Lists;
import org.junit.Test;

public class LocalBulkFileWriterTest {

  private FileClaim claim() {
    return FileClaim.builder()
        .request(FileBuildRequest.builder().publicationId("p").fileId("f").build())
        .page(3)
        .fileName("fake")
        .count(1234)
        .build();
  }

  @Test
  public void localFileWriteDoesNotExplode() throws Exception {
    LocalBulkFileWriter fileWriter = new LocalBulkFileWriter();
    fileWriter.writeFile(claim(), Lists.newArrayList("HELLO").stream());
    Path filePath = Paths.get("fake.ndjson");
    /* Make sure the file is created */
    assertThat(Files.exists(filePath)).isTrue();
    /* Delete the file */
    Files.delete(filePath);
  }
}
