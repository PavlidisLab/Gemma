package ubic.gemma.core.search.source;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;
import org.compass.core.*;
import org.compass.core.config.CompassSettings;
import org.compass.core.spi.InternalCompassSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.util.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class CompassSearchSourceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class CompassSearchSourceTestContextConfiguration {
        @Bean
        public SearchSource compassSearchSource() {
            return new CompassSearchSource();
        }

        @Bean
        public Compass compassArray() {
            return mock( Compass.class );
        }

        @Bean
        public Compass compassBibliographic() {
            return mock( Compass.class );
        }

        @Bean
        public Compass compassBiosequence() {
            return mock( Compass.class );
        }

        @Bean
        public Compass compassExperimentSet() {
            return mock( Compass.class );
        }

        @Bean
        public Compass compassExpression() {
            return mock( Compass.class );
        }

        @Bean
        public Compass compassGene() {
            return mock( Compass.class );
        }

        @Bean
        public Compass compassGeneSet() {
            return mock( Compass.class );
        }

        @Bean
        public Compass compassProbe() {
            return mock( Compass.class );
        }

        @Bean
        public BioSequenceService bioSequenceService() {
            return mock( BioSequenceService.class );
        }
    }

    @Autowired
    @Qualifier("compassSearchSource")
    private SearchSource searchSource;

    @Autowired
    @Qualifier("compassGene")
    private Compass compassGene;

    /* fixtures */
    CompassSettings mockedSettings;
    CompassSession mockedSession;
    CompassQueryBuilder mockedQueryBuilder;
    CompassQuery compassQuery;

    @Before
    public void setupCompassMocks() {
        mockedSettings = mock( CompassSettings.class );
        mockedSession = mock( InternalCompassSession.class );
        mockedQueryBuilder = mock( CompassQueryBuilder.class, RETURNS_SELF );
        compassQuery = mock( CompassQuery.class );
        CompassQueryBuilder.CompassQueryStringBuilder qs = mock( CompassQueryBuilder.CompassQueryStringBuilder.class, RETURNS_SELF );
        when( compassGene.openSession() ).thenReturn( mockedSession );
        when( compassGene.getSettings() ).thenReturn( mockedSettings );
        when( mockedSession.getSettings() ).thenReturn( mockedSettings );
        when( mockedSession.beginTransaction() ).thenReturn( mock( CompassTransaction.class ) );
        when( mockedSession.queryBuilder() ).thenReturn( mockedQueryBuilder );
        when( qs.toQuery() ).thenReturn( compassQuery );
        when( compassQuery.hits() ).thenReturn( mock( CompassHits.class ) );
        when( mockedQueryBuilder.queryString( any() ) ).thenReturn( qs );
    }

    @After
    public void tearDown() {
        reset( compassGene );
    }

    @Test
    public void test() throws SearchException {
        searchSource.searchGene( SearchSettings.geneSearch( "BRCA1", null ) );
        verify( compassGene ).openSession();
        verify( mockedSession ).beginTransaction();
        verify( mockedQueryBuilder ).queryString( "BRCA1" );
    }

    @Test
    public void test_quotedTerm() throws SearchException {
        searchSource.searchGene( SearchSettings.geneSearch( "\"collections of materials\"", null ) );
        verify( compassGene ).openSession();
        verify( mockedSession ).beginTransaction();
        verify( mockedQueryBuilder ).queryString( "\"collections of materials\"" );
    }

    @Test
    public void test_multipleQuotedTerms() throws SearchException {
        searchSource.searchGene( SearchSettings.geneSearch( "\"collections of materials\" \"lung cancer\"", null ) );
        verify( compassGene ).openSession();
        verify( mockedSession ).beginTransaction();
        verify( mockedQueryBuilder ).queryString( "\"collections of materials\" \"lung cancer\"" );
    }

    @Test
    public void test_multipleQuotedTermsWithOr() throws SearchException {
        searchSource.searchGene( SearchSettings.geneSearch( "\"collections of materials\" OR \"lung cancer\"", null ) );
        verify( compassGene ).openSession();
        verify( mockedSession ).beginTransaction();
        verify( mockedQueryBuilder ).queryString( "\"collections of materials\" OR \"lung cancer\"" );
    }

    @Test
    public void test_whenQueryIsEmpty_thenReturnNothing() throws SearchException {
        searchSource.searchGene( SearchSettings.geneSearch( "", null ) );
        verifyNoInteractions( mockedQueryBuilder );
    }

    @Test
    public void test_whenQueryOnlyContainsAWildCard_thenReturnNothing() throws SearchException {
        searchSource.searchGene( SearchSettings.geneSearch( "*", null ) );
        verifyNoInteractions( mockedQueryBuilder );
    }

    @Test
    public void test_whenQueryOnlyContainsNonWordCharacters_thenReturnNothing() throws SearchException {
        searchSource.searchGene( SearchSettings.geneSearch( "\"*\" * \"*\"", null ) );
        verifyNoInteractions( mockedQueryBuilder );
    }

    @Test
    public void luceneQueryParser_whenTermIsQuoted_thenPreserveQuoting() throws ParseException {
        assertThat( new QueryParser( Version.LUCENE_36, "test", new EnglishAnalyzer( Version.LUCENE_36 ) )
                .parse( "\"collection of materials\"" ).toString() ).isEqualTo( "test:\"collect ? materi\"" );
    }
}