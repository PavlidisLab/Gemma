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
import ubic.basecode.util.FileTools;
import ubic.gemma.core.datastructure.matrix.DoubleSingleCellExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.core.util.TsvUtils.SUB_DELIMITER;
import static ubic.gemma.core.util.TsvUtils.format;
import static ubic.gemma.persistence.util.ByteArrayUtils.byteArrayToDoubles;

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

    /**
     * Use Ensembl gene IDs instead of gene symbols.
     */
    private boolean useEnsemblIds = false;

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
                    writeBarcodes( matrix.getSingleCellDimension(), i, baos );
                    TarArchiveEntry entry = new TarArchiveEntry( ba.getId() + "_" + FileTools.cleanForFileName( ba.getName() ) + "/barcodes.tsv" );
                    entry.setSize( baos.size() );
                    aos.putArchiveEntry( entry );
                    aos.write( baos.toByteArray() );
                    aos.closeArchiveEntry();
                }
                try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
                    writeFeatures( matrix, cs2gene, baos );
                    TarArchiveEntry entry = new TarArchiveEntry( ba.getId() + "_" + FileTools.cleanForFileName( ba.getName() ) + "/features.tsv" );
                    entry.setSize( baos.size() );
                    aos.putArchiveEntry( entry );
                    aos.write( baos.toByteArray() );
                    aos.closeArchiveEntry();
                }
                try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
                    TarArchiveEntry entry = new TarArchiveEntry( ba.getId() + "_" + FileTools.cleanForFileName( ba.getName() ) + "/matrix.mtx" );
                    writeMatrix( matrix, i, baos );
                    entry.setSize( baos.size() );
                    aos.putArchiveEntry( entry );
                    aos.write( baos.toByteArray() );
                    aos.closeArchiveEntry();
                }
            }
        }
        return matrix.rows();
    }

    /**
     * Write a matrix to a directory.
     */
    public int write( SingleCellExpressionDataMatrix<?> matrix, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Path outputDir ) throws IOException {
        if ( Files.exists( outputDir ) ) {
            throw new IllegalArgumentException( "Output directory " + outputDir + " already exists." );
        }
        List<BioAssay> bioAssays = matrix.getSingleCellDimension().getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            BioAssay ba = bioAssays.get( i );
            Path sampleDir = outputDir.resolve( ba.getId() + "_" + FileTools.cleanForFileName( ba.getName() ) );
            Files.createDirectories( sampleDir );
            try ( OutputStream baos = new GZIPOutputStream( Files.newOutputStream( sampleDir.resolve( "barcodes.tsv.gz" ) ) ) ) {
                writeBarcodes( matrix.getSingleCellDimension(), i, baos );
            }
            try ( OutputStream baos = new GZIPOutputStream( Files.newOutputStream( sampleDir.resolve( "features.tsv.gz" ) ) ) ) {
                writeFeatures( matrix, cs2gene, baos );
            }
            try ( OutputStream baos = new GZIPOutputStream( Files.newOutputStream( sampleDir.resolve( "matrix.mtx.gz" ) ) ) ) {
                writeMatrix( matrix, i, baos );
            }
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
        OutputStream features = null;
        MatrixVectorWriter[] matrices = null;
        try {
            Iterator<SingleCellExpressionDataVector> vecit = vectors.iterator();

            SingleCellExpressionDataVector firstVec = vecit.next();

            SingleCellDimension dimension = firstVec.getSingleCellDimension();
            for ( int i = 0; i < dimension.getBioAssays().size(); i++ ) {
                BioAssay ba = dimension.getBioAssays().get( i );
                Path sampleDir = outputDir.resolve( getBioAssayDirName( ba ) );
                Files.createDirectories( sampleDir );
                try ( OutputStream out = new GZIPOutputStream( Files.newOutputStream( sampleDir.resolve( "barcodes.tsv.gz" ) ) ) ) {
                    writeBarcodes( dimension, i, out );
                }
            }

            // create a file for the first sample and hard-links for the remaining
            Iterator<BioAssay> it = dimension.getBioAssays().iterator();
            Path ff = outputDir.resolve( getBioAssayDirName( it.next() ) ).resolve( "features.tsv.gz" );
            features = new GZIPOutputStream( Files.newOutputStream( ff ) );
            while ( it.hasNext() ) {
                Files.createLink( outputDir.resolve( getBioAssayDirName( it.next() ) ).resolve( "features.tsv.gz" ), ff );
            }

            matrices = new MatrixVectorWriter[dimension.getBioAssays().size()];
            for ( int i = 0; i < dimension.getBioAssays().size(); i++ ) {
                BioAssay ba = dimension.getBioAssays().get( i );
                int numberOfCells = dimension.getNumberOfCellsBySample( i );
                matrices[i] = new MatrixVectorWriter( new GZIPOutputStream( Files.newOutputStream( outputDir.resolve( getBioAssayDirName( ba ) ).resolve( "matrix.mtx.gz" ) ), 8192 ) );
                MatrixInfo.MatrixField field;
                switch ( firstVec.getQuantitationType().getRepresentation() ) {
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
                matrices[i].printMatrixInfo( new MatrixInfo( true, field, MatrixInfo.MatrixSymmetry.General ) );
                matrices[i].printMatrixSize( new MatrixSize( numVecs, numberOfCells, nnzBySample.get( ba ).intValue() ) );
            }

            int row = 0;
            writeFeature( firstVec.getDesignElement(), cs2gene, true, features );
            writeVector( firstVec, row++, matrices );

            while ( vecit.hasNext() ) {
                SingleCellExpressionDataVector vec = vecit.next();
                writeFeature( vec.getDesignElement(), cs2gene, false, features );
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

    private String getBioAssayDirName( BioAssay ba ) {
        return ba.getId() + "_" + FileTools.cleanForFileName( ba.getName() );
    }

    private void writeBarcodes( SingleCellDimension dimension, int sampleIndex, OutputStream out ) throws IOException {
        boolean first = true;
        for ( String cellId : dimension.getCellIdsBySample( sampleIndex ) ) {
            out.write( ( ( first ? "" : "\n" ) + format( cellId ) ).getBytes( StandardCharsets.UTF_8 ) );
            first = false;
        }
    }

    private void writeFeatures( SingleCellExpressionDataMatrix<?> matrix, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, OutputStream out ) throws IOException {
        List<CompositeSequence> designElements = matrix.getDesignElements();
        for ( int i = 0; i < designElements.size(); i++ ) {
            CompositeSequence de = designElements.get( i );
            writeFeature( de, cs2gene, i == 0, out );
        }
    }

    private void writeFeature( CompositeSequence de, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, boolean first, OutputStream out ) throws IOException {
        String f;
        if ( cs2gene != null ) {
            f = ( first ? "" : "\n" ) + format( de.getName() ) + "\t" + formatGenes( cs2gene.get( de ) );
        } else {
            f = ( first ? "" : "\n" ) + format( de.getName() );
        }
        out.write( f.getBytes( StandardCharsets.UTF_8 ) );
    }

    private String formatGenes( @Nullable Collection<Gene> genes ) {
        if ( genes == null || genes.isEmpty() ) {
            return "\t";
        }
        if ( useEnsemblIds ) {
            List<Gene> sortedGenes = genes.stream()
                    .filter( gene -> gene.getEnsemblId() != null )
                    .sorted( Comparator.comparing( Gene::getEnsemblId ) )
                    .collect( Collectors.toList() );
            return formatGenesAttribute( sortedGenes, Gene::getEnsemblId ) + "\t" + formatGenesAttribute( sortedGenes, Gene::getOfficialName );
        } else {
            List<Gene> sortedGenes = genes.stream()
                    .sorted( Comparator.comparing( Gene::getOfficialSymbol ) )
                    .collect( Collectors.toList() );
            return formatGenesAttribute( sortedGenes, Gene::getOfficialSymbol ) + "\t" + formatGenesAttribute( sortedGenes, Gene::getOfficialName );
        }
    }

    private String formatGenesAttribute( List<Gene> genes, Function<Gene, String> func ) {
        return genes.stream().map( func ).map( TsvUtils::format ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }

    private void writeMatrix( SingleCellExpressionDataMatrix<?> mat, int sampleIndex, OutputStream out ) {
        Assert.isInstanceOf( DoubleSingleCellExpressionDataMatrix.class, mat );
        writeDoubleMatrix( ( DoubleSingleCellExpressionDataMatrix ) mat, sampleIndex, out );
    }

    private void writeDoubleMatrix( DoubleSingleCellExpressionDataMatrix mat, int sampleIndex, OutputStream out ) {
        CompRowMatrix matrix = mat.getMatrix();
        int[] rowptr = matrix.getRowPointers();
        int[] colind = matrix.getColumnIndices();
        double[] data = matrix.getData();

        int sampleOffset = mat.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
        int numberOfCells = mat.getSingleCellDimension().getNumberOfCellsBySample( sampleIndex );
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

        try ( MatrixVectorWriter writer = new MatrixVectorWriter( out ) ) {
            writer.printMatrixInfo( new MatrixInfo( true, MatrixInfo.MatrixField.Real, MatrixInfo.MatrixSymmetry.General ) );
            writer.printMatrixSize( new MatrixSize( matrix.numRows(), numberOfCells, sampleData.length ) );
            writer.printCoordinate( sampleRows, sampleCols, sampleData );
        }
    }

    private void writeVector( SingleCellExpressionDataVector vector, int row, MatrixVectorWriter[] writers ) {
        if ( vector.getQuantitationType().getRepresentation() != PrimitiveType.DOUBLE ) {
            throw new UnsupportedOperationException( "Unsupported vector representation type " + vector.getQuantitationType().getRepresentation() );
        }
        writeDoubleVector( vector, byteArrayToDoubles( vector.getData() ), row, writers );
    }

    private void writeDoubleVector( SingleCellExpressionDataVector vector, double[] data, int row, MatrixVectorWriter[] writers ) {
        int[] colind = vector.getDataIndices();
        // the first sample always start at zero
        int start = 0;
        for ( int sampleIndex = 0; sampleIndex < vector.getSingleCellDimension().getBioAssays().size(); sampleIndex++ ) {
            int sampleOffset = vector.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
            int numberOfCells = vector.getSingleCellDimension().getNumberOfCellsBySample( sampleIndex );
            int nextSampleOffset = sampleOffset + numberOfCells;

            // check where the next sample begins, only search past this sample starting point
            int end = Arrays.binarySearch( colind, start, colind.length, nextSampleOffset );
            if ( end < 0 ) {
                end = -end - 1;
            }
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
}
