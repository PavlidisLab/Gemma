package ubic.gemma.core.loader.util;

import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixSize;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import ubic.gemma.core.util.FileUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Utilities for reading <a href="https://math.nist.gov/MatrixMarket/">MatrixMarket</a> format.
 * @author poirigui
 */
public class MatrixMarketUtils {

    public static MatrixVectorReader readMatrixMarketFromPath( Path path ) throws IOException {
        if ( path.toString().endsWith( ".gz" ) ) {
            return new MatrixVectorReader( new InputStreamReader( FileUtils.openCompressedFile( path ) ) );
        } else {
            return new MatrixVectorReader( Files.newBufferedReader( path ) );
        }
    }

    /**
     * Obtain the position of non-empty columns for a given matrix.
     */
    public static int[] getNonEmptyColumns( Path path ) throws IOException {
        try ( MatrixVectorReader reader = readMatrixMarketFromPath( path ) ) {
            MatrixInfo matrixInfo = reader.readMatrixInfo();
            MatrixSize size = reader.readMatrixSize( matrixInfo );
            int[] rows = new int[size.numEntries()];
            int[] columns = new int[size.numEntries()];
            double[] data = new double[size.numEntries()];
            reader.readCoordinate( rows, columns, data );
            return Arrays.stream( columns )
                    // mtx is 1-based
                    .map( c -> c - 1 )
                    .sorted()
                    .distinct()
                    .toArray();
        }
    }
}
