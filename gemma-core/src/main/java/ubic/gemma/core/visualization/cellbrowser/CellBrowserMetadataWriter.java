package ubic.gemma.core.visualization.cellbrowser;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.constructAssayName;

/**
 * Write metadata file for the Cell Browser visualization tool.
 * @author poirigui
 */
@CommonsLog
public class CellBrowserMetadataWriter {

    @Setter
    private boolean useBioAssayIds = false;
    @Setter
    private boolean autoFlush = false;

    public void write( ExpressionExperiment ee, SingleCellDimension singleCellDimension, Writer writer ) throws IOException {
        List<ExperimentalFactor> factors;
        if ( ee.getExperimentalDesign() != null ) {
            factors = ee.getExperimentalDesign().getExperimentalFactors().stream()
                    .sorted()
                    .collect( Collectors.toList() );
        } else {
            log.warn( ee + " does not have an experimental design, no factors will be written." );
            factors = Collections.emptyList();
        }
        List<CellLevelCharacteristics> clcs = new ArrayList<>();
        // TODO: sort these
        clcs.addAll( singleCellDimension.getCellTypeAssignments() );
        clcs.addAll( singleCellDimension.getCellLevelCharacteristics() );
        writeHeader( factors, clcs, writer );
        int cellIndex = 0;
        for ( int sampleIndex = 0; sampleIndex < singleCellDimension.getBioAssays().size(); sampleIndex++ ) {
            BioAssay bioAssay = singleCellDimension.getBioAssays().get( sampleIndex );
            String sampleId = getSampleId( bioAssay );
            for ( String cellId : singleCellDimension.getCellIdsBySample( sampleIndex ) ) {
                writeCell( bioAssay, sampleId, cellId, cellIndex++, factors, clcs, writer );
            }
        }
    }

    private void writeHeader( List<ExperimentalFactor> factors, List<CellLevelCharacteristics> clcs, Writer writer ) throws IOException {
        writer.append( "cell_id" );
        for ( ExperimentalFactor factor : factors ) {
            writer.append( "\t" );
            writer.append( factor.getName() );
        }
        for ( CellLevelCharacteristics clc : clcs ) {
            writer.append( "\t" );
            writer.append( clc.getName() );
        }
        if ( autoFlush ) {
            writer.flush();
        }
    }

    public void writeCell( BioAssay bioAssay, String sampleId, String cellId, int cellIndex, List<ExperimentalFactor> factors, List<CellLevelCharacteristics> clcs, Writer writer ) throws IOException {
        writer.append( sampleId ).append( "_" ).append( cellId );
        writer.append( "\n" );
        for ( ExperimentalFactor factor : factors ) {
            writer.append( "\t" );
            for ( FactorValue fv : bioAssay.getSampleUsed().getAllFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( factor ) ) {
                    writer.append( FactorValueUtils.getValue( fv ) );
                    break;
                }
            }
        }
        for ( CellLevelCharacteristics clc : clcs ) {
            writer.append( "\t" );
            Characteristic c = clc.getCharacteristic( cellIndex );
            if ( c != null ) {
                writer.append( TsvUtils.format( c.getValue() ) );
            }
        }
        if ( autoFlush ) {
            writer.flush();
        }
    }

    private String getSampleId( BioAssay bioAssay ) {
        if ( useBioAssayIds ) {
            return String.valueOf( bioAssay.getId() );
        } else {
            return constructAssayName( bioAssay );
        }
    }
}
