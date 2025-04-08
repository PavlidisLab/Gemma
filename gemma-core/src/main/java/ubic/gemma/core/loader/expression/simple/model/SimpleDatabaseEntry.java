package ubic.gemma.core.loader.expression.simple.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class SimpleDatabaseEntry implements Serializable {

    public static SimpleDatabaseEntry fromAccession( String accession, String externalDatabaseName ) {
        SimpleDatabaseEntry entry = new SimpleDatabaseEntry();
        entry.setAccession( accession );
        entry.setExternalDatabaseName( externalDatabaseName );
        return entry;
    }

    private String accession;
    private Long externalDatabaseId;
    private String externalDatabaseName;
}
