package ubic.gemma.core.loader.expression.simple.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

@Data
public class SimpleSampleMetadata implements Serializable {

    private String name;
    @Nullable
    private String description;
    private SimplePlatformMetadata platformUsed;
    private Collection<SimpleCharacteristic> characteristics = new HashSet<>();
    @Nullable
    private SimpleDatabaseEntry accession;
}
