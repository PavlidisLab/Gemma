package ubic.gemma.core.visualization.cellbrowser;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
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
        writeHeader( factors, clcs, writer );
        int cellIndex = 0;
        for ( int sampleIndex = 0; sampleIndex < singleCellDimension.getBioAssays().size(); sampleIndex++ ) {
            BioAssay bioAssay = singleCellDimension.getBioAssays().get( sampleIndex );
            for ( String cellId : singleCellDimension.getCellIdsBySample( sampleIndex ) ) {
                writeCell( bioAssay, cellId, cellIndex++, factors, factorValueMap, clcs, writer );
            }
        }
    }

    private void writeHeader( List<ExperimentalFactor> factors, List<CellLevelCharacteristics> clcs, Writer writer ) throws IOException {
        String[] columnNames = CellBrowserUtils.createMetaColumnNames( factors, clcs, useRawColumnNames );
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

    public void writeCell( BioAssay bioAssay, String cellId, int cellIndex, List<ExperimentalFactor> factors, Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap, List<CellLevelCharacteristics> clcs, Writer writer ) throws IOException {
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
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }
}
