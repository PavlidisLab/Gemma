package ubic.gemma.web.services.rest.swagger.resolvers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.web.services.rest.SearchWebService;
import ubic.gemma.web.services.rest.swagger.CustomModelConverter;
import ubic.gemma.web.services.rest.util.args.LimitArg;
import ubic.gemma.web.services.rest.util.args.PlatformArg;
import ubic.gemma.web.services.rest.util.args.TaxonArg;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves allowed values for the {@link ubic.gemma.web.services.rest.SearchWebService#search(String, TaxonArg, PlatformArg, List, LimitArg)}
 * resultTypes argument.
 *
 * This ensures that the OpenAPI specification exposes all supported search result types in the {@link SearchService} as
 * allowable values.
 *
 * @author poirigui
 */
@Component
public class SearchResultTypeAllowableValuesModelResolver extends ModelResolver implements CustomModelConverter {

    private final SearchService searchService;

    @Autowired
    public SearchResultTypeAllowableValuesModelResolver( @Qualifier("swaggerObjectMapper") ObjectMapper objectMapper, SearchService searchService ) {
        super( objectMapper );
        this.searchService = searchService;
    }

    @Override
    protected List<String> resolveAllowableValues( Annotated a, Annotation[] annotations, Schema schema ) {
        if ( schema != null && schema.name().equals( SearchWebService.RESULT_TYPES_SCHEMA_NAME ) ) {
            return searchService.getSupportedResultTypes().stream().map( Class::getName ).collect( Collectors.toList() );
        }
        return super.resolveAllowableValues( a, annotations, schema );
    }
}
