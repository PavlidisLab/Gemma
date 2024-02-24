package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.util.ByteArrayUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnDataSingleCellDataLoaderTest {

    @Before
    public void setUp() {

    }

    @Test
    public void test() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath();
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( dataPath );
        loader.setSampleFactorName( "ID" );
        loader.setCellTypeFactorName( "celltype1" );
        loader.setUnknownCellTypeIndicator( "UNK_ALL" );

        Collection<BioAssay> bas = new HashSet<>();
        for ( String sampleName : loader.getSampleNames() ) {
            bas.add( BioAssay.Factory.newInstance( sampleName, null, BioMaterial.Factory.newInstance( sampleName ) ) );
        }

        SingleCellDimension dimension = loader.getSingleCellDimension( bas );
        assertThat( dimension.getBioAssays() ).hasSize( 22 ).extracting( ba -> ba.getSampleUsed().getName() )
                .startsWith( "C-1034", "C-1252", "C-1262", "C-1366" );
        assertThat( dimension.getCellIds() ).startsWith(
                "CCTCTAGCAAGTGATA_1", "GGGATGACAGTCAGCC_1", "AGACAGGGTACCTATG_1",
                "GCGAGAATCCGGTAAT_1", "CAGGTATCACGGTAGA_1", "TGCAGGCTCCGTAGGC_1",
                "GACACGCCAAGGGTCA_1", "CAGATCAGTACGCTAT_1", "TGAATGCCATACGCAT_1" );
        assertThat( dimension.getNumberOfCells() ).isEqualTo( 1000 );

        assertThat( loader.getGenes() )
                .hasSize( 1000 )
                .startsWith( "SLC4A1AP" );

        assertThat( loader.getCellTypeAssignment() ).hasValueSatisfying( assignment -> {
            assertThat( assignment.getCellTypes() )
                    .hasSize( 8 )
                    .extracting( Characteristic::getValue )
                    .containsExactly( "Astrocytes", "Endothelial", "Interneurons", "MSNs", "Microglia", "Mural/Fibroblast", "Oligos", "Oligos_Pre" );
            assertThat( assignment.getNumberOfCellTypes() )
                    .isEqualTo( 8 );
            assertThat( assignment.getCellTypeIndices() )
                    .startsWith( 7, 6, 6, 3, 4, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 6, 4, 6, 6, 0, 6 );
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
                            .flatExtracting( FactorValue::getCharacteristics )
                            .extracting( Characteristic::getValue )
                            .contains( "Aspiration", "Cardiac Tamponade", "Cardiovascular Disease" );
                } );

        Set<QuantitationType> qts = loader.getQuantitationTypes();
        assertThat( qts ).hasSize( 1 ).extracting( QuantitationType::getName ).containsExactly( "X" );

        Map<String, CompositeSequence> elementsMapping = new HashMap<>();
        elementsMapping.put( "SLCO3A1", CompositeSequence.Factory.newInstance( "SLCO3A1" ) );

        QuantitationType qt = qts.iterator().next();
        try ( Stream<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping, dimension, qt ) ) {
            List<SingleCellExpressionDataVector> v = vectors.collect( Collectors.toList() );
            assertThat( v )
                    .hasSize( 1 )
                    .satisfiesExactly( vector -> {
                        assertThat( vector.getDesignElement().getName() ).isEqualTo( "SLCO3A1" );
                        assertThat( ByteArrayUtils.byteArrayToDoubles( vector.getData() ) )
                                .hasSize( 779 )
                                .usingComparatorWithPrecision( 0.00000001 )
                                .startsWith(
                                        2.51329864, 2.34829477, 0.58414776, 2.14333764, 2.85609327,
                                        1.72601606, 1.71459498, 1.79520526, 2.28259792, 2.26435748,
                                        2.71593599, 2.69432186, 2.44482503, 2.90114993, 2.31621068,
                                        2.05506585, 2.38949621, 2.80856107, 2.76760284, 2.67542341,
                                        2.68732885, 1.28165171, 2.48619096, 1.59628869, 2.71340522,
                                        1.44516265, 1.38749556, 0.31626671, 2.07581874, 1.78905626 );
                        assertThat( vector.getDataIndices() )
                                .hasSize( 779 )
                                .startsWith(
                                        1, 2, 3, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15,
                                        16, 17, 19, 21, 22, 24, 25, 26, 27, 28, 29, 31, 32,
                                        36, 37, 38, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
                                        51, 52, 53, 55, 56, 57, 61, 62, 63, 64, 65, 67, 68,
                                        69, 72, 73, 74, 76, 78, 79, 80, 82, 83, 84, 86, 87,
                                        88, 89, 90, 91, 92, 93, 95, 96, 97, 98, 99, 103, 104 );
                    } );
        }
    }

    @Test
    public void testLoadSpecificSamples() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath();
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( dataPath );
        loader.setSampleFactorName( "ID" );
        loader.setCellTypeFactorName( "celltype1" );
        loader.setUnknownCellTypeIndicator( "UNK_ALL" );

        // load two samples
        Set<BioAssay> bas = new HashSet<>();
        BioAssay.Factory.newInstance( "", null, BioMaterial.Factory.newInstance( "" ) );
        BioAssay.Factory.newInstance( "", null, BioMaterial.Factory.newInstance( "" ) );

        loader.getSingleCellDimension( bas );
    }
}