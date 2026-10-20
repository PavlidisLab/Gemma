package ubic.gemma.core.analysis.preprocess;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.security.SecurityService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.EeWriteService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * The parts of a split are committed before they are post-processed, so a part whose post-processing fails is left
 * without processed data. That must reach the caller rather than be reported as a successful split.
 */
public class SplitExperimentPostprocessingFailureTest {

    @Test
    public void testAPostprocessingFailureOfOnePartIsReportedAfterTheSplitCompletes() {
        SplitExperimentServiceImpl splitService = new SplitExperimentServiceImpl();
        PreprocessorService preprocessor = mock();
        ExpressionExperimentService eeService = mock();
        RawExpressionDataVectorService rawExpressionDataVectorService = mock();
        EeWriteService eeWriteService = mock();
        SecurityService securityService = mock();
        ExpressionExperimentSetService expressionExperimentSetService = mock();
        ReflectionTestUtils.setField( splitService, "preprocessor", preprocessor );
        ReflectionTestUtils.setField( splitService, "eeService", eeService );
        ReflectionTestUtils.setField( splitService, "rawExpressionDataVectorService", rawExpressionDataVectorService );
        ReflectionTestUtils.setField( splitService, "eeWriteService", eeWriteService );
        ReflectionTestUtils.setField( splitService, "securityService", securityService );
        ReflectionTestUtils.setField( splitService, "dataFileService", mock( ExpressionDataFileService.class ) );
        ReflectionTestUtils.setField( splitService, "expressionExperimentSetService", expressionExperimentSetService );
        ReflectionTestUtils.setField( splitService, "factorValueService", mock( FactorValueService.class ) );

        ArrayDesign platform = ArrayDesign.Factory.newInstance();
        platform.setId( 1L );
        platform.setShortName( "GPL1" );

        ExpressionExperiment toSplit = new ExpressionExperiment();
        toSplit.setId( 1L );
        toSplit.setShortName( "GSE1" );
        toSplit.setName( "An experiment" );
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setId( 1L );
        toSplit.setExperimentalDesign( ed );
        ExperimentalFactor treatment = ExperimentalFactor.Factory.newInstance( ed, "treatment", FactorType.CATEGORICAL );
        treatment.setId( 1L );
        ed.getExperimentalFactors().add( treatment );
        List<FactorValue> fvs = new ArrayList<>();
        for ( long i = 1; i <= 2; i++ ) {
            FactorValue fv = FactorValue.Factory.newInstance( treatment );
            fv.setId( i );
            //noinspection deprecation
            fv.setValue( "level" + i );
            treatment.getFactorValues().add( fv );
            fvs.add( fv );
        }
        List<BioAssay> assays = new ArrayList<>();
        for ( long i = 1; i <= 4; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "sample" + i );
            bm.setId( i );
            bm.getFactorValues().add( fvs.get( i <= 2 ? 0 : 1 ) );
            BioAssay ba = BioAssay.Factory.newInstance( "assay" + i, platform, bm );
            ba.setId( i );
            bm.getBioAssaysUsedIn().add( ba );
            toSplit.getBioAssays().add( ba );
            assays.add( ba );
        }

        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setId( 1L );
        qt.setName( "intensity" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsPreferred( true );
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance( assays );
        bad.setId( 1L );
        List<RawExpressionDataVector> vectors = new ArrayList<>();
        for ( long i = 1; i <= 2; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "probe" + i, platform );
            cs.setId( i );
            RawExpressionDataVector v = new RawExpressionDataVector();
            v.setId( i );
            v.setExpressionExperiment( toSplit );
            v.setQuantitationType( qt );
            v.setDesignElement( cs );
            v.setBioAssayDimension( bad );
            v.setDataAsDoubles( new double[] { 1, 2, 3, 4 } );
            vectors.add( v );
        }

        when( eeService.getArrayDesignsUsed( toSplit ) ).thenReturn( Collections.singleton( platform ) );
        when( eeService.getQuantitationTypes( toSplit ) ).thenReturn( Collections.singleton( qt ) );
        when( rawExpressionDataVectorService.findAndThaw( qt ) ).thenReturn( vectors );
        AtomicLong ids = new AtomicLong( 100 );
        when( eeWriteService.create( any( ExpressionExperiment.class ) ) ).thenAnswer( a -> {
            ExpressionExperiment created = a.getArgument( 0 );
            created.setId( ids.incrementAndGet() );
            return created;
        } );
        when( expressionExperimentSetService.create( any( ExpressionExperimentSet.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        doThrow( new PreprocessingException( toSplit, "PCA failed" ) )
                .when( preprocessor ).process( argThat( ( ExpressionExperiment ee ) -> "GSE1.1".equals( ee.getShortName() ) ) );

        assertThatThrownBy( () -> splitService.split( toSplit, treatment, true ) )
                .hasMessageContaining( "GSE1 was split into 2 parts, but post-processing failed for 1 of them: GSE1.1" )
                .hasMessageContaining( "makeProcessedData" )
                .hasCauseInstanceOf( PreprocessingException.class );
        // the other part is still post-processed, and the split is completed
        verify( preprocessor, times( 2 ) ).process( any( ExpressionExperiment.class ) );
        verify( expressionExperimentSetService ).create( any( ExpressionExperimentSet.class ) );
        verify( securityService ).makePrivate( toSplit );
    }
}
