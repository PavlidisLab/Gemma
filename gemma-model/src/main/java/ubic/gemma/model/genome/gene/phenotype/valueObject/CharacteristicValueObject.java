/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * CharacteristicValueObject containing a category to a value
 * 
 * @author ??
 * @version $Id$
 */
public class CharacteristicValueObject implements Comparable<CharacteristicValueObject> {

    /**
     * @param characteristics
     * @return
     */
    public static Collection<CharacteristicValueObject> characteristic2CharacteristicVO(
            Collection<? extends Characteristic> characteristics ) {

        Collection<CharacteristicValueObject> characteristicValueObjects;

        if ( characteristics instanceof List )
            characteristicValueObjects = new ArrayList<>();
        else
            characteristicValueObjects = new HashSet<>();

        for ( Characteristic characteristic : characteristics ) {
            CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( characteristic );
            characteristicValueObjects.add( characteristicValueObject );
        }
        return characteristicValueObjects;
    }

    /** id used by url on the client side */
    protected String urlId = "";

    private boolean alreadyPresentInDatabase = false;
    private boolean alreadyPresentOnGene = false;

    private int numTimesUsed = 0;

    public int getNumTimesUsed() {
        return numTimesUsed;
    }

    public void setNumTimesUsed( int numTimesUsed ) {
        this.numTimesUsed = numTimesUsed;
    }

    private String category = "";
    private String categoryUri = null;

    /** child term from a root */
    private boolean child = false;
    private Long id = null; // MUST be initialized with null or have equality problems in javascript.

    /** what Ontology uses this term */
    private String ontologyUsed = null;

    private long privateGeneCount = 0L;
    /** number of occurrences in all genes */
    private long publicGeneCount = 0L;

    /** root of a query */
    private boolean root = false;
    private String taxon = "";

    private String value = "";

    private String valueUri = null;

    public CharacteristicValueObject() {
        super();
    }

    public CharacteristicValueObject( Characteristic characteristic ) {
        if ( characteristic instanceof VocabCharacteristic ) {
            this.valueUri = ( ( VocabCharacteristic ) characteristic ).getValueUri();
        }
        this.category = characteristic.getCategory();
        this.categoryUri = characteristic.getCategoryUri();
        this.value = characteristic.getValue();
        this.id = characteristic.getId();
    }

    public CharacteristicValueObject( String valueUri ) {
        super();
        this.valueUri = valueUri;
        if ( this.valueUri != null ) {
            if ( !this.valueUri.equals( "" ) && this.valueUri.indexOf( "#" ) > 0 ) {
                this.urlId = this.valueUri.substring( this.valueUri.lastIndexOf( "#" ) + 1, this.valueUri.length() );
            } else if ( this.valueUri.lastIndexOf( "/" ) > 0 ) {
                this.urlId = this.valueUri.substring( this.valueUri.lastIndexOf( "/" ) + 1, this.valueUri.length() );
            }
        }
    }

    public CharacteristicValueObject( String value, String valueUri ) {
        this( valueUri );
        this.value = value;
    }

    public CharacteristicValueObject( String value, String category, String valueUri, String categoryUri ) {
        this( valueUri );
        this.category = category;
        this.categoryUri = categoryUri;
        this.value = value;
    }

    @Override
    public int compareTo( CharacteristicValueObject o ) {

        if ( this.category != null && o.category != null && !this.category.equalsIgnoreCase( o.category ) ) {
            return ( this.category.compareToIgnoreCase( o.category ) );
        } else if ( this.taxon != null && o.taxon != null && !this.taxon.equalsIgnoreCase( o.taxon ) ) {
            return this.taxon.compareToIgnoreCase( o.taxon );
        } else if ( !this.value.equalsIgnoreCase( o.value ) ) {
            return this.value.compareToIgnoreCase( o.value );
        } else if ( this.valueUri != null ) {
            return this.valueUri.compareToIgnoreCase( o.valueUri );
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CharacteristicValueObject other = ( CharacteristicValueObject ) obj;
        if ( this.valueUri == null ) {
            if ( other.valueUri != null ) return false;
        } else if ( !this.valueUri.equals( other.valueUri ) ) return false;

        if ( this.value == null ) {
            if ( other.value != null ) return false;
        } else if ( !this.value.equals( other.value ) ) return false;

        return true;
    }

    public String getCategory() {
        return this.category;
    }

    public String getCategoryUri() {
        return this.categoryUri;
    }

    public Long getId() {
        return this.id;
    }

    public String getOntologyUsed() {
        return this.ontologyUsed;
    }

    public long getPrivateGeneCount() {
        return this.privateGeneCount;
    }

    public long getPublicGeneCount() {
        return this.publicGeneCount;
    }

    public String getTaxon() {
        return this.taxon;
    }

    public String getUrlId() {
        return this.urlId;
    }

    public String getValue() {
        return this.value;
    }

    public String getValueUri() {
        return this.valueUri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if ( this.valueUri != null ) {
            result = prime * result + this.valueUri.hashCode();
        } else {
            result = prime * result + this.value.hashCode();
        }
        return result;
    }

    public boolean isAlreadyPresentInDatabase() {
        return this.alreadyPresentInDatabase;
    }

    public boolean isAlreadyPresentOnGene() {
        return this.alreadyPresentOnGene;
    }

    public boolean isChild() {
        return this.child;
    }

    public boolean isRoot() {
        return this.root;
    }

    public void setAlreadyPresentInDatabase( boolean alreadyPresentInDatabase ) {
        this.alreadyPresentInDatabase = alreadyPresentInDatabase;
    }

    public void setAlreadyPresentOnGene( boolean alreadyPresentOnGene ) {
        this.alreadyPresentOnGene = alreadyPresentOnGene;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public void setChild( boolean child ) {
        this.child = child;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setOntologyUsed( String ontologyUsed ) {
        this.ontologyUsed = ontologyUsed;
    }

    public void setPrivateGeneCount( long privateGeneCount ) {
        this.privateGeneCount = privateGeneCount;
    }

    public void setPublicGeneCount( long publicGeneCount ) {
        this.publicGeneCount = publicGeneCount;
    }

    public void setRoot( boolean root ) {
        this.root = root;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    public void setUrlId( String urlId ) {
        this.urlId = urlId;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public void setValueUri( String valueUri ) {
        if ( valueUri == null )
            this.valueUri = null;
        else
            this.valueUri = valueUri.toLowerCase();
    }

    @Override
    public String toString() {
        return "[Category= " + category + " Value=" + value + ( valueUri != null ? " (" + valueUri + ")" : "" ) + "]";
    }

    /**
     * 
     */
    public void incrementOccurrenceCount() {
        this.numTimesUsed++;
    }

}
