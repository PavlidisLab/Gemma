package ubic.gemma.core.loader.expression.simple.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.io.Serializable;

@Data
public class SimpleTaxonMetadata implements Serializable {

    public static SimpleTaxonMetadata forId( Long id ) {
        SimpleTaxonMetadata t = new SimpleTaxonMetadata();
        t.setId( id );
        return t;
    }

    public static SimpleTaxonMetadata forNcbiId( Integer ncbiId ) {
        SimpleTaxonMetadata t = new SimpleTaxonMetadata();
        t.setNcbiId( ncbiId );
        return t;
    }

    public static SimpleTaxonMetadata forName( String name ) {
        SimpleTaxonMetadata t = new SimpleTaxonMetadata();
        t.setName( name );
        return t;
    }

    @Nullable
    private Long id;
    // fields below are ignored if an ID is provided
    @Nullable
    private Integer ncbiId;
    /**
     * Taxon name, either common or scientific.
     */
    @Nullable
    private String name;
}
