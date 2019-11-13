package gov.va.api.health.bulkfhir.service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.dstu2.api.resources.Resource;
import java.util.function.Function;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class JsonStringConverter<T extends Resource> implements Function<T, String> {

  private final ObjectMapper jacksonMapper;

  @Override
  public String apply(T resource) {
    if (resource == null) {
      return null;
    }
    try {
      return jacksonMapper.writeValueAsString(resource);
    } catch (JsonProcessingException e) {
      log.error("Failed to JSONify resource: {}", resource, e);
      return null;
    }
  }
}
