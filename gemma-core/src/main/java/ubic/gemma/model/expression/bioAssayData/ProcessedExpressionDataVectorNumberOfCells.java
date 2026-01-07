package ubic.gemma.model.expression.bioAssayData;

import java.util.Objects;

/**
 * @author poirigui
 */
class ProcessedExpressionDataVectorNumberOfCells extends BulkExpressionDataVectorNumberOfCells {

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof ProcessedExpressionDataVectorNumberOfCells ) ) {
            return false;
        }
        ProcessedExpressionDataVectorNumberOfCells that = ( ProcessedExpressionDataVectorNumberOfCells ) object;
        return Objects.equals( getId(), that.getId() );

    }

    static class Factory {
        static ProcessedExpressionDataVectorNumberOfCells newInstance( ProcessedExpressionDataVector vector, int[] numberOfCells ) {
            ProcessedExpressionDataVectorNumberOfCells result = new ProcessedExpressionDataVectorNumberOfCells();
            result.setVector( vector );
            result.setNumberOfCells( numberOfCells );
            return result;
        }
    }
}
