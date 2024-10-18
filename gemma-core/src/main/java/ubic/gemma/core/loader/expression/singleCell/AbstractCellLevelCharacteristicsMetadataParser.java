package ubic.gemma.core.loader.expression.singleCell;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.util.zip.GZIPInputStream;

public abstract class AbstractCellLevelCharacteristicsMetadataParser<T extends CellLevelCharacteristics> implements CellLevelCharacteristicsMetadataParser<T> {

    protected final Log log = LogFactory.getLog( getClass() );

    private final SingleCellDimension singleCellDimension;
    private final BioAssayToSampleNameMatcher bioAssayToSampleNameMatcher;

    public AbstractCellLevelCharacteristicsMetadataParser( SingleCellDimension singleCellDimension, BioAssayToSampleNameMatcher bioAssayToSampleNameMatcher ) {
        this.singleCellDimension = singleCellDimension;
        this.bioAssayToSampleNameMatcher = bioAssayToSampleNameMatcher;
    }

    @Override
    public Set<T> parse( Path metadataFile ) throws IOException {
        Map<BioAssay, Integer> bioAssayToIndex = ListUtils.indexOfElements( singleCellDimension.getBioAssays() );
        // maps cell IDs to their position in the cell id vector
        Map<BioAssay, Map<String, Integer>> bioAssayToCellIdIndex = new HashMap<>();
        Map<String, BioAssay> bioAssayByName = new HashMap<>();
        Set<BioAssay> assignedBioAssays = new HashSet<>();

        Set<String> categoryIds = new HashSet<>();
        Map<String, List<Characteristic>> characteristicsByCategoryId = new HashMap<>();
        Map<String, Map<Characteristic, Integer>> cellTypesToIdByCategoryId = new HashMap<>();
        Map<String, int[]> indicesByCategoryId = new HashMap<>();

        try ( CSVParser reader = openMetadataFile( metadataFile ) ) {
            for ( CSVRecord record : reader ) {
                String sample = getSampleName( record );
                String cellId = getCellId( record );
                Category category = getCategory( record );
                String value = getValue( record );
                String valueUri = getValueUri( record );
                if ( value == null && valueUri != null ) {
                    throw new IllegalArgumentException( "A value with a non-blank URI must have a label." );
                }
                BioAssay ba = bioAssayByName.computeIfAbsent( sample, ignored -> {
                    Set<BioAssay> bas = bioAssayToSampleNameMatcher.match( singleCellDimension.getBioAssays(), sample );
                    if ( bas.isEmpty() ) {
                        throw new IllegalArgumentException( "No BioAssay found for " + sample );
                    } else if ( bas.size() > 1 ) {
                        throw new IllegalArgumentException( "More than one BioAssay match " + sample + "." );
                    }
                    return bas.iterator().next();
                } );
                int bioAssayIndex = bioAssayToIndex.get( ba );
                int sampleOffset = singleCellDimension.getBioAssaysOffset()[bioAssayIndex];
                Map<String, Integer> cellIdIndex = bioAssayToCellIdIndex
                        .computeIfAbsent( ba, ( ignored ) -> ListUtils.indexOfElements( singleCellDimension.getCellIdsBySample( bioAssayIndex ) ) );
                Integer j = cellIdIndex.get( cellId );
                if ( j == null ) {
                    throw new IllegalArgumentException( "No cell with ID " + cellId + " found for sample " + sample + "." );
                }

                String categoryId = getCategoryId( record );

                categoryIds.add( categoryId );

                List<Characteristic> characteristics = characteristicsByCategoryId.computeIfAbsent( categoryId, k -> new ArrayList<>() );
                Map<Characteristic, Integer> cellTypesToId = cellTypesToIdByCategoryId.computeIfAbsent( categoryId, k -> new HashMap<>() );
                int[] indices = indicesByCategoryId.computeIfAbsent( categoryId, k -> {
                    // unassigned cells will default to NAs
                    int[] ret = new int[singleCellDimension.getCellIds().size()];
                    Arrays.fill( ret, CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC );
                    return ret;
                } );

                if ( indices[sampleOffset + j] != -1 ) {
                    throw new IllegalStateException( "There is already a characteristic with category " + categoryId + " assigned to cell " + cellId + " for sample " + sample + "." );
                }

                int k;
                if ( value != null ) {
                    Characteristic c = Characteristic.Factory.newInstance( category, value, valueUri );
                    if ( cellTypesToId.containsKey( c ) ) {
                        k = cellTypesToId.get( c );
                    } else {
                        k = characteristics.size();
                        characteristics.add( c );
                        cellTypesToId.put( c, k );
                        log.info( "New " + categoryId + " detected: " + c + ", it will be coded as " + k + "." );
                    }
                    assignedBioAssays.add( ba );
                } else {
                    // indicate a missing cell type
                    log.warn( sample + "[" + cellId + "]: Both the value and value URI columns are blank, will be treated as unassigned." );
                    k = -1;
                }
                indices[sampleOffset + j] = k;
            }
        }

        // warn if some samples have no cell type assigned
        if ( !assignedBioAssays.containsAll( singleCellDimension.getBioAssays() ) ) {
            Set<BioAssay> missingBioAssays = new HashSet<>( singleCellDimension.getBioAssays() );
            missingBioAssays.removeAll( assignedBioAssays );
            log.warn( "The following BioAssays did not have any cells assigned:\n"
                    + missingBioAssays.stream().map( BioAssay::getName ).sorted().collect( Collectors.joining( "\n\t" ) ) );
        }

        return categoryIds.stream().map( categoryId -> createCellLevelCharacteristics( characteristicsByCategoryId.get( categoryId ), indicesByCategoryId.get( categoryId ) ) ).collect( Collectors.toSet() );
    }

    protected String getSampleName( CSVRecord record ) {
        String sample = record.get( "sample_id" );
        if ( sample == null ) {
            throw new IllegalArgumentException( "Missing sample ID." );
        }
        return sample;
    }

    protected String getCellId( CSVRecord record ) {
        String cellId = record.get( "cell_id" );
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

    protected abstract T createCellLevelCharacteristics( List<Characteristic> characteristics, int[] indices );

    private CSVParser openMetadataFile( Path metadataFile ) throws IOException {
        if ( metadataFile.toString().endsWith( ".gz" ) ) {
            return getTsvFormat().parse( new InputStreamReader( new GZIPInputStream( Files.newInputStream( metadataFile ) ) ) );
        } else {
            return getTsvFormat().parse( Files.newBufferedReader( metadataFile ) );
        }
    }

    private CSVFormat getTsvFormat() {
        return CSVFormat.TDF.withFirstRecordAsHeader();
    }
}
