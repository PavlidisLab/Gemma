package ubic.gemma.core.loader.expression.singleCell;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.expression.singleCell.transform.*;
import ubic.gemma.core.loader.util.mapper.MapBasedDesignElementMapper;
import ubic.gemma.core.loader.util.mapper.RenamingBioAssayMapper;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.core.loader.util.mapper.SimpleDesignElementMapper;
import ubic.gemma.core.util.test.BaseTest5;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ContextConfiguration
/**
 * Two methods here are tagged slow and excluded by default; the other six are not. Measured warm:
 * testUnrawAndTranspose 6.21 s and testGSE216457 3.18 s against 0.9 s for the remaining six
 * together, so excluding the pair takes ~9.4 s off every run and leaves the loader's structural
 * checks guarding it. No network is involved — the cost is in-JVM work on large .h5ad payloads.
 * Run the excluded pair with mvn verify -DexcludedGroups= (everything) or
 * -Dgroups=slow -DexcludedGroups=network (slow only).
 */
public class AnnDataSingleCellDataLoaderTest extends BaseTest5 {

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, SingleCellTransformationConfig.class })
    static class CC {
    }

    @Autowired
    private SingleCellDataTransformationFactory singleCellDataTransformationFactory;

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
                                    // read independently from X/data[indptr[SLCO3A1] + k] with h5py
                                    .containsExactly( 0.32058679343687807, 2.489041734935803, 1.0352715246016675, 2.100010684718088, 2.446798480282353, 2.972731254345479, 3.0836587599890892, 2.3763581876539868, 1.1083054178737692, 1.9492174280771368, 2.5060311893290903, 0.5352290162772927, 0.3279202269027818, 3.057954473115221, 0.7420905921004042, 2.744449036302547, 2.6645745908692153, 1.244037334787663, 1.5826264447810388, 2.848261861768566, 0.8183093921425209, 0.9549351156322151, 0.30258347859403356, 3.1336043780201055, 2.444953247551195, 1.4333802215107896, 1.497885426528353, 2.4036292334626896, 2.4600472475862274, 0.0, 2.20393709026688, 0.2827519601785218, 2.7866778182210394, 0.5208909971004136, 2.2043674648485996, 2.2353799155010177, 3.135800353999809, 3.2822055921734217, 0.40255312722848324, 2.3157401931160932, 2.772120096709859, 2.959147935854633, 2.721262731093644, 0.3149509706753488, 0.9396515279365568, 1.8335796513328328, 3.0100527336155403, 3.1892936180974534, 2.4867649386327493, 1.1030317414217965, 0.7231815080025367, 1.4673635521518809, 2.643517485539627, 0.8131763399870034, 1.796916922011614, 2.372271206532833, 3.550793348702048, 2.780722676927545, 2.132907559546073, 3.0912238350096723 );
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
                                // read independently from X/data[indptr[SLCO3A1] + k] with h5py; the previous
                                // expectation here was the first 23 entries of X/data, i.e. another gene's values
                                .containsExactly( 0.0, 0.716112915252836, 2.214778478482705, 0.5758798972565586, 1.9506393066224583, 2.548130973103208, 2.651496115198279, 2.459775324928163, 1.6131459781510926, 0.8223473134665759, 0.0, 2.7186720282817123, 0.7766673371829081, 1.900744185763727, 1.2565256720442, 3.138752172049673, 2.201323948120984, 2.7090047956795558, 3.074207781957206, 2.3211079335334817, 2.734023472933365, 2.1883847407670785, 2.486512338858759 );
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

            Set<QuantitationType> qts = loader.getQuantitationTypes();
            QuantitationType qt = qts.iterator().next();

            Map<String, CompositeSequence> elementsMapping = new HashMap<>();
            elementsMapping.put( "SLCO3A1", CompositeSequence.Factory.newInstance( "SLCO3A1" ) );
            loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "test", elementsMapping ) );

            // reference: each sample's values when the dimension follows the order of the data
            Map<BioAssay, List<Double>> expectedBySample = new HashMap<>();
            try ( Stream<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping.values(), dimension, qt ) ) {
                SingleCellExpressionDataVector vector = vectors.collect( Collectors.toList() ).iterator().next();
                double[] data = vector.getDataAsDoubles();
                int[] indices = vector.getDataIndices();
                int[] offsets = dimension.getBioAssaysOffset();
                for ( int k = 0; k < offsets.length; k++ ) {
                    int lo = offsets[k];
                    int hi = k + 1 < offsets.length ? offsets[k + 1] : dimension.getNumberOfCellIds();
                    List<Double> values = new ArrayList<>();
                    for ( int n = 0; n < indices.length; n++ ) {
                        if ( indices[n] >= lo && indices[n] < hi ) {
                            values.add( data[n] );
                        }
                    }
                    expectedBySample.put( dimension.getBioAssays().get( k ), values );
                }
            }
            assertThat( expectedBySample.values().stream().mapToInt( List::size ).sum() ).isEqualTo( 779 );

            //  reverse the BAs
            ArrayList<BioAssay> reversedBas = new ArrayList<>( dimension.getBioAssays() );
            Collections.reverse( reversedBas );
            dimension.setBioAssays( reversedBas );

            // every sample must carry the same values whichever order the dimension lists it in
            try ( Stream<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping.values(), dimension, qt ) ) {
                List<SingleCellExpressionDataVector> v = vectors.collect( Collectors.toList() );
                assertThat( v ).hasSize( 1 );
                double[] data = v.get( 0 ).getDataAsDoubles();
                assertThat( data ).hasSize( 779 );
                int n = 0;
                for ( BioAssay ba : reversedBas ) {
                    List<Double> expected = expectedBySample.get( ba );
                    for ( Double e : expected ) {
                        assertThat( data[n++] ).as( "%s", ba.getName() ).isEqualTo( e );
                    }
                }
            }
        }
    }

    /**
     * AnnData on-disk format was formalized in the 0.8.x series. This file was generated with 0.7.x.
     */
    @Tag("slow")
    @Test
    public void testGSE216457() throws IOException {
        SingleCellDataTransformationPipeline transformation = singleCellDataTransformationFactory.createPipeline( Arrays.asList(
                SingleCellDataUnraw.class,
                SingleCellDataTranspose.class
        ) );
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE216457.h5ad" ).getFile().toPath();
        Path dataPath2 = Files.createTempFile( null, null );
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

            // the sum of the 'counts' layer over this sample's 192 cells, computed independently with h5py
            assertThat( loader.getSequencingMetadata( dim ) )
                    .containsValue( SequencingMetadata.builder().readCount( 79860L ).build() );
        }
    }

    /**
     * This test exercise the ability of the Configurer to detect datasets that need to be unrawed and transposed.
     */
    @Tag("slow")
    @Test
    public void testUnrawAndTranspose() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE216457.h5ad" ).getFile().toPath();
        Collection<BioAssay> bioAssays = Arrays.asList(
                BioAssay.Factory.newInstance( "0", null, BioMaterial.Factory.newInstance( "0" ) ),
                BioAssay.Factory.newInstance( "1", null, BioMaterial.Factory.newInstance( "1" ) ),
                BioAssay.Factory.newInstance( "2", null, BioMaterial.Factory.newInstance( "2" ) ),
                BioAssay.Factory.newInstance( "3", null, BioMaterial.Factory.newInstance( "3" ) )
        );
        AnnDataSingleCellDataLoaderConfigurer configurer = new AnnDataSingleCellDataLoaderConfigurer( dataPath, bioAssays, new SimpleBioAssayMapper(), singleCellDataTransformationFactory );
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