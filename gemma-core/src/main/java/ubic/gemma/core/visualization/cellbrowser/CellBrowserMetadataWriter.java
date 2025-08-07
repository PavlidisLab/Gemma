package ubic.gemma.core.visualization.cellbrowser;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellLevelMeasurements;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import java.io.IOException;
import java.io.Writer;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Write metadata file for the Cell Browser visualization tool.
 * @author poirigui
 */
@CommonsLog
public class CellBrowserMetadataWriter {

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
        List<ExperimentalFactor> factors = CellBrowserUtils.getFactors( ee );
        List<BioMaterial> samples = singleCellDimension.getBioAssays().stream()
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toList() );
        Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap = ExperimentalDesignUtils.getFactorValueMap( factors, samples );
        List<CellLevelCharacteristics> clcs = CellBrowserUtils.getCellLevelCharacteristics( singleCellDimension );
        List<CellLevelMeasurements> clms = CellBrowserUtils.getCellLevelMeasurements( singleCellDimension );
        Map<CellLevelMeasurements, Object> clm2Values = clms.stream().collect( Collectors.toMap( Function.identity(), this::getValues ) );
        writeHeader( factors, clcs, clms, writer );
        int cellIndex = 0;
        for ( int sampleIndex = 0; sampleIndex < singleCellDimension.getBioAssays().size(); sampleIndex++ ) {
            BioAssay bioAssay = singleCellDimension.getBioAssays().get( sampleIndex );
            for ( String cellId : singleCellDimension.getCellIdsBySample( sampleIndex ) ) {
                writeCell( bioAssay, cellId, cellIndex++, factors, factorValueMap, clcs, clms, clm2Values, writer );
            }
        }
    }

    private void writeHeader( List<ExperimentalFactor> factors, List<CellLevelCharacteristics> clcs, List<CellLevelMeasurements> clms, Writer writer ) throws IOException {
        String[] columnNames = CellBrowserUtils.createMetaColumnNames( factors, clcs, clms, useRawColumnNames );
        for ( int j = 0; j < columnNames.length; j++ ) {
            String colName = columnNames[j];
            if ( j > 0 ) {
                writer.append( "\t" );
            }
            writer.append( TsvUtils.format( colName ) );
        }
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }

    public void writeCell( BioAssay bioAssay, String cellId, int cellIndex, List<ExperimentalFactor> factors, Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap, List<CellLevelCharacteristics> clcs, List<CellLevelMeasurements> clms, Map<CellLevelMeasurements, Object> clmValuesMap, Writer writer ) throws IOException {
        writer.append( CellBrowserUtils.constructCellId( bioAssay, cellId, useBioAssayIds, useRawColumnNames ) );
        for ( ExperimentalFactor factor : factors ) {
            FactorValue value = factorValueMap.get( factor ).get( bioAssay.getSampleUsed() );
            writer.append( "\t" );
            if ( value != null ) {
                writer.append( TsvUtils.format( FactorValueUtils.getValue( value, String.valueOf( TsvUtils.SUB_DELIMITER ) ) ) );
            } else {
                writer.append( TsvUtils.format( ( String ) null ) );
            }
        }
        for ( CellLevelCharacteristics clc : clcs ) {
            writer.append( "\t" );
            Characteristic c = clc.getCharacteristic( cellIndex );
            if ( c != null ) {
                writer.append( TsvUtils.format( c.getValue() ) );
            }
        }
        for ( CellLevelMeasurements clm : clms ) {
            writer.append( "\t" );
            writer.append( formatFast( clm, clmValuesMap.get( clm ), cellIndex ) );
        }
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }

    /**
     * Extract the values of a cell-level measurement for accessing later with {@link #formatFast(CellLevelMeasurements, Object, int)}.
     */
    private Object getValues( CellLevelMeasurements clm ) {
        switch ( clm.getRepresentation() ) {
            case DOUBLE:
                return clm.getDataAsDoubles();
            case FLOAT:
                return clm.getDataAsFloats();
            case LONG:
                return clm.getDataAsLongs();
            case INT:
                return clm.getDataAsInts();
            case BOOLEAN:
                return clm.getDataAsBooleans();
            case BITSET:
                return clm.getDataAsBitSet();
            default:
                throw new UnsupportedOperationException( "Unsupported representation for cell-level measurement: " + clm.getRepresentation() );
        }
    }

    /**
     * Format the value of a cell-level measurement for a given cell.
     */
    private String formatFast( CellLevelMeasurements clm, Object clmValues, int cellIndex ) {
        switch ( clm.getRepresentation() ) {
            case FLOAT:
                return TsvUtils.formatFast( ( ( float[] ) clmValues )[cellIndex] );
            case DOUBLE:
                return TsvUtils.formatFast( ( ( double[] ) clmValues )[cellIndex] );
            case LONG:
                return TsvUtils.formatFast( ( ( long[] ) clmValues )[cellIndex] );
            case INT:
                return TsvUtils.formatFast( ( ( int[] ) clmValues )[cellIndex] );
            case BITSET:
                return TsvUtils.formatFast( ( ( BitSet ) clmValues ).get( cellIndex ) );
            case BOOLEAN:
                return TsvUtils.formatFast( ( ( boolean[] ) clmValues )[cellIndex] );
            default:
                throw new UnsupportedOperationException( "Unsupported representation for cell-level measurement: " + clm.getRepresentation() );
        }
    }
}
