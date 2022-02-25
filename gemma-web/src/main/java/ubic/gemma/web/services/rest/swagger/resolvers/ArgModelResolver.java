package ubic.gemma.web.services.rest.swagger.resolvers;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ubic.gemma.web.services.rest.util.args.Arg;

import java.util.Iterator;

/**
 * Resolve {@link Arg} parameters' schema.
 *
 * This should always be loaded last to take priority as it addresses a glitch in the original {@link ModelResolver}.
 *
 * @author poirigui
 */
@Component
@SuppressWarnings("DefaultAnnotationParam")
@Order(Ordered.LOWEST_PRECEDENCE) // it's the default, but we want to make it explicit
public class ArgModelResolver extends ModelResolver {

    @Autowired
    public ArgModelResolver( @Qualifier("swaggerObjectMapper") ObjectMapper objectMapper ) {
        super( objectMapper );
    }

    @Override
    public Schema resolve( AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain ) {
        JavaType t = Json.mapper().constructType( type.getType() );
        if ( Arg.class.isAssignableFrom( t.getRawClass() ) ) {
            // I'm suspecting there's a bug in Swagger that causes request parameters annotations to shadow the
            // definitions in the class's Schema annotation
            return super.resolve( new AnnotatedType( ( t.getRawClass() ) ), context, chain );
        }
        if ( chain.hasNext() ) {
            return chain.next().resolve( type, context, chain );
        } else {
            return null;
        }
    }
}
