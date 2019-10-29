package gov.va.api.health.bulkfhir.service.controller.status;

import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(
    value = {"internal/publication"},
    produces = {"application/json"})
@AllArgsConstructor(onConstructor = @__({@Autowired}))
@Builder
public class InternalPublicationController {

  private final StatusRepository repository;

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

  @DeleteMapping(path = "{id}")
  public void deletePublication(@PathVariable("id") String publicationId) {}
}
