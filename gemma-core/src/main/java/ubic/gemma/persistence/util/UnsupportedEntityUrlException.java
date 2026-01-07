package ubic.gemma.persistence.util;

import lombok.Getter;
import ubic.gemma.model.common.Identifiable;

public class UnsupportedEntityUrlException extends RuntimeException {

    @Getter
    private final Class<? extends Identifiable> entityType;

    public <T extends Identifiable> UnsupportedEntityUrlException( String message, Class<T> entityType ) {
        super( message );
        this.entityType = entityType;
    }
}
