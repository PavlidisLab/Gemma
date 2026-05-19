package ubic.gemma.rest.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import org.springframework.lang.Nullable;

/**
 * A warning, loosely modeled on {@link WellComposedError}.
 * @author poirigui
 * @see ResponseDataObject#getWarnings()
 */
@Value
@Builder
@Jacksonized
public class WellComposedWarning {

    String reason;

    String message;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String location;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    LocationType locationType;
}
