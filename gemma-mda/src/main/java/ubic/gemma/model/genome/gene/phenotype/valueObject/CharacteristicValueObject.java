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

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * CharacteristicValueObject containing a category to a value
 * 
 * @author ??
 * @version $Id$
 */
public class CharacteristicValueObject implements Comparable<CharacteristicValueObject> {

    private Long id = null; // MUST be initialized with null or have equality problems in javascript.

    /** id used by url on the client side */
    protected String urlId = "";

    private String category = "";
    private String categoryUri = "";

    private String value = "";
    private String valueUri = "";

    /** number of occurrences in all genes */
    private long publicGeneCount = 0L;
    private long privateGeneCount = 0L;

    /** what Ontology uses this term */
    private String ontologyUsed = null;

    private boolean alreadyPresentOnGene = false;
    private boolean alreadyPresentInDatabase = false;

    /** child term from a root */
    private boolean child = false;
    /** root of a query */
    private boolean root = false;

    private String taxon = "";

    public static Collection<CharacteristicValueObject> characteristic2CharacteristicVO(
            Collection<Characteristic> characteristics ) {

        Collection<CharacteristicValueObject> characteristicValueObjects = new HashSet<CharacteristicValueObject>();

        for ( Characteristic characteristic : characteristics ) {

            CharacteristicValueObject characteristicValueObject = null;

            if ( characteristic instanceof VocabCharacteristic ) {
                characteristicValueObject = new CharacteristicValueObject( ( VocabCharacteristic ) characteristic );
            } else {
                characteristicValueObject = new CharacteristicValueObject( characteristic );
            }

            characteristicValueObjects.add( characteristicValueObject );

            if ( characteristic.getDescription() != null && characteristic.getDescription().indexOf( " -USED- " ) != -1 ) {
                characteristicValueObject.setAlreadyPresentInDatabase( true );
            }
        }
        return characteristicValueObjects;
    }

    public CharacteristicValueObject() {
        super();
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

    public CharacteristicValueObject( VocabCharacteristic vocabCharacteristic ) {
        this( vocabCharacteristic.getValueUri() );
        this.category = vocabCharacteristic.getCategory();
        this.categoryUri = vocabCharacteristic.getCategoryUri();
        this.value = vocabCharacteristic.getValue();
        if ( vocabCharacteristic.getId() != null ) {
            this.id = vocabCharacteristic.getId();
        }
    }

    public CharacteristicValueObject( Characteristic characteristic ) {
        this.category = characteristic.getCategory();
        this.value = characteristic.getValue();
        this.id = characteristic.getId();
    }

    public long getPublicGeneCount() {
        return this.publicGeneCount;
    }

    public void setPublicGeneCount( long publicGeneCount ) {
        this.publicGeneCount = publicGeneCount;
    }

    public long getPrivateGeneCount() {
        return this.privateGeneCount;
    }

    public void setPrivateGeneCount( long privateGeneCount ) {
        this.privateGeneCount = privateGeneCount;
    }

    public String getCategoryUri() {
        return this.categoryUri;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public String getValueUri() {
        return this.valueUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public boolean isChild() {
        return this.child;
    }

    public void setChild( boolean child ) {
        this.child = child;
    }

    public boolean isRoot() {
        return this.root;
    }

    public void setRoot( boolean root ) {
        this.root = root;
    }

    public String getOntologyUsed() {
        return this.ontologyUsed;
    }

    public void setOntologyUsed( String ontologyUsed ) {
        this.ontologyUsed = ontologyUsed;
    }

    public boolean isAlreadyPresentOnGene() {
        return this.alreadyPresentOnGene;
    }

    public void setAlreadyPresentOnGene( boolean alreadyPresentOnGene ) {
        this.alreadyPresentOnGene = alreadyPresentOnGene;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getUrlId() {
        return this.urlId;
    }

    public void setUrlId( String urlId ) {
        this.urlId = urlId;
    }

    public boolean isAlreadyPresentInDatabase() {
        return this.alreadyPresentInDatabase;
    }

    public void setAlreadyPresentInDatabase( boolean alreadyPresentInDatabase ) {
        this.alreadyPresentInDatabase = alreadyPresentInDatabase;
    }

    public String getTaxon() {
        return this.taxon;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
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

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CharacteristicValueObject other = ( CharacteristicValueObject ) obj;
        if ( this.valueUri == null ) {
            if ( other.valueUri != null ) return false;
        } else if ( !this.valueUri.equals( other.valueUri ) ) return false;

        if ( this.valueUri == null && other.valueUri == null ) {
            if ( this.value == null ) {
                if ( other.value != null ) return false;
            } else if ( !this.value.equals( other.value ) ) return false;
        }
        return true;
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
            return -1;
        }
    }

}
