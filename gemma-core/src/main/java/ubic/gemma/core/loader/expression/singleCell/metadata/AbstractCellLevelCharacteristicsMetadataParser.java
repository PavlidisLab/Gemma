package ubic.gemma.core.loader.expression.singleCell.metadata;

import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.loader.util.mapper.*;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.model.expression.bioAssayData.SingleCellDimensionUtils.createReverseIndex;

/**
 * Base class for cell-level characteristics parser.
 * @author poirigui
 */
@Setter
abstract class AbstractCellLevelCharacteristicsMetadataParser<T extends CellLevelCharacteristics> {

    private static final int MAXIMUM_COLLISION_TO_REPORT = 5;

    protected final Log log = LogFactory.getLog( getClass() );

    private final SingleCellDimension singleCellDimension;
    private final BioAssayMapper bioAssayMapper;
    @Nullable
    private final List<String> names;

    /**
     * If the sample name is missing, use the cell ID to infer it. If the cell ID is ambiguous (i.e. a barcode
     * collision), the characteristic will be considered missing and a warning will be logged. If this is not enabled,
     * {@link #getSampleName(CSVRecord)} must always return a non-null value.
     */
    private boolean useCellIdsIfSampleNameIsMissing;
    /**
     * If the sample name is provided, but does not directly match any bioassay, the parser will attempt to infer
     * {@link BioAssay} correspondence using cell IDs overlap.
     * <p>
     * If the mapping fails, the matching will be delegated to the supplied {@link BioAssayMapper} from the constructor.
     */
    private boolean inferSamplesFromCellIdsOverlap;
    /**
     * If true, samples that do not match any assays will be ignored.
     */
    private boolean ignoreUnmatchedSamples;
    /**
     * If true, cell IDs that do not match any cell in the dimension will be ignored.
     */
    private boolean ignoreUnmatchedCellIds;

    private final List<String> cellIds;
    private final Map<BioAssay, Integer> bioAssayToIndex;
    private final Map<String, Set<BioAssay>> reverseIndex;

