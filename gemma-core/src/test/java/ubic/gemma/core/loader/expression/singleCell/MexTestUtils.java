package ubic.gemma.core.loader.expression.singleCell;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MexTestUtils {

    public static MexSingleCellDataLoader createLoaderForResourceDir( String resourceDir ) throws IOException {
        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodeFiles = new ArrayList<>();
        List<Path> geneFiles = new ArrayList<>();
        List<Path> matrixFiles = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources( resourceDir + "/*" );
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
        return new MexSingleCellDataLoader( sampleNames, barcodeFiles, geneFiles, matrixFiles );
    }
}
