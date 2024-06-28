package ubic.gemma.persistence.service.expression.experiment;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class SingleCellExpressionExperimentSplitServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class SingleCellExpressionExperimentSplitServiceTestContextConfiguration {

        @Bean
        public SingleCellExpressionExperimentSplitService service() {
            return new SingleCellExpressionExperimentSplitServiceImpl();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSubSetService expressionExperimentSubSetService() {
            return mock();
        }

        @Bean
        public BioAssayService bioAssayService() {
            return mock();
        }

        @Bean
        public BioMaterialService bioMaterialService() {
            return mock();
        }
    }

    @Autowired
    private SingleCellExpressionExperimentSplitService service;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Test
    public void test() {
        CellTypeAssignment cta = new CellTypeAssignment();
        ExpressionExperiment ee = new ExpressionExperiment();
        ExperimentalFactor cf = new ExperimentalFactor();
        ArrayDesign ad = new ArrayDesign();
        // create 4 samples
        for ( int i = 0; i < 4; i++ ) {
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setName( "ba" + i );
            BioMaterial bm = new BioMaterial();
            bm.setName( "bm" + i );
            ba.setSampleUsed( bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        // create 10 cell types
        for ( int i = 0; i < 10; i++ ) {
            Characteristic ct = Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct" + i, null );
            cta.getCellTypes().add( ct );
            FactorValue fv = new FactorValue();
            fv.getCharacteristics().add( Statement.Factory.newInstance( Categories.CELL_TYPE, ct ) );
            cf.getFactorValues().add( fv );
        }
        when( bioMaterialService.create( any( BioMaterial.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( bioAssayService.create( any( BioAssay.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee ) )
                .thenReturn( cta );
        when( singleCellExpressionExperimentService.getCellTypeFactor( ee ) )
                .thenReturn( cf );
        when( expressionExperimentSubSetService.create( any( ExpressionExperimentSubSet.class ) ) )
                .thenAnswer( a -> a.getArgument( 0 ) );
        List<ExpressionExperimentSubSet> subsets = service.splitByCellType( ee );
        assertThat( subsets )
                .hasSize( 10 )
                .allSatisfy( subset -> {
                    Characteristic cellType = subset.getCharacteristics().iterator().next();
                    String cellTypeName = cellType.getValue();
                    assertThat( subset.getSourceExperiment() ).isEqualTo( ee );
                    assertThat( subset.getName() ).isEqualTo( ee.getName() + " - " + cellTypeName );
                    assertThat( subset.getCharacteristics() )
                            .hasSize( 1 )
                            .first()
                            .satisfies( c -> {
                                assertThat( c.getCategory() ).isEqualTo( Categories.CELL_TYPE.getCategory() );
                                assertThat( c.getCategoryUri() ).isEqualTo( Categories.CELL_TYPE.getCategoryUri() );
                                assertThat( c.getValue() ).isEqualTo( cellTypeName );
                            } );
                    assertThat( subset.getBioAssays() )
                            .hasSize( 4 )
                            .allSatisfy( ba -> {
                                assertThat( ba.getName() )
                                        .matches( "ba\\d+ - " + cellTypeName );
                                assertThat( ba.getArrayDesignUsed() )
                                        .isEqualTo( ad );
                                assertThat( ba.getSampleUsed() )
                                        .isNotNull()
                                        .satisfies( bm -> {
                                            assertThat( bm.getName() )
                                                    .matches( "bm\\d+ - " + cellTypeName );
                                            assertThat( bm.getBioAssaysUsedIn() )
                                                    .contains( ba );
                                            assertThat( bm.getCharacteristics() )
                                                    .hasSize( 1 )
                                                    .first()
                                                    .satisfies( c -> {
                                                        assertThat( c.getCategory() ).isEqualTo( Categories.CELL_TYPE.getCategory() );
                                                        assertThat( c.getCategoryUri() ).isEqualTo( Categories.CELL_TYPE.getCategoryUri() );
                                                        assertThat( c.getValue() ).isEqualTo( cellTypeName );
                                                    } );
                                        } );
                            } );
                } );
    }
}