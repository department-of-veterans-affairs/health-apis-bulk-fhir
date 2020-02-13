package gov.va.api.health.bulkfhir.service.controller.publication;

import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import org.assertj.core.util.Lists;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3BulkFileWriterTest {

  @Test
  public void attemptToWriteToS3ExpectExplosion() throws Exception {
    S3BulkFileWriter fileWriter = new S3BulkFileWriter();
    fileWriter.setAwsRegion("us-gov-west-1");
    fileWriter.setS3Bucket("fake-test-bucket");
    try {
      fileWriter.writeFile(claim(), Lists.newArrayList("HELLO").stream());
    } catch (SdkClientException | S3Exception e) {
      // Eat it - the test bucket shouldn't exist
    }
  }

  private FileClaim claim() {
    return FileClaim.builder()
        .request(FileBuildRequest.builder().publicationId("p").fileId("f").build())
        .page(3)
        .count(1234)
        .build();
  }
}
