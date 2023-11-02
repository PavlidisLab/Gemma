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
package ubic.gemma.model.common.description;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static ubic.gemma.model.common.description.CharacteristicUtils.compareTerm;

/**
 * Value object representation of a {@link Characteristic}.
 * @see Characteristic
 * @author poirigui
 */
@Data
public class CharacteristicValueObject extends IdentifiableValueObject<Characteristic> implements Comparable<CharacteristicValueObject> {

    private static final Comparator<CharacteristicValueObject> COMPARATOR = Comparator
            .comparing( ( CharacteristicValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getCategory(), c1.getCategoryUri(), c2.getCategory(), c2.getCategoryUri() ) )
            .thenComparing( CharacteristicValueObject::getTaxon, Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) )
            .thenComparing( ( CharacteristicValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getValue(), c1.getValueUri(), c2.getValue(), c2.getValueUri() ) )
            .thenComparing( CharacteristicValueObject::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    private String category;
    private String categoryUri;
    private String value;
    private String valueUri;

    // TODO: all the following fields are Phenocarta-specific and should be relocated

    /**
     * id used by url on the client side
     */
    @GemmaWebOnly
    private String urlId = "";
    @GemmaWebOnly
    private boolean alreadyPresentInDatabase = false;
    @GemmaWebOnly
    private boolean alreadyPresentOnGene = false;
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
    @GemmaWebOnly
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
    /**
     * The definition of the value, if it is an ontology term, as supplied by the ontology. If the value is
     * free text, this will be empty
     */
    @GemmaWebOnly
    private String valueDefinition = "";

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
        this.category = characteristic.getCategory();
        this.categoryUri = characteristic.getCategoryUri();
        this.value = characteristic.getValue();
        this.valueUri = characteristic.getValueUri();
        this.urlId = parseUrlId( characteristic.getValueUri() );
    }

    public CharacteristicValueObject( String value, @Nullable String valueUri ) {
        this.valueUri = valueUri;
        this.value = value;
        this.urlId = parseUrlId( valueUri );
    }

    public CharacteristicValueObject( String value, @Nullable String valueUri, String category, @Nullable String categoryUri ) {
        this( value, valueUri );
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
        if ( this.getId() != null ) {
            return super.hashCode();
        }
        return Objects.hash( StringUtils.lowerCase( categoryUri != null ? categoryUri : category ),
                StringUtils.lowerCase( valueUri != null ? valueUri : value ) );
    }

    @Override
    public boolean equals( Object object ) {
        if ( object == null )
            return false;
        if ( this == object )
            return true;
        if ( !( object instanceof CharacteristicValueObject ) )
            return false;
        CharacteristicValueObject that = ( CharacteristicValueObject ) object;
        if ( this.getId() != null && that.getId() != null )
            return super.equals( object );
        return CharacteristicUtils.equals( category, categoryUri, that.category, that.categoryUri )
                && CharacteristicUtils.equals( value, valueUri, that.value, that.valueUri );
    }

    @Override
    public int compareTo( @Nonnull CharacteristicValueObject that ) {
        return COMPARATOR.compare( this, that );
    }

    @Override
    public String toString() {
        return String.format( "[Category=%s%s Value=%s%s]",
                category,
                categoryUri != null ? " (" + categoryUri + ")" : "",
                value,
                valueUri != null ? " (" + valueUri + ")" : "" );
    }

    public void incrementOccurrenceCount() {
        this.numTimesUsed++;
    }

    private static String parseUrlId( @Nullable String valueUri ) {
        if ( StringUtils.isBlank( valueUri ) )
            return "";
        if ( valueUri.indexOf( "#" ) > 0 ) {
            return valueUri.substring( valueUri.lastIndexOf( "#" ) + 1 );
        } else if ( valueUri.lastIndexOf( "/" ) > 0 ) {
            return valueUri.substring( valueUri.lastIndexOf( "/" ) + 1 );
        } else {
            return "";
        }
    }
}
