package ubic.gemma.core.visualization.cellbrowser;

import lombok.extern.apachecommons.CommonsLog;
import ubic.basecode.util.StringUtil;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class CellBrowserUtils {

    /**
     * Regex matching disallowed characters in dataset names and other identifiers used by the Cell Browser.
     */
    private static final String DISALLOWED_CHARS = "[^A-Za-z0-9_]";

    public static String constructDatasetName( ExpressionExperiment ee ) {
        return ee.getShortName().replaceAll( DISALLOWED_CHARS, "_" );
    }

    /**
     * Construct a cell ID for the Cell Browser.
     * @param useBioAssayIds    use the BioAssay ID as the sample ID instead of the short name (or name)
     * @param useRawColumnNames if true, the sample ID and cell ID will be concatenated as-is, otherwise
     * {@link StringUtil#makeNames(String)} will be used it make it R-friendly
     */
    public static String constructCellId( BioAssay bioAssay, String cellId, boolean useBioAssayIds, boolean useRawColumnNames ) {
        String sampleId;
        if ( useBioAssayIds ) {
            sampleId = String.valueOf( bioAssay.getId() );
        } else {
            sampleId = bioAssay.getShortName() != null ? bioAssay.getShortName() : bioAssay.getName();
        }
        if ( useRawColumnNames ) {
            return sampleId + "_" + cellId;
        } else {
            return StringUtil.makeNames( sampleId + "_" + cellId );
        }
    }

    /**
     * Select factors that will be written to the metadata file.
     */
    public static List<ExperimentalFactor> getFactors( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() != null ) {
            return ee.getExperimentalDesign().getExperimentalFactors().stream()
                    .sorted( ExperimentalFactor.COMPARATOR )
                    .collect( Collectors.toList() );
        } else {
            log.warn( ee + " does not have an experimental design, no factors will be written." );
            return Collections.emptyList();
        }
    }

    /**
     * Select cell-level characteristics (include cell type assignments) that will be written to the metadata file.
     */
    public static List<CellLevelCharacteristics> getCellLevelCharacteristics( SingleCellDimension singleCellDimension ) {
        List<CellLevelCharacteristics> clcs = new ArrayList<>( singleCellDimension.getCellTypeAssignments().size() + singleCellDimension.getCellLevelCharacteristics().size() );
        singleCellDimension.getCellTypeAssignments().stream()
                .sorted( CellTypeAssignment.COMPARATOR )
                .forEach( clcs::add );
        singleCellDimension.getCellLevelCharacteristics().stream()
                .sorted( CellLevelCharacteristics.COMPARATOR )
                .forEach( clcs::add );
        return clcs;
    }

    /**
     * Generate the column names for the metadata file.
     */
    public static String[] createMetaColumnNames( List<ExperimentalFactor> factors, List<CellLevelCharacteristics> clcs, boolean useRawColumnNames ) {
        String[] columnNames = new String[1 + factors.size() + clcs.size()];
        int i = 0;
        columnNames[i++] = "cellId";
        for ( ExperimentalFactor factor : factors ) {
            columnNames[i++] = factor.getName();
        }
        for ( CellLevelCharacteristics clc : clcs ) {
            if ( clc.getName() != null ) {
                columnNames[i++] = clc.getName();
            } else if ( !clc.getCharacteristics().isEmpty() ) {
                // If the name is null, we can use the first characteristic's category as a fallback
                Characteristic c = clc.getCharacteristics().iterator().next();
                columnNames[i++] = c.getCategory();
            } else {
                throw new IllegalStateException( clc + " has no name nor characteristics, cannot write header." );
            }
        }
        if ( useRawColumnNames ) {
            columnNames = StringUtil.makeUnique( columnNames );
        } else {
            columnNames = StringUtil.makeNames( columnNames, true );
        }
        return columnNames;
    }

    /**
     * Obtain a mapping between Gemma entities (factors and cell-level characteristics) and the Cell Browser metadata columns.
     * @param useRawColumnNames whether to use raw column names
     */
    public static List<CellBrowserMapping> createMetadataMapping( List<ExperimentalFactor> factors, List<CellLevelCharacteristics> clcs, boolean useRawColumnNames ) {
        List<CellBrowserMapping> mapping = new ArrayList<>();
        String[] metaNames = createMetaColumnNames( factors, clcs, useRawColumnNames );
        int i = 1; // column zero is the cell ID
        for ( ExperimentalFactor factor : factors ) {
            String n = metaNames[i++];
            mapping.add( new CellBrowserMapping( CellBrowserMappingType.FACTOR, factor.getId(), getMetaColumnIdentifier( n ), n ) );
        }
        for ( CellLevelCharacteristics clc : clcs ) {
            String n = metaNames[i++];
            if ( clc instanceof CellTypeAssignment ) {
                mapping.add( new CellBrowserMapping( CellBrowserMappingType.CELL_TYPE_ASSIGNMENT, clc.getId(), getMetaColumnIdentifier( n ), n ) );
            } else {
                mapping.add( new CellBrowserMapping( CellBrowserMappingType.CELL_LEVEL_CHARACTERISTICS, clc.getId(), getMetaColumnIdentifier( n ), n ) );
            }
        }
        return mapping;
    }

    /**
     * Given a metadata column name, return the identifier that Cell Browser will use for it.
     * <p>
     * Based on <a href="https://github.com/maximilianh/cellBrowser/blob/ca964f1583f751228b2ccce442e76e02846f051e/src/cbPyLib/cellbrowser/cellbrowser.py#L2692-L2699">cellbrowser.py#L2692-L2699</a>
     */
    static String getMetaColumnIdentifier( String s ) {
        return s
                .replaceAll( "\\+", "Plus" )
                .replaceAll( "-", "Minus" )
                .replaceAll( DISALLOWED_CHARS, "_" )
                .replaceAll( "_", "" );
    }
}
