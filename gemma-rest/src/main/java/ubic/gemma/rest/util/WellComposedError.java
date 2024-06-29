package ubic.gemma.rest.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

import ubic.gemma.core.lang.Nullable;

@Value
public class WellComposedError {

    String reason;

    String message;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String location;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    LocationType locationType;
}
