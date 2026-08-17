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
import org.mockito.ArgumentCaptor;
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
import com.fasterxml.jackson.databind.JsonNode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.measurement.MeasurementValueObject;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.DesignApplyOutcome;
import ubic.gemma.model.expression.experiment.DesignPreflightReport;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.expression.experiment.StatementValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.common.measurement.UnitDao;
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
        public org.hibernate.SessionFactory sessionFactory() {
            return mock( org.hibernate.SessionFactory.class );
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

        @Bean
        public UnitDao unitDao() {
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

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private UnitDao unitDao;

    /**
     * The subset source {@code previewDesignChange} actually reads. It delegates through
     * {@code getSubSetsWithBioAssays}, NOT through {@code eeDao.getSubSets} — stubbing the latter leaves this
     * one returning an empty list, which is why the stale-anchor case looked undetectable and sat
     * {@code @Disabled} as a "pre-existing failure".
     */
    @Autowired
    private ExpressionExperimentSubSetReadService subSetReadService;

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
        reset( eeDao, deaService, subSetReadService );
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

    // ============================================================================================
    // applyDesignChange() tests
    //
    // Coverage focus: idempotent no-op + blocker rejection. The mutation path itself is covered by
    // the previewDesignChange tests above (apply re-runs preview as its gate). These tests live in
    // the AOP-less impl test context, so the @AuditedConditional annotation does not fire here;
    // aspect-level coverage lives in AuditedAspectTest.
    // ============================================================================================

    @Test
    public void testApplyNoChangeIsIdempotentNoOp() {
        buildFixture();
        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, mirrorProposal() );
        assertThat( outcome ).isNotNull();
        assertThat( outcome.isApplied() ).isFalse();
        assertThat( outcome.getPreflightAtApply() ).isNotNull();
        assertThat( outcome.getPreflightAtApply().getBlockers() ).isEmpty();
        // No-op branch must not write any structural mutation.
        verify( experimentalFactorService, never() ).remove( any( ExperimentalFactor.class ) );
        verify( factorValueService, never() ).remove( any( FactorValue.class ) );
        verify( bioMaterialService, never() ).update( anyCollection() );
    }

    @Test
    public void testApplyRejectsBlockerWithIllegalArgumentException() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        // type change while factor has values is a hard blocker
        proposal.getExperimentalFactors().get( 0 ).setType( "continuous" );
        assertThatThrownBy( () -> svc.applyDesignChange( fixture, proposal ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Cannot apply proposed design" );
    }

    /**
     * Setting only a factor value's baseline flag is a real change: it must NOT be short-circuited as a no-op, must
     * be written to the entity, and must be reflected in the rebuilt design returned by the apply. Regression guard
     * for the round-trip gap where isNoOpDesignApply ignored in-place kept-FV edits (baseline PUT accepted but the
     * flag never came back).
     */
    @Test
    public void testApplyBaselineOnlyChangeIsAppliedAndReflected() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposalFv( proposal, 100L ).setBaseline( true );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        // entity mutated (the persist half of the round-trip)
        assertThat( controlFv.getIsBaseline() ).isTrue();
        // read half: constructing the design VO from the mutated entity surfaces the flag (the symptom was
        // "PUT accepts isBaseline but it doesn't come back"). getExperimentalDesignValueObject delegates to a
        // read service that is mocked in this AOP-less context, so we exercise the VO construction directly.
        assertThat( designFv( new ExperimentalDesignValueObject(
                fixture.getExperimentalDesign(), fixture.getBioAssays() ), 100L ).getBaseline() ).isTrue();
    }

    /**
     * Designating a baseline clears any stale baseline on a sibling factor value, so at most one baseline survives
     * per factor even when the client leaves the previous baseline's flag untouched (null = no change).
     */
    @Test
    public void testApplyDesignatingBaselineClearsSibling() {
        buildFixture();
        controlFv.setIsBaseline( true ); // pre-existing baseline on FV 100
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposalFv( proposal, 100L ).setBaseline( null ); // client leaves the old baseline untouched
        proposalFv( proposal, 101L ).setBaseline( true ); // and designates a new one

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( treatedFv.getIsBaseline() ).isTrue();
        assertThat( controlFv.getIsBaseline() ).isFalse();
    }

    /**
     * Editing only a kept factor value's statement (no structural add/delete) is a real change: it must not be
     * short-circuited as a no-op, and the edit must reach the entity. Guards the statement half of the kept-FV
     * no-op gap.
     */
    @Test
    public void testApplyStatementOnlyEditIsAppliedAndReflected() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        // change the subject of FV 100's single statement
        proposalFv( proposal, 100L ).getStatements().get( 0 ).setSubject( "control-edited" );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( controlFv.getCharacteristics() )
                .anySatisfy( s -> assertThat( s.getSubject() ).isEqualTo( "control-edited" ) );
    }

    /** A factor cannot designate more than one baseline; previewDesignChange flags it and apply rejects it. */
    @Test
    public void testMultipleBaselinesInFactorIsBlocked() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposalFv( proposal, 100L ).setBaseline( true );
        proposalFv( proposal, 101L ).setBaseline( true );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );
        assertThat( report.getBlockers() )
                .anySatisfy( b -> assertThat( b.getType() ).isEqualTo( "MULTIPLE_BASELINES" ) );

        assertThatThrownBy( () -> svc.applyDesignChange( fixture, proposal ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    private static FactorValueBasicValueObject proposalFv( ExperimentalDesignValueObject vo, long fvId ) {
        return designFv( vo, fvId );
    }

    private static FactorValueBasicValueObject designFv( ExperimentalDesignValueObject vo, long fvId ) {
        return vo.getExperimentalFactors().stream()
                .flatMap( f -> f.getValues().stream() )
                .filter( v -> v.getId() != null && v.getId() == fvId )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "no factor value " + fvId + " in design VO" ) );
    }

    // ============================================================================================
    // Gold write-back acceptance cases (handoffs/CAB_TO_GEMBRO_2026_08_16_GOLD_WRITE_BACK_CASES.md).
    //
    // W-numbers are that document's case labels. These cover edits the curation side intends to send
    // that the tests above never reach: factor-level metadata edits, continuous-factor measurements,
    // and the two id-addressing cases where name- or label-keyed matching would fail silently.
    // ============================================================================================

    /**
     * W11 — a factor description-only edit is a real change. Nothing structural moves, so every preflight
     * counter stays at zero and {@code hasKeptFactorValueEdits} (which only inspects factor <em>values</em>)
     * sees nothing; the apply must still reach {@code updateFactorMetadata} rather than short-circuiting.
     */
    @Test
    public void testApplyFactorDescriptionOnlyEditIsNotANoOp() {
        buildFixture();
        treatmentFactor.setDescription( "WT, p53 heterozygous, p53 KO" );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().get( 0 ).setDescription( "Mycn oe, Myc oe" );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( treatmentFactor.getDescription() ).isEqualTo( "Mycn oe, Myc oe" );
        // a metadata-only edit must not disturb the factor values
        assertThat( treatmentFactor.getFactorValues() ).hasSize( 2 );
        verify( factorValueService, never() ).remove( any( FactorValue.class ) );
    }

    /**
     * W11 — same gap, reached through the factor name. A rename with no structural change is the other
     * metadata-only edit the curation side sends.
     */
    @Test
    public void testApplyFactorNameOnlyEditIsNotANoOp() {
        buildFixture();

        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().get( 0 ).setName( "treatment (corrected)" );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( treatmentFactor.getName() ).isEqualTo( "treatment (corrected)" );
    }

    /**
     * W11 / W1 — a factor category re-term with the factor values left in place. This is the edit that
     * changes what the factor <em>means</em> while every id survives, so dropping it silently is the worst
     * of the three: readers keep seeing the old category and nothing signals otherwise.
     */
    @Test
    public void testApplyFactorCategoryOnlyEditIsNotANoOp() {
        buildFixture();
        treatmentFactor.setCategory( Characteristic.Factory.newInstance( "cell line",
                "http://purl.obolibrary.org/obo/EFO_0000322", "cell line", "http://purl.obolibrary.org/obo/EFO_0000322" ) );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().get( 0 ).setCategory( new CharacteristicValueObject(
                "individual", "http://www.ebi.ac.uk/efo/EFO_0000542",
                "individual", "http://www.ebi.ac.uk/efo/EFO_0000542" ) );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( treatmentFactor.getCategory().getCategory() ).isEqualTo( "individual" );
        assertThat( treatmentFactor.getCategory().getCategoryUri() ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000542" );
    }

    /**
     * W14 — editing the measurement on a kept continuous factor value. {@code applyFactorValueChanges} updates
     * statements, the deprecated {@code value}, and the baseline flag on an existing factor value; the
     * measurement is the fourth field a continuous factor actually carries.
     */
    @Test
    public void testApplyMeasurementEditOnKeptFactorValueIsApplied() {
        buildContinuousFixture();

        ExperimentalDesignValueObject proposal = mirrorProposal();
        MeasurementValueObject edited = new MeasurementValueObject();
        edited.setValue( "37" );
        edited.setUnit( "day" );
        edited.setType( MeasurementType.ABSOLUTE.name() );
        edited.setRepresentation( PrimitiveType.DOUBLE.name() );
        designFv( proposal, 200L ).setMeasurementObject( edited );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( day7Fv.getMeasurement() ).isNotNull();
        assertThat( day7Fv.getMeasurement().getValue() ).isEqualTo( "37" );
    }

    /**
     * W14 — a newly created continuous factor value must keep its unit. {@code createFactorValue} copies the
     * measurement's value, representation and type; the unit is what makes "37" mean anything.
     */
    @Test
    public void testCreateFactorValueCarriesMeasurementUnit() {
        buildContinuousFixture();
        when( factorValueService.create( any( FactorValue.class ) ) ).thenAnswer( a -> {
            FactorValue created = a.getArgument( 0 );
            created.setId( 299L );
            return created;
        } );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        FactorValueBasicValueObject newFv = new FactorValueBasicValueObject();
        newFv.setStatements( Collections.emptyList() );
        newFv.setCharacteristics( Collections.emptyList() );
        MeasurementValueObject m = new MeasurementValueObject();
        m.setValue( "48" );
        m.setUnit( "day" );
        m.setType( MeasurementType.ABSOLUTE.name() );
        m.setRepresentation( PrimitiveType.DOUBLE.name() );
        newFv.setMeasurementObject( m );
        proposal.getExperimentalFactors().get( 0 ).getValues().add( newFv );

        svc.applyDesignChange( fixture, proposal );

        ArgumentCaptor<FactorValue> captor = ArgumentCaptor.forClass( FactorValue.class );
        verify( factorValueService ).create( captor.capture() );
        assertThat( captor.getValue().getMeasurement() ).isNotNull();
        assertThat( captor.getValue().getMeasurement().getValue() ).isEqualTo( "48" );
        assertThat( captor.getValue().getMeasurement().getUnit() ).isNotNull();
        assertThat( captor.getValue().getMeasurement().getUnit().getUnitNameCV() ).isEqualTo( "day" );
    }

    /**
     * W9 — two factors sharing both category and name. The write path must address them by id; any
     * (category, name) keying would edit whichever one it hit first. 20 of the curation side's 500
     * experiments carry such sibling pairs.
     */
    @Test
    public void testApplyAddressesSameCategorySameNameFactorsById() {
        buildFixture();
        // a second "treatment" factor, same name and category as the first
        ExperimentalFactor sibling = new ExperimentalFactor();
        sibling.setId( 11L );
        sibling.setName( "treatment" );
        sibling.setType( FactorType.CATEGORICAL );
        fixture.getExperimentalDesign().getExperimentalFactors().add( sibling );
        FactorValue siblingFv = makeFv( 102L, sibling, 1002L, "treatment", "estradiol" );
        bm1000.getFactorValues().add( siblingFv );
        when( deaService.findByFactor( sibling ) ).thenReturn( Collections.emptyList() );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        // edit the statement on the SIBLING's factor value only
        designFv( proposal, 102L ).getStatements().get( 0 ).setSubject( "estradiol, 10 nM" );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( siblingFv.getCharacteristics() )
                .anySatisfy( s -> assertThat( s.getSubject() ).isEqualTo( "estradiol, 10 nM" ) );
        // the same-named sibling's values are untouched
        assertThat( controlFv.getCharacteristics() )
                .allSatisfy( s -> assertThat( s.getSubject() ).isEqualTo( "control" ) );
        assertThat( treatedFv.getCharacteristics() )
                .allSatisfy( s -> assertThat( s.getSubject() ).isEqualTo( "treated" ) );
    }

    /**
     * W10 — two factor values under one factor whose statements serialize identically (distinguished only by
     * a zygosity statement the curation side adds). Addressing one by id must change only that one; any
     * label-keyed match would edit both, or the wrong one, and would do it silently.
     */
    @Test
    public void testApplyAddressesIdenticalLabelFactorValuesById() {
        buildFixture();
        // two factor values whose single statement has identical content, different ids
        FactorValue trp53a = makeFv( 110L, treatmentFactor, 1010L, "genotype", "Trp53" );
        FactorValue trp53b = makeFv( 111L, treatmentFactor, 1011L, "genotype", "Trp53" );
        bm1000.getFactorValues().add( trp53a );
        bm1001.getFactorValues().add( trp53b );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        designFv( proposal, 110L ).getStatements().get( 0 ).setSubject( "Trp53 homozygous negative" );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( trp53a.getCharacteristics() )
                .allSatisfy( s -> assertThat( s.getSubject() ).isEqualTo( "Trp53 homozygous negative" ) );
        assertThat( trp53b.getCharacteristics() )
                .allSatisfy( s -> assertThat( s.getSubject() ).isEqualTo( "Trp53" ) );
    }

    /**
     * W8 — a factor value carrying deliberately ungrounded free text (JAX strain nomenclature, hybrid
     * backgrounds with no ontology term in existence) round-trips with the text intact. The apply path must
     * not coerce, drop, or auto-bind it to a nearest term.
     */
    @Test
    public void testApplyPreservesUngroundedFreeTextStatement() {
        buildFixture();

        ExperimentalDesignValueObject proposal = mirrorProposal();
        StatementValueObject s = designFv( proposal, 100L ).getStatements().get( 0 );
        s.setSubject( "129SvEv-Tac/C57BL/6" );
        s.setSubjectUri( null );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( controlFv.getCharacteristics() ).singleElement().satisfies( st -> {
            assertThat( st.getSubject() ).isEqualTo( "129SvEv-Tac/C57BL/6" );
            assertThat( st.getSubjectUri() ).isNull();
        } );
    }

    /**
     * W16 — non-ASCII in curated text survives the apply byte-exact. The curation side repaired unicode our
     * GEO import mangled in 68 experiments, so a write path that re-mangles undoes the repair.
     */
    @Test
    public void testApplyPreservesNonAsciiCuratedText() {
        buildFixture();
        String curated = "10 µM β-estradiol, 37 °C";

        ExperimentalDesignValueObject proposal = mirrorProposal();
        designFv( proposal, 100L ).getStatements().get( 0 ).setSubject( curated );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( controlFv.getCharacteristics() ).singleElement()
                .satisfies( st -> assertThat( st.getSubject() ).isEqualTo( curated ) );
    }

    /**
     * W3 — deleting a continuous factor takes its measurement-bearing factor values with it, and the
     * preflight says so before the caller commits.
     */
    @Test
    public void testPreviewDeletingContinuousFactorReportsItsMeasurementValues() {
        buildContinuousFixture();

        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().clear();
        proposal.getBioMaterialAssignments().forEach( a -> a.setFactorValueIds( new ArrayList<>() ) );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );

        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.getFactorsToDelete() ).hasSize( 1 );
        assertThat( report.getFactorsToDelete().get( 0 ).getId() ).isEqualTo( 20L );
        assertThat( report.getFactorValuesToDelete() ).hasSize( 1 );
        assertThat( report.getFactorValuesToDelete().get( 0 ).getId() ).isEqualTo( 200L );
    }

    /**
     * W13 — supporting evidence attached to a factor-value statement reaches the entity. The design section is
     * the bulk of what curation produces, and until now it was the one section that could not carry a
     * justification at all.
     */
    @Test
    public void testApplyWritesSupportingEvidenceOntoAStatement() {
        buildFixture();
        JsonNode evidence = CharacteristicUtils.parseSupportingEvidence(
                "[{\"quote\":\"organism part: stroma\",\"source\":\"characteristic\",\"location\":\"GSM1197956\"}]" );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        designFv( proposal, 100L ).getStatements().get( 0 ).setSupportingEvidence( evidence );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( controlFv.getCharacteristics() ).singleElement()
                .satisfies( s -> assertThat( s.getSupportingEvidence() ).contains( "GSM1197956" ) );
    }

    /**
     * Attaching evidence to an otherwise-unchanged statement leaves every content key identical, so the
     * structural summary and {@code statementsChanged} both see nothing. Without a dedicated check this is
     * swallowed as a no-op — the same defect class as the factor description-only edit above.
     */
    @Test
    public void testApplyEvidenceOnlyEditIsNotANoOp() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        // identical content, evidence added
        designFv( proposal, 100L ).getStatements().get( 0 ).setSupportingEvidence(
                CharacteristicUtils.parseSupportingEvidence( "[{\"quote\":\"only the evidence changed\"}]" ) );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isTrue();
        assertThat( controlFv.getCharacteristics() ).singleElement()
                .satisfies( s -> assertThat( s.getSupportingEvidence() ).contains( "only the evidence changed" ) );
    }

    /**
     * Evidence follows the {@code null = "no change"} convention the rest of the payload uses, so a client that
     * does not carry provenance cannot wipe provenance somebody else recorded. Re-sending such a statement stays
     * a no-op rather than becoming a silent erasure.
     */
    @Test
    public void testApplyDoesNotWipeExistingEvidenceWhenThePayloadOmitsIt() {
        buildFixture();
        controlFv.getCharacteristics().iterator().next().setSupportingEvidence( "[{\"quote\":\"recorded earlier\"}]" );

        ExperimentalDesignValueObject proposal = mirrorProposal();
        designFv( proposal, 100L ).getStatements().get( 0 ).setSupportingEvidence( null );

        DesignApplyOutcome outcome = svc.applyDesignChange( fixture, proposal );

        assertThat( outcome.isApplied() ).isFalse();
        assertThat( controlFv.getCharacteristics() ).singleElement()
                .satisfies( s -> assertThat( s.getSupportingEvidence() ).contains( "recorded earlier" ) );
    }

    /**
     * A stranded subset requires the same explicit consent as the analysis cascade. It is the more dangerous of
     * the two precisely because it survives the change: still there, still named, still listed, and now anchored
     * on factor values that were deleted out from under it.
     */
    @Test
    public void testPreviewSubsetWithLostAnchorRequiresForce() {
        buildFixture();
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( 7001L );
        subset.setName( "control arm" );
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setId( 200L );
        ba.setSampleUsed( bm1000 );
        subset.getBioAssays().add( ba );
        when( subSetReadService.getSubSetsWithBioAssays( fixture ) ).thenReturn( Collections.singletonList( subset ) );

        // drop the FV the subset is anchored on
        ExperimentalDesignValueObject proposal = mirrorProposal();
        proposal.getExperimentalFactors().get( 0 ).getValues().removeIf( v -> v.getId().equals( 100L ) );
        proposal.getBioMaterialAssignments().stream()
                .filter( a -> a.getBioMaterialId().equals( 1000L ) )
                .forEach( a -> a.setFactorValueIds( new ArrayList<>() ) );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );

        assertThat( report.getBlockers() ).as( "a stale anchor is a consent question, not a payload error" ).isEmpty();
        assertThat( report.getSubsetsWithStaleAnchor() ).hasSize( 1 );
        assertThat( report.getSubsetsWithStaleAnchor().get( 0 ).getLostFactorValueIds() ).containsExactly( 100L );
        assertThat( report.requiresForce() ).isTrue();
    }

    /** A change with no analyses to delete and no stranded subsets proceeds without consent. */
    @Test
    public void testPreviewWithoutConsequencesDoesNotRequireForce() {
        buildFixture();
        ExperimentalDesignValueObject proposal = mirrorProposal();
        designFv( proposal, 100L ).getStatements().get( 0 ).setSubject( "vehicle" );

        DesignPreflightReport report = svc.previewDesignChange( fixture, proposal );

        assertThat( report.getBlockers() ).isEmpty();
        assertThat( report.requiresForce() ).isFalse();
    }

    private ExperimentalFactor timepointFactor;
    private FactorValue day7Fv;

    /**
     * A one-factor CONTINUOUS fixture: factor 20 ("timepoint") with a single measurement-bearing factor value
     * 200 (7 days) on one sample. Continuous factors carry {@link ubic.gemma.model.common.measurement.Measurement}
     * objects that categorical ones don't, which is why they need their own fixture.
     */
    private void buildContinuousFixture() {
        reset( eeDao, deaService, unitDao );
        when( eeDao.getElementClass() ).thenAnswer( a -> ExpressionExperiment.class );
        // no persistent Unit row exists in this mocked context; create echoes what it was handed
        when( unitDao.find( any( Unit.class ) ) ).thenReturn( null );
        when( unitDao.create( any( Unit.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );

        fixture = new ExpressionExperiment();
        fixture.setId( 2L );
        fixture.setShortName( "GSE0002" );

        ExperimentalDesign ed = new ExperimentalDesign();
        ed.setId( 6L );
        fixture.setExperimentalDesign( ed );

        timepointFactor = new ExperimentalFactor();
        timepointFactor.setId( 20L );
        timepointFactor.setName( "timepoint" );
        timepointFactor.setType( FactorType.CONTINUOUS );
        ed.getExperimentalFactors().add( timepointFactor );

        day7Fv = new FactorValue();
        day7Fv.setId( 200L );
        day7Fv.setExperimentalFactor( timepointFactor );
        day7Fv.setMeasurement( Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, "7",
                PrimitiveType.DOUBLE ) );
        day7Fv.getMeasurement().setUnit( Unit.Factory.newInstance( "day" ) );
        timepointFactor.getFactorValues().add( day7Fv );

        BioMaterial bm = makeBm( 2000L, "sample-T", day7Fv );
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setId( 300L );
        ba.setSampleUsed( bm );
        fixture.getBioAssays().add( ba );

        when( eeDao.reload( fixture ) ).thenReturn( fixture );
        when( eeDao.getSubSets( fixture ) ).thenReturn( Collections.emptyList() );
        when( deaService.findByFactor( timepointFactor ) ).thenReturn( Collections.emptyList() );
        when( deaService.findByExperiment( fixture, true ) ).thenReturn( Collections.emptyList() );
    }

    // ============================================================================================
    // commitCuration() design helpers: order-based clientRef→id correlation + second-pass assignment
    // ============================================================================================

    @Test
    public void testCorrelateNewDesignIdsMapsNewFactorsAndValues() {
        ExpressionExperimentServiceImpl impl = new ExpressionExperimentServiceImpl( eeDao );
        // Rebuilt design: existing factor 5 (existing FV 10) + new factor 7 (new FV 20).
        ExperimentalDesignValueObject rebuilt = new ExperimentalDesignValueObject();
        ExperimentalDesignValueObject.ExperimentalFactorEntry f5 = new ExperimentalDesignValueObject.ExperimentalFactorEntry();
        f5.setId( 5L );
        f5.setValues( new ArrayList<>( List.of( new FactorValueBasicValueObject( 10L ) ) ) );
        ExperimentalDesignValueObject.ExperimentalFactorEntry f7 = new ExperimentalDesignValueObject.ExperimentalFactorEntry();
        f7.setId( 7L );
        f7.setValues( new ArrayList<>( List.of( new FactorValueBasicValueObject( 20L ) ) ) );
        rebuilt.setExperimentalFactors( new ArrayList<>( List.of( f5, f7 ) ) );

        DesignCommitPlan plan = new DesignCommitPlan();
        plan.setPreExistingFactorIds( new HashSet<>( List.of( 5L ) ) );
        plan.setPreExistingFactorValueIds( new HashSet<>( List.of( 10L ) ) );
        plan.setNewFactorClientRefs( new ArrayList<>( List.of( "f1" ) ) );
        java.util.Map<String, List<String>> byParent = new java.util.HashMap<>();
        byParent.put( DesignCommitPlan.newFactorKey( "f1" ), new ArrayList<>( List.of( "fv1" ) ) );
        plan.setNewFactorValueClientRefsByParentKey( byParent );

        java.util.Map<String, Long> idMap = new java.util.LinkedHashMap<>();
        impl.correlateNewDesignIds( rebuilt, plan, idMap );
        assertThat( idMap ).containsEntry( "f1", 7L ).containsEntry( "fv1", 20L ).hasSize( 2 );
    }

    @Test
    public void testCorrelateNewFactorValueUnderExistingFactor() {
        ExpressionExperimentServiceImpl impl = new ExpressionExperimentServiceImpl( eeDao );
        ExperimentalDesignValueObject rebuilt = new ExperimentalDesignValueObject();
        ExperimentalDesignValueObject.ExperimentalFactorEntry f5 = new ExperimentalDesignValueObject.ExperimentalFactorEntry();
        f5.setId( 5L );
        // existing FV 10 + new FV 21 (higher id) under the same existing factor
        f5.setValues( new ArrayList<>( List.of( new FactorValueBasicValueObject( 10L ), new FactorValueBasicValueObject( 21L ) ) ) );
        rebuilt.setExperimentalFactors( new ArrayList<>( List.of( f5 ) ) );

        DesignCommitPlan plan = new DesignCommitPlan();
        plan.setPreExistingFactorIds( new HashSet<>( List.of( 5L ) ) );
        plan.setPreExistingFactorValueIds( new HashSet<>( List.of( 10L ) ) );
        java.util.Map<String, List<String>> byParent = new java.util.HashMap<>();
        byParent.put( DesignCommitPlan.existingFactorKey( 5L ), new ArrayList<>( List.of( "fvX" ) ) );
        plan.setNewFactorValueClientRefsByParentKey( byParent );

        java.util.Map<String, Long> idMap = new java.util.LinkedHashMap<>();
        impl.correlateNewDesignIds( rebuilt, plan, idMap );
        assertThat( idMap ).containsEntry( "fvX", 21L ).hasSize( 1 );
    }

    @Test
    public void testBuildAssignmentPassWiresNewFvToBiomaterial() {
        ExpressionExperimentServiceImpl impl = new ExpressionExperimentServiceImpl( eeDao );
        ExperimentalDesignValueObject rebuilt = new ExperimentalDesignValueObject();
        rebuilt.setBioMaterialAssignments( new ArrayList<>( List.of(
                new ExperimentalDesignValueObject.BioMaterialFactorValueAssignment( 100L, "bm", new ArrayList<>() ) ) ) );

        DesignCommitPlan plan = new DesignCommitPlan();
        plan.getPendingAssignments().add( new DesignCommitPlan.PendingAssignment( "fv1", new HashSet<>( List.of( 100L ) ) ) );
        java.util.Map<String, Long> idMap = new java.util.LinkedHashMap<>();
        idMap.put( "fv1", 20L );

        ExperimentalDesignValueObject edvo2 = impl.buildAssignmentPass( rebuilt, plan, idMap );
        assertThat( edvo2 ).isNotNull();
        assertThat( edvo2.getBioMaterialAssignments().get( 0 ).getFactorValueIds() ).containsExactly( 20L );
        // Nothing pending → null (caller skips the redundant second apply).
        assertThat( impl.buildAssignmentPass( rebuilt, new DesignCommitPlan(), idMap ) ).isNull();
    }

    /**
     * Re-enabled 2026-08-16. It was not a preflight defect: the test stubbed {@code eeDao.getSubSets}, while
     * {@code previewDesignChange} reads through {@code subSetReadService.getSubSetsWithBioAssays}, so the
     * unstubbed mock returned an empty list and no subset could ever be flagged. Stubbing the source the code
     * actually reads makes the detection visible.
     */
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
        when( subSetReadService.getSubSetsWithBioAssays( fixture ) ).thenReturn( Collections.singletonList( ss ) );

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