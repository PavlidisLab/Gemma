package ubic.gemma.model.genome.sequenceAnalysis;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.genome.biosequence.SequenceType;

import java.io.Serializable;

/**
 * @see SequenceType
 */
@Schema(implementation = SequenceType.class)
@Setter
public class SequenceTypeValueObject implements Serializable {

    @JsonValue
    @Getter
    private String value;

    @SuppressWarnings("unused")
    public SequenceTypeValueObject() {

    }

    public SequenceTypeValueObject( SequenceType sequenceType ) {
        this.value = sequenceType.name();
    }
}