    /**
     * @param singleCellDimension dimension to use to populate the cell-level characteristics
     * @param bioAssayMapper      strategy to use to match sample name from the file to bioassays
     * @param names               names to use for naming the resulting CLCs, those must match the number and ordering
     *                            encountered by the parser
     */
    protected AbstractCellLevelCharacteristicsMetadataParser( SingleCellDimension singleCellDimension, BioAssayMapper bioAssayMapper, @Nullable List<String> names ) {
        this.singleCellDimension = singleCellDimension;
        this.bioAssayMapper = bioAssayMapper;
        this.cellIds = singleCellDimension.getCellIds();
        this.names = names;
        this.bioAssayToIndex = ListUtils.indexOfElements( singleCellDimension.getBioAssays() );
        // we don't need the position in the bioassay
        reverseIndex = createReverseIndex( singleCellDimension ).entrySet()
                .stream().collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().keySet() ) );
    }

    public Set<T> parse( Path metadataFile ) throws IOException {
        // maps cell IDs to their position in the cell id vector
        Map<BioAssay, Map<String, Integer>> bioAssayToCellIdIndex = new HashMap<>();
        Map<String, BioAssay> bioAssayByName = new HashMap<>();
        Set<BioAssay> assignedBioAssays = new HashSet<>();

        LinkedHashSet<String> categoryIds = new LinkedHashSet<>();
        Map<String, List<Characteristic>> characteristicsByCategoryId = new HashMap<>();
        Map<String, Map<Characteristic, Integer>> cellTypesToIdByCategoryId = new HashMap<>();
        Map<String, int[]> indicesByCategoryId = new HashMap<>();

        EntityMapper<BioAssay> bioAssayMapper;
        if ( inferSamplesFromCellIdsOverlap ) {
            log.info( "Inferring sample correspondence from cell IDs overlap..." );
            bioAssayMapper = new ChainedEntityMapper<>( this.bioAssayMapper, createBioAssayMapperFromCellIdOverlap( metadataFile ) );
        } else {
            bioAssayMapper = this.bioAssayMapper;
        }

        int collisions = 0;

        try ( CSVParser reader = openMetadataFile( metadataFile ) ) {
            for ( CSVRecord record : reader ) {
                String sampleName = getSampleName( record );
                String cellId = getCellId( record );
                Category category = getCategory( record );
                String categoryId = getCategoryId( record );
                String value = getValue( record );
                String valueUri = getValueUri( record );
                if ( value == null && valueUri != null ) {
                    throw new IllegalArgumentException( "A value with a non-blank URI must have a label." );
                }

                BioAssay ba;
                if ( sampleName != null ) {
                    ba = bioAssayByName.computeIfAbsent( sampleName, ignored -> {
                        String sampleNameCandidates = EntityMapperUtils.getPossibleIdentifiers( singleCellDimension.getBioAssays(), bioAssayMapper );
                        Set<BioAssay> bas = bioAssayMapper.matchAll( singleCellDimension.getBioAssays(), sampleName );
                        if ( bas.isEmpty() ) {
                            String m = String.format( "No assay found for %s. Possible values are:\n\t%s",
                                    sampleName, sampleNameCandidates );
                            if ( ignoreUnmatchedSamples ) {
                                log.warn( m );
                                return null;
                            } else {
                                throw new IllegalArgumentException( m );
                            }
                        } else if ( bas.size() > 1 ) {
                            throw new IllegalArgumentException( String.format( "More than one assay match %s. There might be a better identifier to use, possible values are: \n\t%s",
                                    sampleName, sampleNameCandidates ) );
                        }
                        return bas.iterator().next();
                    } );
                    if ( ba == null ) {
                        continue;
                    }
                } else if ( useCellIdsIfSampleNameIsMissing ) {
                    Set<BioAssay> bioAssays = requireNonNull( reverseIndex ).get( cellId );
                    if ( bioAssays == null ) {
                        String m = "No BioAssay found for cell ID " + cellId + ".";
                        if ( ignoreUnmatchedCellIds ) {
                            log.warn( m );
                            continue;
                        } else {
                            throw new IllegalArgumentException( m );
                        }
                    } else if ( bioAssays.size() > 1 ) {
                        collisions++;
                        String msg = String.format( "More than one assays: %s found for cell ID %s due to barcode collision, ignoring.",
                                bioAssays.stream().map( BioAssay::getName ).sorted().collect( Collectors.joining( ", " ) ), cellId );
                        if ( collisions < MAXIMUM_COLLISION_TO_REPORT ) {
                            log.warn( msg );
                        } else if ( collisions == MAXIMUM_COLLISION_TO_REPORT ) {
                            log.warn( msg + "\nFurther messages will be suppressed, enable debug logs for " + getClass().getSimpleName() + " for more details." );
                        } else {
                            log.debug( msg );
                        }
                        continue;
                    } else {
                        ba = bioAssays.iterator().next();
                    }
                } else {
                    throw new IllegalArgumentException( "No sample name provided and resolving sample by cell ID is not enabled." );
                }

                int bioAssayIndex = bioAssayToIndex.get( ba );
                int sampleOffset = singleCellDimension.getBioAssaysOffset()[bioAssayIndex];
                Map<String, Integer> cellIdIndex = bioAssayToCellIdIndex
                        .computeIfAbsent( ba, ( ignored ) -> ListUtils.indexOfElements( singleCellDimension.getCellIdsBySample( bioAssayIndex ) ) );
                Integer j = cellIdIndex.get( cellId );
                if ( j == null ) {
                    String m = "No cell with ID " + cellId + " found for assay " + ba + ".";
                    if ( ignoreUnmatchedCellIds ) {
                        log.warn( m );
                        continue;
                    } else {
                        throw new IllegalArgumentException( m );
                    }
                }

                categoryIds.add( categoryId );

                List<Characteristic> characteristics = characteristicsByCategoryId.computeIfAbsent( categoryId, k -> new ArrayList<>() );
                Map<Characteristic, Integer> cellTypesToId = cellTypesToIdByCategoryId.computeIfAbsent( categoryId, k -> new HashMap<>() );
                int[] indices = indicesByCategoryId.computeIfAbsent( categoryId, k -> {
                    // unassigned cells will default to NAs
                    int[] ret = new int[cellIds.size()];
                    Arrays.fill( ret, CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC );
                    return ret;
                } );

                int k;
                if ( value != null ) {
                    Characteristic c = Characteristic.Factory.newInstance( category, value, valueUri );
                    if ( cellTypesToId.containsKey( c ) ) {
                        k = cellTypesToId.get( c );
                    } else {
                        k = characteristics.size();
                        characteristics.add( c );
                        cellTypesToId.put( c, k );
                        log.debug( "New " + categoryId + " detected: " + c + ", it will be coded as " + k + "." );
                    }
                    assignedBioAssays.add( ba );
                } else {
                    // indicate a missing cell type
                    log.warn( sampleName + "[" + cellId + "]: Both the value and value URI columns are blank, will be treated as unassigned." );
                    k = -1;
                }

                int assignedK = indices[sampleOffset + j];

                if ( assignedK == -1 ) {
                    indices[sampleOffset + j] = k;
                } else if ( assignedK == k ) {
                    log.warn( String.format( "There is a duplicate characteristic with category %s assigned to cell %s for assay %s, ignoring.",
                            categoryId, cellId, ba ) );
                } else {
                    throw new IllegalStateException( String.format( "There is already a characteristic with category %s assigned to cell %s for assay %s: %s.",
                            categoryId, cellId, ba, characteristics.get( assignedK ) ) );
                }
            }
        }

        Map<String, String> nameByCategoryId;
        if ( names != null ) {
            if ( names.size() != categoryIds.size() ) {
                throw new IllegalStateException( "The number of names provided (" + names.size() + ") does not match the number of categories found in the metadata file (" + categoryIds.size() + ")." );
            }
            nameByCategoryId = new HashMap<>();
            int i = 0;
            for ( String cid : categoryIds ) {
                nameByCategoryId.put( cid, names.get( i++ ) );
            }
        } else {
            nameByCategoryId = null;
        }

        StringBuilder descriptionBuilder = new StringBuilder();

        if ( collisions > 0 ) {
            String m = String.format( "Rejected %d records due to barcode collisions.", collisions );
            descriptionBuilder.append( m );
            log.warn( m );
        }

        // warn if some samples have no cell type assigned
        if ( !assignedBioAssays.containsAll( singleCellDimension.getBioAssays() ) ) {
            Set<BioAssay> missingBioAssays = new HashSet<>( singleCellDimension.getBioAssays() );
            missingBioAssays.removeAll( assignedBioAssays );
            String m = "The following assays did not have any cells assigned:\n\t"
                    + missingBioAssays.stream().map( BioAssay::getName ).sorted().collect( Collectors.joining( "\n\t" ) );
            if ( descriptionBuilder.length() > 0 ) {
                descriptionBuilder.append( "\n\n" );
            }
            descriptionBuilder.append( m );
            log.warn( m );
        }

        String description = descriptionBuilder.toString();
        Set<T> result = new HashSet<>();
        for ( String categoryId : categoryIds ) {
            String name = nameByCategoryId != null ? nameByCategoryId.get( categoryId ) : null;
            List<Characteristic> characteristics = characteristicsByCategoryId.get( categoryId );
            int[] indices = indicesByCategoryId.get( categoryId );
            result.add( createCellLevelCharacteristics( name, description, characteristics, indices ) );
        }
        return result;
    }

    @Nullable
    protected String getSampleName( CSVRecord record ) {
        return record.isMapped( "sample_id" ) ? StringUtils.stripToNull( record.get( "sample_id" ) ) : null;
    }

    protected String getCellId( CSVRecord record ) {
        String cellId = StringUtils.stripToNull( record.get( "cell_id" ) );
        if ( cellId == null ) {
            throw new IllegalArgumentException( "Missing cell ID." );
        }
        return cellId;
    }

    protected abstract Category getCategory( CSVRecord record );

    /**
     * An identifier for a category to disambiguate cases where more than one term from a given category is applicable
     * to a cell.
     * <p>
     * Results from {@link #parse(Path)} will be grouped by category ID.
     */
    @Nullable
    protected abstract String getCategoryId( CSVRecord record );

    protected abstract String getValue( CSVRecord record );

    @Nullable
    protected abstract String getValueUri( CSVRecord record );

    /**
     * Create a cell-level characteristics object from the parsed data.
     */
    protected abstract T createCellLevelCharacteristics( @Nullable String name, @Nullable String description, List<Characteristic> characteristics, int[] indices );

    /**
     * Create a mapper to match sample names from the metadata file to bioassays using cell IDs correspondence.
     */
    private BioAssayMapper createBioAssayMapperFromCellIdOverlap( Path annotationFile ) throws IOException {
        Map<BioAssay, Set<String>> bioAssayToCellIds = new HashMap<>();
        Map<String, Set<String>> sampleNameToCellIds = new HashMap<>();

        List<BioAssay> bioAssays = singleCellDimension.getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            BioAssay ba = bioAssays.get( i );
            bioAssayToCellIds.put( ba, new HashSet<>( singleCellDimension.getCellIdsBySample( i ) ) );
        }

        try ( CSVParser parser = openMetadataFile( annotationFile ) ) {
            for ( CSVRecord record : parser ) {
                String sampleName = getSampleName( record );
                if ( sampleName == null ) {
                    continue;
                }
                String cellId = getCellId( record );
                sampleNameToCellIds.computeIfAbsent( sampleName, k -> new HashSet<>() ).add( cellId );
            }
        }

        return new CellIdOverlapBioAssayMapper( bioAssayToCellIds, sampleNameToCellIds );
    }

    private CSVParser openMetadataFile( Path metadataFile ) throws IOException {
        if ( metadataFile.toString().endsWith( ".gz" ) ) {
            return getTsvFormat().parse( new InputStreamReader( FileUtils.openCompressedFile( metadataFile ) ) );
        } else {
            return getTsvFormat().parse( Files.newBufferedReader( metadataFile ) );
        }
    }

    private CSVFormat getTsvFormat() {
        return CSVFormat.TDF.withFirstRecordAsHeader();
    }
}
