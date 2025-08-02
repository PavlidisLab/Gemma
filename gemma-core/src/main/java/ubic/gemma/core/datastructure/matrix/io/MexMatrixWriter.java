package ubic.gemma.core.datastructure.matrix.io;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixSize;
import no.uib.cipr.matrix.io.MatrixVectorWriter;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils;
import ubic.gemma.core.analysis.preprocess.convert.UnsupportedQuantitationScaleConversionException;
import ubic.gemma.core.analysis.preprocess.convert.UnsupportedQuantitationTypeConversionException;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataIntMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.formatBioAssayFilename;
import static ubic.gemma.core.util.TsvUtils.SUB_DELIMITER;
import static ubic.gemma.core.util.TsvUtils.format;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.getSampleEnd;

/**
 * Writes {@link SingleCellExpressionDataMatrix} to the <a href="https://www.10xgenomics.com/support/software/cell-ranger/latest/analysis/outputs/cr-outputs-mex-matrices">10x MEX format</a>.
 * <p>
 * The data is written as a TAR archive containing the following entries for each bioassay: {@code {bioAssayName}/barcodes.tsv},
 * {@code {bioAssayName}/features.tsv}, {@code {bioAssayName}/matrix.mtx}. If using the directory output, individual
 * files are compressed and will have a {@code .gz} extension.
 * @author poirigui
 */
@CommonsLog
@Setter
public class MexMatrixWriter implements SingleCellExpressionDataMatrixWriter {

    private boolean autoFlush;

    @Nullable
    private ScaleType scaleType;

    /**
     * Use Ensembl gene IDs instead of gene symbols.
     */
    private boolean useEnsemblIds = false;

    /**
     * Executor to use to write MEX files in parallel.
     */
    private Executor executorService;

    @Override
    public int write( SingleCellExpressionDataMatrix<?> matrix, Writer stream ) throws IOException {
        throw new UnsupportedOperationException( "MEX is a binary format as it bundles the files in a TAR archive." );
    }

    @Override
    public int write( SingleCellExpressionDataMatrix<?> matrix, OutputStream stream ) throws IOException {
        return write( matrix, null, stream );
    }

