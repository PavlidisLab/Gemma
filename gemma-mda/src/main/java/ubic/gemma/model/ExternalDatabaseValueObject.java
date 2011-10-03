package ubic.gemma.model;

import ubic.gemma.model.common.description.ExternalDatabase;

public class ExternalDatabaseValueObject {

    private String name;
    private String webURI;

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public static ExternalDatabaseValueObject fromEntity( ExternalDatabase ed ) {
        if ( ed == null ) return null;
        ExternalDatabaseValueObject vo = new ExternalDatabaseValueObject();
        vo.setName( ed.getName() );
        vo.setWebURI( ed.getWebUri() );
        return vo;
    }

    public String getWebURI() {
        return webURI;
    }

    public void setWebURI( String webURI ) {
        this.webURI = webURI;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( webURI == null ) ? 0 : webURI.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        ExternalDatabaseValueObject other = ( ExternalDatabaseValueObject ) obj;
        if ( name == null ) {
            if ( other.name != null ) return false;
        } else if ( !name.equals( other.name ) ) return false;
        if ( webURI == null ) {
            if ( other.webURI != null ) return false;
        } else if ( !webURI.equals( other.webURI ) ) return false;
        return true;
    }
}
