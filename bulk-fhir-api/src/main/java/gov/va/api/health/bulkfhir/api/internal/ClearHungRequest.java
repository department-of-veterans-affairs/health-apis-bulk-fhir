package gov.va.api.health.bulkfhir.api.internal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;


/**
* The `/internal/publication/hung` endpoint will be automatically triggered using a
* timer. This is the post body for the endpoint should someone wish to run it
* manually (testing purposes, etc.).
*/
@Value
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ClearHungRequest {

    /**
    * We don't want to allow clearing of builds that have only just begun (0)
    * therefor the min will be 0, max can be however long the person wants to
    * all a build to wait for.
    */
    @NotNull
    @Min(1)
    Long hangTime;
}
