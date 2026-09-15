package ubic.gemma.core.ontology;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicReadService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * The correction path writes to production annotations, so what is pinned here is mostly what it must NOT do:
 * not touch a term a curator has to decide, not sweep up a deferred term, not write anything on a dry run, and
 * not lose a curator's existing supportingEvidence.
 */
public class ObsoleteTermCorrectionServiceTest {

    private static final String OBSOLETE = "http://www.ebi.ac.uk/efo/EFO_0000546";
    private static final String REPLACEMENT = "http://purl.obolibrary.org/obo/MONDO_0021178";
    private static final String DEFERRED = "http://www.ebi.ac.uk/efo/EFO_0000408";

    private ObsoleteTermCorrectionServiceImpl service;
    private OntologyService ontologyService;
    private CharacteristicReadService characteristicReadService;
    private TableMaintenanceUtil tableMaintenanceUtil;
    private AuditTrailService auditTrailService;
    private ExpressionExperimentService expressionExperimentService;

    @BeforeEach
    public void setUp() {
        service = new ObsoleteTermCorrectionServiceImpl();
        ontologyService = mock( OntologyService.class );
        characteristicReadService = mock( CharacteristicReadService.class );
        tableMaintenanceUtil = mock( TableMaintenanceUtil.class );
        auditTrailService = mock( AuditTrailService.class );
        expressionExperimentService = mock( ExpressionExperimentService.class );
        setField( service, "ontologyService", ontologyService );
        setField( service, "characteristicReadService", characteristicReadService );
        setField( service, "tableMaintenanceUtil", tableMaintenanceUtil );
        setField( service, "auditTrailService", auditTrailService );
        setField( service, "expressionExperimentService", expressionExperimentService );
    }

    private ObsoleteTermUsage usage( String uri, boolean autoCorrectable ) {
        ObsoleteTermUsage u = new ObsoleteTermUsage();
        u.setUri( uri );
        u.setStoredValue( "injury" );
        u.setAutoCorrectable( autoCorrectable );
        if ( autoCorrectable ) {
            u.setReplacedByUri( REPLACEMENT );
            u.setReplacedByLabel( "injury" );
            u.setResolvedVia( "IAO:0100001" );
        } else {
            u.setBlockedReason( "a curator has to choose" );
        }
        return u;
    }

    @Test
    public void testDryRunWritesNothing() throws TimeoutException {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setValue( "injury" );
        c.setValueUri( OBSOLETE );
        when( ontologyService.findObsoleteTermsInUse( anyLong(), any() ) )
                .thenReturn( Collections.singletonList( usage( OBSOLETE, true ) ) );
        when( characteristicReadService.findByUriInAnySlot( OBSOLETE ) ).thenReturn( Collections.singletonList( c ) );
        when( characteristicReadService.findExperimentIdsByUriInAnySlot( OBSOLETE ) )
                .thenReturn( Collections.singletonList( 42L ) );

        ObsoleteTermCorrectionResult r = service.apply( Collections.emptyList(), true, 5, TimeUnit.SECONDS );

        assertTrue( r.isDryRun() );
        assertEquals( 1, r.getCharacteristicsRewritten(), "the dry run must still report what it would do" );
        assertEquals( OBSOLETE, c.getValueUri(), "nothing may be written on a dry run" );
        assertNull( c.getSupportingEvidence() );
        assertNull( r.getResync(), "no resync on a dry run" );
        verifyNoInteractions( tableMaintenanceUtil, auditTrailService );
    }

    @Test
    public void testTermNeedingAJudgementIsNeverTouched() throws TimeoutException {
        when( ontologyService.findObsoleteTermsInUse( anyLong(), any() ) )
                .thenReturn( Collections.singletonList( usage( OBSOLETE, false ) ) );

        ObsoleteTermCorrectionResult r = service.apply( Collections.emptyList(), false, 5, TimeUnit.SECONDS );

        assertTrue( r.getTerms().isEmpty() );
        assertEquals( 0, r.getCharacteristicsRewritten() );
        verify( characteristicReadService, never() ).findByUriInAnySlot( any() );
    }

    @Test
    public void testDeferredTermIsSkippedByABlanketRun() throws TimeoutException {
        ObsoleteTermUsage deferred = usage( DEFERRED, true );
        when( ontologyService.findObsoleteTermsInUse( anyLong(), any() ) )
                .thenReturn( Collections.singletonList( deferred ) );

        ObsoleteTermCorrectionResult r = service.apply( Collections.emptyList(), false, 5, TimeUnit.SECONDS );

        assertTrue( r.getTerms().isEmpty(), "a run over everything must not sweep up a deferred term" );
        assertEquals( Collections.singletonList( DEFERRED ), r.getSkippedDeferred() );
        verify( characteristicReadService, never() ).findByUriInAnySlot( any() );
    }

