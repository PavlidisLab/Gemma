package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Load single cell data from <a href="https://kb.10xgenomics.com/hc/en-us/articles/115000794686-How-is-the-MEX-format-used-for-the-gene-barcode-matrices">10X Genomics MEX format</a>.
 *
 * @author poirigui
 */
@CommonsLog
@Setter
public class MexSingleCellDataLoader implements SingleCellDataLoader {

    private static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    private final List<String> sampleNames;

    private final List<Path> barcodeFiles;
    private final List<Path> genesFiles;
    private final List<Path> matrixFiles;

    private final int numberOfSamples;

    /**
     * Allow mapping probe to gene symbols.
     * <p>
     * This is used as fallback if the gene ID cannot be found in the supplied platform. If this is set to true, the
     * second column of the genes file will be looked up.
     */
    private boolean allowMappingProbeNamesToGeneSymbols = false;

    public MexSingleCellDataLoader( List<String> sampleNames, List<Path> barcodeFiles, List<Path> genesFiles, List<Path> matrixFiles ) {
        Assert.isTrue( sampleNames.size() == barcodeFiles.size()
                        && barcodeFiles.size() == genesFiles.size()
                        && genesFiles.size() == matrixFiles.size(),
                "There must be exactly the same number of each type of files." );
        this.sampleNames = Collections.unmodifiableList( sampleNames );
        this.barcodeFiles = barcodeFiles;
        this.genesFiles = genesFiles;
        this.matrixFiles = matrixFiles;
        this.numberOfSamples = barcodeFiles.size();
    }

