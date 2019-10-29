package gov.va.api.health.bulkfhir.service.controller.status;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

public interface StatusRepository extends PagingAndSortingRepository<StatusEntity, String> {

  @Transactional
  int deleteByPublicationName(String publicationName);
}
