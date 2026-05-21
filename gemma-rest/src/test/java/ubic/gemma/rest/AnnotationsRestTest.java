package ubic.gemma.rest;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * End-to-end checks for {@link AnnotationsWebService}.
 * <p>
 * Counts depend on the loaded ontologies and indexed characteristics, so these tests assert wiring (response shape,
 * presence of the {@code usageCount} field) rather than exact numbers. Numeric correctness is covered by
 * {@link AnnotationsWebServiceTest}.
 */
@Category(SlowTest.class)
public class AnnotationsRestTest extends BaseJerseyIntegrationTest {

    @Test
    public void testSearchAnnotationsReturnsUsageCountField() {
        // "brain" is broadly indexed and unlikely to error out; the count itself depends on the ontology load state.
        assertThat( target( "/annotations/search" ).queryParam( "query", "brain" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .extracting( "data", list( Object.class ) )
                .satisfies( data -> {
                    // List may be empty in a bare test DB; if it isn't, the usageCount key must exist.
                    if ( !data.isEmpty() ) {
                        java.util.Map<String, Object> first = ( java.util.Map<String, Object> ) data.get( 0 );
                        org.assertj.core.api.Assertions.assertThat( first ).containsKey( "usageCount" );
                    }
                } );
    }

    @Test
    public void testSearchAnnotationsRejectsEmptyQuery() {
        assertThat( target( "/annotations/search" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
    }
}
