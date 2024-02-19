package ubic.gemma.model.analysis;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.protocol.Protocol;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProtocolValueObject extends IdentifiableValueObject<Protocol> {

    private String name;

    private String description;

    private Set<CharacteristicValueObject> characteristics;

    public ProtocolValueObject( Protocol protocol ) {
        super( protocol );
        this.name = protocol.getName();
        this.description = protocol.getDescription();
    }
}
