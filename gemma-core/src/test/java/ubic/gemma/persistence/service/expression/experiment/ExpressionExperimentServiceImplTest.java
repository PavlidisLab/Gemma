/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.core.security.SecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.DesignPreflightReport;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * @author daq2101
 * @author paul
 * @author poirigui
 */
@ContextConfiguration
public class ExpressionExperimentServiceImplTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class ExpressionExperimentServiceImplTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao() {
            return mock( ExpressionExperimentDao.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService( ExpressionExperimentDao expressionExperimentDao ) {
            return new ExpressionExperimentServiceImpl( expressionExperimentDao );
        }

        @Bean
        public ExpressionExperimentReadService expressionExperimentReadService() {
            return mock( ExpressionExperimentReadService.class );
        }

        @Bean
        public ExpressionExperimentWriteService expressionExperimentWriteService() {
            return mock( ExpressionExperimentWriteService.class );
        }

        @Bean
        public ExpressionExperimentSubSetReadService expressionExperimentSubSetReadService() {
            return mock( ExpressionExperimentSubSetReadService.class );
        }

        @Bean
        public ExpressionExperimentDataVectorService expressionExperimentDataVectorService(
                ExpressionExperimentDao expressionExperimentDao,
                BioAssayDimensionService bioAssayDimensionService,
                QuantitationTypeService quantitationTypeService ) {
            return new ExpressionExperimentDataVectorServiceImpl( expressionExperimentDao,
                    bioAssayDimensionService, quantitationTypeService );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public BioAssayDimensionService bioAssayDimensionService() {
            return mock( BioAssayDimensionService.class );
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock( DifferentialExpressionAnalysisService.class );
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock( ExpressionExperimentSetService.class );
        }

        @Bean
        public ExpressionExperimentFilterRewriteHelperService expressionExperimentFilterInferenceHelperService() {
            return mock( ExpressionExperimentFilterRewriteHelperService.class );
        }

        @Bean
        public ExpressionExperimentSubSetService expressionExperimentSubSetService() {
            return mock( ExpressionExperimentSubSetService.class );
        }

        @Bean
        public ExperimentalFactorService experimentalFactorService() {
            return mock( ExperimentalFactorService.class );
        }

        @Bean
        public ExperimentalDesignService experimentalDesignService() {
            return mock( ExperimentalDesignService.class );
        }

        @Bean
        public FactorValueService factorValueService() {
            return mock( FactorValueService.class );
        }

        @Bean
        public OntologyService ontologyService() {
            return mock( OntologyService.class );
        }

        @Bean
        public PrincipalComponentAnalysisService principalComponentAnalysisService() {
            return mock( PrincipalComponentAnalysisService.class );
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock( QuantitationTypeService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public SecurityService securityService() {
            return mock( SecurityService.class );
        }

        @Bean
        public SVDService svdService() {
            return mock( SVDService.class );
        }

        @Bean
        public BioMaterialService bioMaterialService() {
            return mock();
        }

        @Bean
        public SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService() {
            return mock( SampleCoexpressionAnalysisService.class );
        }

        @Bean
        public BlacklistedEntityService blacklistedEntityService() {
            return mock( BlacklistedEntityService.class );
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock( AccessDecisionManager.class );
        }

        @Bean
        public CharacteristicService characteristicService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentService svc;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ExpressionExperimentDao eeDao;

    @Autowired
    private DifferentialExpressionAnalysisService deaService;

    @BeforeEach
    public void setupMocks() {
        when( eeDao.getElementClass() ).thenAnswer( a -> ExpressionExperiment.class );
    }

    @AfterEach
    public void resetMocks() {
        reset( bioAssayDimensionService, quantitationTypeService, eeDao );
    }

    @Test
    public void testExpressionExperimentFindAll() {

        svc = new ExpressionExperimentServiceImpl( eeDao );

        User nobody = User.Factory.newInstance( "foo" );

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );
        ee.setOwner( nobody );

        ArrayDesign ad1 = ArrayDesign.Factory.newInstance();
        ad1.setShortName( "foo" );
        ArrayDesign ad2 = ArrayDesign.Factory.newInstance();
        ad2.setShortName( "bar" );

        for ( long i = 0; i < 10; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setId( i + 1 );
            if ( i % 2 == 0 ) {
                ba.setArrayDesignUsed( ad1 );
            } else {
                ba.setArrayDesignUsed( ad2 );
            }
            ee.getBioAssays().add( ba );
        }

        Collection<ExpressionExperiment> c = new HashSet<>();
        ExpressionExperiment numberTwelve = ExpressionExperiment.Factory.newInstance();
        numberTwelve.setId( 12L );

        c.add( numberTwelve );
        c.add( ExpressionExperiment.Factory.newInstance() );
        c.add( ExpressionExperiment.Factory.newInstance() );

        Collection<ExpressionExperiment> cJustTwelve = new HashSet<>();
        cJustTwelve.add( numberTwelve );

        when( eeDao.loadAll() ).thenReturn( c );
        assertThat( svc.loadAll() ).isEqualTo( c );
        verify( eeDao ).loadAll();
    }

    @Test
    public void testReplaceAllRawDataVectors() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( eeDao.load( 1L ) ).thenReturn( ee );
        QuantitationType qt = new QuantitationType();
        qt.setIsPreferred( true );
        BioAssayDimension bad = new BioAssayDimension();
        bad.setBioAssays( Collections.singletonList( new BioAssay() ) );
        ArrayDesign ad = new ArrayDesign();
        when( bioAssayDimensionService.findOrCreate( bad ) ).thenReturn( bad );
        when( quantitationTypeService.create( qt, RawExpressionDataVector.class ) ).thenReturn( qt );
        Set<RawExpressionDataVector> vectors = createRawVectors( ee, qt, bad, ad );
        svc.replaceAllRawDataVectors( ee, vectors );
        verify( bioAssayDimensionService ).findOrCreate( bad );
        verify( quantitationTypeService ).create( qt, RawExpressionDataVector.class );
        verify( eeDao ).addRawDataVectors( ee, qt, vectors );
    }

    @Test
    public void testReplaceAllRawDataVectorsWithMoreThanOnePreferredQt() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( eeDao.load( 1L ) ).thenReturn( ee );
        QuantitationType qt1 = new QuantitationType();
        qt1.setName( "qt1" );
        qt1.setIsPreferred( true );
        QuantitationType qt2 = new QuantitationType();
        qt2.setName( "qt2" );
        qt2.setIsPreferred( true );
        BioAssayDimension bad = new BioAssayDimension();
        ArrayDesign ad = new ArrayDesign();
        Set<RawExpressionDataVector> vectors = new HashSet<>();
        vectors.addAll( createRawVectors( ee, qt1, bad, ad ) );
        vectors.addAll( createRawVectors( ee, qt2, bad, ad ) );
        assertThatThrownBy( () -> svc.replaceAllRawDataVectors( ee, vectors ) )
                .isInstanceOf( IllegalArgumentException.class );
        verifyNoInteractions( eeDao );
    }

    private Set<RawExpressionDataVector> createRawVectors( ExpressionExperiment ee, QuantitationType qt, BioAssayDimension bad, ArrayDesign ad ) {
        Set<RawExpressionDataVector> vectors = new HashSet<>();
        for ( int i = 0; i < 10; i++ ) {
            RawExpressionDataVector v = new RawExpressionDataVector();
            v.setExpressionExperiment( ee );
            v.setQuantitationType( qt );
            v.setBioAssayDimension( bad );
            CompositeSequence cs = new CompositeSequence();
            cs.setName( "cs" + i );
            cs.setArrayDesign( ad );
            v.setDesignElement( cs );
            vectors.add( v );
        }
        return vectors;
    }

    // ============================================================================================
    // previewDesignChange() tests
    //
    // Fixture: one experiment with one categorical factor "treatment" (id=10) containing two FVs
    // (id=100 "control", id=101 "treated"), each with one Statement. Two biomaterials (id=1000 and
    // id=1001) each assigned to one of the factor values, surfaced via BioAssays. The mocked DAO
    // returns this fixture from reload(). Hibernate.initialize() calls inside the service are
    // no-ops on already-populated Java collections.
    // ============================================================================================

    private ExpressionExperiment fixture;
    private ExperimentalFactor treatmentFactor;
    private FactorValue controlFv;
    private FactorValue treatedFv;
    private BioMaterial bm1000;
    private BioMaterial bm1001;

    private void buildFixture() {
        // @After in this class only resets a subset of mocks; reset the ones we touch here to keep
        // stubs from leaking between preflight tests.
        reset( eeDao, deaService );
        when( eeDao.getElementClass() ).thenAnswer( a -> ExpressionExperiment.class );

        fixture = new ExpressionExperiment();
        fixture.setId( 1L );
        fixture.setShortName( "GSE0001" );

        ExperimentalDesign ed = new ExperimentalDesign();
        ed.setId( 5L );
        fixture.setExperimentalDesign( ed );

        treatmentFactor = new ExperimentalFactor();
        treatmentFactor.setId( 10L );
        treatmentFactor.setName( "treatment" );
        treatmentFactor.setType( FactorType.CATEGORICAL );
        ed.getExperimentalFactors().add( treatmentFactor );

        controlFv = makeFv( 100L, treatmentFactor, 1000L, "treatment", "control" );
        treatedFv = makeFv( 101L, treatmentFactor, 1001L, "treatment", "treated" );

        bm1000 = makeBm( 1000L, "sample-A", controlFv );
        bm1001 = makeBm( 1001L, "sample-B", treatedFv );

        BioAssay ba1 = BioAssay.Factory.newInstance();
        ba1.setId( 200L );
        ba1.setSampleUsed( bm1000 );
        BioAssay ba2 = BioAssay.Factory.newInstance();
        ba2.setId( 201L );
        ba2.setSampleUsed( bm1001 );
        fixture.getBioAssays().add( ba1 );
        fixture.getBioAssays().add( ba2 );

        when( eeDao.reload( fixture ) ).thenReturn( fixture );
        when( eeDao.getSubSets( fixture ) ).thenReturn( Collections.emptyList() );
        when( deaService.findByFactor( treatmentFactor ) ).thenReturn( Collections.emptyList() );
        when( deaService.findByExperiment( fixture, true ) ).thenReturn( Collections.emptyList() );
    }

    private FactorValue makeFv( long id, ExperimentalFactor parent, long statementId, String category, String subject ) {
        FactorValue fv = new FactorValue();
        fv.setId( id );
        fv.setExperimentalFactor( parent );
        Statement s = new Statement();
        s.setId( statementId );
        s.setCategory( category );
        s.setSubject( subject );
        fv.getCharacteristics().add( s );
        parent.getFactorValues().add( fv );
        return fv;
    }

    private BioMaterial makeBm( long id, String name, FactorValue... assignedFvs ) {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setId( id );
        bm.setName( name );
        for ( FactorValue fv : assignedFvs ) {
            bm.getFactorValues().add( fv );
        }
        return bm;
    }

    /** Build a proposed design VO that mirrors the current fixture. Starting point for diff-test mutations. */
    private ExperimentalDesignValueObject mirrorProposal() {
        return new ExperimentalDesignValueObject( fixture.getExperimentalDesign(), fixture.getBioAssays() );
    }

    @Test
    public void testPreviewNoChange() {
        buildFixture();
        DesignPreflightReport report = svc.previewDesignChange( fixture, mirrorProposal() );
        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.getFactorsToDelete() ).isEmpty();
        assertThat( report.getFactorValuesToDelete() ).isEmpty();
        assertThat( report.getDifferentialExpressionAnalysesToDelete() ).isEmpty();
        assertThat( report.getSummary().getBiomaterialsWithChangedAssignments() ).isZero();
    }