    public Set<String> getSampleNames() {
        return new HashSet<>( sampleNames );
    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {

    }

    @Override
    public void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements ) {

    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException {
        SingleCellDimension scd = new SingleCellDimension();
        List<String> cellIds = new ArrayList<>();
        List<BioAssay> bas = new ArrayList<>( bioAssays.size() );
        int[] basO = new int[bioAssays.size()];
        for ( int i = 0; i < numberOfSamples; i++ ) {
            String sampleName = sampleNames.get( i );
            BioAssay ba = bioAssays.stream()
                    .filter( b -> b.getSampleUsed().getName().equals( sampleName ) ).findFirst()
                    .orElseThrow( () -> new IllegalArgumentException( "No matching sample found for " + sampleName ) );
            bas.add( ba );
            basO[i] = cellIds.size();
            List<String> sampleCellIds = readLinesFromPath( barcodeFiles.get( i ) );
            if ( sampleCellIds.stream().distinct().count() < sampleCellIds.size() ) {
                throw new IllegalArgumentException( "Sample " + sampleName + " has duplicate cell IDs." );
            }
            cellIds.addAll( sampleCellIds );
        }
        scd.setCellIds( cellIds );
        scd.setNumberOfCells( cellIds.size() );
        scd.setBioAssays( bas );
        scd.setBioAssaysOffset( basO );
        return scd;
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        boolean allCounts = true;
        for ( Path matrix : matrixFiles ) {
            try ( MatrixVectorReader reader = readMatrixMarketFromPath( matrix ) ) {
                if ( !reader.hasInfo() ) {
                    log.info( matrix + " does not have an info line, impossible to tell if it contains counts." );
                    allCounts = false;
                    break;
                }
                MatrixInfo matrixInfo = reader.readMatrixInfo();
                if ( !matrixInfo.isInteger() ) {
                    allCounts = false;
                    break;
                }
            }
        }
        if ( allCounts ) {
            qt.setType( StandardQuantitationType.COUNT );
            qt.setScale( ScaleType.COUNT );
        } else {
            qt.setType( StandardQuantitationType.AMOUNT );
            // TODO: detect scale type from data
            log.warn( "Scale type cannot be detected from non-counting data." );
            qt.setScale( ScaleType.OTHER );
        }
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return Collections.singleton( qt );
    }

    /**
     * MEX does not provide cell type labels.
     */
    @Override
    public Optional<CellTypeAssignment> getCellTypeAssignment() {
        return Optional.empty();
    }

    /**
     * MEX does not provide experimental factors.
     */
    @Override
    public Set<ExperimentalFactor> getFactors() throws IOException {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getGenes() throws IOException {
        Set<String> result = new HashSet<>();
        for ( Path gf : genesFiles ) {
            readLinesFromPath( gf ).stream()
                    .map( line -> line.split( "\t", 2 )[0] )
                    .forEach( result::add );
        }
        return result;
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Map<String, CompositeSequence> elementsMapping, SingleCellDimension scd, QuantitationType quantitationType ) throws IOException {
        // location of a given element in individual matrices
        Map<CompositeSequence, int[]> elementsToSampleMatrixRow = new HashMap<>();
        ArrayList<CompRowMatrix> matrices = new ArrayList<>( numberOfSamples );
        for ( int i = 0; i < numberOfSamples; i++ ) {
            Path genesFile = genesFiles.get( i );
            Path matrixFile = matrixFiles.get( i );

            Set<String> missingElements = new HashSet<>();
            List<CompositeSequence> elements = new ArrayList<>();
            int k = 0;
            for ( String s : readLinesFromPath( genesFile ) ) {
                String[] pieces = s.split( "\t", 3 );
                String geneId = pieces[0];
                String geneSymbol = pieces[1];
                CompositeSequence probe = elementsMapping.get( geneId );
                if ( probe == null && allowMappingProbeNamesToGeneSymbols ) {
                    probe = elementsMapping.get( geneSymbol );
                }
                if ( probe == null ) {
                    missingElements.add( geneId );
                }
                elements.add( probe );
                if ( probe != null ) {
                    if ( !elementsToSampleMatrixRow.containsKey( probe ) ) {
                        int[] W = new int[numberOfSamples];
                        Arrays.fill( W, -1 );
                        elementsToSampleMatrixRow.put( probe, W );
                    }
                    elementsToSampleMatrixRow.get( probe )[i] = k;
                }
                k++;
            }

            if ( missingElements.size() == elements.size() ) {
                throw new IllegalArgumentException( "None of the elements matched genes from " + genesFile + "." );
            } else if ( missingElements.size() > 10 ) {
                log.warn( String.format( "The supplied mapping does not have elements for %d/%d genes from %s.", missingElements.size(), elements.size(), genesFile ) );
            } else if ( !missingElements.isEmpty() ) {
                log.warn( String.format( "The supplied mapping does not have elements for the following genes: %s from %s.",
                        missingElements.stream().sorted().collect( Collectors.joining( ", " ) ), genesFile ) );
            }

            StopWatch timer = StopWatch.createStarted();
            CompRowMatrix matrix;
            try ( MatrixVectorReader mvr = readMatrixMarketFromPath( matrixFile ) ) {
                matrix = new CompRowMatrix( mvr );
            }
            log.info( String.format( "Loading %s took %d ms", matrixFile, timer.getTime() ) );

            Assert.isTrue( matrix.numColumns() == scd.getNumberOfCellsBySample( i ),
                    "Matrix file " + matrixFile + " does not have the expected number of columns: " + scd.getNumberOfCellsBySample( i ) + "." );
            Assert.isTrue( matrix.numRows() == elements.size(),
                    "Matrix file " + matrixFile + " does not have the expected number of rows." );

            matrices.add( matrix );
        }

        return elementsToSampleMatrixRow.entrySet().stream().map( e -> {
            CompositeSequence probe = e.getKey();
            int[] I = e.getValue();
            SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
            vector.setDesignElement( probe );
            vector.setQuantitationType( quantitationType );
            vector.setSingleCellDimension( scd );
            // number of non-zero in the vector
            int nnz = 0;
            for ( int k = 0; k < matrices.size(); k++ ) {
                int i = I[k];
                if ( i > -1 ) {
                    CompRowMatrix matrix = matrices.get( k );
                    nnz += matrix.getRowPointers()[i + 1] - matrix.getRowPointers()[i];
                }
            }
            double[] X = new double[nnz];
            int[] IX = new int[nnz];
            int offset = 0;
            for ( int k = 0; k < matrices.size(); k++ ) {
                CompRowMatrix matrix = matrices.get( k );
                int i = I[k];
                if ( i == -1 ) {
                    // ignore genes with no data for a given sample
                    continue;
                }
                // location of the sample in the single-cell vector (indices from the sparse matrix have to be shifted by that offset)
                int baOffset = scd.getBioAssaysOffset()[k];
                // number of non-zero for the sample
                int baNnz = matrix.getRowPointers()[i + 1] - matrix.getRowPointers()[i];
                System.arraycopy( matrix.getData(), matrix.getRowPointers()[i], X, offset, baNnz );
                System.arraycopy( matrix.getColumnIndices(), matrix.getRowPointers()[i], IX, offset, baNnz );
                // the index in the matrix has to be adjusted for the sample position in the vector
                for ( int z = 0; z < baNnz; z++ ) {
                    IX[z + offset] += baOffset;
                }
                offset += baNnz;
            }
            vector.setData( byteArrayConverter.doubleArrayToBytes( X ) );
            vector.setDataIndices( IX );
            return vector;
        } );
    }

    private MatrixVectorReader readMatrixMarketFromPath( Path path ) throws IOException {
        if ( path.toString().endsWith( ".gz" ) ) {
            return new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( path ) ) ) );
        } else {
            return new MatrixVectorReader( Files.newBufferedReader( path ) );
        }
    }

    private List<String> readLinesFromPath( Path path ) throws IOException {
        if ( path.toString().endsWith( ".gz" ) ) {
            try ( BufferedReader br = new BufferedReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( path ) ) ) ) ) {
                return br.lines().collect( Collectors.toList() );
            }
        } else {
            return Files.readAllLines( path );
        }
    }
}
