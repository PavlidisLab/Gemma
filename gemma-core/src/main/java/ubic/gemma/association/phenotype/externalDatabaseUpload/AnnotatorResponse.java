package ubic.gemma.association.phenotype.externalDatabaseUpload;

import java.util.HashSet;

public class AnnotatorResponse implements Comparable<AnnotatorResponse> {

    private Integer score = 0;
    private String value = "";
    private String valueUri = "";
    private boolean synonym = false;

    private String searchQuery = "";
    private String ontologyUsed = "";

    private HashSet<String> synonyms = new HashSet<String>();

    public AnnotatorResponse( Integer score, String value, String valueUri, String searchQuery, String ontologyUsed ) {
        super();
        this.score = score;
        this.value = value;
        this.valueUri = valueUri;
        this.searchQuery = searchQuery;
        this.ontologyUsed = ontologyUsed;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore( Integer score ) {
        this.score = score;
    }

    public String getValue() {
        return value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public String getValueUri() {
        return valueUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery( String searchQuery ) {
        this.searchQuery = searchQuery;
    }

    @Override
    public int compareTo( AnnotatorResponse annotatorResponse ) {

        if ( isMatch() ) {
            if ( ontologyUsed.equalsIgnoreCase( "DOID" ) ) {
                return -1;
            }
            if ( annotatorResponse.isMatch() ) {
                return 1;
            }
            return -1;
        }
        return 1;
    }

    @Override
    public String toString() {
        return "AnnotatorResponse [score=" + score + ", value=" + value + ", valueUri=" + valueUri + ", searchQuery="
                + searchQuery + "]";
    }

    public HashSet<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms( HashSet<String> synonyms ) {
        this.synonyms = synonyms;
    }

    public void addSynonyms( String line ) {

        String[] tokens = line.split( "\n" );

        for ( String token : tokens ) {

            token = token.trim();

            if ( !token.isEmpty() ) {

                this.synonyms.add( token );

            }

        }

    }

    public boolean isSynonym() {
        return synonym;
    }

    public void setSynonym( boolean synonym ) {
        this.synonym = synonym;
    }

    public boolean isExactMatch() {

        if ( this.value.equalsIgnoreCase( this.searchQuery ) ) {
            return true;
        }

        return false;
    }

    public String getOntologyUsed() {
        return ontologyUsed;
    }

    public void setOntologyUsed( String ontologyUsed ) {
        this.ontologyUsed = ontologyUsed;
    }

    public boolean isMatch() {

        if ( this.value.equalsIgnoreCase( searchQuery ) || synonym ) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( ontologyUsed == null ) ? 0 : ontologyUsed.hashCode() );
        result = prime * result + ( ( score == null ) ? 0 : score.hashCode() );
        result = prime * result + ( ( searchQuery == null ) ? 0 : searchQuery.hashCode() );
        result = prime * result + ( ( valueUri == null ) ? 0 : valueUri.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        AnnotatorResponse other = ( AnnotatorResponse ) obj;
        if ( ontologyUsed == null ) {
            if ( other.ontologyUsed != null ) return false;
        } else if ( !ontologyUsed.equals( other.ontologyUsed ) ) return false;
        if ( score == null ) {
            if ( other.score != null ) return false;
        } else if ( !score.equals( other.score ) ) return false;
        if ( searchQuery == null ) {
            if ( other.searchQuery != null ) return false;
        } else if ( !searchQuery.equals( other.searchQuery ) ) return false;
        if ( valueUri == null ) {
            if ( other.valueUri != null ) return false;
        } else if ( !valueUri.equals( other.valueUri ) ) return false;
        return true;
    }

}