    @Test
    public void testPreviewDeletingAFactorValueFlagsDependentAnalyses() {
        buildFixture();
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setId( 9001L );
        dea.setName( "treatment effect" );
        when( deaService.findByFactor( treatmentFactor ) ).thenReturn( Collections.singletonList( dea ) );

        // remove controlFv from the proposal, AND remove its id from bm1000's assignment
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().get( 0 ).getValues().removeIf( v -> v.getId().equals( 100L ) );
        proposal.getBioMaterialAssignments().stream()
                .filter( a -> a.getBioMaterialId().equals( 1000L ) )
                .forEach( a -> a.setFactorValueIds( new ArrayList<>() ) );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.getFactorValuesToDelete() ).hasSize( 1 );
        assertThat( report.getFactorValuesToDelete().get( 0 ).getId() ).isEqualTo( 100L );
        assertThat( report.getDifferentialExpressionAnalysesToDelete() ).hasSize( 1 );
        assertThat( report.getDifferentialExpressionAnalysesToDelete().get( 0 ).getId() ).isEqualTo( 9001L );
    }

    @Test
    public void testPreviewDeletingAFactorCascadesToItsValuesAndAnalyses() {
        buildFixture();
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setId( 9001L );
        when( deaService.findByFactor( treatmentFactor ) ).thenReturn( Collections.singletonList( dea ) );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().clear();
        proposal.getBioMaterialAssignments().forEach( a -> a.setFactorValueIds( new ArrayList<>() ) );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.getFactorsToDelete() ).hasSize( 1 );
        assertThat( report.getFactorValuesToDelete() ).hasSize( 2 );
        assertThat( report.getDifferentialExpressionAnalysesToDelete() ).hasSize( 1 );
    }

    @Test
    public void testPreviewReassigningBiomaterialFlagsAnalyses() {
        buildFixture();
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setId( 9001L );
        when( deaService.findByFactor( treatmentFactor ) ).thenReturn( Collections.singletonList( dea ) );

        // No FVs deleted, no factors changed; only bm1000 switches from controlFv (100) to treatedFv (101)
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getBioMaterialAssignments().stream()
                .filter( a -> a.getBioMaterialId().equals( 1000L ) )
                .forEach( a -> a.setFactorValueIds( Collections.singletonList( 101L ) ) );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.getFactorValuesToDelete() ).isEmpty();
        assertThat( report.getDifferentialExpressionAnalysesToDelete() ).hasSize( 1 );
        assertThat( report.getSummary().getBiomaterialsWithChangedAssignments() ).isEqualTo( 1 );
    }

    @Test
    public void testPreviewAddingNewFvFlagsAnalysesOnTheParentFactor() {
        buildFixture();
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setId( 9001L );
        when( deaService.findByFactor( treatmentFactor ) ).thenReturn( Collections.singletonList( dea ) );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        FactorValueBasicValueObject newFv = new FactorValueBasicValueObject();
        newFv.setId( null );
        newFv.setCharacteristics( Collections.emptyList() );
        newFv.setStatements( Collections.emptyList() );
        proposal.getExperimentalFactors().get( 0 ).getValues().add( newFv );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.getSummary().getFactorValuesToCreate() ).isEqualTo( 1 );
        assertThat( report.getDifferentialExpressionAnalysesToDelete() ).hasSize( 1 );
    }

    @Test
    public void testPreviewEditingStatementOnKeptFvDoesNotFlagAnalyses() {
        buildFixture();
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setId( 9001L );
        when( deaService.findByFactor( treatmentFactor ) ).thenReturn( Collections.singletonList( dea ) );

        // rename "control" -> "vehicle" on the kept FV; nothing structural changes
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().get( 0 ).getValues().stream()
                .filter( v -> v.getId().equals( 100L ) )
                .findFirst()
                .ifPresent( v -> v.getStatements().get( 0 ).setSubject( "vehicle" ) );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.getDifferentialExpressionAnalysesToDelete() ).isEmpty();
    }

    @Test
    public void testPreviewBlocksUnknownFactorId() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        ExperimentalDesignValueObject.ExperimentalFactorEntry bogus = new ExperimentalDesignValueObject.ExperimentalFactorEntry();
        bogus.setId( 99999L );
        bogus.setType( "categorical" );
        proposal.getExperimentalFactors().add( bogus );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).extracting( DesignPreflightReport.Blocker::getType )
                .contains( "UNKNOWN_FACTOR_ID" );
    }

    @Test
    public void testPreviewBlocksUnknownFactorValueId() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        FactorValueBasicValueObject bogus = new FactorValueBasicValueObject();
        bogus.setId( 88888L );
        bogus.setCharacteristics( Collections.emptyList() );
        bogus.setStatements( Collections.emptyList() );
        proposal.getExperimentalFactors().get( 0 ).getValues().add( bogus );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).extracting( DesignPreflightReport.Blocker::getType )
                .contains( "UNKNOWN_FACTOR_VALUE_ID" );
    }

    @Test
    public void testPreviewBlocksTypeChangeWhenFactorHasValues() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().get( 0 ).setType( "continuous" );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).extracting( DesignPreflightReport.Blocker::getType )
                .contains( "FACTOR_TYPE_CHANGE_WITH_VALUES" );
    }

    @Test
    public void testPreviewBlocksAssignmentReferencingUnknownFv() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getBioMaterialAssignments().stream()
                .filter( a -> a.getBioMaterialId().equals( 1000L ) )
                .forEach( a -> a.setFactorValueIds( new ArrayList<>( Arrays.asList( 100L, 99999L ) ) ) );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).extracting( DesignPreflightReport.Blocker::getType )
                .contains( "ASSIGNMENT_REFERENCES_UNKNOWN_FV" );
    }

    @Test
    public void testPreviewBlocksUnknownBiomaterial() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        ExperimentalDesignValueObject.BioMaterialFactorValueAssignment ghost =
                new ExperimentalDesignValueObject.BioMaterialFactorValueAssignment( 77777L, "ghost", Collections.singletonList( 100L ) );
        proposal.getBioMaterialAssignments().add( ghost );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).extracting( DesignPreflightReport.Blocker::getType )
                .contains( "UNKNOWN_BIOMATERIAL_ID" );
    }

    @Test
    public void testPreviewSubsetWithLostAnchorIsFlaggedButNotBlocked() {
        buildFixture();
        // subset whose single BM is bm1000, which is assigned to controlFv (id=100) — when we delete
        // controlFv, the subset's shared FV disappears.
        ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet ss =
                new ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet();
        ss.setId( 5000L );
        ss.setName( "controls" );
        BioAssay subsetBa = BioAssay.Factory.newInstance();
        subsetBa.setId( 300L );
        subsetBa.setSampleUsed( bm1000 );
        ss.getBioAssays().add( subsetBa );
        when( eeDao.getSubSets( fixture ) ).thenReturn( Collections.singletonList( ss ) );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().get( 0 ).getValues().removeIf( v -> v.getId().equals( 100L ) );
        proposal.getBioMaterialAssignments().stream()
                .filter( a -> a.getBioMaterialId().equals( 1000L ) )
                .forEach( a -> a.setFactorValueIds( new ArrayList<>() ) );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.getSubsetsWithStaleAnchor() ).hasSize( 1 );
        assertThat( report.getSubsetsWithStaleAnchor().get( 0 ).getLostFactorValueIds() ).containsExactly( 100L );
    }
}