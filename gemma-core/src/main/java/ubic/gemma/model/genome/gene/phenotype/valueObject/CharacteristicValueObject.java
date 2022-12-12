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

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.description.Characteristic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * ValueObject wrapper for a Characteristic
 *
 * @see Characteristic
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Used in frontend
@Data
public class CharacteristicValueObject extends IdentifiableValueObject<Characteristic>
        implements Comparable<CharacteristicValueObject> {

    private static final Log log = LogFactory.getLog( CharacteristicValueObject.class );
    /**
     * id used by url on the client side
     */
    @GemmaWebOnly
    private String urlId = "";
    @GemmaWebOnly
    private boolean alreadyPresentInDatabase = false;
    @GemmaWebOnly
    private boolean alreadyPresentOnGene = false;
    private String category = "";
    private String categoryUri = null;
    /**
     * child term from a root
     */
    @GemmaWebOnly
    private boolean child = false;
    @GemmaWebOnly
    private int numTimesUsed = 0;
    /**
     * what Ontology uses this term
     */
    private String ontologyUsed = null;
    @GemmaWebOnly
    private long privateGeneCount = 0L;
    /**
     * number of occurrences in all genes
     */
    @GemmaWebOnly
    private long publicGeneCount = 0L;
    /**
     * root of a query
     */
    @GemmaWebOnly
    private boolean root = false;
    @GemmaWebOnly
    private String taxon = "";
    private String value = "";
    private String valueUri = null;

    /**
     * Required when using the class as a spring bean.
     */
    public CharacteristicValueObject() {
        super();
    }

    public CharacteristicValueObject( Long id ) {
        super( id );
    }

    public CharacteristicValueObject( Characteristic characteristic ) {
        super( characteristic );
        {
            this.valueUri = characteristic.getValueUri();
            if ( this.valueUri != null )
                this.urlId = parseUrlId( this.valueUri );
        }
        this.category = characteristic.getCategory();
        this.categoryUri = characteristic.getCategoryUri();
        this.value = characteristic.getValue();

        if ( this.value == null ) {
            CharacteristicValueObject.log
                    .warn( "Characteristic with null value. Id: " + this.id + " cat: " + this.category + " cat uri: "
                            + this.categoryUri );
        }
    }

    public CharacteristicValueObject( Long id, String valueUri ) {
        super( id );
        this.valueUri = valueUri;
        this.urlId = parseUrlId( this.valueUri );
        if ( StringUtils.isNotBlank( this.urlId ) ) {
            try {
                // we don't always populate from the database, give it an id anyway
                this.id = new Long( this.urlId.replaceAll( "[^\\d.]", "" ) );
            } catch ( Exception e ) {
                CharacteristicValueObject.log
                        .error( "Problem making an id for Phenotype: " + this.urlId + ": " + e.getMessage() );
            }
        }
    }

    public CharacteristicValueObject( Long id, String value, String valueUri ) {
        this( id, valueUri );
        this.value = value;
        if ( this.value == null ) {
            CharacteristicValueObject.log
                    .warn( "Characteristic with null value. Id: " + this.id + " cat: " + this.category + " cat uri: "
                            + this.categoryUri );
        }
    }

    public CharacteristicValueObject( Long id, String value, String category, String valueUri, String categoryUri ) {
        this( id, value, valueUri );
        this.category = category;
        this.categoryUri = categoryUri;
    }

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if ( this.valueUri != null ) {
            result = prime * result + this.valueUri.hashCode();
        } else if ( this.value != null ) {
            result = prime * result + this.value.hashCode();
        } else {
            result = prime * result + this.id.hashCode();
        }
        return result;
    }

    @Override
    public int compareTo( CharacteristicValueObject o ) {
        return ComparisonChain.start()
                .compare( category, o.category, Ordering.from( String.CASE_INSENSITIVE_ORDER ).nullsLast() )
                .compare( taxon, o.taxon, Ordering.from( String.CASE_INSENSITIVE_ORDER ).nullsLast() )
                .compare( value, o.value, Ordering.from( String.CASE_INSENSITIVE_ORDER ).nullsLast() )
                .compare( valueUri, o.valueUri, Ordering.from( String.CASE_INSENSITIVE_ORDER ).nullsLast() ).result();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        CharacteristicValueObject other = ( CharacteristicValueObject ) obj;
        if ( this.valueUri == null ) {
            if ( other.valueUri != null )
                return false;
        } else {
            return this.valueUri.equals( other.valueUri );
        }

        if ( this.value == null ) {
            return other.value == null;
        }
        return this.value.equals( other.value );

    }

    @Override
    public String toString() {
        return "[Category= " + category + " Value=" + value + ( valueUri != null ? " (" + valueUri + ")" : "" ) + "]";
    }

    public void incrementOccurrenceCount() {
        this.numTimesUsed++;
    }

    private static String parseUrlId( String valueUri ) {
        if ( StringUtils.isBlank( valueUri ) )
            return "";
        if ( valueUri.indexOf( "#" ) > 0 ) {
            return valueUri.substring( valueUri.lastIndexOf( "#" ) + 1, valueUri.length() );
        } else if ( valueUri.lastIndexOf( "/" ) > 0 ) {
            return valueUri.substring( valueUri.lastIndexOf( "/" ) + 1, valueUri.length() );
        } else {
            return "";
        }
    }
}
