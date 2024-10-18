package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
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

import static ubic.gemma.persistence.util.ByteArrayUtils.doubleArrayToBytes;

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

    private BioAssayToSampleNameMatcher sampleNameComparator;
    private boolean ignoreUnmatchedSamples = true;
    private boolean ignoreUnmatchedDesignElements = true;

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
    public void setBioAssayToSampleNameMatcher( BioAssayToSampleNameMatcher sampleNameComparator ) {
        this.sampleNameComparator = sampleNameComparator;
    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {
        this.ignoreUnmatchedSamples = ignoreUnmatchedSamples;
    }

    @Override
    public void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements ) {
        this.ignoreUnmatchedDesignElements = ignoreUnmatchedDesignElements;
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException {
        Assert.isTrue( !bioAssays.isEmpty(), "At least one bioassay must be provided" );
        Assert.notNull( sampleNameComparator, "A sample name comparator is necessary to match sample names with BioMaterials" );
        SingleCellDimension scd = new SingleCellDimension();
        List<String> cellIds = new ArrayList<>();
        List<BioAssay> bas = new ArrayList<>();
        int[] basO = new int[0];
        Set<String> unmatchedSamples = new HashSet<>();
        for ( int i = 0; i < numberOfSamples; i++ ) {
            String sampleName = sampleNames.get( i );
            Set<BioAssay> matchedBas = sampleNameComparator.match( bioAssays, sampleName );
            if ( matchedBas.size() == 1 ) {
                BioAssay ba = matchedBas.iterator().next();
                bas.add( ba );
                basO = ArrayUtils.add( basO, cellIds.size() );
                List<String> sampleCellIds = readLinesFromPath( barcodeFiles.get( i ) );
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
            throw new IllegalArgumentException( "No samples were matched." );
        } else if ( !unmatchedSamples.isEmpty() ) {
            String message = "No matching samples found for: " + unmatchedSamples.stream().sorted().collect( Collectors.joining( ", " ) );
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
        qt.setDescription( "10x MEX data loaded from: " + matrixFiles.stream().map( Path::getFileName ).map( Path::toString ).collect( Collectors.joining( ", " ) ) + "." );
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
    public Stream<SingleCellExpressionDataVector> loadVectors( Map<String, CompositeSequence> elementsMapping, SingleCellDimension scd, QuantitationType quantitationType ) throws IOException {
        // location of a given element in individual matrices
        Map<CompositeSequence, int[]> elementsToSampleMatrixRow = new HashMap<>();
        Map<CompositeSequence, String[]> elementsToOriginalGeneIds = new HashMap<>();
        ArrayList<CompRowMatrix> matrices = new ArrayList<>( scd.getBioAssays().size() );

        List<BioAssay> bioAssays = scd.getBioAssays();
        Map<String, Set<BioAssay>> bioAssayBySampleName = sampleNames.stream()
                .collect( Collectors.toMap( sn -> sn, sn -> sampleNameComparator.match( bioAssays, sn ) ) );

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
                CompositeSequence probe = elementsMapping.get( geneId );
                if ( probe == null && pieces.length > 1 && allowMappingProbeNamesToGeneSymbols ) {
                    String geneSymbol = pieces[1];
                    probe = elementsMapping.get( geneSymbol );
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

            Assert.isTrue( matrix.numColumns() == scd.getNumberOfCellsBySample( j ),
                    "Matrix file " + matrixFile + " does not have the expected number of columns: " + scd.getNumberOfCellsBySample( j ) + "." );
            Assert.isTrue( matrix.numRows() == elements.size(),
                    "Matrix file " + matrixFile + " does not have the expected number of rows." );

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
            vector.setData( doubleArrayToBytes( X ) );
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
