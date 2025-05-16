package ubic.gemma.core.loader.expression.simple.model;

import lombok.Data;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;

import java.io.Serializable;

@Data
public class SimplePlatformMetadata implements Serializable {

    public static SimplePlatformMetadata forId( Long id ) {
        SimplePlatformMetadata pm = new SimplePlatformMetadata();
        pm.setId( id );
        return pm;
    }

    public static SimplePlatformMetadata forName( String name ) {
        SimplePlatformMetadata pm = new SimplePlatformMetadata();
        pm.setName( name );
        return pm;
    }

    private Long id;
    // fields below are ignored if an ID is provided
    private String name;
    private String shortName;
    private String description;
    private TechnologyType technologyType;
}
