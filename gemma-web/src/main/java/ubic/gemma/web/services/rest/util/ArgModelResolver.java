package ubic.gemma.web.services.rest.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import ubic.gemma.web.services.rest.util.args.Arg;

import java.util.Iterator;

/**
 * Resolve {@link Arg} parameters' schema.
 *
 * @author poirigui
 */
public class ArgModelResolver extends ModelResolver {

    public ArgModelResolver( ObjectMapper mapper ) {
        super( mapper );
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
