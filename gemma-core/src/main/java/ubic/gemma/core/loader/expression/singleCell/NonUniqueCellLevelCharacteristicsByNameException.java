package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.common.NonUniqueDescribableByNameException;

public class NonUniqueCellLevelCharacteristicsByNameException extends NonUniqueDescribableByNameException {

    public NonUniqueCellLevelCharacteristicsByNameException( NonUniqueDescribableByNameException e ) {
        super( e.getMessage(), e.getCause() );
    }
}
