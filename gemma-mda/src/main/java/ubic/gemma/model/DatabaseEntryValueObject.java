package ubic.gemma.model;

import ubic.gemma.model.common.description.DatabaseEntry;

public class DatabaseEntryValueObject {

    private String accession;
    private ExternalDatabaseValueObject externalDatabase;

    public String getAccession() {
        return accession;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public ExternalDatabaseValueObject getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase( ExternalDatabaseValueObject externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public static DatabaseEntryValueObject fromEntity( DatabaseEntry de ) {
        if ( de == null ) return null;
        DatabaseEntryValueObject vo = new DatabaseEntryValueObject();
        vo.setAccession( de.getAccession() );
        vo.setExternalDatabase( ExternalDatabaseValueObject.fromEntity( de.getExternalDatabase() ) );
        return vo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( accession == null ) ? 0 : accession.hashCode() );
        result = prime * result + ( ( externalDatabase == null ) ? 0 : externalDatabase.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        DatabaseEntryValueObject other = ( DatabaseEntryValueObject ) obj;
        if ( accession == null ) {
            if ( other.accession != null ) return false;
        } else if ( !accession.equals( other.accession ) ) return false;
        if ( externalDatabase == null ) {
            if ( other.externalDatabase != null ) return false;
        } else if ( !externalDatabase.equals( other.externalDatabase ) ) return false;
        return true;
    }

}
