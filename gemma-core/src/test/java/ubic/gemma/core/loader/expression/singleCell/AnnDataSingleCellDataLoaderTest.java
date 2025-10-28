package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTransformationPipeline;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTranspose;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataUnraw;
import ubic.gemma.core.loader.util.mapper.MapBasedDesignElementMapper;
import ubic.gemma.core.loader.util.mapper.RenamingBioAssayMapper;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.core.loader.util.mapper.SimpleDesignElementMapper;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AnnDataSingleCellDataLoaderTest {

    private static final Path pythonExecutable = Paths.get( Settings.getString( "python.exe" ) );

    @Test
    public void testGSE225158() throws IOException {
        try ( AnnDataSingleCellDataLoader loader = createLoader() ) {
            Collection<BioAssay> bas = new HashSet<>();
            for ( String sampleName : loader.getSampleNames() ) {
                bas.add( BioAssay.Factory.newInstance( sampleName, null, BioMaterial.Factory.newInstance( sampleName ) ) );
            }

            SingleCellDimension dimension = loader.getSingleCellDimension( bas );
            assertThat( dimension.getBioAssays() ).hasSize( 22 ).extracting( ba -> ba.getSampleUsed().getName() )
                    .startsWith( "C-1034", "C-1252", "C-1262", "C-1366" );
            assertThat( dimension.getCellIds() )
                    .startsWith(
                            "CCTCTAGCAAGTGATA_1", "GGGATGACAGTCAGCC_1", "AGACAGGGTACCTATG_1",
                            "GCGAGAATCCGGTAAT_1", "CAGGTATCACGGTAGA_1", "TGCAGGCTCCGTAGGC_1",
                            "GACACGCCAAGGGTCA_1", "CAGATCAGTACGCTAT_1", "TGAATGCCATACGCAT_1" );
            assertThat( dimension.getNumberOfCellIds() ).isEqualTo( 1000 );

            assertThat( loader.getGenes() )
                    .hasSize( 1000 )
                    .startsWith( "SLC4A1AP" );

            assertThat( loader.getCellTypeAssignments( dimension ) )
                    .singleElement()
                    .satisfies( assignment -> {
                        assertThat( assignment.getCellTypes() )
                                .hasSize( 8 )
                                .extracting( Characteristic::getValue )
                                .containsExactly( "Astrocytes", "Endothelial", "Interneurons", "MSNs", "Microglia", "Mural/Fibroblast", "Oligos", "Oligos_Pre" );
                        assertThat( assignment.getNumberOfCellTypes() )
                                .isEqualTo( 8 );
                        assertThat( assignment.getCellTypeIndices() )
                                .startsWith( 7, 6, 6, 3, 4, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 6, 4, 6, 6, 0, 6 );
                    } );

            loader.setMaxCharacteristics( 1000 );
            assertThat( loader.getOtherCellLevelCharacteristics( dimension ) )
                    .hasSize( 17 )
                    .allSatisfy( s -> {
                        assertThat( s.getCharacteristics() )
                                .allSatisfy( c -> {
                                    assertThat( c.getCategory() ).isNotNull();
                                    assertThat( c.getValue() ).isNotNull();
                                } );
                    } );

            assertThat( loader.getSamplesCharacteristics( bas ) )
                    .hasSize( 22 )
                    .extractingByKey( BioMaterial.Factory.newInstance( "C-1262" ) )
                    .satisfies( c -> {
                        assertThat( c )
                                .hasSize( 36 )
                                .contains( Characteristic.Factory.newInstance( "Manner.of.Death", null, "Accidental", null ) )
                                .contains( Characteristic.Factory.newInstance( "Age", null, "41.0", null ) )
                                .contains( Characteristic.Factory.newInstance( "DSM.IV.AUD", null, "1", null ) );
                    } );

            Map<BioMaterial, Set<FactorValue>> fva = new HashMap<>();
            assertThat( loader.getFactors( bas, fva ) )
                    .hasSize( 36 )
                    .noneSatisfy( factor -> {
                        assertThat( factor.getName() ).isEqualTo( "ID" );
                        assertThat( factor.getName() ).isEqualTo( "celltype1" );
                    } )
                    .satisfiesOnlyOnce( factor -> {
                        assertThat( factor.getName() ).isEqualTo( "Cause.of.Death" );
                        assertThat( factor.getFactorValues() )
                                .hasSize( 7 )
                                .flatExtracting( FactorValue::getCharacteristics )
                                .extracting( Characteristic::getValue )
                                .contains( "Aspiration", "Cardiac Tamponade", "Cardiovascular Disease" );
                    } )
                    .satisfiesOnlyOnce( factor -> {
                        assertThat( factor.getName() ).isEqualTo( "BMI" );
                        assertThat( factor.getType() ).isEqualTo( FactorType.CONTINUOUS );
                        assertThat( factor.getFactorValues() ).isNotEmpty().allSatisfy( fv -> {
                            assertThat( fv.getMeasurement() ).isNotNull();
                            assertThat( fv.getMeasurement().getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
                        } );
                    } )
                    .satisfiesOnlyOnce( factor -> {
                        assertThat( factor.getName() ).isEqualTo( "Age" );
                        assertThat( factor.getType() ).isEqualTo( FactorType.CONTINUOUS );
                        assertThat( factor.getFactorValues() ).isNotEmpty().allSatisfy( fv -> {
                            assertThat( fv.getMeasurement() ).isNotNull();
                            assertThat( fv.getMeasurement().getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
                        } );
                    } );

            assertThat( fva )
                    .hasSize( 22 )
                    .allSatisfy( ( bm, fvs ) -> {
                        assertThat( fvs )
                                .hasSize( 36 )
                                .satisfiesOnlyOnce( fv -> {
                                    assertThat( fv.getExperimentalFactor().getName() ).isEqualTo( "DSM.IV.CUD" );
                                    assertThat( fv.getMeasurement() ).isNotNull();
                                    assertThat( fv.getMeasurement().getRepresentation() ).isEqualTo( PrimitiveType.INT );
                                } )
                                .satisfiesOnlyOnce( fv -> {
                                    assertThat( fv.getExperimentalFactor().getName() ).isEqualTo( "Age" );
                                    assertThat( fv.getMeasurement() ).isNotNull();
                                    assertThat( fv.getMeasurement().getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
                                } );
                    } );

            Set<QuantitationType> qts = loader.getQuantitationTypes();
            assertThat( qts ).hasSize( 1 ).first().satisfies( qt -> {
                assertThat( qt.getName() ).isEqualTo( "AnnData" );
                assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
                assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG1P );
                assertThat( qt.getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
            } );

            assertThat( loader.getSequencingMetadata( dimension ) ).isEmpty();

            Map<String, CompositeSequence> elementsMapping = new HashMap<>();
            elementsMapping.put( "SLCO3A1", CompositeSequence.Factory.newInstance( "SLCO3A1" ) );
            loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "test", elementsMapping ) );

            QuantitationType qt = qts.iterator().next();
            try ( Stream<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping.values(), dimension, qt ) ) {
                List<SingleCellExpressionDataVector> v = vectors.collect( Collectors.toList() );
                assertThat( v )
                        .hasSize( 1 )
                        .satisfiesExactly( vector -> {
                            assertThat( vector.getDesignElement().getName() ).isEqualTo( "SLCO3A1" );
                            assertThat( vector.getOriginalDesignElement() ).isEqualTo( "SLCO3A1" );
                            assertThat( vector.getDataAsDoubles() )
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
    }

    @Test
    public void testSubsetOfGSE225158() throws IOException {
        try ( AnnDataSingleCellDataLoader loader = createLoader() ) {
            Collection<BioAssay> bas = new HashSet<>();
            for ( String sampleName : loader.getSampleNames() ) {
                if ( sampleName.equals( "C-1262" ) ) {
                    bas.add( BioAssay.Factory.newInstance( sampleName, null, BioMaterial.Factory.newInstance( sampleName ) ) );
                }
            }

            SingleCellDimension dimension = loader.getSingleCellDimension( bas );
            assertThat( dimension.getBioAssays() ).hasSize( 1 ).extracting( ba -> ba.getSampleUsed().getName() )
                    .containsExactly( "C-1262" );
            assertThat( dimension.getCellIds() )
                    .startsWith(
                            "AGACAAACATCATCTT_3",
                            "TTTCATGAGGCGCTCT_3",
                            "AGTAGTCTCGAGAATA_3",
                            "GTCTTTATCATTTGCT_3",
                            "GAACTGTTCAGCGCAC_3",
                            "ACGGAAGTCATTGGTG_3",
                            "AGGGTGAAGACCTCCG_3",
                            "TCAGGGCAGGTCACCC_3",
                            "TCGTAGAGTGGGCTCT_3",
                            "ATCGCCTCATCAGTGT_3" );
            assertThat( dimension.getNumberOfCellIds() ).isEqualTo( 81 );

            assertThat( loader.getGenes() )
                    .hasSize( 1000 )
                    .startsWith( "SLC4A1AP" );

            assertThat( loader.getCellTypeAssignments( dimension ) )
                    .singleElement()
                    .satisfies( assignment -> {
                        assertThat( assignment.getCellTypes() )
                                .hasSize( 8 )
                                .extracting( Characteristic::getValue )
                                .containsExactly( "Astrocytes", "Endothelial", "Interneurons", "MSNs", "Microglia", "Mural/Fibroblast", "Oligos", "Oligos_Pre" );
                        assertThat( assignment.getNumberOfCellTypes() )
                                .isEqualTo( 8 );
                        assertThat( assignment.getCellTypeIndices() )
                                .startsWith( 3, 6, 3, 6, 6, 7, 6, 6, 0, 6, 6, 3, 6, 6, 3, 3, 3, 1, 0, 6, 3, 0, 6, 6, 3 );
                    } );

            loader.setMaxCharacteristics( 1000 );
            assertThat( loader.getOtherCellLevelCharacteristics( dimension ) )
                    .hasSize( 17 )
                    .allSatisfy( s -> {
                        assertThat( s.getCharacteristics() )
                                .allSatisfy( c -> {
                                    assertThat( c.getCategory() ).isNotNull();
                                    assertThat( c.getValue() ).isNotNull();
                                } );
                    } );

            assertThat( loader.getSamplesCharacteristics( bas ) )
                    .hasSize( 1 )
                    .extractingByKey( BioMaterial.Factory.newInstance( "C-1262" ) )
                    .satisfies( c -> {
                        assertThat( c )
                                .hasSize( 36 )
                                .contains( Characteristic.Factory.newInstance( "Manner.of.Death", null, "Accidental", null ) )
                                .contains( Characteristic.Factory.newInstance( "Age", null, "41.0", null ) )
                                .contains( Characteristic.Factory.newInstance( "DSM.IV.AUD", null, "1", null ) );
                    } );

            Map<BioMaterial, Set<FactorValue>> fva = new HashMap<>();
            assertThat( loader.getFactors( bas, fva ) )
                    .hasSize( 36 )
                    .noneSatisfy( factor -> {
                        assertThat( factor.getName() ).isEqualTo( "ID" );
                        assertThat( factor.getName() ).isEqualTo( "celltype1" );
                    } )
                    .satisfiesOnlyOnce( factor -> {
                        assertThat( factor.getName() ).isEqualTo( "Cause.of.Death" );
                        assertThat( factor.getFactorValues() )
                                .hasSize( 7 )
                                .flatExtracting( FactorValue::getCharacteristics )
                                .extracting( Characteristic::getValue )
                                .contains( "Aspiration", "Cardiac Tamponade", "Cardiovascular Disease" );
                    } )
                    .satisfiesOnlyOnce( factor -> {
                        assertThat( factor.getName() ).isEqualTo( "BMI" );
                        assertThat( factor.getType() ).isEqualTo( FactorType.CONTINUOUS );
                        assertThat( factor.getFactorValues() ).isNotEmpty().allSatisfy( fv -> {
                            assertThat( fv.getMeasurement() ).isNotNull();
                            assertThat( fv.getMeasurement().getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
                        } );
                    } )
                    .satisfiesOnlyOnce( factor -> {
                        assertThat( factor.getName() ).isEqualTo( "Age" );
                        assertThat( factor.getType() ).isEqualTo( FactorType.CONTINUOUS );
                        assertThat( factor.getFactorValues() ).isNotEmpty().allSatisfy( fv -> {
                            assertThat( fv.getMeasurement() ).isNotNull();
                            assertThat( fv.getMeasurement().getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
                        } );
                    } );

            assertThat( fva )
                    .hasSize( 1 )
                    .allSatisfy( ( bm, fvs ) -> {
                        assertThat( fvs )
                                .hasSize( 36 )
                                .satisfiesOnlyOnce( fv -> {
                                    assertThat( fv.getExperimentalFactor().getName() ).isEqualTo( "DSM.IV.CUD" );
                                    assertThat( fv.getMeasurement() ).isNotNull();
                                    assertThat( fv.getMeasurement().getRepresentation() ).isEqualTo( PrimitiveType.INT );
                                } )
                                .satisfiesOnlyOnce( fv -> {
                                    assertThat( fv.getExperimentalFactor().getName() ).isEqualTo( "Age" );
                                    assertThat( fv.getMeasurement() ).isNotNull();
                                    assertThat( fv.getMeasurement().getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
                                } );
                    } );

            Set<QuantitationType> qts = loader.getQuantitationTypes();
            assertThat( qts ).hasSize( 1 ).first().satisfies( qt -> {
                assertThat( qt.getName() ).isEqualTo( "AnnData" );
                assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
                assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG1P );
                assertThat( qt.getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
            } );

            assertThat( loader.getSequencingMetadata( dimension ) ).isEmpty();

            Map<String, CompositeSequence> elementsMapping = new HashMap<>();
            elementsMapping.put( "SLCO3A1", CompositeSequence.Factory.newInstance( "SLCO3A1" ) );
            loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "test", elementsMapping ) );

            QuantitationType qt = qts.iterator().next();
            try ( Stream<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping.values(), dimension, qt ) ) {
                List<SingleCellExpressionDataVector> v = vectors.collect( Collectors.toList() );
                assertThat( v )
                        .hasSize( 1 )
                        .satisfiesExactly( vector -> {
                            assertThat( vector.getDesignElement().getName() ).isEqualTo( "SLCO3A1" );
                            assertThat( vector.getOriginalDesignElement() ).isEqualTo( "SLCO3A1" );
                            assertThat( vector.getDataAsDoubles() )
                                    .hasSize( 60 )
                                    .usingComparatorWithPrecision( 0.00000001 )
                                    .containsExactly( 1.3746704255794753, 0.3528762689055167, 1.2686111607353916, 0.8369941121819804, 0.8096822440089331, 1.189743096791675, 0.3554619839116539, 0.7549583134112616, 1.0395058886552426, 0.6767597674068522, 0.9992692732369385, 0.39051215823682767, 0.5028740781699685, 0.5704111114098488, 0.7122815983068315, 1.0005248669579279, 0.9017513483943169, 1.4048782927520853, 0.7246730672907271, 1.1954195609383826, 0.9042812403576517, 0.7186910883498587, 0.8754570708525086, 0.7727145540332053, 0.23376983378330393, 1.3960480771579002, 0.6050393661256476, 0.8148388704750358, 0.5459014185781306, 0.6340705809018696, 1.208930533226765, 0.7653234520936772, 0.8287030575836597, 0.8682524973241574, 0.8420750270558868, 0.7013980942153507, 1.507802159022156, 0.9080604772845193, 0.6027463528053245, 0.748796847566076, 0.7101189007663451, 1.021765039066014, 0.8028628966055134, 0.8064426530224313, 0.39845672834965723, 0.7365002604921542, 0.6033648257916698, 0.5554795302904862, 1.1330746770067306, 0.0, 0.7088592671395884, 0.6152600808118198, 1.5843188523387683, 0.7178833324887561, 1.1720673685656684, 0.5266727695315606, 0.0, 0.5817358478762545, 0.6674277001523695, 0.4217527063654217 );
                            assertThat( vector.getDataIndices() )
                                    .hasSize( 60 )
                                    .containsExactly( 0, 1, 2, 4, 6, 7, 9, 10, 11, 12, 13, 14, 15, 19, 20, 22,
                                            23, 25, 26, 28, 29, 30, 34, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 48,
                                            49, 50, 51, 53, 54, 55, 56, 58, 59, 60, 61, 62, 63, 64, 65, 67, 68, 70, 71,
                                            73, 74, 75, 77, 79, 80 );
                        } );
            }
        }
    }

    @Test
    public void testLoadSpecificSamples() throws IOException {
        try ( AnnDataSingleCellDataLoader loader = createLoader() ) {
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
            assertThat( dim.getBioAssaysOffset() )
                    .hasSize( 2 )
                    .containsExactly( 0, 24 );
            assertThat( dim.getCellIds() )
                    .hasSize( 35 )
                    .containsExactly( "GTAGAGGCACCTGTCT_5", "GACTATGAGACTCCGC_5", "TTCGATTCAGCAAGAC_5",
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
            loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "test", elementsMapping ) );
            assertThat( loader.loadVectors( elementsMapping.values(), dim, qt ) )
                    .first().satisfies( v -> {
                        assertThat( v.getDesignElement().getName() ).isEqualTo( "SLCO3A1" );
                        assertThat( v.getDataAsDoubles() )
                                .usingComparatorWithPrecision( 0.00000001 )
                                .containsExactly( 0.6739648514595455, 0.7677706254995275, 0.6383317723172576, 0.5878103695891294, 0.8011261489030318, 0.0, 1.3309770937292646, 0.5760162923898698, 0.5934602352497815, 0.9103620079492927, 1.0648371080601746, 0.9262942145070746, 0.7022630667714341, 1.0986789597795417, 0.8775858333963932, 0.8038433069152187, 1.2648576336817823, 0.7208544003194703, 0.9766299840448736, 0.0, 0.7585615525610088, 1.1335982621532354, 0.6050393661256476 );
                        assertThat( v.getDataIndices() )
                                .containsExactly( 0, 1, 5, 6, 7, 8, 10, 12, 13, 15, 16, 17, 18, 19, 25, 26, 27, 28, 29, 30, 31, 32, 33 );
                    } );
        }
    }

    @Test
    public void testLoadSampleInDifferentOrder() throws IOException {
        try ( AnnDataSingleCellDataLoader loader = createLoader() ) {
            Collection<BioAssay> bas = new HashSet<>();
            for ( String sampleName : loader.getSampleNames() ) {
                bas.add( BioAssay.Factory.newInstance( sampleName, null, BioMaterial.Factory.newInstance( sampleName ) ) );
            }

            SingleCellDimension dimension = loader.getSingleCellDimension( bas );

            assertThat( dimension.getBioAssays() ).hasSize( 22 )
                    .extracting( ba -> ba.getSampleUsed().getName() )
                    .startsWith( "C-1034", "C-1252", "C-1262", "C-1366" );
            assertThat( dimension.getCellIds() )
                    .startsWith(
                            "CCTCTAGCAAGTGATA_1", "GGGATGACAGTCAGCC_1", "AGACAGGGTACCTATG_1",
                            "GCGAGAATCCGGTAAT_1", "CAGGTATCACGGTAGA_1", "TGCAGGCTCCGTAGGC_1",
                            "GACACGCCAAGGGTCA_1", "CAGATCAGTACGCTAT_1", "TGAATGCCATACGCAT_1" );
            assertThat( dimension.getNumberOfCellIds() ).isEqualTo( 1000 );

            //  reverse the BAs
            ArrayList<BioAssay> reversedBas = new ArrayList<>( dimension.getBioAssays() );
            Collections.reverse( reversedBas );
            dimension.setBioAssays( reversedBas );

            Set<QuantitationType> qts = loader.getQuantitationTypes();

            Map<String, CompositeSequence> elementsMapping = new HashMap<>();
            elementsMapping.put( "SLCO3A1", CompositeSequence.Factory.newInstance( "SLCO3A1" ) );
            loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "test", elementsMapping ) );


            QuantitationType qt = qts.iterator().next();
            try ( Stream<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping.values(), dimension, qt ) ) {
                List<SingleCellExpressionDataVector> v = vectors.collect( Collectors.toList() );
                assertThat( v )
                        .hasSize( 1 )
                        .satisfiesExactly( vector -> {
                            assertThat( vector.getDesignElement().getName() ).isEqualTo( "SLCO3A1" );
                            assertThat( vector.getOriginalDesignElement() ).isEqualTo( "SLCO3A1" );
                            assertThat( vector.getDataAsDoubles() )
                                    .hasSize( 779 )
                                    .usingComparatorWithPrecision( 0.00000001 )
                                    .containsExactly( 0.7977567735549889, 0.7774502838894912, 1.2316817727254563, 0.7256343394218843, 0.47168976718586614, 1.0033539299428842, 0.0, 0.7379785938566755, 0.7394059290476955, 0.9079427849759827, 1.0322366290897975, 0.663522462775021, 0.4275831912506782, 0.5244118960779243, 0.34817194302493165, 0.8746412363014412, 0.1788203003178712, 0.6613380041051704, 0.9089442092350636, 0.9681946514857853, 0.6889240964050705, 0.7993029735361683, 0.8535172872879357, 1.0674768450836833, 0.684802895346009, 1.1478667878225994, 1.0568277510819086, 1.167091205285706, 0.5444087455876303, 1.7542917723050881, 1.4845477697547902, 1.5607154660620601, 0.8932273829432321, 0.8775858333963932, 0.8038433069152187, 1.2648576336817823, 0.7208544003194703, 0.9766299840448736, 0.0, 0.7585615525610088, 1.1335982621532354, 0.6050393661256476, 0.6980327325979178, 0.26480535802192146, 0.23899989381239284, 0.763177721047326, 0.8789836794613445, 0.24147024426751576, 0.44134718405063234, 0.6947007930206875, 1.1564563384622404, 0.9846998341413731, 0.7953492319272086, 0.8652146006131592, 1.0223344097455243, 1.1868877924410026, 0.8232782298741427, 0.9877566690895443, 0.5289250402013361, 0.8359190585549544, 0.5704111114098488, 0.10208879725270598, 0.4978890343741721, 0.7246730672907271, 0.0, 0.979321341391189, 0.8706668909026043, 0.8148388704750358, 0.6435686106573407, 0.08816434316472856, 1.3759959152357917, 0.9761657904950641, 1.0145373963931055, 0.34651458564765447, 0.5554795302904862, 0.8468409785974171, 0.6780946042950293, 0.7034026392812326, 1.218996450005765, 0.5604312911245974, 0.839227674229143, 0.9565958433543011, 0.5747571646117852, 0.7123345185081125, 0.9575839590617364, 0.0, 0.9085021137682419, 0.7483859108676736, 0.0, 0.967688459450668, 0.36213676295875263, 0.9388923036192749, 0.8416240863012958, 1.3174088805702235, 0.3554555995642188, 0.7219411581187611, 0.7616527597075402, 1.232771421347873, 0.12728482961278506, 0.6484506519365258, 0.418902002780407, 0.5466968603838761, 0.1926055102165597, 0.5459014185781306, 0.49164358110887835, 0.48860475229223405, 0.7256343394218843, 0.4517774286387227, 0.3099223769234711, 0.49063454938285966, 0.5060189379849324, 0.418563965735516, 0.9124083916237119, 0.9319313244152274, 0.5647857703563695, 0.69384791636118, 1.0876666433213498, 0.9546261736708536, 0.418902002780407, 0.981976532520958, 1.4751163030370638, 0.0, 1.0271444699370522, 0.0, 0.48977651720718196, 1.4468998655844605, 0.9970514987671377, 0.6461081158113543, 1.3734579763699275, 0.8896627996823039, 0.43985763806792005, 0.7049527318779053, 0.8739897313953633, 0.5822235640981832, 1.1790359025567543, 0.3279202269027818, 0.845245993038644, 1.023018564599321, 0.8681722624322963, 0.9820809742916952, 1.1633708560162728, 0.46560592797623324, 0.8390037506345203, 0.6964635998601058, 1.3830034082618987, 0.8294202892797977, 0.7178833324887561, 0.6050393661256476, 0.2456944484936158, 0.24147024426751576, 0.16960729445900247, 0.26480535802192146, 0.8104444120968717, 0.43175436632806186, 0.6678439882919354, 0.2817854800730078, 0.6050393661256476, 0.8725276100499626, 0.12183310451514716, 0.34715856888407215, 0.17472752362878496, 0.21614602878428402, 0.4697038241168223, 0.14292669721382034, 0.393448258666501, 0.23647301873989046, 0.1788203003178712, 0.6613380041051704, 0.21014137688853995, 0.7163805749657525, 1.0243898615374172, 0.7656310181332674, 0.7250023468011634, 0.9696150283481986, 0.21392724959077553, 1.091135247283271, 0.6317927312120414, 0.6227276828015903, 0.8120429347021177, 0.5776925369494512, 0.9485267353072241, 1.4147817952167954, 0.2584119821042715, 0.2052584282452627, 0.3149509706753488, 0.9296973921542704, 0.5028740781699685, 0.7180985488953054, 0.6340705809018696, 0.08816434316472856, 0.7101189007663451, 0.22024680799921428, 0.12129600692583506, 0.5188593733176227, 0.6446851653504534, 0.24018230652631697, 0.1358181665739182, 0.29271072425539657, 0.7428551970196062, 0.6214089718770475, 0.3102519285706663, 0.19071534093933318, 0.3485765130742611, 0.3279202269027818, 0.8486334198511679, 0.37618379775275235, 0.9093867644441443, 0.2827519601785218, 0.808783445587906, 1.144426617165647, 0.12166539904414046, 0.5822933064860112, 0.507303933953493, 0.3111423292943684, 0.24025188524785696, 1.7000594688291029, 0.2460803113509777, 1.0733964998404733, 0.30053558553256066, 0.38085979219095667, 0.9855936848747291, 0.7079698388613953, 0.5060189379849324, 0.8773806664407151, 0.1650286513902909, 0.22646239104231322, 0.2822859347565325, 0.7987644497541281, 0.18728337746961438, 2.2199436072345713, 0.3498948579834142, 0.23834169638641559, 0.7053672918947206, 0.8294931252184805, 0.22962209425696442, 0.2679205380972798, 0.5827470510606237, 0.4522346143676621, 0.5732671648991057, 1.948313996366708, 0.0818445778712558, 0.2861651686334927, 0.3691112431171144, 0.9502261239247911, 0.9388923036192749, 1.2920562137441334, 0.9396515279365568, 0.9058591531643394, 0.3554555995642188, 0.9960812009286217, 0.3055179890612196, 0.23640000667022715, 1.1356740524666784, 0.7616527597075402, 0.7233044216253766, 0.4358256235177118, 0.6788598252849458, 0.3809405583621015, 0.23899989381239284, 0.24836549905138872, 0.15078545718437286, 0.6030835397009857, 0.16914144288881702, 0.48871030177426367, 0.0, 0.32836568137200306, 0.7776409810422437, 0.7256343394218843, 0.34715856888407215, 0.721451694455864, 0.7865205561376537, 0.6290815264942431, 0.25183402449212106, 0.970428689693086, 0.18218832116655084, 0.5874313304398211, 0.1077758041801382, 0.14292669721382034, 0.27868300040167765, 0.5758798972565586, 0.3691525869000966, 0.6059275439948288, 0.1788203003178712, 0.9860676096733152, 1.1339555586720977, 0.19295220394143436, 0.7743872154247868, 1.3821090037396973, 0.5248061146314854, 0.7993029735361683, 0.7865856787871349, 1.0479685558493548, 0.7920967575234693, 0.0, 0.8602274898475109, 0.21879532407166336, 0.18855485606720482, 1.1734537376943868, 0.8404240725821372, 0.8766838611188735, 0.9151821673515372, 1.7434408919572473, 0.19655901353820643, 0.26117433234617005, 1.2790879750717803, 1.8333588808717303, 0.5880356622444696, 0.170844677100676, 0.6979312763017662, 0.3553598621543675, 0.9489630971923699, 0.5464824693809106, 0.18024879181147566, 1.528887744557818, 0.1691816003809188, 1.1211778381551314, 0.5911485535856058, 0.11617476986648025, 0.8497434099438383, 0.1570272908079368, 0.4473991625010104, 0.55111461345011, 0.28131867015810885, 0.13037404123310456, 0.6281423502509494, 0.6861709258608849, 0.1685328238279738, 1.2493804369193349, 0.3273678238767094, 0.555036689696349, 0.4960068320292003, 0.2688008117222814, 0.9591695184863753, 1.0161089522440132, 0.48823974745639365, 0.9185164975236834, 0.9636593438402915, 0.9172518083537057, 0.3392669026482406, 0.2951384123639238, 1.0406493233222025, 0.3102519285706663, 1.0271444699370522, 0.13347427424666902, 0.8585002952603487, 0.3540376545629321, 0.18542359594649419, 1.0711209308034144, 0.6846079248406023, 1.352897163214561, 0.9279369796965173, 0.44910178505742093, 0.4383587462322929, 0.6369943288196231, 1.0763196173518712, 0.20567785011193024, 0.30258347859403356, 0.7836663348974252, 0.2827519601785218, 0.221397380539542, 0.8349207684873312, 1.3859194782663904, 0.635236745627966, 0.7283123998236541, 0.4255878850852866, 0.782762732122225, 1.131530837342478, 0.13195036462720386, 1.189743096791675, 0.25986384604763957, 0.10208879725270598, 0.6739648514595455, 0.7677706254995275, 0.6383317723172576, 0.5878103695891294, 0.8011261489030318, 0.0, 1.3309770937292646, 0.5760162923898698, 0.5934602352497815, 0.9103620079492927, 1.0648371080601746, 0.9262942145070746, 0.7022630667714341, 1.0986789597795417, 1.0375016401106372, 1.6299081260285349, 0.7677912595189804, 1.0595708564980364, 0.7865856787871349, 0.7935093552943617, 1.040392798193749, 1.2765668849219054, 1.4341709181910012, 1.4718312107642537, 0.0, 0.7622010095366862, 0.7453479461532957, 0.8288235440038778, 0.624494977095304, 0.681407477491389, 0.8800558206380173, 0.8681900910472211, 1.444914878624758, 0.9532525835638711, 1.0222584538560957, 0.0, 0.5393538861051339, 1.4147817952167954, 0.9643116161714117, 0.5847451601967367, 0.6525265003351229, 0.7790903041773067, 1.2006155223516737, 0.6617381974132832, 0.6924479147605113, 0.6149009102007272, 0.8912715881989353, 0.2861651686334927, 0.4642414613091288, 0.8498711921681003, 0.376822091614816, 0.6159562279387256, 0.967688459450668, 0.0, 0.36213676295875263, 1.2565256720442, 0.695455144054953, 0.5763575708435532, 0.49484614182498915, 0.9960812009286217, 1.243905514661307, 1.1736851539410897, 0.675667367486575, 0.5917968389181568, 0.6096030945202237, 1.1183274071712828, 0.6289588607917386, 1.2248211390182802, 1.4060396185814157, 0.8773806664407151, 2.354934100783185, 0.5674848805597531, 0.6632029648258991, 0.8373654988245186, 0.8541368074152396, 0.540990635502385, 1.003541521341774, 1.625632598354081, 0.49948852032060564, 1.5455856293004595, 0.5667141488006193, 0.4917915995069276, 0.5021777711298612, 1.0848485928243305, 1.2222810107491848, 1.264492779064545, 1.0467675564736343, 0.7079593888331221, 1.5013997073016203, 1.034452529467075, 0.0, 0.9956506126177807, 0.8447914506444336, 1.1546675140207199, 0.7250023468011634, 0.9696150283481986, 0.9815068410328767, 0.9151821673515372, 1.695685015835958, 0.8343180875982097, 0.639409862352554, 0.9558561940720176, 0.8883830239038812, 0.44500050899035914, 0.6583136623676002, 0.914554256481976, 0.0, 0.9752388291098582, 0.6226315818007484, 0.8782430538951654, 0.3553598621543675, 0.7985627002055745, 0.4469721699901201, 1.2040078054634766, 0.9531546325059792, 1.0705186929354689, 0.5667251441213518, 0.7721364107146215, 0.40876997037957663, 0.6875446472510506, 1.4837563944377932, 0.7771961737067624, 0.6317927312120414, 0.6342785000125705, 0.548102315260738, 0.24025188524785696, 0.8350685041850392, 0.44340588431043154, 1.0733964998404733, 0.5492727528961355, 0.8810479619275524, 0.38352117241105377, 0.0, 0.8891560455511937, 1.3498561744418351, 0.38085979219095667, 0.9511026447932145, 1.091266613390511, 0.6227276828015903, 0.5047926216431747, 1.3798788225168617, 0.7727772988706403, 0.7058602417362126, 0.4994579559297951, 0.8563126570623747, 0.5927720151003226, 0.6043608719301281, 0.47168976718586614, 1.2212682644625987, 1.7066734937877484, 0.8386308254460758, 1.0033539299428842, 1.452893278653912, 0.63679654839362, 0.7394059290476955, 0.8069181267560969, 0.9079427849759827, 0.47098822528392387, 0.6755606987660788, 0.5874313304398211, 0.8452291488995305, 0.1077758041801382, 0.714937684918821, 1.097741253105532, 0.6079215115459419, 0.27868300040167765, 0.5244118960779243, 0.716112915252836, 0.5187630333750476, 0.5758798972565586, 1.3904335956280698, 0.5781904926276439, 1.4669784233562013, 0.4621084925075116, 0.7260471283404601, 0.6613380041051704, 0.8757838578901117, 0.9860676096733152, 0.681407477491389, 0.19295220394143436, 1.013324924085914, 0.8812135502352436, 0.963458835671719, 0.9242752339445246, 1.154221266132434, 0.9089442092350636, 1.3067005469314699, 0.9681946514857853, 0.0, 0.18334056951252548, 1.1479157493332999, 1.1835758468135555, 1.380769897648095, 0.7993029735361683, 0.9383237482190459, 1.3066023214725306, 1.1240910836240543, 1.547475918747013, 0.7920967575234693, 0.0, 0.4954216181568629, 0.2582140446126171, 0.579862498685217, 0.21879532407166336, 1.0246187992699753, 0.7656310181332674, 0.6654461713098303, 0.6752409022051319, 0.3471342237387782, 0.572895904086047, 1.711489875563755, 0.9923271486909491, 0.9257234764784316, 0.5006246523631008, 0.49315268347271535, 0.6950023291277974, 0.4969797274007035, 0.913301193025789, 1.3310693041702646, 1.0538971591618969, 0.750232954701149, 1.0892997585184276, 0.6488552413357285, 0.7426049563300692, 1.0443745324292255, 0.7359899703964766, 0.7123345185081125, 0.6734464123850017, 0.17702072790067763, 0.8372381269953062, 1.3782671261365715, 1.0664347018374793, 1.1443828476239895, 0.6550212851327208, 2.1597201818777654, 0.5170989329107145, 1.39020708243973, 0.763177721047326, 0.7213701844711812, 0.5984840847366603, 0.31457635751767554, 1.0286267847800221, 0.6055177667131247, 0.8229199218723373, 0.6468581304775671, 0.3809405583621015, 0.7323263577118915, 0.8794781683923961, 0.9444266895916118, 0.587147380878429, 0.8337684832066853, 0.8755387433044344, 0.5589362783332745, 1.2648576336817823, 0.6030835397009857, 1.3045434623969163, 0.4380953125959903, 1.3408959827898994, 1.1849139121962866, 1.1103940599911128, 0.0, 0.9620078322661108, 0.9563984832405599, 1.2753703160253935, 1.711118194946114, 1.061476943125704, 1.3746704255794753, 0.3528762689055167, 1.2686111607353916, 0.8369941121819804, 0.8096822440089331, 1.189743096791675, 0.3554619839116539, 0.7549583134112616, 1.0395058886552426, 0.6767597674068522, 0.9992692732369385, 0.39051215823682767, 0.5028740781699685, 0.5704111114098488, 0.7122815983068315, 1.0005248669579279, 0.9017513483943169, 1.4048782927520853, 0.7246730672907271, 1.1954195609383826, 0.9042812403576517, 0.7186910883498587, 0.8754570708525086, 0.7727145540332053, 0.23376983378330393, 1.3960480771579002, 0.6050393661256476, 0.8148388704750358, 0.5459014185781306, 0.6340705809018696, 1.208930533226765, 0.7653234520936772, 0.8287030575836597, 0.8682524973241574, 0.8420750270558868, 0.7013980942153507, 1.507802159022156, 0.9080604772845193, 0.6027463528053245, 0.748796847566076, 0.7101189007663451, 1.021765039066014, 0.8028628966055134, 0.8064426530224313, 0.39845672834965723, 0.7365002604921542, 0.6033648257916698, 0.5554795302904862, 1.1330746770067306, 0.0, 0.7088592671395884, 0.6152600808118198, 1.5843188523387683, 0.7178833324887561, 1.1720673685656684, 0.5266727695315606, 0.0, 0.5817358478762545, 0.6674277001523695, 0.4217527063654217, 0.6846079248406023, 0.9777149558823675, 0.6651798284611002, 0.0, 1.5196397688235346, 0.5822235640981832, 1.728234043852964, 1.263144882023977, 0.30300383760200855, 0.3279202269027818, 0.5342626698400788, 1.0641083510019487, 1.2966707851337258, 0.9747417993340799, 1.061084143773904, 0.26903362232634026, 0.6678285606108672, 1.2561662209875073, 0.6330662396531979, 0.9074555493714543, 0.2827519601785218, 0.8616461433822976, 1.6866597437659954, 1.0927798225054168, 2.3566623728050287, 0.635236745627966, 0.9085683988095604, 1.7364702148108102, 1.3232427472144426, 0.6964635998601058, 1.3072902529463095, 0.2352657427340162, 1.216399722490458, 0.6633550669265769, 1.232685317705865, 0.6629749471680995, 0.6214089718770475, 0.979165635925326, 0.8005174515571168, 0.500279425988066, 0.653631220873666, 0.22646239104231322, 0.6953544654934454, 0.7031793233006957, 0.0, 1.5614093509795126, 1.2239448696999637, 1.0022611579964085, 0.5139112178427778, 0.801532488920766, 1.7163399139696798, 0.69384791636118, 0.6849491989463129, 1.2151476854507086, 0.9636593438402915, 0.9546261736708536, 0.0, 0.5561768888258394, 1.2010756174555461, 1.221015371852409, 0.4736700291346131, 0.4764525169601458, 1.0495343533768444, 0.7941511851031798, 1.7720349638802764, 0.9818721147185049, 0.5466968603838761, 1.9016482659116143, 1.310247898921823, 0.9045438182837546, 1.260676109564062, 0.8416616451736589, 0.0, 0.9678908678522468, 0.3485765130742611, 0.7379785938566755, 0.3745443617513051, 0.5960374809914485, 0.5536967766186225, 0.6321367725708132, 1.0807371319163748, 0.49752658891986723 );
                            assertThat( vector.getDataIndices() )
                                    .hasSize( 779 )
                                    .containsExactly( 2, 3, 4, 5, 7, 8, 9, 10, 11, 13, 14, 16, 17, 18, 19, 20, 21, 22, 24, 25, 27, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 63, 64, 65, 66, 67, 68, 69, 70, 71, 110, 112, 114, 191, 192, 193, 194, 196, 197, 251, 252, 253, 255, 256, 257, 259, 260, 262, 264, 265, 267, 268, 269, 270, 273, 275, 276, 277, 278, 279, 281, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 297, 298, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 357, 358, 359, 360, 361, 364, 365, 366, 368, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 396, 397, 399, 400, 401, 402, 403, 404, 405, 406, 409, 410, 411, 412, 414, 415, 417, 419, 420, 421, 422, 423, 424, 425, 426, 428, 429, 431, 432, 433, 434, 436, 437, 438, 439, 440, 443, 447, 448, 450, 451, 452, 453, 454, 456, 457, 458, 459, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 499, 500, 501, 502, 503, 504, 506, 508, 509, 513, 515, 516, 518, 520, 523, 524, 526, 527, 528, 529, 530, 531, 532, 533, 535, 536, 537, 538, 539, 540, 541, 545, 548, 550, 551, 552, 553, 554, 555, 556, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567, 568, 569, 571, 572, 573, 574, 575, 577, 579, 580, 564, 566, 567, 568, 569, 570, 571, 572, 574, 575, 576, 578, 579, 580, 581, 582, 584, 586, 587, 588, 589, 590, 592, 593, 594, 595, 596, 598, 599, 600, 601, 602, 603, 605, 607, 609, 610, 611, 612, 613, 615, 616, 617, 618, 619, 622, 623, 625, 626, 628, 632, 636, 637, 638, 639, 640, 643, 597, 598, 599, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610, 612, 614, 616, 619, 620, 621, 622, 623, 628, 629, 677, 678, 679, 680, 681, 682, 683, 685, 686, 687, 688, 689, 690, 691, 692, 693, 695, 696, 697, 698, 699, 700, 701, 702, 703, 705, 706, 707, 708, 710, 711, 712, 713, 714, 715, 716, 717, 735, 736, 740, 741, 742, 743, 745, 747, 748, 750, 751, 752, 753, 754, 757, 759, 761, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 783, 784, 786, 787, 788, 789, 791, 792, 794, 795, 798, 799, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 810, 811, 813, 814, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792, 794, 795, 796, 797, 819, 820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 831, 832, 833, 834, 836, 837, 840, 841, 842, 843, 845, 846, 847, 849, 850, 851, 852, 853, 854, 855, 857, 858, 859, 860, 863, 864, 865, 866, 867, 868, 870, 871, 872, 875, 876, 877, 879, 880, 881, 882, 883, 886, 886, 888, 889, 890, 891, 894, 895, 896, 897, 898, 899, 900, 901, 903, 904, 905, 906, 907, 909, 910, 911, 913, 914, 916, 917, 918, 919, 920, 923, 924, 926, 927, 928, 929, 930, 931, 932, 933, 934, 935, 937, 940, 941, 942, 943, 944, 945, 946, 947, 948, 950, 952, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 966, 967, 968, 969, 971, 972, 973, 974, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 944, 945, 946, 947, 948, 950, 952, 953, 954, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 967, 968, 969, 970, 971, 972, 973, 974, 975, 976, 977, 978, 979, 980, 981, 983, 984, 985, 987, 988, 989, 991, 992, 993, 941, 942, 943, 945, 947, 948, 950, 951, 952, 953, 954, 955, 956, 960, 961, 963, 964, 966, 967, 969, 970, 971, 975, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986, 988, 989, 990, 991, 992, 994, 995, 996, 997, 999, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1008, 1009, 1011, 1012, 1014, 1015, 1016, 1018, 1020, 1021, 947, 948, 949, 950, 952, 953, 954, 957, 958, 959, 961, 963, 964, 965, 967, 968, 969, 971, 972, 973, 974, 975, 976, 977, 978, 980, 981, 982, 983, 984, 988, 989, 990, 991, 992, 993, 959, 960, 961, 963, 964, 965, 966, 967, 969, 970, 971, 972, 973, 974, 975, 977, 979, 980, 982, 983, 984, 985, 986, 987, 989, 990, 994, 995, 996, 999, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1013, 1014, 1015, 1019 );
                        } );
            }
        }
    }

    /**
     * AnnData on-disk format was formalized in the 0.8.x series. This file was generated with 0.7.x.
     */
    @Test
    public void testGSE216457() throws IOException {
        SingleCellDataTransformationPipeline transformation = new SingleCellDataTransformationPipeline( Arrays.asList(
                new SingleCellDataUnraw(),
                new SingleCellDataTranspose()
        ) );
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE216457.h5ad" ).getFile().toPath();
        Path dataPath2 = Files.createTempFile( null, null );
        transformation.setPythonExecutable( pythonExecutable );
        transformation.setInputFile( dataPath, SingleCellDataType.ANNDATA );
        transformation.setOutputFile( dataPath2, SingleCellDataType.ANNDATA );
        transformation.perform();
        try ( AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( dataPath2 ) ) {
            loader.setSampleFactorName( "batch" );
            Set<CompositeSequence> designElements = Collections.singleton( CompositeSequence.Factory.newInstance( "CDH1" ) );
            loader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( designElements ) );
            loader.setBioAssayToSampleNameMapper( new RenamingBioAssayMapper( new SimpleBioAssayMapper(), new String[] { "test" }, new String[] { "0" } ) );
            Collection<BioAssay> bas = Arrays.asList( BioAssay.Factory.newInstance( "test", null, BioMaterial.Factory.newInstance( "test" ) ) );
            SingleCellDimension dim = loader.getSingleCellDimension( bas );
            QuantitationType qt = loader.getQuantitationTypes().iterator().next();
            assertThat( loader.getSequencingMetadata( dim ) ).isEmpty();
            assertThat( loader.loadVectors( designElements, dim, qt ) ).singleElement()
                    .satisfies( vec -> {
                        assertThat( vec.getDataAsFloats() ).isEmpty();
                    } );
        }
    }

    @Test
    public void testRawDataset() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE216457.h5ad" ).getFile().toPath();
        try ( AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( dataPath ) ) {
            loader.setTranspose( true );
            loader.setSampleFactorName( "batch" );
            Set<CompositeSequence> designElements = Collections.singleton( CompositeSequence.Factory.newInstance( "SERPINE2" ) );
            loader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( designElements ) );
            loader.setBioAssayToSampleNameMapper( new RenamingBioAssayMapper( new SimpleBioAssayMapper(), new String[] { "test" }, new String[] { "0" } ) );
            Collection<BioAssay> bas = Arrays.asList( BioAssay.Factory.newInstance( "test", null, BioMaterial.Factory.newInstance( "test" ) ) );
            SingleCellDimension dim = loader.getSingleCellDimension( bas );

            // raw.X and raw.var are not accessible as to prevent unintentional loading of filtered values
            assertThatThrownBy( loader::getQuantitationTypes ).isInstanceOf( IllegalArgumentException.class );
            assertThatThrownBy( loader::getGenes ).isInstanceOf( IllegalArgumentException.class );

            loader.setUseRawX( false );
            assertThat( loader.getGenes() ).hasSize( 100 );
            QuantitationType qt = loader.getQuantitationTypes().iterator().next();
            assertThat( qt.getName() ).isEqualTo( "AnnData" );
            assertThat( qt.getDescription() ).isEqualTo( "Data from a layer located at 'X' originally encoded as an array of floats." );
            // we could load data in principle, but in transpose mode the matrix would have to be encoded in CSC
            assertThatThrownBy( () -> loader.loadVectors( designElements, dim, qt ) )
                    .isInstanceOf( UnsupportedOperationException.class );

            loader.setUseRawX( true );
            assertThat( loader.getGenes() ).hasSize( 21978 );
            QuantitationType qt2 = loader.getQuantitationTypes().iterator().next();
            assertThat( qt2.getName() ).isEqualTo( "AnnData" );
            assertThat( qt2.getDescription() ).isEqualTo( "Data from a layer located at 'raw/X' originally encoded as an csr_matrix of floats." );

            loader.getSequencingMetadata( dim );

            // we could load data in principle, but in transpose mode the matrix would have to be encoded in CSC
            assertThatThrownBy( () -> loader.loadVectors( designElements, dim, qt2 ) )
                    .isInstanceOf( UnsupportedOperationException.class );
        }
    }

    @Test
    public void testLayeredAnnDataFile() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE221593.h5ad" ).getFile().toPath();
        try ( AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( dataPath ) ) {
            loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
            Collection<CompositeSequence> designElements = Collections.singleton( CompositeSequence.Factory.newInstance( "PGLYRP4" ) );
            loader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( designElements ) );
            loader.setSampleFactorName( "nbatch" );
            Collection<BioAssay> bas = Arrays.asList( BioAssay.Factory.newInstance( "1 naive Egfp", null, BioMaterial.Factory.newInstance( "1 naive Egfp" ) ) );

            SingleCellDimension dim = loader.getSingleCellDimension( bas );
            assertThat( dim.getCellIds() ).hasSize( 192 );
            assertThat( loader.getGenes() ).hasSize( 1000 );

            assertThat( loader.getQuantitationTypes() )
                    .hasSize( 3 )
                    .allSatisfy( qt -> {
                        assertThat( loader.loadVectors( designElements, dim, qt ) )
                                .singleElement().satisfies( vec -> {
                                    assertThat( vec.getDesignElement() ).isEqualTo( designElements.iterator().next() );
                                } );
                    } );

            assertThat( loader.getSequencingMetadata( dim ) )
                    .containsValue( SequencingMetadata.builder().readCount( 14535L ).build() );
        }
    }

    /**
     * This test exercise the ability of the Configurer to detect datasets that need to be unrawed and transposed.
     */
    @Test
    public void testUnrawAndTranspose() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE216457.h5ad" ).getFile().toPath();
        Collection<BioAssay> bioAssays = Arrays.asList(
                BioAssay.Factory.newInstance( "0", null, BioMaterial.Factory.newInstance( "0" ) ),
                BioAssay.Factory.newInstance( "1", null, BioMaterial.Factory.newInstance( "1" ) ),
                BioAssay.Factory.newInstance( "2", null, BioMaterial.Factory.newInstance( "2" ) ),
                BioAssay.Factory.newInstance( "3", null, BioMaterial.Factory.newInstance( "3" ) )
        );
        AnnDataSingleCellDataLoaderConfigurer configurer = new AnnDataSingleCellDataLoaderConfigurer( dataPath, bioAssays, new SimpleBioAssayMapper() );
        configurer.setPythonExecutable( pythonExecutable );
        configurer.setScratchDir( Files.createTempDirectory( "gemma-scratch-" ) );
        try ( AnnDataSingleCellDataLoader loader = configurer.configureLoader( SingleCellDataLoaderConfig.builder().build() ) ) {
            assertThat( loader.getGenes() )
                    .hasSize( 21978 )
                    .contains( "SERPINE2" );
            assertThat( loader.getSampleNames() ).containsExactlyInAnyOrder( "0", "1", "2", "3" );
            SingleCellDimension dim = loader.getSingleCellDimension( bioAssays );
            assertThat( dim.getBioAssays() ).hasSize( 4 );
            assertThat( dim.getNumberOfCellIds() ).isEqualTo( 100 );
            Collection<QuantitationType> qts = loader.getQuantitationTypes();
            assertThat( qts ).singleElement()
                    .satisfies( qt -> {
                        assertThat( qt.getName() ).isEqualTo( "AnnData" );
                        assertThat( qt.getDescription() ).isEqualTo( "Data from a layer located at 'X' originally encoded as an csr_matrix of floats." );
                    } );
            QuantitationType qt = qts.iterator().next();
            Collection<CompositeSequence> designElements = Arrays.asList( CompositeSequence.Factory.newInstance( "CRHBP" ) );
            loader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( designElements ) );
            assertThat( loader.loadVectors( designElements, dim, qt ) ).singleElement().satisfies( vec -> {
                assertThat( vec.getDesignElement() ).isEqualTo( designElements.iterator().next() );
                assertThat( vec.getDataIndices() ).containsExactly( 19, 80 );
                assertThat( vec.getDataAsFloats() ).containsExactly( 1.1773239374160767f, 0.6338212490081787f );
            } );
        }
    }

    private AnnDataSingleCellDataLoader createLoader() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath();
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( dataPath );
        loader.setSampleFactorName( "ID" );
        loader.setCellTypeFactorName( "celltype1" );
        loader.setUnknownCellTypeIndicator( "UNK_ALL" );
        loader.setIgnoreUnmatchedSamples( true );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        return loader;
    }
}