package ubic.gemma.core.visualization.cellbrowser;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.tuple.Pair;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayUtils;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.util.TsvUtils.format;
import static ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils.createCharacteristicMap;

/**
 * Write metadata file for the Cell Browser visualization tool.
 * @author poirigui
 */
@CommonsLog
public class CellBrowserMetadataWriter {

    /**
     * If true, write the sample identifier and the assay identifiers in separate columns.
     * <p>
     * The column will be named "Sample" and "Assay" respectively.
     * <p>
     * The default column name for mangled sample and assay identifiers is "Bioassay".
     */
    @Setter
    private boolean separateSampleFromAssayIdentifiers = false;
    /**
     * This is only used for the cell ID and does not affect the 'Bioassay', 'Sample' and 'Assay' columns.
     */
    @Setter
    private boolean useBioAssayIds = false;
    /**
     * If true, use column names as they appear in the database.
     */
    @Setter
    private boolean useRawColumnNames = false;
    @Setter
    private boolean autoFlush = false;

    public void write( ExpressionExperiment ee, SingleCellDimension singleCellDimension, Writer writer ) throws IOException {
        List<BioAssay> assays = singleCellDimension.getBioAssays();
        List<BioMaterial> samples = singleCellDimension.getBioAssays().stream()
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toList() );
        // only retain factors that are actually used in the samples from the single-cell dimension
        // some factors are assigned to pseudo-bulk samples such as the cell type, so they should not be included in
        // the Cell Browser metadata
        Set<ExperimentalFactor> usedFactors = samples.stream()
                .map( BioMaterial::getAllFactorValues )
                .flatMap( Collection::stream )
                .map( FactorValue::getExperimentalFactor )
                .collect( Collectors.toSet() );
        List<ExperimentalFactor> factors;
        if ( ee.getExperimentalDesign() != null ) {
            factors = ee.getExperimentalDesign().getExperimentalFactors().stream()
                    .filter( usedFactors::contains )
                    .sorted( ExperimentalFactor.COMPARATOR )
                    .collect( Collectors.toList() );
        } else {
            log.warn( ee + " does not have an experimental design, no factors will be written." );
            factors = Collections.emptyList();
        }
        Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap = ExperimentalDesignUtils.getFactorValueMap( factors, samples );
        SortedMap<Category, Map<BioAssay, Characteristic>> bioAssayCharacteristics = selectCharacteristics( BioAssayUtils.createCharacteristicMap( assays ), assays.size() );
        SortedMap<Category, Map<BioMaterial, Characteristic>> sampleCharacteristics = selectCharacteristics( createCharacteristicMap( samples ), samples.size() );
        List<CellLevelCharacteristics> clcs = new ArrayList<>( singleCellDimension.getCellTypeAssignments().size() + singleCellDimension.getCellLevelCharacteristics().size() );
        singleCellDimension.getCellTypeAssignments().stream()
                .sorted( CellTypeAssignment.COMPARATOR )
                .forEach( clcs::add );
        singleCellDimension.getCellLevelCharacteristics().stream()
                .sorted( CellLevelCharacteristics.COMPARATOR )
                .forEach( clcs::add );
        writeHeader( factors, bioAssayCharacteristics, sampleCharacteristics, clcs, writer );
        int cellIndex = 0;
        for ( int sampleIndex = 0; sampleIndex < singleCellDimension.getBioAssays().size(); sampleIndex++ ) {
            BioAssay bioAssay = singleCellDimension.getBioAssays().get( sampleIndex );
            for ( String cellId : singleCellDimension.getCellIdsBySample( sampleIndex ) ) {
                writeCell( bioAssay, cellId, cellIndex++, factors, factorValueMap, bioAssayCharacteristics, sampleCharacteristics, clcs, writer );
            }
        }
    }

    /**
     * Select characteristics suitable for the Cell Browser metadata file.
     */
    private <T> SortedMap<Category, Map<T, Characteristic>> selectCharacteristics( Map<Category, Map<T, Collection<Characteristic>>> characteristics, int numSamples ) {
        return characteristics.entrySet()
                .stream()
                // only keep categories that have at most one characteristic per sample
                // note: sample without a characteristic for that category lack an entry in the map
                .filter( e -> {
                    if ( e.getValue().values().stream().allMatch( v -> v.size() == 1 ) ) {
                        return true;
                    } else {
                        log.warn( e.getKey() + " has multiple characteristics for some samples, skipping it." );
                        return false;
                    }
                } )
                .map( e -> Pair.of( e.getKey(), e.getValue().entrySet().stream()
                        .map( e2 -> Pair.of( e2.getKey(), e2.getValue().iterator().next() ) )
                        .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) ) )
                .filter( e -> {
                    Set<Characteristic> distinctValues = new HashSet<>( e.getValue().values() );
                    // only keep categories that have more than one distinct values
                    // it's not possible to have a category with zero distinct values at this point, so if it has
                    // missing values, we can safely include it
                    boolean hasMissingValues = e.getValue().size() < numSamples;
                    if ( distinctValues.size() > 1 || hasMissingValues ) {
                        return true;
                    } else {
                        log.warn( e.getKey() + " has no distinct values, skipping it." );
                        return false;
                    }
                } )
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue, ( a, b ) -> b, () -> new TreeMap<>( Comparator.comparing( Category::getCategory ) ) ) );
    }

    private void writeHeader( List<ExperimentalFactor> factors, SortedMap<Category, Map<BioAssay, Characteristic>> bioAssayCharacteristics, SortedMap<Category, Map<BioMaterial, Characteristic>> sampleCharacteristics, List<CellLevelCharacteristics> clcs, Writer writer ) throws IOException {
        String[] columnNames = new String[1 + ( separateSampleFromAssayIdentifiers ? 2 : 1 ) + factors.size() + bioAssayCharacteristics.size() + sampleCharacteristics.size() + clcs.size()];
        int i = 0;
        columnNames[i++] = "cellId";
        if ( separateSampleFromAssayIdentifiers ) {
            columnNames[i++] = "Sample";
            columnNames[i++] = "Assay";
        } else {
            columnNames[i++] = "Bioassay";
        }
        for ( ExperimentalFactor factor : factors ) {
            columnNames[i++] = factor.getName();
        }
        // assay metadata
        for ( Category category : bioAssayCharacteristics.keySet() ) {
            columnNames[i++] = category.getCategory();
        }
        for ( Category category : sampleCharacteristics.keySet() ) {
            columnNames[i++] = category.getCategory();
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
        for ( int j = 0; j < columnNames.length; j++ ) {
            String colName = columnNames[j];
            if ( j > 0 ) {
                writer.append( "\t" );
            }
            writer.append( format( colName ) );
        }
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }

    public void writeCell( BioAssay bioAssay, String cellId, int cellIndex, List<ExperimentalFactor> factors, Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap, SortedMap<Category, Map<BioAssay, Characteristic>> bioAssayCharacteristics, SortedMap<Category, Map<BioMaterial, Characteristic>> sampleCharacteristics, List<CellLevelCharacteristics> clcs, Writer writer ) throws IOException {
        writer.append( format( CellBrowserUtils.constructCellId( bioAssay, cellId, useBioAssayIds, useRawColumnNames ) ) );
        // ignore the useBioAssayIds for the sample and assay names, this is only intended for the cell ID
        if ( separateSampleFromAssayIdentifiers ) {
            writer.append( "\t" ).append( format( ExpressionDataWriterUtils.constructSampleName( bioAssay.getSampleUsed(), false, true ) ) );
            writer.append( "\t" ).append( format( ExpressionDataWriterUtils.constructAssayName( bioAssay, false, true ) ) );
        } else {
            writer.append( "\t" ).append( format( ExpressionDataWriterUtils.constructSampleName( bioAssay.getSampleUsed(), Collections.singleton( bioAssay ), false, true, TsvUtils.SUB_DELIMITER ) ) );
        }
        for ( ExperimentalFactor factor : factors ) {
            FactorValue value = factorValueMap.get( factor ).get( bioAssay.getSampleUsed() );
            writer.append( "\t" );
            if ( value != null ) {
                writer.append( format( FactorValueUtils.getValues( value ) ) );
            } else {
                writer.append( TsvUtils.NA );
            }
        }
        for ( Category category : bioAssayCharacteristics.keySet() ) {
            Map<BioAssay, Characteristic> characteristics = bioAssayCharacteristics.get( category );
            Characteristic c = characteristics.get( bioAssay );
            writer.append( "\t" );
            if ( c != null ) {
                writer.append( format( c.getValue() ) );
            } else {
                writer.append( TsvUtils.NA );
            }
        }
        for ( Category category : sampleCharacteristics.keySet() ) {
            Map<BioMaterial, Characteristic> characteristics = sampleCharacteristics.get( category );
            Characteristic c = characteristics.get( bioAssay.getSampleUsed() );
            writer.append( "\t" );
            if ( c != null ) {
                writer.append( format( c.getValue() ) );
            } else {
                writer.append( TsvUtils.NA );
            }
        }
        for ( CellLevelCharacteristics clc : clcs ) {
            writer.append( "\t" );
            Characteristic c = clc.getCharacteristic( cellIndex );
            if ( c != null ) {
                writer.append( format( c.getValue() ) );
            }
        }
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }
}
