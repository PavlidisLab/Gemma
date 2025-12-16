package ubic.gemma.model.expression.bioAssayData;

import java.util.Objects;

/**
 * @author poirigui
 */
class RawExpressionDataVectorNumberOfCells extends BulkExpressionDataVectorNumberOfCells {

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof RawExpressionDataVectorNumberOfCells ) ) {
            return false;
        }
        RawExpressionDataVectorNumberOfCells that = ( RawExpressionDataVectorNumberOfCells ) object;
        return Objects.equals( getId(), that.getId() );
    }

    static class Factory {
        static RawExpressionDataVectorNumberOfCells newInstance( RawExpressionDataVector vector, int[] numberOfCells ) {
            RawExpressionDataVectorNumberOfCells result = new RawExpressionDataVectorNumberOfCells();
            result.setVector( vector );
            result.setNumberOfCells( numberOfCells );
            return result;
        }
    }
}