    @Test
    public void testDeferredTermIsActedOnWhenNamedExplicitly() throws TimeoutException {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setValueUri( DEFERRED );
        when( ontologyService.findObsoleteTermsInUse( anyLong(), any() ) )
                .thenReturn( Collections.singletonList( usage( DEFERRED, true ) ) );
        when( characteristicReadService.findByUriInAnySlot( DEFERRED ) ).thenReturn( Collections.singletonList( c ) );
        when( characteristicReadService.findExperimentIdsByUriInAnySlot( DEFERRED ) )
                .thenReturn( Collections.emptyList() );

        ObsoleteTermCorrectionResult r = service.apply( Collections.singletonList( DEFERRED ), true, 5, TimeUnit.SECONDS );

        assertEquals( 1, r.getTerms().size(), "naming a deferred term explicitly is the deliberate override" );
        assertTrue( r.getSkippedDeferred().isEmpty() );
    }

    @Test
    public void testEverySlotIsRewrittenNotJustTheSubject() throws TimeoutException {
        Statement s = new Statement();
        s.setSubject( "injury" );
        s.setValueUri( OBSOLETE );
        s.setObjectUri( OBSOLETE );
        s.setSecondObjectUri( OBSOLETE );
        s.setPredicateUri( OBSOLETE );
        Characteristic cat = Characteristic.Factory.newInstance();
        cat.setCategoryUri( OBSOLETE );

        when( ontologyService.findObsoleteTermsInUse( anyLong(), any() ) )
                .thenReturn( Collections.singletonList( usage( OBSOLETE, true ) ) );
        when( characteristicReadService.findByUriInAnySlot( OBSOLETE ) ).thenReturn( Arrays.asList( s, cat ) );
        when( characteristicReadService.findExperimentIdsByUriInAnySlot( OBSOLETE ) )
                .thenReturn( Collections.emptyList() );

        ObsoleteTermCorrectionResult r = service.apply( Collections.emptyList(), false, 5, TimeUnit.SECONDS );

        assertEquals( REPLACEMENT, s.getValueUri() );
        assertEquals( REPLACEMENT, s.getObjectUri() );
        assertEquals( REPLACEMENT, s.getSecondObjectUri() );
        assertEquals( REPLACEMENT, s.getPredicateUri() );
        assertEquals( REPLACEMENT, cat.getCategoryUri(), "the category slot holds terms too" );
        ObsoleteTermCorrectionResult.TermCorrection tc = r.getTerms().get( 0 );
        assertEquals( 1, tc.getInValue() );
        assertEquals( 2, tc.getInObject() );
        assertEquals( 1, tc.getInPredicate() );
        assertEquals( 1, tc.getInCategory() );
    }

    @Test
    public void testProvenanceRecordsWhatAssertedTheReplacement() throws TimeoutException {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setValueUri( OBSOLETE );
        when( ontologyService.findObsoleteTermsInUse( anyLong(), any() ) )
                .thenReturn( Collections.singletonList( usage( OBSOLETE, true ) ) );
        when( characteristicReadService.findByUriInAnySlot( OBSOLETE ) ).thenReturn( Collections.singletonList( c ) );
        when( characteristicReadService.findExperimentIdsByUriInAnySlot( OBSOLETE ) ).thenReturn( Collections.emptyList() );

        service.apply( Collections.emptyList(), false, 5, TimeUnit.SECONDS );

        String ev = c.getSupportingEvidence();
        assertNotNull( ev );
        assertTrue( ev.contains( "obsoleteTermCorrection" ) );
        assertTrue( ev.contains( "\"assertedBy\":\"IAO:0100001\"" ),
                "without assertedBy this is indistinguishable from someone retyping the annotation: " + ev );
        assertTrue( ev.contains( REPLACEMENT ) );
    }

    @Test
    public void testExistingCuratorEvidenceSurvives() throws TimeoutException {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setValueUri( OBSOLETE );
        c.setSupportingEvidence( "{\"curatorNote\":\"checked against the paper\"}" );
        when( ontologyService.findObsoleteTermsInUse( anyLong(), any() ) )
                .thenReturn( Collections.singletonList( usage( OBSOLETE, true ) ) );
        when( characteristicReadService.findByUriInAnySlot( OBSOLETE ) ).thenReturn( Collections.singletonList( c ) );
        when( characteristicReadService.findExperimentIdsByUriInAnySlot( OBSOLETE ) ).thenReturn( Collections.emptyList() );

        service.apply( Collections.emptyList(), false, 5, TimeUnit.SECONDS );

        assertTrue( c.getSupportingEvidence().contains( "curatorNote" ),
                "the field is shared; a correction must not overwrite curation: " + c.getSupportingEvidence() );
        assertTrue( c.getSupportingEvidence().contains( "obsoleteTermCorrection" ) );
    }

    @Test
    public void testUnparseableEvidenceIsPreservedRatherThanDiscarded() throws TimeoutException {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setValueUri( OBSOLETE );
        c.setSupportingEvidence( "not json at all" );
        when( ontologyService.findObsoleteTermsInUse( anyLong(), any() ) )
                .thenReturn( Collections.singletonList( usage( OBSOLETE, true ) ) );
        when( characteristicReadService.findByUriInAnySlot( OBSOLETE ) ).thenReturn( Collections.singletonList( c ) );
        when( characteristicReadService.findExperimentIdsByUriInAnySlot( OBSOLETE ) ).thenReturn( Collections.emptyList() );

        service.apply( Collections.emptyList(), false, 5, TimeUnit.SECONDS );

        assertTrue( c.getSupportingEvidence().contains( "not json at all" ),
                "content we cannot parse still belongs to someone: " + c.getSupportingEvidence() );
    }
}
