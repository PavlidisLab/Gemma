package ubic.gemma.core.analysis.preprocess;

import gemma.gsec.SecurityService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class SplitExperimentServiceTest extends BaseTest {

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public SplitExperimentService splitExperimentService() {
            return new SplitExperimentServiceImpl();
        }

        @Bean
        public SplitExperimentHelperService splitExperimentHelperService() {
            return new SplitExperimentHelperService();
        }

        @Bean
        public PreprocessorService preprocessor() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService eeService() {
            return mock();
        }

        @Bean
        public RawExpressionDataVectorService rawExpressionDataVectorService() {
            return mock();
        }

        @Bean
        public Persister persister() {
            return mock();
        }

        @Bean
        public SecurityService securityService() {
            return mock();
        }

        @Bean
        public ExpressionDataFileService dataFileService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }
    }

    @Autowired
    private SplitExperimentService splitExperimentService;

    @Autowired
    private Persister persister;

    @Before
    public void setUp() {
        when( persister.persist( any( Identifiable.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
        when( expressionExperimentSetService.create( any( ExpressionExperimentSet.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
    }

    @Test
    public void test() {
        Taxon taxon = new Taxon();
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i, ad );
            ad.getCompositeSequences().add( cs );
        }
        ExpressionExperiment ee = RandomExpressionExperimentUtils.randomExpressionExperiment( taxon, 16, ad );
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ExperimentalFactor factor = RandomExperimentalDesignUtils.randomCategoricalFactor( ee, "test", 4 );

        // setup some vectors
        QuantitationType scQt = QuantitationType.Factory.newInstance();
        scQt.setName( "counts" );
        scQt.setGeneralType( GeneralType.QUANTITATIVE );
        scQt.setType( StandardQuantitationType.COUNT );
        scQt.setScale( ScaleType.COUNT );
        scQt.setRepresentation( PrimitiveType.DOUBLE );
        List<SingleCellExpressionDataVector> scVectors = RandomSingleCellDataUtils.randomSingleCellVectors( ee, ad, scQt );
        ee.getQuantitationTypes().add( scQt );
        ee.getSingleCellExpressionDataVectors().addAll( scVectors );
        when( singleCellExpressionExperimentService.getSingleCellDataVectors( ee, scQt ) )
                .thenReturn( scVectors );

        QuantitationType rawQt = QuantitationType.Factory.newInstance();
        rawQt.setName( "log2cpm" );
        rawQt.setGeneralType( GeneralType.QUANTITATIVE );
        rawQt.setType( StandardQuantitationType.AMOUNT );
        rawQt.setScale( ScaleType.LOG2 );
        rawQt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<RawExpressionDataVector> rawVectors = RandomBulkDataUtils.randomBulkVectors( ee, ad, rawQt, RawExpressionDataVector.class );
        ee.getQuantitationTypes().add( rawQt );
        ee.getRawExpressionDataVectors().addAll( rawVectors );

        Map<Class<? extends DataVector>, Set<QuantitationType>> qtsByVt = new HashMap<>();
        qtsByVt.put( SingleCellExpressionDataVector.class, Collections.singleton( scQt ) );
        qtsByVt.put( RawExpressionDataVector.class, Collections.singleton( rawQt ) );
        when( expressionExperimentService.getQuantitationTypesByVectorType( ee ) )
                .thenReturn( qtsByVt );

        ExpressionExperimentSet parts = splitExperimentService.split( ee, factor, false, false );

        assertThat( parts.getExperiments() )
                .hasSize( 4 )
                .allSatisfy( split -> {
                    assertThat( split.getShortName() ).startsWith( ee.getShortName() + "." );
                    assertThat( split.getBioAssays() )
                            .isSubsetOf( ee.getBioAssays() )
                            // make sure that fresh copies are being used
                            .usingElementComparator( Comparator.comparingInt( System::identityHashCode ) )
                            .doesNotContainAnyElementsOf( ee.getBioAssays() );
                    assertThat( split.getSingleCellExpressionDataVectors() )
                            .hasSize( 100 )
                            .first()
                            .satisfies( vec -> {
                                assertThat( vec.getDesignElement() )
                                        .isIn( ad.getCompositeSequences() );
                                assertThat( vec.getQuantitationType() )
                                        .isEqualTo( scQt )
                                        .isNotSameAs( scQt );
                                assertThat( vec.getSingleCellDimension().getBioAssays() )
                                        .hasSizeLessThan( 16 )
                                        .isSubsetOf( ee.getBioAssays() )
                                        // make sure that fresh copies are being used
                                        .usingElementComparator( Comparator.comparingInt( System::identityHashCode ) )
                                        .doesNotContainAnyElementsOf( ee.getBioAssays() )
                                        // make sure it contains the same entities that the split experiment has
                                        .containsExactlyInAnyOrderElementsOf( split.getBioAssays() );
                            } );
                } );
    }
}