package ubic.gemma.model.common;

import java.io.Serializable;

public interface Describable extends Identifiable, Serializable {

    /**
     * Obtain a human-readable description of the object
     */
    String getDescription();

    /**
     * Obtain the name of an object is a possibly ambiguous human-readable identifier that need not be an external
     * database reference.
     */
    String getName();
}
