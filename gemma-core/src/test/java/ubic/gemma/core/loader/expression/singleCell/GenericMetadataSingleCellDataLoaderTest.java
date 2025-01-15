package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import ubic.gemma.core.loader.expression.singleCell.metadata.GenericMetadataSingleCellDataLoader;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class GenericMetadataSingleCellDataLoaderTest {

    private ArrayDesign ad = new ArrayDesign();

    @Test
    public void test() throws URISyntaxException, IOException {
        SingleCellDimension dim = new SingleCellDimension();
        dim.setBioAssays( Arrays.asList( createBioAssay( "A" ), createBioAssay( "B" ) ) );
        dim.setBioAssaysOffset( new int[] { 0, 10 } );
        dim.setCellIds( IntStream.rangeClosed( 1, 20 ).mapToObj( c -> "c" + c ).collect( Collectors.toList() ) );
        dim.setNumberOfCells( 20 );
        SingleCellDataLoader delegate = mock();
        GenericMetadataSingleCellDataLoader loader = new GenericMetadataSingleCellDataLoader( delegate,
                Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/generic-single-cell-metadata.tsv" ) ).toURI() ),
                Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/additional-cell-type-metadata.tsv" ) ).toURI() ) );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        assertThat( loader.getCellTypeAssignments( dim ) )
                .singleElement()
                .satisfies( cta -> {
                    assertThat( cta.getCellTypes() ).extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "C", "D" );
                    assertThat( cta.getNumberOfCellTypes() )
                            .isEqualTo( 2 );
                    assertThat( cta.getCellTypeIndices() )
                            .hasSize( 20 )
                            .containsExactly( 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1 );
                    assertThat( cta.isPreferred() ).isFalse();
                    assertThat( cta.getProtocol() ).isNull();
                    assertThat( cta.getDescription() ).isEmpty();
                } );
        assertThat( loader.getOtherCellLevelCharacteristics( dim ) )
                .hasSize( 2 )
                .satisfiesExactlyInAnyOrder( clc -> {
                    assertThat( clc.getCharacteristics() )
                            .allSatisfy( c -> assertThat( c.getCategory() ).isEqualTo( "treatment" ) )
                            .extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "x", "y" );
                    assertThat( clc.getIndices() )
                            .hasSize( 20 )
                            .containsExactly( -1, 0, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1 );
                }, clc -> {
                    assertThat( clc.getCharacteristics() )
                            .allSatisfy( c -> assertThat( c.getCategory() ).isEqualTo( "genotype" ) )
                            .extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "A/a", "A/b" );
                    assertThat( clc.getIndices() )
                            .hasSize( 20 )
                            .containsExactly( -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1 );
                } );
    }

    @Test
    public void testWithUnmatchedSampleId() throws URISyntaxException, IOException {
        SingleCellDimension dim = new SingleCellDimension();
        dim.setBioAssays( Arrays.asList( createBioAssay( "A" ) ) );
        dim.setBioAssaysOffset( new int[] { 0, 10 } );
        dim.setCellIds( IntStream.rangeClosed( 1, 10 ).mapToObj( c -> "c" + c ).collect( Collectors.toList() ) );
        dim.setNumberOfCells( 10 );
        SingleCellDataLoader delegate = mock();
        GenericMetadataSingleCellDataLoader loader = new GenericMetadataSingleCellDataLoader( delegate,
                Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/generic-single-cell-metadata.tsv" ) ).toURI() ),
                Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/additional-cell-type-metadata.tsv" ) ).toURI() ) );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        // the default behavior is aligned with SingleCellDataLoader in general to ignore extraneous samples
        assertThat( loader.getCellTypeAssignments( dim ) )
                .singleElement()
                .satisfies( cta -> {
                    assertThat( cta.getCellTypes() ).extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "C" );
                    assertThat( cta.getNumberOfCellTypes() )
                            .isEqualTo( 1 );
                    assertThat( cta.getCellTypeIndices() )
                            .hasSize( 10 )
                            .containsExactly( 0, -1, -1, -1, -1, -1, -1, -1, -1, -1 );
                    assertThat( cta.isPreferred() ).isFalse();
                    assertThat( cta.getProtocol() ).isNull();
                    assertThat( cta.getDescription() ).isEmpty();
                } );
        loader.setIgnoreUnmatchedSamples( false );
        assertThatThrownBy( () -> loader.getCellTypeAssignments( dim ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testWithMissingSampleId() throws URISyntaxException, IOException {
        SingleCellDimension dim = new SingleCellDimension();
        dim.setBioAssays( Arrays.asList( createBioAssay( "A" ), createBioAssay( "B" ) ) );
        dim.setBioAssaysOffset( new int[] { 0, 10 } );
        dim.setCellIds( IntStream.rangeClosed( 1, 20 ).mapToObj( c -> "c" + c ).collect( Collectors.toList() ) );
        dim.setNumberOfCells( 20 );
        SingleCellDataLoader delegate = mock();
        GenericMetadataSingleCellDataLoader loader = new GenericMetadataSingleCellDataLoader( delegate,
                Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/generic-single-cell-metadata-without-sample-id.tsv" ) ).toURI() ),
                null );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        loader.setUseCellIdsIfSampleNameIsMissing( true );
        assertThat( loader.getCellTypeAssignments( dim ) )
                .singleElement()
                .satisfies( cta -> {
                    assertThat( cta.getCellTypes() ).extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "C", "D" );
                    assertThat( cta.getNumberOfCellTypes() )
                            .isEqualTo( 2 );
                    assertThat( cta.getCellTypeIndices() )
                            .hasSize( 20 )
                            .containsExactly( 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1 );
                    assertThat( cta.isPreferred() ).isFalse();
                    assertThat( cta.getProtocol() ).isNull();
                    assertThat( cta.getDescription() ).isEmpty();
                } );
    }

    @Test
    public void testWithDuplicateCellId() throws URISyntaxException, IOException {
        SingleCellDimension dim = new SingleCellDimension();
        dim.setBioAssays( Arrays.asList( createBioAssay( "A" ), createBioAssay( "B" ) ) );
        dim.setBioAssaysOffset( new int[] { 0, 10 } );
        dim.setCellIds( IntStream.rangeClosed( 1, 20 ).mapToObj( c -> "c" + c ).collect( Collectors.toList() ) );
        dim.setNumberOfCells( 20 );
        SingleCellDataLoader delegate = mock();
        GenericMetadataSingleCellDataLoader loader = new GenericMetadataSingleCellDataLoader( delegate,
                Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/generic-single-cell-metadata-with-duplicate-cell-ids.tsv" ) ).toURI() ),
                null );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        loader.setUseCellIdsIfSampleNameIsMissing( true );
        assertThat( loader.getCellTypeAssignments( dim ) )
                .singleElement()
                .satisfies( cta -> {
                    assertThat( cta.getCellTypes() ).extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "C" );
                    assertThat( cta.getNumberOfCellTypes() )
                            .isEqualTo( 1 );
                    assertThat( cta.getCellTypeIndices() )
                            .hasSize( 20 )
                            .containsExactly( 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 );
                    assertThat( cta.isPreferred() ).isFalse();
                    assertThat( cta.getProtocol() ).isNull();
                    assertThat( cta.getDescription() ).isEqualTo( "The following assays did not have any cells assigned:\n\tB" );
                } );
    }

    @Test
    public void testWithDuplicateCellIdButDifferentValues() throws URISyntaxException, IOException {
        SingleCellDimension dim = new SingleCellDimension();
        dim.setBioAssays( Arrays.asList( createBioAssay( "A" ), createBioAssay( "B" ) ) );
        dim.setBioAssaysOffset( new int[] { 0, 10 } );
        dim.setCellIds( IntStream.rangeClosed( 1, 20 ).mapToObj( c -> "c" + c ).collect( Collectors.toList() ) );
        dim.setNumberOfCells( 20 );
        SingleCellDataLoader delegate = mock();
        GenericMetadataSingleCellDataLoader loader = new GenericMetadataSingleCellDataLoader( delegate,
                Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/generic-single-cell-metadata-with-duplicate-cell-ids-but-different-values.tsv" ) ).toURI() ),
                null );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        loader.setUseCellIdsIfSampleNameIsMissing( true );
        assertThatThrownBy( () -> loader.getCellTypeAssignments( dim ) )
                .isInstanceOf( IllegalStateException.class );
    }

    @Test
    public void testWithBarcodeCollisions() throws IOException, URISyntaxException {
        SingleCellDimension dim = new SingleCellDimension();
        dim.setBioAssays( Arrays.asList( createBioAssay( "A" ), createBioAssay( "B" ) ) );
        dim.setBioAssaysOffset( new int[] { 0, 10 } );
        // make c1 and c11 collide
        dim.setCellIds( IntStream.rangeClosed( 1, 20 ).mapToObj( c -> "c" + ( c == 11 ? 1 : c ) ).collect( Collectors.toList() ) );
        dim.setNumberOfCells( 20 );
        SingleCellDataLoader delegate = mock();
        GenericMetadataSingleCellDataLoader loader = new GenericMetadataSingleCellDataLoader( delegate,
                Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/generic-single-cell-metadata-with-barcode-collisions.tsv" ) ).toURI() ),
                null );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        loader.setUseCellIdsIfSampleNameIsMissing( true );
        assertThat( loader.getCellTypeAssignments( dim ) )
                .singleElement()
                .satisfies( cta -> {
                    assertThat( cta.getCellTypes() ).extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "C", "D" );
                    assertThat( cta.getNumberOfCellTypes() )
                            .isEqualTo( 2 );
                    assertThat( cta.getCellTypeIndices() )
                            .hasSize( 20 )
                            .containsExactly( -1, 0, 1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1 );
                    assertThat( cta.isPreferred() ).isFalse();
                    assertThat( cta.getProtocol() ).isNull();
                } );
    }

    private BioAssay createBioAssay( String name ) {
        return BioAssay.Factory.newInstance( name, ad, BioMaterial.Factory.newInstance( name ) );
    }
}