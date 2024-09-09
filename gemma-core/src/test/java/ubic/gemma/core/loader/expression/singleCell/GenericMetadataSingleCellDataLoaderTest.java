package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GenericMetadataSingleCellDataLoaderTest {

    @Test
    public void test() throws URISyntaxException, IOException {
        SingleCellDimension dim = new SingleCellDimension();
        dim.setBioAssays( Arrays.asList( BioAssay.Factory.newInstance( "A" ), BioAssay.Factory.newInstance( "B" ) ) );
        dim.setBioAssaysOffset( new int[] { 0, 10 } );
        dim.setCellIds( IntStream.rangeClosed( 1, 20 ).mapToObj( c -> "c" + c ).collect( Collectors.toList() ) );
        SingleCellDataLoader delegate = mock();
        GenericMetadataSingleCellDataLoader loader = new GenericMetadataSingleCellDataLoader( delegate, Paths.get( Objects.requireNonNull( getClass().getResource( "/data/loader/expression/singleCell/generic-single-cell-metadata.tsv" ) ).toURI() ) );
        loader.setBioAssayToSampleNameMatcher( ( bioAssays, sampleNameFromData ) -> bioAssays.stream().filter( ba -> ba.getName().equals( sampleNameFromData ) ).collect( Collectors.toSet() ) );
        assertThat( loader.getCellTypeAssignment( dim ) )
                .hasValueSatisfying( cta -> {
                    assertThat( cta.getCellTypes() ).extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "C", "D" );
                    assertThat( cta.getNumberOfCellTypes() )
                            .isEqualTo( 2 );
                    assertThat( cta.getCellTypeIndices() )
                            .hasSize( 20 )
                            .containsExactly( 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1 );
                    assertThat( cta.isPreferred() ).isFalse();
                    assertThat( cta.getProtocol() ).isNull();
                } );
    }
}