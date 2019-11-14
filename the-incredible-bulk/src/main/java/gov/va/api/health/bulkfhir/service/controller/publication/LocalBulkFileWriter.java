package gov.va.api.health.bulkfhir.service.controller.publication;

import gov.va.api.health.bulkfhir.service.filebuilder.BulkFileWriter;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "bulk.file.writer", havingValue = "local")
public class LocalBulkFileWriter implements BulkFileWriter {

  @Override
  public void writeFile(FileClaim claim, Stream<String> resources) throws IOException {
    Path filePath = Paths.get(claim.fileName() + ".ndjson");
    Files.writeString(filePath, resources.collect(Collectors.joining(Strings.LINE_SEPARATOR)));
    log.info("File written to {}", filePath.toAbsolutePath().toString());
  }
}
