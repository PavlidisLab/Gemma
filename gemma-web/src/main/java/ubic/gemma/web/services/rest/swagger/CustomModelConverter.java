package ubic.gemma.web.services.rest.swagger;

import io.swagger.v3.core.converter.ModelConverter;
import org.springframework.core.Ordered;

/**
 * @author poirigui
 */
public interface CustomModelConverter extends ModelConverter, Ordered {

    @Override
    default int getOrder() {
        return 0;
    }
}
