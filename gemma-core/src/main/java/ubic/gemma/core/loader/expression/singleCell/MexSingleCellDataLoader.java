package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixSize;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.core.loader.util.mapper.DesignElementMapper;
import ubic.gemma.core.loader.util.mapper.EntityMapper;
import ubic.gemma.core.loader.util.mapper.EntityMapperUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
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

    private final List<String> sampleNames;

    private final List<Path> barcodeFiles;
    private final List<Path> genesFiles;
    private final List<Path> matrixFiles;

    private final int numberOfSamples;

    private BioAssayMapper bioAssayToSampleNameMapper;
    private boolean ignoreUnmatchedSamples = true;

    private DesignElementMapper designElementToGeneMapper;
    private boolean ignoreUnmatchedDesignElements = true;

    /**
     * Discard empty cells.
     */
    private boolean discardEmptyCells;

    /**
     * Allow mapping probe to gene symbols.
     * <p>
     * This is used as fallback if the gene ID cannot be found in the supplied platform. If this is set to true, the
     * second column of the genes file will be looked up.
     */
    private boolean allowMappingDesignElementsToGeneSymbols = false;

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
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException {
        Assert.isTrue( !bioAssays.isEmpty(), "At least one assay must be provided." );
        Assert.notNull( bioAssayToSampleNameMapper, "No mapper is set to match sample names with assays." );
        SingleCellDimension scd = new SingleCellDimension();
        List<String> cellIds = new ArrayList<>();
        List<BioAssay> bas = new ArrayList<>();
        int[] basO = new int[0];
        Set<String> unmatchedSamples = new HashSet<>();
        for ( int i = 0; i < numberOfSamples; i++ ) {
            String sampleName = sampleNames.get( i );
            Set<BioAssay> matchedBas = bioAssayToSampleNameMapper.matchAll( bioAssays, sampleName );
            if ( matchedBas.size() == 1 ) {
                BioAssay ba = matchedBas.iterator().next();
                bas.add( ba );
                basO = ArrayUtils.add( basO, cellIds.size() );
                List<String> sampleCellIds;
                if ( discardEmptyCells ) {
                    sampleCellIds = readLinesFromPath( barcodeFiles.get( i ), getNonEmptyCellColumns( matrixFiles.get( i ) ) );
                } else {
                    sampleCellIds = readLinesFromPath( barcodeFiles.get( i ) );
                }
                if ( sampleCellIds.stream().distinct().count() < sampleCellIds.size() ) {
                    throw new IllegalArgumentException( "Sample " + sampleName + " has duplicate cell IDs." );
                }
                cellIds.addAll( sampleCellIds );
            } else if ( matchedBas.size() > 1 ) {
                throw new IllegalArgumentException( "There is more than one BioAssay matching " + sampleName );
            } else {
                unmatchedSamples.add( sampleName );
            }
        }
        if ( bas.isEmpty() ) {
            throw new IllegalArgumentException( String.format( "No samples were matched. Possible identifiers are:\n\t%s",
                    EntityMapperUtils.getPossibleIdentifiers( bioAssays, bioAssayToSampleNameMapper ) ) );
        } else if ( !unmatchedSamples.isEmpty() ) {
            String message = String.format( "No matching samples found for: %s. Possible identifiers are:\n\t%s",
                    unmatchedSamples.stream().sorted().collect( Collectors.joining( ", " ) ),
                    EntityMapperUtils.getPossibleIdentifiers( bas, bioAssayToSampleNameMapper ) );
            if ( ignoreUnmatchedSamples ) {
                log.warn( message );
            } else {
                throw new IllegalArgumentException( message );
            }
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
        qt.setName( "10x MEX" );
        qt.setDescription( "10x MEX data loaded from " + numberOfSamples + " sets of files (i.e. features.tsv.gz, barcodes.tsv.gz and matrix.mtx.gz)." );
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
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) {
        throw new UnsupportedOperationException( "Loading cell-type assignments from MEX data is not supported." );
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) {
        throw new UnsupportedOperationException( "Loading cell-level characteristics from MEX data is not supported." );
    }

    /**
     * MEX does not provide experimental factors.
     */
    @Override
    public Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) {
        return Collections.emptySet();
    }

    /**
     * MEX does not provide sample characteristics.
     */
    @Override
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) {
        return Collections.emptyMap();
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
    public Stream<SingleCellExpressionDataVector> loadVectors( Collection<CompositeSequence> designElements, SingleCellDimension scd, QuantitationType quantitationType ) throws IOException {
        Assert.notNull( designElementToGeneMapper, "A design element mapper must be set to load vectors." );

        // location of a given element in individual matrices
        Map<CompositeSequence, int[]> elementsToSampleMatrixRow = new HashMap<>();
        Map<CompositeSequence, String[]> elementsToOriginalGeneIds = new HashMap<>();
        ArrayList<CompRowMatrix> matrices = new ArrayList<>( scd.getBioAssays().size() );

        List<BioAssay> bioAssays = scd.getBioAssays();
        Map<String, Set<BioAssay>> bioAssayBySampleName = sampleNames.stream()
                .collect( Collectors.toMap( sn -> sn, sn -> bioAssayToSampleNameMapper.matchAll( bioAssays, sn ) ) );

        EntityMapper.StatefulEntityMapper<CompositeSequence> statefulGeneMapper = designElementToGeneMapper
                .forCandidates( designElements );

        for ( int j = 0; j < bioAssays.size(); j++ ) {
            BioAssay ba = bioAssays.get( j );
            // match corresponding sample in the SCD
            Set<String> matchedSampleNames = bioAssayBySampleName.entrySet().stream()
                    .filter( e -> e.getValue().contains( ba ) )
                    .map( Map.Entry::getKey )
                    .collect( Collectors.toSet() );
            if ( matchedSampleNames.isEmpty() ) {
                throw new IllegalArgumentException( ba + " does not match any sample." );
            } else if ( matchedSampleNames.size() > 1 ) {
                throw new IllegalArgumentException( ba + " match more than one sample: " + String.join( ", ", matchedSampleNames ) );
            }

            String sampleName = matchedSampleNames.iterator().next();
            Path genesFile = genesFiles.get( sampleNames.indexOf( sampleName ) );
            Path matrixFile = matrixFiles.get( sampleNames.indexOf( sampleName ) );

            Set<String> missingElements = new HashSet<>();
            List<CompositeSequence> elements = new ArrayList<>();
            int k = 0;
            for ( String s : readLinesFromPath( genesFile ) ) {
                String[] pieces = s.split( "\t", 3 );
                String geneId = pieces[0];
                CompositeSequence probe = statefulGeneMapper.matchOne( geneId ).orElse( null );
                if ( probe == null && pieces.length > 1 && allowMappingDesignElementsToGeneSymbols ) {
                    String geneSymbol = pieces[1];
                    probe = statefulGeneMapper.matchOne( geneSymbol ).orElse( null );
                }
                if ( probe == null ) {
                    missingElements.add( geneId );
                }
                elements.add( probe );
                if ( probe != null ) {
                    elementsToSampleMatrixRow.computeIfAbsent( probe, ignored -> {
                        int[] W = new int[scd.getBioAssays().size()];
                        Arrays.fill( W, -1 );
                        return W;
                    } )[j] = k;
                    elementsToOriginalGeneIds.computeIfAbsent( probe, ignored -> new String[scd.getBioAssays().size()] )[j] = geneId;
                }
                k++;
            }

            if ( elementsToSampleMatrixRow.isEmpty() ) {
                throw new IllegalArgumentException( "None of the elements matched genes from " + genesFile + "." );
            } else if ( !missingElements.isEmpty() ) {
                String message;
                if ( missingElements.size() > 10 ) {
                    ArrayList<String> randomizedMissingElements = new ArrayList<>( missingElements );
                    Collections.shuffle( randomizedMissingElements );
                    message = String.format( "The supplied mapping does not have elements for %d/%d genes from %s. Here's 10 random genes that were not mapped: %s",
                            missingElements.size(), elements.size(), genesFile, randomizedMissingElements.stream().limit( 10 ).collect( Collectors.joining( ", " ) ) );
                } else {
                    message = String.format( "The supplied mapping does not have elements for the following genes: %s from %s.",
                            missingElements.stream().sorted().collect( Collectors.joining( ", " ) ), genesFile );
                }
                if ( ignoreUnmatchedDesignElements ) {
                    log.warn( message );
                } else {
                    throw new IllegalArgumentException( message );
                }
            }

            StopWatch timer = StopWatch.createStarted();
            CompRowMatrix matrix;
            try ( MatrixVectorReader mvr = readMatrixMarketFromPath( matrixFile ) ) {
                log.info( "Reading " + matrixFile + "..." );
                matrix = new CompRowMatrix( mvr );
            } catch ( RuntimeException e ) {
                throw new RuntimeException( "Failed to read " + matrixFile + ": " + ExceptionUtils.getRootCauseMessage( e ), e );
            }
            log.info( String.format( "Loading %s took %d ms", matrixFile, timer.getTime() ) );

            int[] nonEmptyCellIndices;
            if ( discardEmptyCells ) {
                nonEmptyCellIndices = getNonEmptyCellColumns( matrixFile );
                int numEmptyCells = matrix.numColumns() - nonEmptyCellIndices.length;
                if ( numEmptyCells > 0 ) {
                    // rewrite the column indices to account for discarded empty cells
                    int[][] nz = new int[matrix.numRows()][];
                    double[][] nzData = new double[matrix.numRows()][];
                    for ( int i = 0; i < matrix.numRows(); i++ ) {
                        nz[i] = new int[matrix.getRowPointers()[i + 1] - matrix.getRowPointers()[i]];
                        nzData[i] = new double[matrix.getRowPointers()[i + 1] - matrix.getRowPointers()[i]];
                        for ( int w = 0; w < nz[i].length; w++ ) {
                            int oldColumn = matrix.getColumnIndices()[matrix.getRowPointers()[i] + w];
                            int newColumn = Arrays.binarySearch( nonEmptyCellIndices, oldColumn );
                            assert newColumn >= 0;
                            nz[i][w] = newColumn;
                            nzData[i][w] = matrix.get( i, oldColumn );
                        }
                    }
                    CompRowMatrix newMatrix = new CompRowMatrix( matrix.numRows(), nonEmptyCellIndices.length, nz );
                    for ( int i = 0; i < nz.length; i++ ) {
                        for ( int w = 0; w < nz[i].length; w++ ) {
                            newMatrix.set( i, nz[i][w], nzData[i][w] );
                        }
                    }
                    matrix = newMatrix;
                    log.info( "Removed " + nonEmptyCellIndices.length + " empty cells from " + sampleName + "." );
                }
            }

            Assert.isTrue( matrix.numColumns() == scd.getNumberOfCellsBySample( j ),
                    String.format( "Matrix file %s does not have the expected number of columns: %d, found %d.",
                            matrixFile, scd.getNumberOfCellsBySample( j ), matrix.numColumns() ) );
            Assert.isTrue( matrix.numRows() == elements.size(),
                    String.format( "Matrix file %s does not have the expected number of rows: %d, found %d.",
                            matrixFile, elements.size(), matrix.numRows() ) );

            matrices.add( matrix );
        }

        return elementsToSampleMatrixRow.entrySet().stream().map( e -> {
            CompositeSequence probe = e.getKey();
            int[] I = e.getValue();
            SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
            vector.setDesignElement( probe );
            // it should all be the same, with nulls if the gene is not present in the sample
            String[] geneIds = elementsToOriginalGeneIds.get( probe );
            Set<String> uniqueGeneIDs = Arrays.stream( geneIds ).filter( Objects::nonNull ).collect( Collectors.toSet() );
            if ( uniqueGeneIDs.size() > 1 ) {
                log.warn( "More than one gene ID was matched for " + probe + ": " + String.join( ", ", uniqueGeneIDs ) + ", will retain an arbitrary one as original gene ID." );
            }
            vector.setOriginalDesignElement( uniqueGeneIDs.iterator().next() );
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
            for ( int k = 0; k < scd.getBioAssays().size(); k++ ) {
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
            vector.setDataAsDoubles( X );
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

    private List<String> readLinesFromPath( Path path, int[] linesToKeep ) throws IOException {
        List<String> lines = readLinesFromPath( path );
        return Arrays.stream( linesToKeep )
                .mapToObj( lines::get )
                .collect( Collectors.toList() );
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

    /**
     * Obtain the position of empty cells for a given matrix.
     */
    private int[] getNonEmptyCellColumns( Path path ) throws IOException {
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
