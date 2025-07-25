package ubic.gemma.core.visualization.cellbrowser;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils;
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
import java.util.Comparator;
import java.util.List;
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
    private boolean useRawColumnNames = true;
    @Setter
    private boolean autoFlush = false;

    public void write( ExpressionExperiment ee, SingleCellDimension singleCellDimension, Writer writer ) throws IOException {
        List<ExperimentalFactor> factors;
        if ( ee.getExperimentalDesign() != null ) {
            factors = ee.getExperimentalDesign().getExperimentalFactors().stream()
                    .sorted( Comparator.comparing( ExperimentalFactor::getName ) )
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
            for ( String cellId : singleCellDimension.getCellIdsBySample( sampleIndex ) ) {
                writeCell( bioAssay, cellId, cellIndex++, factors, clcs, writer );
            }
        }
    }

    private void writeHeader( List<ExperimentalFactor> factors, List<CellLevelCharacteristics> clcs, Writer writer ) throws IOException {
        writer.append( "cellId" );
        for ( ExperimentalFactor factor : factors ) {
            writer.append( "\t" );
            writer.append( useRawColumnNames ? factor.getName() : ExpressionDataWriterUtils.constructExperimentalFactorName( factor ) );
        }
        for ( CellLevelCharacteristics clc : clcs ) {
            writer.append( "\t" );
            if ( clc.getName() != null ) {
                writer.append( useRawColumnNames ? clc.getName() : StringUtil.makeValidForR( clc.getName() ) );
            } else if ( !clc.getCharacteristics().isEmpty() ) {
                // If the name is null, we can use the first characteristic's category as a fallback
                Characteristic c = clc.getCharacteristics().iterator().next();
                writer.append( useRawColumnNames ? c.getCategory() : StringUtil.makeValidForR( c.getCategory() ) );
            } else {
                throw new IllegalStateException( clc + " has no name nor characteristics, cannot write header." );
            }
        }
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }

    public void writeCell( BioAssay bioAssay, String cellId, int cellIndex, List<ExperimentalFactor> factors, List<CellLevelCharacteristics> clcs, Writer writer ) throws IOException {
        writer.append( CellBrowserUtils.constructCellId( bioAssay, cellId, useBioAssayIds ) );
        for ( ExperimentalFactor factor : factors ) {
            writer.append( "\t" );
            for ( FactorValue fv : bioAssay.getSampleUsed().getAllFactorValues() ) {
                if ( fv.getExperimentalFactor().equals( factor ) ) {
                    writer.append( TsvUtils.format( FactorValueUtils.getValue( fv ) ) );
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
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }
}
