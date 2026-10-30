package ubic.gemma.core.loader.expression.singleCell.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CellLevelCharacteristicsWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    public void testWriteIsOrderedByNameAndNamesEachRow() throws IOException {
        SingleCellDimension dimension = createDimension();
        // inserted out of order, and two of them share a category
        Set<CellLevelCharacteristics> clcs = new LinkedHashSet<>( Arrays.asList(
                createUmiOutlier( "umi_outlier_b", 1, 0, 0, 1 ),
                createUmiOutlier( "umi_outlier_a", 0, 1, 1, 0 ) ) );
        StringWriter writer = new StringWriter();
        new CellLevelCharacteristicsWriter().write( clcs, dimension, writer );
        assertThat( writer.toString().split( "\r?\n" ) ).containsExactly(
                "sample_id\tcell_id\tcategory\tcategory_uri\tcategory_id\tvalue\tvalue_uri\tname",
                "A\tc1\tumi_outlier\t\t1\tfalse\t\tumi_outlier_a",
                "A\tc2\tumi_outlier\t\t1\ttrue\t\tumi_outlier_a",
                "B\tc3\tumi_outlier\t\t1\ttrue\t\tumi_outlier_a",
                "B\tc4\tumi_outlier\t\t1\tfalse\t\tumi_outlier_a",
                "A\tc1\tumi_outlier\t\t2\ttrue\t\tumi_outlier_b",
                "A\tc2\tumi_outlier\t\t2\tfalse\t\tumi_outlier_b",
                "B\tc3\tumi_outlier\t\t2\tfalse\t\tumi_outlier_b",
                "B\tc4\tumi_outlier\t\t2\ttrue\t\tumi_outlier_b" );
    }

    /**
     * A file written by the REST API can be loaded back without passing names: each characteristic comes back under
     * its own name with its own values, even when two share a category.
     */
    @Test
    public void testRoundTripWithoutNames() throws IOException {
        SingleCellDimension dimension = createDimension();
        Set<CellLevelCharacteristics> clcs = new LinkedHashSet<>( Arrays.asList(
                createUmiOutlier( "umi_outlier_b", 1, 0, 0, 1 ),
                createUmiOutlier( "umi_outlier_a", 0, 1, 1, 0 ) ) );
        Path file = tempDir.resolve( "clc.tsv" );
        StringWriter writer = new StringWriter();
        new CellLevelCharacteristicsWriter().write( clcs, dimension, writer );
        Files.writeString( file, writer.toString(), StandardCharsets.UTF_8 );

        GenericCellLevelCharacteristicsMetadataParser parser = new GenericCellLevelCharacteristicsMetadataParser(
                dimension, new SimpleBioAssayMapper(), null, null, null );
        assertThat( parser.parse( file ) )
                .satisfiesExactlyInAnyOrder( clc -> {
                    assertThat( clc.getName() ).isEqualTo( "umi_outlier_a" );
                    assertThat( valuesByCell( clc ) ).containsExactly( "false", "true", "true", "false" );
                }, clc -> {
                    assertThat( clc.getName() ).isEqualTo( "umi_outlier_b" );
                    assertThat( valuesByCell( clc ) ).containsExactly( "true", "false", "false", "true" );
                } );
    }

    @Test
    public void testExplicitNamesWinOverTheNameColumn() throws IOException {
        SingleCellDimension dimension = createDimension();
        Path file = tempDir.resolve( "clc.tsv" );
        StringWriter writer = new StringWriter();
        new CellLevelCharacteristicsWriter().write( Collections.singleton( createUmiOutlier( "from_file", 0, 1, 1, 0 ) ), dimension, writer );
        Files.writeString( file, writer.toString(), StandardCharsets.UTF_8 );

        GenericCellLevelCharacteristicsMetadataParser parser = new GenericCellLevelCharacteristicsMetadataParser(
                dimension, new SimpleBioAssayMapper(), Collections.singletonList( "given" ), null, null );
        assertThat( parser.parse( file ) )
                .singleElement()
                .satisfies( clc -> assertThat( clc.getName() ).isEqualTo( "given" ) );
    }

    @Test
    public void testConflictingNamesForOneCategoryIdAreRejected() throws IOException {
        SingleCellDimension dimension = createDimension();
        Path file = tempDir.resolve( "clc.tsv" );
        Files.writeString( file, "sample_id\tcell_id\tcategory\tcategory_uri\tcategory_id\tvalue\tvalue_uri\tname\n"
                + "A\tc1\tumi_outlier\t\t1\ttrue\t\tfirst\n"
                + "A\tc2\tumi_outlier\t\t1\tfalse\t\tsecond\n", StandardCharsets.UTF_8 );
        GenericCellLevelCharacteristicsMetadataParser parser = new GenericCellLevelCharacteristicsMetadataParser(
                dimension, new SimpleBioAssayMapper(), null, null, null );
        assertThatThrownBy( () -> parser.parse( file ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Category 1 is named both first and second." );
    }

    private SingleCellDimension createDimension() {
        SingleCellDimension dimension = new SingleCellDimension();
        dimension.setBioAssays( Arrays.asList( createBioAssay( "A" ), createBioAssay( "B" ) ) );
        dimension.setBioAssaysOffset( new int[] { 0, 2 } );
        dimension.setCellIds( IntStream.rangeClosed( 1, 4 ).mapToObj( i -> "c" + i ).collect( Collectors.toList() ) );
        dimension.setNumberOfCellIds( 4 );
        return dimension;
    }

    private BioAssay createBioAssay( String name ) {
        return BioAssay.Factory.newInstance( name, new ArrayDesign(), BioMaterial.Factory.newInstance( name ) );
    }

    /**
     * Create a boolean characteristic where {@code 1} is {@code true} and {@code 0} is {@code false}.
     */
    private CellLevelCharacteristics createUmiOutlier( String name, int... flags ) {
        List<Characteristic> values = Arrays.asList(
                Characteristic.Factory.newInstance( "umi_outlier", null, "false", null ),
                Characteristic.Factory.newInstance( "umi_outlier", null, "true", null ) );
        return CellLevelCharacteristics.Factory.newInstance( name, null, values, flags );
    }

    private List<String> valuesByCell( CellLevelCharacteristics clc ) {
        return IntStream.range( 0, clc.getIndices().length )
                .mapToObj( i -> clc.getCharacteristic( i ).getValue() )
                .collect( Collectors.toList() );
    }
}
