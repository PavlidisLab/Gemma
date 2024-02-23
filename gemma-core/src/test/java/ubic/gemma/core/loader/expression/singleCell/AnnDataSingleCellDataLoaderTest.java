package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.util.ByteArrayUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnDataSingleCellDataLoaderTest {

    @Test
    public void test() throws IOException {
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( Paths.get( "/home/guillaume/Téléchargements/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ) );
        loader.setUnknownCellTypeIndicator( "UNK_ALL" );

        assertThat( loader.getCellTypeAssignment() ).hasValueSatisfying( assignment -> {
            assertThat( assignment.getCellTypes() )
                    .hasSize( 8 )
                    .extracting( Characteristic::getValue )
                    .containsExactly( "Astrocytes", "Endothelial", "Interneurons", "MSNs", "Microglia", "Mural/Fibroblast", "Oligos", "Oligos_Pre" );
            assertThat( assignment.getNumberOfCellTypes() )
                    .isEqualTo( 8 );
            assertThat( assignment.getCellTypeIndices() )
                    .startsWith( 4, 4, 6, 6, 6, 6, 4, 6, 6, 6, 6, 4, 3, 4, 3, 3, 6, 3, 6 );
        } );

        assertThat( loader.getFactors() )
                .hasSize( 56 )
                .satisfiesOnlyOnce( factor -> {
                    assertThat( factor.getName() ).isEqualTo( "BMI" );
                    assertThat( factor.getType() ).isEqualTo( FactorType.CONTINUOUS );
                    // continuous factors are not populated (yet!)
                    assertThat( factor.getFactorValues() ).isEmpty();
                } )
                .satisfiesOnlyOnce( factor -> {
                    assertThat( factor.getName() ).isEqualTo( "Cause.of.Death" );
                    assertThat( factor.getFactorValues() )
                            .hasSize( 7 )
                            .extracting( FactorValue::getValue )
                            .contains( "Aspiration", "Cardiac Tamponade", "Cardiovascular Disease" );
                } );

        try ( Stream<SingleCellExpressionDataVector> vectors = loader.loadVectors( null, null, null ) ) {
            List<SingleCellExpressionDataVector> v = vectors.limit( 20 ).collect( Collectors.toList() );
            assertThat( v ).first().satisfies( vector -> {
                assertThat( ByteArrayUtils.byteArrayToDoubles( vector.getData() ) )
                        .usingComparatorWithPrecision( 0.00001 )
                        .startsWith( 0.934177, 0.934177, 0.934177, 0.934177, 0.934177, 0.934177, 0, 2.16626, 0.934177,
                                0.934177, 0, 0.934177, 2.70177, 0.934177 )
                        .hasSize( 3069 );
                assertThat( vector.getDataIndices() )
                        .hasSize( 3069 )
                        .startsWith( 9, 60, 69, 73, 75, 79, 82, 87, 150, 154, 157, 169, 180, 182, 186, 196, 198, 205,
                                220, 224, 248, 255, 256, 267, 273, 278, 288, 296 );
            } );
        }
    }
}