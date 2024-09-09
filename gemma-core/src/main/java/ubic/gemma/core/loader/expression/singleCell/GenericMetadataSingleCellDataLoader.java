package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * A generic loader that can be used to load single cell with a tabular metadata file.
 * @author poirigui
 */
@Setter
public class GenericMetadataSingleCellDataLoader extends AbstractDelegatingSingleCellDataLoader implements SingleCellDataLoader {

    private final Path metadataFile;

    private String sampleColumn = "sample_id";
    private String cellIdColumn = "cell_id";
    private String cellTypeColumn = "cell_type";
    /**
     * Column storing a URL for the cell type. Free-text are used if null.
     */
    @Nullable
    private String cellTypeUriColumn = "cell_type_uri";
    private BioAssayToSampleNameMatcher bioAssayToSampleNameMatcher;

    protected GenericMetadataSingleCellDataLoader( SingleCellDataLoader delegate, Path metadataFile ) {
        super( delegate );
        this.metadataFile = metadataFile;
    }

    @Override
    public void setBioAssayToSampleNameMatcher( BioAssayToSampleNameMatcher sampleNameComparator ) {
        this.bioAssayToSampleNameMatcher = sampleNameComparator;
        super.setBioAssayToSampleNameMatcher( sampleNameComparator );
    }

    @Override
    public Optional<CellTypeAssignment> getCellTypeAssignment( SingleCellDimension singleCellDimension ) throws IOException {
        Assert.notNull( bioAssayToSampleNameMatcher, "A bioAssayToSampleNameMatcher must be set" );
        List<Characteristic> cellTypes = new ArrayList<>();
        Map<Characteristic, Integer> cellTypesToId = new HashMap<>();
        int[] cellTypeIndices = new int[singleCellDimension.getCellIds().size()];
        Map<BioAssay, Integer> bioAssayToIndex = ListUtils.indexOfElements( singleCellDimension.getBioAssays() );
        // maps cell IDs to their position in the cell id vector??
        Map<BioAssay, Map<String, Integer>> bioAssayToCellIdIndex = new HashMap<>();
        Map<String, BioAssay> bioAssayByName = new HashMap<>();
        // unassigned cells will default to NAs
        Arrays.fill( cellTypeIndices, -1 );
        try ( CSVParser reader = openMetadataFile() ) {
            for ( CSVRecord record : reader ) {
                String sample = record.get( sampleColumn );
                String cellId = record.get( cellIdColumn );
                String cellType = StringUtils.stripToNull( record.get( cellTypeColumn ) );
                String cellTypeUri = StringUtils.stripToNull( record.get( cellTypeUriColumn ) );
                if ( sample == null ) {
                    throw new IllegalArgumentException( "Missing sample ID." );
                }
                if ( cellId == null ) {
                    throw new IllegalArgumentException( "Missing cell ID." );
                }
                if ( cellType == null && cellTypeUri != null ) {
                    throw new IllegalArgumentException( "A cell type with a non-blank URI must have a label." );
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
                Map<String, Integer> cellIdsIdId = bioAssayToCellIdIndex
                        .computeIfAbsent( ba, ( ignored ) -> ListUtils.indexOfElements( singleCellDimension.getCellIdsBySample( bioAssayIndex ) ) );
                int j = cellIdsIdId.get( cellId );
                if ( j == -1 ) {
                    throw new IllegalArgumentException( "No cell ID " + cellId + " found for sample " + sample + "." );
                }
                int k;
                if ( cellType != null ) {
                    Characteristic c = Characteristic.Factory.newInstance( Categories.CELL_TYPE, cellType, cellTypeUri );
                    if ( cellTypesToId.containsKey( c ) ) {
                        k = cellTypesToId.get( c );
                    } else {
                        k = cellTypes.size();
                        cellTypes.add( c );
                        cellTypesToId.put( c, k );
                    }
                } else {
                    // indicate a missing cell type
                    k = -1;
                }
                cellTypeIndices[sampleOffset + j] = k;
            }
        }
        CellTypeAssignment cta = new CellTypeAssignment();
        cta.setCellTypes( cellTypes );
        cta.setNumberOfCellTypes( cellTypes.size() );
        cta.setCellTypeIndices( cellTypeIndices );
        return Optional.of( cta );
    }

    private CSVParser openMetadataFile() throws IOException {
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
