package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
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

    @Test
    public void test() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath();
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( dataPath );
        loader.setSampleFactorName( "ID" );
        loader.setCellTypeFactorName( "celltype1" );
        loader.setUnknownCellTypeIndicator( "UNK_ALL" );
        loader.setIgnoreUnmatchedSamples( false );
        loader.setSampleNameComparator( ( bm, n ) -> n.equals( bm.getName() ) );

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

        assertThat( loader.getCellTypeAssignment( dimension ) ).hasValueSatisfying( assignment -> {
            assertThat( assignment.getCellTypes() )
                    .hasSize( 8 )
                    .extracting( Characteristic::getValue )
                    .containsExactly( "Astrocytes", "Endothelial", "Interneurons", "MSNs", "Microglia", "Mural/Fibroblast", "Oligos", "Oligos_Pre" );
            assertThat( assignment.getNumberOfCellTypes() )
                    .isEqualTo( 8 );
            assertThat( assignment.getCellTypeIndices() )
                    .startsWith( 7, 6, 6, 3, 4, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 6, 4, 6, 6, 0, 6 );
        } );

        Set<BioMaterial> samples = bas.stream().map( BioAssay::getSampleUsed ).collect( Collectors.toSet() );
        assertThat( loader.getSampleCharacteristics( samples ) )
                .hasSize( 22 )
                .extractingByKey( BioMaterial.Factory.newInstance( "C-1262" ) )
                .satisfies( c -> {
                    assertThat( c )
                            .hasSize( 23 )
                            .contains( Characteristic.Factory.newInstance( "Manner.of.Death", null, "Accidental", null ) );
                } );

        assertThat( loader.getFactors( samples, null ) )
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
        assertThat( qts ).hasSize( 1 ).first().satisfies( qt -> {
            assertThat( qt.getName() ).isEqualTo( "X" );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG1P );
        } );

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
        loader.setIgnoreUnmatchedSamples( true );
        loader.setSampleNameComparator( ( bm, n ) -> n.equals( bm.getName() ) );

        // load two samples
        Set<BioAssay> bas = new HashSet<>();
        bas.add( BioAssay.Factory.newInstance( "C-13151", null, BioMaterial.Factory.newInstance( "C-13151" ) ) );
        bas.add( BioAssay.Factory.newInstance( "P-13281", null, BioMaterial.Factory.newInstance( "P-13281" ) ) );

        SingleCellDimension dim = loader.getSingleCellDimension( bas );
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();

        assertThat( dim.getBioAssays() )
                .hasSize( 2 )
                .extracting( BioAssay::getName )
                .containsExactly( "C-13151", "P-13281" );
        assertThat( dim.getCellIds() ).containsExactly( "GTAGAGGCACCTGTCT_5", "GACTATGAGACTCCGC_5", "TTCGATTCAGCAAGAC_5",
                "TCTCACGGTCGGTACC_5", "GGGTCTGGTCACCCTT_5", "CTTCAATAGTAGGCCA_5",
                "CATTGCCGTCCGAAAG_5", "TCACGGGTCCTGTACC_5", "TCCTAATGTAGATTGA_5",
                "CCAAGCGCACGTTGGC_5", "GTTCGCTTCACGACTA_5", "GCTTGGGGTTACCCTC_5",
                "CGTGCTTTCATGAGGG_5", "CCGTTCAAGCGCCTAC_5", "CTTTCGGGTGCATGAG_5",
                "TCATGAGTCGTAATGC_5", "TCATACTTCCCTCATG_5", "CTAGGTATCATCTATC_5",
                "TCATGTTCACCTGTCT_5", "ATTCCATCACGCTTAA_5", "TTCGGTCAGGTTGGTG_5",
                "ATGAGGGCAATGACCT_5", "AACCTTTCAACTTCTT_5", "TGTGATGCATGCACTA_5",
                "CCCTAACAGTTAGTGA_16", "TGTAACGTCGTTCTCG_16", "CTACATTCAAGTGGTG_16",
                "TCATCATCAGACGCTC_16", "GACCCTTGTGACCTGC_16", "AACCTGACAAAGGGTC_16",
                "ATAGACCGTGCTCCGA_16", "CCCTCTCGTGGAGAAA_16", "AGCGCCAGTCGTCTCT_16",
                "AAAGGTAGTTTGATCG_16", "CACAACATCGAACCAT_16" );

        Map<String, CompositeSequence> elementsMapping = new HashMap<>();
        elementsMapping.put( "SLCO3A1", CompositeSequence.Factory.newInstance( "SLCO3A1" ) );
        assertThat( loader.loadVectors( elementsMapping, dim, qt ) )
                .first().satisfies( v -> {
                    assertThat( v.getDesignElement().getName() ).isEqualTo( "SLCO3A1" );
                    assertThat( ByteArrayUtils.byteArrayToDoubles( v.getData() ) )
                            .usingComparatorWithPrecision( 0.00000001 )
                            .containsExactly(
                                    0., 0.71611292, 2.21477848, 0.5758799, 1.95063931, 2.54813097, 2.65149612,
                                    2.45977532, 1.61314598, 0.82234731, 0., 2.71867203, 0.77666734, 1.90074419,
                                    1.25652567, 3.13875217, 2.20132395, 2.7090048, 3.07420778, 2.32110793, 2.73402347,
                                    2.18838474, 2.48651234 );
                    assertThat( v.getDataIndices() )
                            .containsExactly( 0, 1, 5, 6, 7, 8, 10, 12, 13, 15, 16, 17, 18, 19, 25, 26, 27, 28,
                                    29, 30, 31, 32, 33 );
                } );
    }
}