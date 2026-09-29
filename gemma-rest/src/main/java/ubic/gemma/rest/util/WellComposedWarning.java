package ubic.gemma.rest.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Machine-readable reason, usually the exception's class name.")
    String reason;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "What the warning is, in one line.")
    String message;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Which parameter or field the warning is about. Null when it is not about one in particular.")
    String location;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Where `location` should be read: a query parameter, a path segment, or the request body.")
    LocationType locationType;
}
