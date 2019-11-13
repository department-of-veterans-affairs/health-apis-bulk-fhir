package gov.va.api.health.bulkfhir.service.controller.publication;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.va.api.health.bulkfhir.service.filebuilder.BulkFileWriter;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Service
@Slf4j
@ConditionalOnProperty(name = "bulk.file.writer", havingValue = "s3")
public class S3BulkFileWriter implements BulkFileWriter {

  @Value("${aws.s3.bucket}")
  private String s3Bucket;

  @Value("${aws.region}")
  private String awsRegion;

  @Override
  public void writeFile(FileClaim claim, Stream<String> resources) throws JsonProcessingException {
    S3Client s3Client = S3Client.builder().region(Region.of(awsRegion)).build();

    PutObjectResponse response =
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(claim.request().publicationId() + "/" + claim.fileName() + ".ndjson")
                .build(),
            RequestBody.fromString(resources.collect(Collectors.joining(Strings.LINE_SEPARATOR))));
    log.info("File successfully uploaded to S3: {}", response);
  }
}
