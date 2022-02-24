package ubic.gemma.web.services.rest.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {

    @Override
    public ObjectMapper getContext( Class<?> aClass ) {
        return new ObjectMapper()
                // parse and render date as ISO 9601
                .setDateFormat( new StdDateFormat() );
    }
}