    /**
     * Write a MEX matrix as a TAR archive to the given output stream.
     */
    public int write( SingleCellExpressionDataMatrix<?> matrix, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, OutputStream stream ) throws IOException {
        try ( TarArchiveOutputStream aos = new TarArchiveOutputStream( stream ) ) {
            List<BioAssay> bioAssays = matrix.getSingleCellDimension().getBioAssays();
            for ( int i = 0; i < bioAssays.size(); i++ ) {
                BioAssay ba = bioAssays.get( i );
                try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
                    try ( Writer writer = newWriter( baos ) ) {
                        writeBarcodes( matrix.getSingleCellDimension(), i, writer, false );
                    }
                    TarArchiveEntry entry = new TarArchiveEntry( formatBioAssayFilename( ba ) + "/barcodes.tsv.gz" );
                    entry.setSize( baos.size() );
                    aos.putArchiveEntry( entry );
                    aos.write( baos.toByteArray() );
                    aos.closeArchiveEntry();
                }
                try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
                    try ( Writer writer = newWriter( baos ) ) {
                        writeFeatures( matrix, cs2gene, writer, false );
                    }
                    TarArchiveEntry entry = new TarArchiveEntry( formatBioAssayFilename( ba ) + "/features.tsv.gz" );
                    entry.setSize( baos.size() );
                    aos.putArchiveEntry( entry );
                    aos.write( baos.toByteArray() );
                    aos.closeArchiveEntry();
                }
                try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
                    try ( Writer writer = newWriter( baos ) ) {
                        writeMatrix( matrix, i, writer, false ); // flushing is pointless when writing to a TAR
                    }
                    TarArchiveEntry entry = new TarArchiveEntry( formatBioAssayFilename( ba ) + "/matrix.mtx.gz" );
                    entry.setSize( baos.size() );
                    aos.putArchiveEntry( entry );
                    aos.write( baos.toByteArray() );
                    aos.closeArchiveEntry();
                }
                log.info( String.format( "Wrote MEX files for %s (%d/%d).", ba, i + 1, bioAssays.size() ) );
            }
        }
        return matrix.rows();
    }

    /**
     * Write a matrix to a directory.
     */
    public int write( SingleCellExpressionDataMatrix<?> matrix, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Path outputDir ) throws IOException {
        Assert.notNull( executorService, "An executor must be set before writing to a directory." );
        if ( Files.exists( outputDir ) ) {
            throw new IllegalArgumentException( "Output directory " + outputDir + " already exists." );
        }
        ExecutorCompletionService<BioAssay> completionService = new ExecutorCompletionService<>( executorService );
        List<BioAssay> bioAssays = matrix.getSingleCellDimension().getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            BioAssay ba = bioAssays.get( i );
            Path sampleDir = outputDir.resolve( formatBioAssayFilename( ba ) );
            Files.createDirectories( sampleDir );
            int finalI = i;
            completionService.submit( () -> {
                try {
                    try ( Writer baos = newWriter( Files.newOutputStream( sampleDir.resolve( "barcodes.tsv.gz" ) ) ) ) {
                        writeBarcodes( matrix.getSingleCellDimension(), finalI, baos, autoFlush );
                    }
                    try ( Writer baos = newWriter( Files.newOutputStream( sampleDir.resolve( "features.tsv.gz" ) ) ) ) {
                        writeFeatures( matrix, cs2gene, baos, autoFlush );
                    }
                    try ( Writer baos = newWriter( Files.newOutputStream( sampleDir.resolve( "matrix.mtx.gz" ) ) ) ) {
                        writeMatrix( matrix, finalI, baos, autoFlush );
                    }
                    return ba;
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } );
        }
        try {
            for ( int i = 0; i < bioAssays.size(); i++ ) {
                BioAssay ba = completionService.take().get();
                Path sampleDir = outputDir.resolve( formatBioAssayFilename( ba ) );
                log.info( String.format( "Wrote MEX files for %s to %s (%d/%d).", ba, sampleDir, i + 1, bioAssays.size() ) );
            }
        } catch ( ExecutionException e ) {
            if ( e.getCause() instanceof IOException ) {
                throw ( IOException ) e.getCause();
            } else {
                throw new RuntimeException( e.getCause() );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
        return matrix.rows();
    }

    /**
     * Writes a stream of vectors to a directory.
     *
     * @param vectors     a stream of vectors
     * @param numVecs     the total number of vectors to write
     * @param nnzBySample the number of non-zeroes by sample
     * @param cs2gene     a mapping of design elements to their corresponding gene(s)
     */
    public int write( Stream<SingleCellExpressionDataVector> vectors, int numVecs, Map<BioAssay, Long> nnzBySample, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Path outputDir ) throws IOException {
        if ( Files.exists( outputDir ) ) {
            throw new IllegalArgumentException( "Output directory " + outputDir + " already exists." );
        }
        // lookup the first vector to get the layout
        Writer features = null;
        MatrixVectorWriter[] matrices = null;
        try {
            Iterator<SingleCellExpressionDataVector> vecit = vectors.iterator();

            SingleCellExpressionDataVector firstVec = vecit.next();

            SingleCellDimension dimension = firstVec.getSingleCellDimension();
            for ( int i = 0; i < dimension.getBioAssays().size(); i++ ) {
                BioAssay ba = dimension.getBioAssays().get( i );
                Path sampleDir = outputDir.resolve( formatBioAssayFilename( ba ) );
                Files.createDirectories( sampleDir );
                try ( Writer out = newWriter( Files.newOutputStream( sampleDir.resolve( "barcodes.tsv.gz" ) ) ) ) {
                    writeBarcodes( dimension, i, out, autoFlush );
                }
            }

            // create a file for the first sample and hard-links for the remaining
            Iterator<BioAssay> it = dimension.getBioAssays().iterator();
            Path ff = outputDir.resolve( formatBioAssayFilename( it.next() ) ).resolve( "features.tsv.gz" );
            features = newWriter( Files.newOutputStream( ff ) );
            while ( it.hasNext() ) {
                Files.createLink( outputDir.resolve( formatBioAssayFilename( it.next() ) ).resolve( "features.tsv.gz" ), ff );
            }

            matrices = new FastMatrixVectorWriter[dimension.getBioAssays().size()];
            for ( int i = 0; i < dimension.getBioAssays().size(); i++ ) {
                BioAssay ba = dimension.getBioAssays().get( i );
                int numberOfCells = dimension.getNumberOfCellsBySample( i );
                matrices[i] = new FastMatrixVectorWriter( newWriter( Files.newOutputStream( outputDir.resolve( formatBioAssayFilename( ba ) ).resolve( "matrix.mtx.gz" ) ) ), autoFlush );
                MatrixInfo.MatrixField field;
                if ( scaleType != null ) {
                    // if data is converted, we always produce doubles
                    field = MatrixInfo.MatrixField.Real;
                } else {
                    switch ( firstVec.getQuantitationType().getRepresentation() ) {
                        case FLOAT:
                        case DOUBLE:
                            field = MatrixInfo.MatrixField.Real;
                            break;
                        case INT:
                        case LONG:
                            field = MatrixInfo.MatrixField.Integer;
                            break;
                        default:
                            throw new UnsupportedOperationException( "Unsupported vector representation " + firstVec.getQuantitationType().getRepresentation() );
                    }
                }
                matrices[i].printMatrixInfo( new MatrixInfo( true, field, MatrixInfo.MatrixSymmetry.General ) );
                matrices[i].printMatrixSize( new MatrixSize( numVecs, numberOfCells, nnzBySample.get( ba ).intValue() ) );
            }

            int row = 0;
            writeFeature( firstVec.getDesignElement(), cs2gene, features, autoFlush );
            writeVector( firstVec, row++, matrices );

            while ( vecit.hasNext() ) {
                SingleCellExpressionDataVector vec = vecit.next();
                writeFeature( vec.getDesignElement(), cs2gene, features, autoFlush );
                writeVector( vec, row++, matrices );
            }

            return row;
        } finally {
            if ( features != null ) {
                features.close();
            }
            if ( matrices != null ) {
                for ( MatrixVectorWriter s : matrices ) {
                    if ( s != null ) {
                        s.close();
                    }
                }
            }
        }
    }

    private void writeBarcodes( SingleCellDimension dimension, int sampleIndex, Writer out, boolean autoFlush ) throws IOException {
        for ( String cellId : dimension.getCellIdsBySample( sampleIndex ) ) {
            out.write( format( cellId ) + "\n" );
            if ( autoFlush ) {
                out.flush();
            }
        }
    }

    private void writeFeatures( SingleCellExpressionDataMatrix<?> matrix, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer out, boolean autoFlush ) throws IOException {
        List<CompositeSequence> designElements = matrix.getDesignElements();
        for ( CompositeSequence de : designElements ) {
            writeFeature( de, cs2gene, out, autoFlush );
        }
    }

    private void writeFeature( CompositeSequence de, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer out, boolean autoFlush ) throws IOException {
        String f = format( de.getName() ) + "\t" + ( cs2gene != null ? formatGenes( cs2gene.get( de ) ) : "" ) + "\t" + "Gene Expression" + "\n";
        out.write( f );
        if ( autoFlush ) {
            out.flush();
        }
    }

    private String formatGenes( @Nullable Collection<Gene> genes ) {
        if ( genes == null || genes.isEmpty() ) {
            return "";
        }
        if ( useEnsemblIds ) {
            List<Gene> sortedGenes = genes.stream()
                    .filter( gene -> gene.getEnsemblId() != null )
                    .sorted( Comparator.comparing( Gene::getEnsemblId ) )
                    .collect( Collectors.toList() );
            return formatGenesAttribute( sortedGenes, Gene::getEnsemblId );
        } else {
            List<Gene> sortedGenes = genes.stream()
                    .sorted( Comparator.comparing( Gene::getOfficialSymbol ) )
                    .collect( Collectors.toList() );
            return formatGenesAttribute( sortedGenes, Gene::getOfficialSymbol );
        }
    }

    private String formatGenesAttribute( List<Gene> genes, Function<Gene, String> func ) {
        return genes.stream().map( func ).map( TsvUtils::format ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }

    private void writeMatrix( SingleCellExpressionDataMatrix<?> mat, int sampleIndex, Writer out, boolean autoFlush ) {
        if ( mat instanceof SingleCellExpressionDataDoubleMatrix ) {
            int sampleOffset = mat.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
            int numberOfCells = mat.getSingleCellDimension().getNumberOfCellsBySample( sampleIndex );
            writeDoubleMatrix( ( ( SingleCellExpressionDataDoubleMatrix ) mat ).getMatrix(), mat.getQuantitationType(), sampleOffset, numberOfCells, out, autoFlush );
        } else if ( mat instanceof SingleCellExpressionDataIntMatrix ) {
            int sampleOffset = mat.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
            int numberOfCells = mat.getSingleCellDimension().getNumberOfCellsBySample( sampleIndex );
            if ( scaleType != null ) {
                // conversions always produce double data vectors
                writeDoubleMatrix( ( ( SingleCellExpressionDataIntMatrix ) mat ).getMatrix(), mat.getQuantitationType(), sampleOffset, numberOfCells, out, autoFlush );
            } else {
                writeIntMatrix( ( ( SingleCellExpressionDataIntMatrix ) mat ).getMatrix(), sampleOffset, numberOfCells, out );
            }
        } else {
            throw new UnsupportedOperationException( "Unsupported matrix type " + mat.getClass().getName() );
        }
    }

    private void writeDoubleMatrix( CompRowMatrix matrix, QuantitationType qt, int sampleOffset, int numberOfCells, Writer out, boolean autoFlush ) {
        int[] rowptr = matrix.getRowPointers();
        int[] colind = matrix.getColumnIndices();
        double[] data = matrix.getData();

        int nextSampleOffset = sampleOffset + numberOfCells;

        int sampleNnz = 0;
        for ( int j : colind ) {
            if ( j >= sampleOffset && j < nextSampleOffset ) {
                sampleNnz++;
            }
        }

        int[] sampleRows = new int[sampleNnz];
        int[] sampleCols = new int[sampleNnz];
        double[] sampleData = new double[sampleNnz];

        // populate
        int k = 0;
        int l = 0;
        for ( int i = 0; i < colind.length; i++ ) {
            int j = colind[i];
            while ( !( i >= rowptr[k] && i < rowptr[k + 1] ) ) {
                k++;
            }
            if ( j >= sampleOffset && j < nextSampleOffset ) {
                // rows/cols are 1-based in MTX
                sampleRows[l] = k + 1;
                sampleCols[l] = j - sampleOffset + 1; // adjust the column index to start at zero
                sampleData[l] = data[i];
                l++;
            }
        }

        if ( scaleType != null ) {
            try {
                sampleData = ScaleTypeConversionUtils.convertData( sampleData, qt, scaleType );
            } catch ( UnsupportedQuantitationScaleConversionException e ) {
                throw new RuntimeException( e );
            }
        }

        try ( MatrixVectorWriter writer = new FastMatrixVectorWriter( out, autoFlush ) ) {
            writer.printMatrixInfo( new MatrixInfo( true, MatrixInfo.MatrixField.Real, MatrixInfo.MatrixSymmetry.General ) );
            writer.printMatrixSize( new MatrixSize( matrix.numRows(), numberOfCells, sampleData.length ) );
            writer.printCoordinate( sampleRows, sampleCols, sampleData );
        }
    }

    private void writeIntMatrix( CompRowMatrix matrix, int sampleOffset, int numberOfCells, Writer out ) {
        int[] rowptr = matrix.getRowPointers();
        int[] colind = matrix.getColumnIndices();
        double[] data = matrix.getData();

        int nextSampleOffset = sampleOffset + numberOfCells;

        int sampleNnz = 0;
        for ( int j : colind ) {
            if ( j >= sampleOffset && j < nextSampleOffset ) {
                sampleNnz++;
            }
        }

        int[] sampleRows = new int[sampleNnz];
        int[] sampleCols = new int[sampleNnz];
        int[] sampleData = new int[sampleNnz];

        // populate
        int k = 0;
        int l = 0;
        for ( int i = 0; i < colind.length; i++ ) {
            int j = colind[i];
            while ( !( i >= rowptr[k] && i < rowptr[k + 1] ) ) {
                k++;
            }
            if ( j >= sampleOffset && j < nextSampleOffset ) {
                // rows/cols are 1-based in MTX
                sampleRows[l] = k + 1;
                sampleCols[l] = j - sampleOffset + 1; // adjust the column index to start at zero
                sampleData[l] = ( int ) Math.rint( data[i] );
                l++;
            }
        }

        try ( MatrixVectorWriter writer = new FastMatrixVectorWriter( out, autoFlush ) ) {
            writer.printMatrixInfo( new MatrixInfo( true, MatrixInfo.MatrixField.Integer, MatrixInfo.MatrixSymmetry.General ) );
            writer.printMatrixSize( new MatrixSize( matrix.numRows(), numberOfCells, sampleData.length ) );
            writer.printCoordinate( sampleRows, sampleCols, sampleData );
        }
    }

    private void writeVector( SingleCellExpressionDataVector vector, int row, MatrixVectorWriter[] writers ) {
        if ( scaleType != null ) {
            try {
                writeDoubleVector( vector, ScaleTypeConversionUtils.convertData( vector, scaleType ), row, writers );
            } catch ( UnsupportedQuantitationTypeConversionException e ) {
                throw new RuntimeException( e );
            }
        } else {
            switch ( vector.getQuantitationType().getRepresentation() ) {
                case FLOAT:
                    writeFloatVector( vector, vector.getDataAsFloats(), row, writers );
                    break;
                case DOUBLE:
                    writeDoubleVector( vector, vector.getDataAsDoubles(), row, writers );
                    break;
                case INT:
                    writeIntVector( vector, vector.getDataAsInts(), row, writers );
                    break;
                case LONG:
                    writeLongVector( vector, vector.getDataAsLongs(), row, writers );
                    break;
                default:
                    throw new UnsupportedOperationException( "Unsupported vector representation type " + vector.getQuantitationType().getRepresentation() );
            }
        }
    }

    private void writeFloatVector( SingleCellExpressionDataVector vector, float[] data, int row, MatrixVectorWriter[] writers ) {
        int[] colind = vector.getDataIndices();
        // the first sample always start at zero
        int start = 0;
        for ( int sampleIndex = 0; sampleIndex < vector.getSingleCellDimension().getBioAssays().size(); sampleIndex++ ) {
            int end = getSampleEnd( vector, sampleIndex, start );
            int sampleOffset = vector.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
            int sampleNnz = end - start;

            int[] sampleRows = new int[sampleNnz];
            Arrays.fill( sampleRows, row + 1 );
            int[] sampleCols = new int[sampleNnz];
            float[] sampleData = new float[sampleNnz];

            // populate
            int l = 0;
            for ( int i = start; i < end; i++ ) {
                sampleCols[l] = colind[i] - sampleOffset + 1; // adjust the column index to start at zero
                sampleData[l] = data[i];
                l++;
            }

            writers[sampleIndex].printCoordinate( sampleRows, sampleCols, sampleData );

            // use the end of the current sample as start for the next one
            start = end;
        }
    }

    private void writeDoubleVector( SingleCellExpressionDataVector vector, double[] data, int row, MatrixVectorWriter[] writers ) {
        int[] colind = vector.getDataIndices();
        // the first sample always start at zero
        int start = 0;
        for ( int sampleIndex = 0; sampleIndex < vector.getSingleCellDimension().getBioAssays().size(); sampleIndex++ ) {
            int end = getSampleEnd( vector, sampleIndex, start );
            int sampleOffset = vector.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
            int sampleNnz = end - start;

            int[] sampleRows = new int[sampleNnz];
            Arrays.fill( sampleRows, row + 1 );
            int[] sampleCols = new int[sampleNnz];
            double[] sampleData = new double[sampleNnz];

            // populate
            int l = 0;
            for ( int i = start; i < end; i++ ) {
                sampleCols[l] = colind[i] - sampleOffset + 1; // adjust the column index to start at zero
                sampleData[l] = data[i];
                l++;
            }

            writers[sampleIndex].printCoordinate( sampleRows, sampleCols, sampleData );

            // use the end of the current sample as start for the next one
            start = end;
        }
    }

    private void writeIntVector( SingleCellExpressionDataVector vector, int[] data, int row, MatrixVectorWriter[] writers ) {
        int[] colind = vector.getDataIndices();
        // the first sample always start at zero
        int start = 0;
        for ( int sampleIndex = 0; sampleIndex < vector.getSingleCellDimension().getBioAssays().size(); sampleIndex++ ) {
            int end = getSampleEnd( vector, sampleIndex, start );
            int sampleOffset = vector.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
            int sampleNnz = end - start;

            int[] sampleRows = new int[sampleNnz];
            Arrays.fill( sampleRows, row + 1 );
            int[] sampleCols = new int[sampleNnz];
            int[] sampleData = new int[sampleNnz];

            // populate
            int l = 0;
            for ( int i = start; i < end; i++ ) {
                sampleCols[l] = colind[i] - sampleOffset + 1; // adjust the column index to start at zero
                sampleData[l] = data[i];
                l++;
            }

            writers[sampleIndex].printCoordinate( sampleRows, sampleCols, sampleData );

            // use the end of the current sample as start for the next one
            start = end;
        }
    }

    private void writeLongVector( SingleCellExpressionDataVector vector, long[] data, int row, MatrixVectorWriter[] writers ) {
        int[] colind = vector.getDataIndices();
        // the first sample always start at zero
        int start = 0;
        for ( int sampleIndex = 0; sampleIndex < vector.getSingleCellDimension().getBioAssays().size(); sampleIndex++ ) {
            int end = getSampleEnd( vector, sampleIndex, start );
            int sampleOffset = vector.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
            int sampleNnz = end - start;

            int[] sampleRows = new int[sampleNnz];
            Arrays.fill( sampleRows, row + 1 );
            int[] sampleCols = new int[sampleNnz];
            long[] sampleData = new long[sampleNnz];

            // populate
            int l = 0;
            for ( int i = start; i < end; i++ ) {
                sampleCols[l] = colind[i] - sampleOffset + 1; // adjust the column index to start at zero
                sampleData[l] = data[i];
                l++;
            }

            writers[sampleIndex].printCoordinate( sampleRows, sampleCols, sampleData );

            // use the end of the current sample as start for the next one
            start = end;
        }
    }

    /**
     * Avoid the formatting overhead of {@link MatrixVectorWriter} when writing sparse real or integer data.
     */
    private static class FastMatrixVectorWriter extends MatrixVectorWriter {

        public FastMatrixVectorWriter( Writer out, boolean autoFlush ) {
            super( out, autoFlush );
        }

        @Override
        public void printCoordinate( int[] row, int[] column, int[] data, int offset ) {
            int size = row.length;
            if ( size != column.length || size != data.length )
                throw new IllegalArgumentException(
                        "All arrays must be of the same size" );
            for ( int i = 0; i < size; ++i ) {
                write( row[i] + " " + column[i] + " " + data[i] + "\n" );
            }
        }

        public void printCoordinate( int[] row, int[] column, double[] data, int offset ) {
            int size = row.length;
            if ( size != column.length || size != data.length )
                throw new IllegalArgumentException(
                        "All arrays must be of the same size" );
            for ( int i = 0; i < size; ++i ) {
                write( row[i] + " " + column[i] + " " + data[i] + "\n" );
            }
        }
    }

    private Writer newWriter( OutputStream outputStream ) throws IOException {
        return new OutputStreamWriter( new GZIPOutputStream( outputStream ), StandardCharsets.UTF_8 );
    }
}

