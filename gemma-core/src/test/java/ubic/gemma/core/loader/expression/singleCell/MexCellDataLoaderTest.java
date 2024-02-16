package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class MexCellDataLoaderTest {

    private static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    @Test
    public void test() throws IOException {
        ArrayDesign platform = ArrayDesign.Factory.newInstance( "GPL12311", null );

        // consider the first file as a platform!
        ClassPathResource cpr = new ClassPathResource( "data/loader/expression/singleCell/GSE224438/GSM7022367_1_features.tsv.gz" );
        try ( BufferedReader br = new BufferedReader( new InputStreamReader( new GZIPInputStream( cpr.getInputStream() ) ) ) ) {
            br.lines().forEach( line -> platform.getCompositeSequences().add( CompositeSequence.Factory.newInstance( line.split( "\t", 2 )[0] ) ) );
        }

        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodeFiles = new ArrayList<>();
        List<Path> geneFiles = new ArrayList<>();
        List<Path> matrixFiles = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources( "data/loader/expression/singleCell/GSE224438/*" );
        Map<String, List<Resource>> f = Arrays.stream( resources )
                .collect( Collectors.groupingBy( r -> r.getFilename().split( "_", 2 )[0], Collectors.toList() ) );
        f = new TreeMap<>( f );
        for ( Map.Entry<String, List<Resource>> entry : f.entrySet() ) {
            String sampleName = entry.getKey();
            Resource barcodeFile = entry.getValue().stream()
                    .filter( p -> p.getFilename().endsWith( "barcodes.tsv.gz" ) )
                    .findFirst()
                    .orElse( null );
            Resource geneFile = entry.getValue().stream().filter( p -> p.getFilename().endsWith( "features.tsv.gz" ) ).findFirst().orElse( null );
            Resource matrixFile = entry.getValue().stream().filter( p -> p.getFilename().endsWith( "matrix.mtx.gz" ) ).findFirst().orElse( null );
            if ( barcodeFile != null && geneFile != null && matrixFile != null ) {
                sampleNames.add( sampleName );
                barcodeFiles.add( barcodeFile.getFile().toPath() );
                geneFiles.add( geneFile.getFile().toPath() );
                matrixFiles.add( matrixFile.getFile().toPath() );
            }
        }
        MexCellDataLoader loader = new MexCellDataLoader( sampleNames, barcodeFiles, geneFiles, matrixFiles );
        ArrayList<BioAssay> bas = new ArrayList<>();
        for ( String sampleName : sampleNames ) {
            bas.add( BioAssay.Factory.newInstance( sampleName, null, BioMaterial.Factory.newInstance( sampleName ) ) );
        }
        assertThat( loader.getCellTypeLabelling() ).isEmpty();
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        assertThat( qt ).isNotNull();
        assertThat( qt.getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
        SingleCellDimension dimension = loader.getSingleCellDimension( bas );
        assertThat( dimension.getCellIds() ).hasSize( 10000 );
        assertThat( dimension.getNumberOfCells() ).isEqualTo( 10000 );
        assertThat( dimension.getNumberOfCellsBySample( 0 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 1 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 9 ) ).isEqualTo( 1000 );
        assertThat( dimension.getBioAssaysOffset() )
                .containsExactly( 0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000 );
        List<SingleCellExpressionDataVector> vectors = loader.loadVectors( platform, dimension, qt ).collect( Collectors.toList() );
        assertThat( vectors )
                .hasSize( 1000 )
                .allSatisfy( v -> {
                    assertThat( v.getDesignElement() ).isNotNull();
                    assertThat( v.getSingleCellDimension() ).isEqualTo( dimension );
                    assertThat( v.getQuantitationType() ).isEqualTo( qt );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000074782" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( byteArrayConverter.byteArrayToDoubles( v.getData() ) )
                            .containsExactly( 1, 1, 1, 1, 1, 1, 1 );
                    assertThat( v.getDataIndices() )
                            .containsExactly( 38, 256, 382, 431, 788, 814, 942 );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000038206" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    int lastSampleOffset = dimension.getBioAssaysOffset()[3];
                    assertThat( byteArrayConverter.byteArrayToDoubles( v.getData() ) )
                            .hasSize( 594 );
                    assertThat( v.getDataIndices() )
                            .hasSize( 594 )
                            // from the first sample, offset is zero
                            .containsSequence( 12, 24, 59, 67, 92, 95, 103, 107 )
                            // from the last sample
                            .containsSequence( lastSampleOffset + 3, lastSampleOffset + 8, lastSampleOffset + 24, lastSampleOffset + 30, lastSampleOffset + 31, lastSampleOffset + 39, lastSampleOffset + 45, lastSampleOffset + 59 );
                } );
    }
}