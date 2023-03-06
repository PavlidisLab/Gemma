package ubic.gemma.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Data;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.TestComponent;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;
import ubic.gemma.rest.util.BaseJerseyTest;
import ubic.gemma.rest.util.JacksonConfig;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubic.gemma.rest.util.Assertions.assertThat;

@ContextConfiguration
public class OpenApiTest extends BaseJerseyTest {

    @Import(JacksonConfig.class)
    @Configuration
    @TestComponent
    static class OpenApiTestContextConfiguration {

        @Bean
        public CustomModelResolver customModelResolver() {
            return new CustomModelResolver( Json.mapper(), mock( SearchService.class ) );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mockFilteringService( ExpressionExperimentService.class, ExpressionExperiment.class );
        }

        @Bean
        public ExpressionAnalysisResultSetService expressionAnalysisResultSetService() {
            return mockFilteringService( ExpressionAnalysisResultSetService.class, ExpressionAnalysisResultSet.class );
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mockFilteringService( ArrayDesignService.class, ArrayDesign.class );
        }

        @Bean
        public TaxonService taxonService() {
            return mockFilteringService( TaxonService.class, Taxon.class );
        }

        private static <S extends Identifiable, T extends FilteringService<S>> T mockFilteringService( Class<T> clazz, Class<S> elementClass ) {
            T ees = mock( clazz );
            when( ees.getElementClass() ).thenAnswer( a -> elementClass );
            when( ees.getFilterableProperties() ).thenReturn( Collections.emptySet() );
            return ees;
        }
    }

    @Autowired
    private CustomModelResolver customModelResolver;

    @Autowired
    @Qualifier("swaggerObjectMapper")
    private ObjectMapper objectMapper;

    private OpenAPI spec;

    @Before
    public void setUpSpec() throws IOException {
        // FIXME: this is normally initialized in the servlet
        ModelConverters.getInstance().addConverter( customModelResolver );
        Response response = target( "/openapi.json" ).request().get();
        assertThat( response )
                .hasStatus( Response.Status.OK );
        spec = objectMapper.readValue( response.readEntity( InputStream.class ), OpenAPI.class );
    }

    @Test
    public void testInfoMatchContentOfOpenApiConfiguration() throws IOException {
        OpenApiConfiguration config;
        try ( InputStream is = new ClassPathResource( "/openapi-configuration.yaml" ).getInputStream() ) {
            config = Yaml.mapper().readValue( is, OpenApiConfiguration.class );
        }
        assertThat( config.getResourcePackages() )
                .containsExactly( getClass().getPackage().getName() );
        assertThat( spec.getInfo().getVersion() )
                .isNotBlank()
                .isEqualTo( config.getOpenAPI().getInfo().getVersion() );
    }

    @Data
    private static class OpenApiConfiguration {
        private String[] resourcePackages;
        private OpenAPI openAPI;
    }


    @Test
    @Ignore("This is broken due to some quirk in Swagger (see https://github.com/PavlidisLab/Gemma/issues/524)")
    public void testFilterArgSchemas() {
        assertThat( spec.getComponents().getSchemas() )
                .doesNotContainKey( "Filter" )
                .containsKeys( "FilterArgExpressionExperiment", "FilterArgArrayDesign", "FilterArgExpressionAnalysisResultSet" );
        Schema<?> schema = spec.getComponents().getSchemas().get( "FilterArgExpressionExperiment" );
        assertThat( schema.getType() )
                .isEqualTo( "string" );
        assertThat( schema.getDescription() ).contains( "Available properties:" );
    }

    @Test
    @Ignore("This is broken due to some quirk in Swagger (see https://github.com/PavlidisLab/Gemma/issues/524)")
    public void testSortArgSchemas() {
        assertThat( spec.getComponents().getSchemas() )
                .doesNotContainKey( "Sort" )
                .containsKeys( "SortArgExpressionExperiment", "SortArgArrayDesign", "SortArgExpressionAnalysisResultSet", "SortArgTaxon" );
        Schema<?> schema = spec.getComponents().getSchemas().get( "SortArgExpressionExperiment" );
        assertThat( schema.getType() )
                .isEqualTo( "string" );
        assertThat( schema.getDescription() ).contains( "Available properties:" );
    }
}
