package ubic.gemma.apps;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.persistence.util.EntityUrlBuilder;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.entrez.pubmed.ExpressionExperimentBibRefFinder;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.description.PublicationAssociation;
import ubic.gemma.model.common.description.PublicationAssociationSource;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.common.description.PublicationAssociationService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * The two rules that must not regress: the command refuses to run without being told which half to
 * run, and a curator's assertion is never written over.
 */
@ContextConfiguration
public class VerifyPublicationEvidenceCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {
        @Bean
        public VerifyPublicationEvidenceCli verifyPublicationEvidenceCli() {
            return new VerifyPublicationEvidenceCli();
        }

        @Bean
        public ExpressionExperimentService eeService() {
            return mock();
        }

        @Bean
        public BibliographicReferenceService bibliographicReferenceService() {
            return mock();
        }

        @Bean
        public PublicationAssociationService publicationAssociationService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock();
        }

        @Bean
        public SearchService searchService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "http://localhost:8080" );
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }

        @Bean
        public CLIAuthenticationManager cliAuthenticationManager() {
            return mock();
        }
    }

    @Autowired
    private VerifyPublicationEvidenceCli cli;
    @Autowired
    private EntityLocator entityLocator;
    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private PublicationAssociationService publicationAssociationService;

    /**
     * 🛑 There is no default mode, deliberately. One half reads GEO and restates a basis; the other
     * creates publication links that a live Gemma 1.32.x displays the moment they are written. A
     * default would make the second one reachable by someone who meant the first.
     */
    @Test
    @WithMockUser
    public void testItRefusesToRunWithoutBeingToldWhichHalf() {
        assertThat( cli )
                .withArguments( "-e", "GSE123" )
                .fails()
                .standardError()
                .asString( java.nio.charset.StandardCharsets.UTF_8 )
                .contains( "--verify" )
                .contains( "--fill" );
    }

    /**
     * A curator outranks GEO, and {@code apply()} declines such a write in silence — so a run that
     * reports what it changed must not attempt one and assume it landed. The held assertion is read
     * first and a curator-held row is left alone.
     */
    @Test
    @WithMockUser
    public void testACuratorsAssertionIsNotWrittenOver() throws Exception {
        ExpressionExperiment ee = geoExperiment();
        when( entityLocator.locateExpressionExperiment( eq( "GSE123" ), anyBoolean() ) ).thenReturn( ee );
        when( eeService.thawLite( ee ) ).thenReturn( ee );

        // 🛑 IIA, not TAS. With TAS the row would be skipped as already-verified and the test would
        // pass with the curator check deleted -- proving nothing. IIA is a row this command WOULD
        // promote, so the only thing standing between it and a write is who asserted it.
        PublicationAssociation held = new PublicationAssociation();
        held.setSource( PublicationAssociationSource.CURATOR );
        held.setEvidenceCode( GOEvidenceCode.IIA );
        when( publicationAssociationService.find( any(), any() ) ).thenReturn( held );

        // GEO agrees with Gemma, so agreement is not what stops the write either
        ExpressionExperimentBibRefFinder finder = mock( ExpressionExperimentBibRefFinder.class );
        when( finder.locatePubMedIds( "GSE123" ) ).thenReturn( java.util.Collections.singletonList( 38064339 ) );
        cli.setFinder( finder );

        assertThat( cli ).withArguments( "-e", "GSE123", "--verify", "--paceMillis", "0" ).succeeds();

        verify( publicationAssociationService, never() ).assertAccepted( any(), any(), any() );
        verify( eeService, never() ).updatePublications( any(), any(), any(), any() );
    }

    /**
     * 🛑 Refused while it is still an argument. The change log is opened after the experiment list is
     * built, and building it loads the corpus — so a bad path used to cost a full scan (25,687
     * datasets on prod) before failing, and failed as a NoSuchFileException naming the FILE when the
     * missing thing was its DIRECTORY. Reported from a real run 2026-08-18.
     *
     * <p>What this pins is the failure and its message. That it happens before the corpus loads is a
     * consequence of the check living in {@code processExperimentOptions}, which runs during argument
     * parsing — asserting it here through mock interactions is not possible, because the beans in this
     * context are shared across tests and carry the other tests' calls.</p>
     */
    @Test
    @WithMockUser
    public void testABadChangeLogPathIsRefusedBeforeAnythingIsLoaded() {
        assertThat( cli )
                .withArguments( "-e", "GSE123", "--verify",
                        "--changeLog", "/no/such/directory/anywhere/out.tsv" )
                .fails()
                .standardError()
                .asString( java.nio.charset.StandardCharsets.UTF_8 )
                .contains( "does not exist" )
                .contains( "/no/such/directory/anywhere" );
    }

    /**
     * 🛑 GEO listing several papers is not GEO disagreeing.
     *
     * <p>GSE934 lists {@code 15802019} and {@code 15867358} — two 2005 papers from the same lab — and
     * Gemma holds the second. That is not a disagreement: GEO does link the paper to the series. Nor
     * is it agreement on the primary. It gets its own outcome and no write.</p>
     */
    @Test
    @WithMockUser
    public void testAPaperGeoListsSecondIsReportedNotCertified() throws Exception {
        ExpressionExperiment ee = geoExperiment();
        ee.getPrimaryPublication().getPubAccession().setAccession( "15867358" );
        when( entityLocator.locateExpressionExperiment( eq( "GSE123" ), anyBoolean() ) ).thenReturn( ee );
        when( eeService.thawLite( ee ) ).thenReturn( ee );

        PublicationAssociation held = new PublicationAssociation();
        held.setSource( PublicationAssociationSource.GEO_SUBMITTER_LINK );
        held.setEvidenceCode( GOEvidenceCode.IIA );
        when( publicationAssociationService.find( any(), any() ) ).thenReturn( held );

        ExpressionExperimentBibRefFinder finder = mock( ExpressionExperimentBibRefFinder.class );
        when( finder.locatePubMedIds( "GSE123" ) )
                .thenReturn( java.util.Arrays.asList( 15802019, 15867358 ) );
        cli.setFinder( finder );

        assertThat( cli ).withArguments( "-e", "GSE123", "--verify", "--paceMillis", "0" ).succeeds();

        // 🛑 NOT promoted. GEO does list this paper, so it is not a mismatch -- but TAS has to mean
        // Gemma and GEO agree on the PRIMARY, and they do not. GSE227854 is why: GEO's own list can
        // contain a paper that is wrong for the dataset (the submitter cross-linked one of their own
        // two NAR papers), and comparing Gemma to GEO cannot detect that. Certifying a partial
        // agreement would close the one case that most needs a person.
        verify( publicationAssociationService, never() ).assertAccepted( any(), any(), any() );
    }

    /**
     * A paper GEO does not link to the series at all IS the disagreement, and nothing is written for
     * it: it splits into "a curator corrected GEO" and "GEO is wrong", which no rule separates.
     */
    @Test
    @WithMockUser
    public void testAPaperGeoDoesNotListAtAllIsNotWritten() throws Exception {
        ExpressionExperiment ee = geoExperiment();
        ee.getPrimaryPublication().getPubAccession().setAccession( "99999999" );
        when( entityLocator.locateExpressionExperiment( eq( "GSE123" ), anyBoolean() ) ).thenReturn( ee );
        when( eeService.thawLite( ee ) ).thenReturn( ee );

        PublicationAssociation held = new PublicationAssociation();
        held.setSource( PublicationAssociationSource.GEO_SUBMITTER_LINK );
        held.setEvidenceCode( GOEvidenceCode.IIA );
        when( publicationAssociationService.find( any(), any() ) ).thenReturn( held );

        ExpressionExperimentBibRefFinder finder = mock( ExpressionExperimentBibRefFinder.class );
        when( finder.locatePubMedIds( "GSE123" ) )
                .thenReturn( java.util.Arrays.asList( 15802019, 15867358 ) );
        cli.setFinder( finder );

        assertThat( cli ).withArguments( "-e", "GSE123", "--verify", "--paceMillis", "0" ).succeeds();

        verify( publicationAssociationService, never() ).assertAccepted( any(), any(), any() );
    }

    private ExpressionExperiment geoExperiment() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE123" );
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( ExternalDatabases.GEO );
        DatabaseEntry acc = new DatabaseEntry();
        acc.setAccession( "GSE123" );
        acc.setExternalDatabase( geo );
        ee.setAccession( acc );
        BibliographicReference primary = new BibliographicReference();
        DatabaseEntry pm = new DatabaseEntry();
        pm.setAccession( "38064339" );
        primary.setPubAccession( pm );
        ee.setPrimaryPublication( primary );
        return ee;
    }
}
