package ubic.gemma.core.loader.expression.singleCell;

import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixSize;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import no.uib.cipr.matrix.io.MatrixVectorWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Sample a given MEX file.
 *
 * @author poirigui
 */
public class SampleMexDataset {

    public static void main( String[] args ) {
        if ( args.length < 2 ) {
            System.out.println( "Usage: sampleMexDataset <datasetDir> <destDir>" );
            System.exit( 1 );
        }
        Path datasetDir = Paths.get( args[0] );
        Path destDir = Paths.get( args[1] );
        System.out.println( "Sampling MEX dataset from " + datasetDir );
        try ( Stream<Path> stream = Files.list( datasetDir ) ) {
            Map<String, List<Path>> f = stream
                    .collect( Collectors.groupingBy( r -> r.getFileName().toString().split( "_", 2 )[0], Collectors.toList() ) );
            for ( Map.Entry<String, List<Path>> entry : f.entrySet() ) {
                String sampleName = entry.getKey();
                List<Path> paths = entry.getValue();
                Path barcodeFile = paths.stream().filter( p -> p.getFileName().toString().endsWith( "barcodes.tsv.gz" ) ).findFirst().orElse( null );
                Path geneFile = paths.stream().filter( p -> p.getFileName().toString().endsWith( "features.tsv.gz" ) ).findFirst().orElse( null );
                Path matrixFile = paths.stream().filter( p -> p.getFileName().toString().endsWith( "matrix.mtx.gz" ) ).findFirst().orElse( null );
                if ( barcodeFile != null && geneFile != null && matrixFile != null ) {
                    System.out.println( "Sampling MEX dataset for " + sampleName );
                    sampleMexDataset( barcodeFile, geneFile, matrixFile, destDir, 1000, 1000 );
                } else {
                    System.err.println( sampleName + " does not have all the required files." );
                }
            }
        } catch ( IOException e ) {
            e.printStackTrace( System.err );
            System.exit( 1 );
        }
    }

    private static void sampleMexDataset( Path barcodesFile, Path genesFile, Path matrix, Path destDir, int numberOfGenes, int numberOfBarcodes ) throws IOException {
        Random random = new Random();
        random.setSeed( 123L );

        List<String> barcodes = readLinesFromPath( barcodesFile );
        List<String> genes = readLinesFromPath( genesFile );

        Set<String> sampledGenes = sampleK( genes, numberOfGenes, random );
        Set<String> sampledBarcodes = sampleK( barcodes, numberOfBarcodes, random );

        int[] gI = new int[numberOfGenes];
        int k = 0;
        for ( String g : sampledGenes ) {
            gI[k++] = genes.indexOf( g );
        }
        Arrays.sort( gI );

        k = 0;
        int[] bI = new int[numberOfBarcodes];
        for ( String b : sampledBarcodes ) {
            bI[k++] = barcodes.indexOf( b );
        }
        Arrays.sort( bI );

        List<String> orderedBarcodes = new ArrayList<>();
        for ( int ix : bI ) {
            orderedBarcodes.add( barcodes.get( ix ) );
        }

        List<String> orderedGenes = new ArrayList<>();
        for ( int ix : gI ) {
            orderedGenes.add( genes.get( ix ) );
        }

        writeLinesToPath( destDir.resolve( barcodesFile.getFileName() ), orderedBarcodes );
        writeLinesToPath( destDir.resolve( genesFile.getFileName() ), orderedGenes );

        try ( MatrixVectorReader reader = readMatrixFromPath( matrix );
                MatrixVectorWriter writer = writeMatrixToPath( destDir.resolve( matrix.getFileName() ) ) ) {
            MatrixInfo mi = reader.readMatrixInfo();
            writer.printMatrixInfo( mi );
            MatrixSize ms = reader.readCoordinateSize();
            int[] rows = new int[ms.numEntries()];
            int[] columns = new int[ms.numEntries()];
            int[] data = new int[ms.numEntries()];
            reader.readCoordinate( rows, columns, data );
            int newSize = 0;
            for ( int i = 0; i < ms.numEntries(); i++ ) {
                if ( Arrays.binarySearch( gI, rows[i] - 1 ) > -1 && Arrays.binarySearch( bI, columns[i] - 1 ) > -1 ) {
                    newSize++;
                }
            }
            int[] newRows = new int[newSize];
            int[] newColumns = new int[newSize];
            int[] newData = new int[newSize];
            int j = 0;
            for ( int i = 0; i < ms.numEntries(); i++ ) {
                int newRow = Arrays.binarySearch( gI, rows[i] - 1 );
                int newCol = Arrays.binarySearch( bI, columns[i] - 1 );
                if ( newRow > -1 && newCol > -1 ) {
                    newRows[j] = newRow + 1; // 1-based (MTX format is 1-based)
                    newColumns[j] = newCol + 1; // 1-based (MTX format is 1-based
                    newData[j] = data[i];
                    j++;
                }
            }
            writer.printMatrixSize( new MatrixSize( gI.length, bI.length, newData.length ) );
            writer.printCoordinate( newRows, newColumns, newData );
        }
    }

    private static <T> Set<T> sampleK( Collection<T> collection, int k, Random random ) {
        List<T> pop = new ArrayList<>( collection );
        Collections.shuffle( pop, random );
        return new HashSet<>( pop.subList( 0, k ) );
    }

    private static MatrixVectorReader readMatrixFromPath( Path path ) throws IOException {
        if ( path.toString().endsWith( ".gz" ) ) {
            return new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( path ) ) ) );
        } else {
            return new MatrixVectorReader( Files.newBufferedReader( path ) );
        }
    }

    private static MatrixVectorWriter writeMatrixToPath( Path path ) throws IOException {
        if ( path.toString().endsWith( ".gz" ) ) {
            return new MatrixVectorWriter( new GZIPOutputStream( Files.newOutputStream( path ) ) );
        } else {
            return new MatrixVectorWriter( Files.newOutputStream( path ) );
        }
    }

    private static List<String> readLinesFromPath( Path path ) throws IOException {
        if ( path.toString().endsWith( ".gz" ) ) {
            try ( BufferedReader br = new BufferedReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( path ) ) ) ) ) {
                return br.lines().collect( Collectors.toList() );
            }
        } else {
            return Files.readAllLines( path );
        }
    }

    private static void writeLinesToPath( Path path, List<String> lines ) throws IOException {
        if ( path.toString().endsWith( ".gz" ) ) {
            try ( PrintWriter br = new PrintWriter( new GZIPOutputStream( Files.newOutputStream( path ) ) ) ) {
                for ( String line : lines ) {
                    br.println( line );
                }
            }
        } else {
            Files.write( path, lines );
        }
    }
}