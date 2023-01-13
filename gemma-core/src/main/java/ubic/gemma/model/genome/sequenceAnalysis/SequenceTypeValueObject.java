package ubic.gemma.model.genome.sequenceAnalysis;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import ubic.gemma.model.genome.biosequence.SequenceType;

import java.io.Serializable;

/**
 * @see SequenceType
 */
public class SequenceTypeValueObject implements Serializable {

    private String value;

    @SuppressWarnings("unused")
    public SequenceTypeValueObject() {

    }

    public SequenceTypeValueObject( SequenceType sequenceType ) {
        this.value = sequenceType.name();
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }
}
