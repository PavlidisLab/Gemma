package ubic.gemma.persistence.service.common.quantitationtype;

import org.hibernate.NonUniqueResultException;

/**
 * Exception raised when retrieving a non-unique QT by name.
 * @author poirigui
 */
public class NonUniqueQuantitationTypeByNameException extends Exception {

    public NonUniqueQuantitationTypeByNameException( String message, NonUniqueResultException cause ) {
        super( message, cause );
    }
}
