package gov.va.api.health.bulkfhir.service.controller.status;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface StatusRepository extends PagingAndSortingRepository<StatusEntity, String> {}
