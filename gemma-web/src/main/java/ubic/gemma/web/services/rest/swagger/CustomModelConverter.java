package ubic.gemma.web.services.rest.swagger;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;

/**
 * @author poirigui
 */
public interface CustomModelConverter extends ModelConverter {

    /**
     * Precedence to use when appending converters to {@link ModelConverters#getInstance()}.
     */
    int getPrecedence();
}
