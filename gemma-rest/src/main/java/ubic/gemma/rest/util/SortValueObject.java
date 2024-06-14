package ubic.gemma.rest.util;

import lombok.Data;
import ubic.gemma.persistence.util.Sort;

/**
 * Represents {@link Sort} as part of a {@link PaginatedResponseDataObject}.
 *
 * @author poirigui
 */
@Data
public class SortValueObject {

    private final String orderBy;
    private final String direction;

    public SortValueObject( Sort sort ) {
        // get the innermost sort
        // TODO: serialize sort as an array of sort objects
        while ( sort.getAndThen() != null ) sort = sort.getAndThen();
        if ( sort.getOriginalProperty() != null ) {
            this.orderBy = sort.getOriginalProperty();
        } else if ( sort.getObjectAlias() != null ) {
            this.orderBy = sort.getObjectAlias() + "." + sort.getPropertyName();
        } else {
            this.orderBy = sort.getPropertyName();
        }
        this.direction = sort.getDirection() != null ? sort.getDirection().toString() : null;
    }
}
