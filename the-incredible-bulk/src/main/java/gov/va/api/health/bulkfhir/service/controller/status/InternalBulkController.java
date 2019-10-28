package gov.va.api.health.bulkfhir.service.controller.status;

import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(
  value = {"internal/bulk"},
  produces = {"application/json", "application/json+fhir", "application/fhir+json"}
)
public class InternalBulkController {

  @Autowired StatusRepository repository;

  @GetMapping
  List<StatusEntity> deleteMe() {
    var result = new LinkedList<StatusEntity>();
    repository.findAll().forEach(result::add);
    return result;
  }

  @GetMapping(value = {"/add"})
  void deleteMeAddOne() {
    repository.save(
        StatusEntity.builder()
            .publicationName("pubby")
            .publicationEpoch(System.currentTimeMillis())
            .recordsPerFile(10)
            .firstRecord(1)
            .lastRecord(10)
            .fileName("filegumbo")
            .buildStartEpoch(0)
            .buildCompleteEpoch(0)
            .build());
  }
}
