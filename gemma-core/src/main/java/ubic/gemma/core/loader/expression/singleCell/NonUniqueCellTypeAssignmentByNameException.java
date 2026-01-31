package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.common.NonUniqueDescribableByNameException;

public class NonUniqueCellTypeAssignmentByNameException extends NonUniqueDescribableByNameException {

    public NonUniqueCellTypeAssignmentByNameException( NonUniqueDescribableByNameException e ) {
        super( e.getMessage(), e.getCause() );
    }
}
