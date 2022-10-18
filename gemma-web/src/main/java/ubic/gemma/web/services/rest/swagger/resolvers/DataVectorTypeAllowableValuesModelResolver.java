package ubic.gemma.web.services.rest.swagger.resolvers;

import com.fasterxml.jackson.databind.introspect.Annotated;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.web.services.rest.swagger.CustomModelConverter;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DataVectorTypeAllowableValuesModelResolver extends ModelResolver implements CustomModelConverter {

    public DataVectorTypeAllowableValuesModelResolver() {
        super( Json.mapper() );
    }

    @Override
    protected List<String> resolveAllowableValues( Annotated a, Annotation[] annotations, Schema schema ) {
        if ( schema != null && schema.name().equals( "vectorType" ) ) {
            return Stream.of( RawExpressionDataVector.class, ProcessedExpressionDataVector.class )
                    .map( Class::getName )
                    .collect( Collectors.toList() );
        } else {
            return super.resolveAllowableValues( a, annotations, schema );
        }
    }
}
