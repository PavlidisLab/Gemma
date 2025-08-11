package ubic.gemma.core.datastructure.matrix.io;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommonsLog
public class TabularSingleCellMetadataWriter implements SingleCellMetadataWriter {

    @Setter
    private boolean useBioAssayIds = false;
    @Setter
    private boolean useRawColumnNames = false;
    @Setter
    private boolean autoFlush = false;

    @Override
    public void write( ExpressionExperiment ee, SingleCellDimension singleCellDimension, Writer writer ) throws IOException {
        List<ExperimentalFactor> factors;
        if ( ee.getExperimentalDesign() != null ) {
            factors = ee.getExperimentalDesign().getExperimentalFactors().stream()
                    .sorted( ExperimentalFactor.COMPARATOR )
                    .collect( Collectors.toList() );
        } else {
            log.warn( ee + " does not have an experimental design, no factors will be written." );
            factors = Collections.emptyList();
        }
        List<BioMaterial> samples = singleCellDimension.getBioAssays().stream()
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toList() );
        Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap = ExperimentalDesignUtils.getFactorValueMap( ee.getExperimentalDesign(), samples );
        List<CellLevelCharacteristics> clcs = new ArrayList<>( singleCellDimension.getCellTypeAssignments().size() + singleCellDimension.getCellLevelCharacteristics().size() );
        singleCellDimension.getCellTypeAssignments().stream()
                .sorted( CellTypeAssignment.COMPARATOR )
                .forEach( clcs::add );
        singleCellDimension.getCellLevelCharacteristics().stream()
                .sorted( CellLevelCharacteristics.COMPARATOR )
                .forEach( clcs::add );
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
        String[] columnNames = new String[2 + factors.size() + clcs.size()];
        int i = 0;
        columnNames[i++] = "sample_id";
        columnNames[i++] = "cell_id";
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
        if ( useBioAssayIds ) {
            writer.append( String.valueOf( bioAssay.getId() ) );
        } else if ( useRawColumnNames ) {
            writer.append( bioAssay.getShortName() != null ? bioAssay.getShortName() : bioAssay.getName() );
        } else {
            writer.append( ExpressionDataWriterUtils.constructAssayName( bioAssay ) );
        }
        writer.append( "\t" ).append( cellId );
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
