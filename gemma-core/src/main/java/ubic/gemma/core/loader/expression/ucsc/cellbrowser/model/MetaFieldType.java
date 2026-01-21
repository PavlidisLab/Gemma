package ubic.gemma.core.loader.expression.ucsc.cellbrowser.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MetaFieldType {
    UNIQUE_STRING( "uniqueString" ),
    INT( "int" ),
    FLOAT( "float" ),
    ENUM( "enum" );

    private final String value;

    MetaFieldType( String value ) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
