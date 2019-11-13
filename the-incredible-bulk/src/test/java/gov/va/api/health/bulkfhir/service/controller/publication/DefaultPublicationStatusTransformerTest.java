package gov.va.api.health.bulkfhir.service.controller.publication;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.bulkfhir.api.internal.BuildStatus;
import gov.va.api.health.bulkfhir.service.controller.publication.DefaultPublicationStatusTransformer.Counter;
import org.junit.jupiter.api.Test;

public class DefaultPublicationStatusTransformerTest {

  private Counter counter(int notStarted, int inProgress, int complete) {
    var c = new Counter();
    c.notStarted = notStarted;
    c.inProgress = inProgress;
    c.completed = complete;
    return c;
  }

  @Test
  void overallStatusCalculation() {
    assertThat(counter(0, 0, 0).overallStatus()).isEqualTo(BuildStatus.NOT_STARTED);
    assertThat(counter(9, 0, 0).overallStatus()).isEqualTo(BuildStatus.NOT_STARTED);
    assertThat(counter(8, 1, 0).overallStatus()).isEqualTo(BuildStatus.IN_PROGRESS);
    assertThat(counter(8, 0, 1).overallStatus()).isEqualTo(BuildStatus.IN_PROGRESS);
    assertThat(counter(7, 1, 1).overallStatus()).isEqualTo(BuildStatus.IN_PROGRESS);
    assertThat(counter(0, 8, 1).overallStatus()).isEqualTo(BuildStatus.IN_PROGRESS);
    assertThat(counter(0, 1, 8).overallStatus()).isEqualTo(BuildStatus.IN_PROGRESS);
    assertThat(counter(0, 0, 9).overallStatus()).isEqualTo(BuildStatus.COMPLETE);
  }

  @Test
  void publicationStatus() {
    assertThat(
            new DefaultPublicationStatusTransformer()
                .apply(PublicationSamples.Entity.create().entitiesWithIds()))
        .isEqualTo(PublicationSamples.Api.create().status());
  }
}
