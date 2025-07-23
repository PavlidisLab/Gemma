package ubic.gemma.core.datastructure.matrix.io;

import ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils;
import ubic.gemma.core.analysis.preprocess.convert.UnsupportedQuantitationScaleConversionException;
import ubic.gemma.core.analysis.preprocess.convert.UnsupportedQuantitationTypeConversionException;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.constructAssayName;

/**
 * Generate a tabular matrix format compatible with <a href="https://cellbrowser.readthedocs.io/en/master/tabsep.html">Cell Browser</a>.
 * @author poirigui
 */
public class CellBrowserTabularMatrixWriter implements SingleCellExpressionDataMatrixWriter {

    private boolean useBioAssayIds = false;
    private boolean autoFlush = false;
    private ScaleType scaleType = null;

    public void setUseBioAssayIds( boolean useBioAssayIds ) {
        this.useBioAssayIds = useBioAssayIds;
    }

    @Override
    public void setAutoFlush( boolean autoFlush ) {
        this.autoFlush = autoFlush;
    }

    @Override
    public void setScaleType( @Nullable ScaleType scaleType ) {
        this.scaleType = scaleType;
    }

    @Override
    public int write( SingleCellExpressionDataMatrix<?> matrix, Writer writer ) throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException( "Writing single-cell matrices is not supported." );
    }

    public int write( Stream<SingleCellExpressionDataVector> vectors, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
        return write( vectors.iterator(), cs2gene, writer );
    }

    public int write( Collection<SingleCellExpressionDataVector> vectors, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
        return write( vectors.iterator(), cs2gene, writer );
    }

    private int write( Iterator<SingleCellExpressionDataVector> vectors, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
        SingleCellExpressionDataVector firstVec = vectors.next();
        writeHeader( firstVec.getSingleCellDimension(), writer );
        int written = 0;
        writeVector( firstVec, cs2gene, writer );
        written++;
        while ( vectors.hasNext() ) {
            writeVector( vectors.next(), cs2gene, writer );
            written++;
        }
        return written;
    }

    private void writeHeader( SingleCellDimension singleCellDimension, Writer writer ) throws IOException {
        writer.append( "gene" );
        for ( int sampleIndex = 0; sampleIndex < singleCellDimension.getBioAssays().size(); sampleIndex++ ) {
            BioAssay bioAssay = singleCellDimension.getBioAssays().get( sampleIndex );
            String sampleId = getSampleId( bioAssay );
            for ( String cellId : singleCellDimension.getCellIdsBySample( sampleIndex ) ) {
                writer.append( "\t" ).append( sampleId ).append( "_" ).append( cellId );
            }
        }
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }

    private void writeVector( SingleCellExpressionDataVector vector, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
        if ( scaleType != null ) {
            String valueIfMissing;
            try {
                Number val = QuantitationTypeUtils.getDefaultValueAsNumber( vector.getQuantitationType() );
                valueIfMissing = TsvUtils.format( ScaleTypeConversionUtils.convertScalar( val, vector.getQuantitationType(), scaleType ) );
            } catch ( UnsupportedQuantitationScaleConversionException e ) {
                throw new RuntimeException( e );
            }
            try {
                writeVector( vector, ScaleTypeConversionUtils.convertData( vector, scaleType ), PrimitiveType.DOUBLE, cs2gene, valueIfMissing, writer );
            } catch ( UnsupportedQuantitationTypeConversionException e ) {
                throw new RuntimeException( e );
            }
        } else {
            String valueIfMissing = TsvUtils.format( QuantitationTypeUtils.getDefaultValueAsNumber( vector.getQuantitationType() ) );
            switch ( vector.getQuantitationType().getRepresentation() ) {
                case DOUBLE:
                    writeVector( vector, vector.getDataAsDoubles(), PrimitiveType.DOUBLE, cs2gene, valueIfMissing, writer );
                    break;
                case FLOAT:
                    writeVector( vector, vector.getDataAsFloats(), PrimitiveType.FLOAT, cs2gene, valueIfMissing, writer );
                    break;
                case LONG:
                    writeVector( vector, vector.getDataAsLongs(), PrimitiveType.LONG, cs2gene, valueIfMissing, writer );
                    break;
                case INT:
                    writeVector( vector, vector.getDataAsInts(), PrimitiveType.INT, cs2gene, valueIfMissing, writer );
                    break;
            }
        }
        if ( autoFlush ) {
            writer.flush();
        }
    }

    private void writeVector( SingleCellExpressionDataVector vector, Object data, PrimitiveType representation, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, String valueIfMissing, Writer writer ) throws IOException {
        writeDesignElement( vector.getDesignElement(), cs2gene, writer );
        int numCells = vector.getSingleCellDimension().getNumberOfCells();
        int k = 0;
        for ( int i = 0; i < numCells; i++ ) {
            writer.append( "\t" );
            if ( k < vector.getDataIndices().length && i == vector.getDataIndices()[k] ) {
                switch ( representation ) {
                    case DOUBLE:
                        writer.append( TsvUtils.format( ( ( double[] ) data )[k] ) );
                        break;
                    case FLOAT:
                        writer.append( TsvUtils.format( ( ( float[] ) data )[k] ) );
                        break;
                    case LONG:
                        writer.append( TsvUtils.format( ( ( long[] ) data )[k] ) );
                        break;
                    case INT:
                        writer.append( TsvUtils.format( ( ( int[] ) data )[k] ) );
                        break;
                }
                k++;
            } else {
                writer.append( valueIfMissing );
            }
        }
        writer.append( "\n" );
    }

    private void writeDesignElement( CompositeSequence designElement, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
        writer.append( designElement.getName() );
        if ( cs2gene != null && cs2gene.containsKey( designElement ) ) {
            for ( Gene gene : cs2gene.get( designElement ) ) {
                if ( gene.getOfficialSymbol() != null ) {
                    writer.append( "|" ).append( gene.getOfficialSymbol() );
                }
            }
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
