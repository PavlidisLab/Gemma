/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.common.description;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import ubic.gemma.model.association.GOEvidenceCode;

import java.util.Objects;

/**
 * <p>
 * A Characteristic that uses terms from ontologies or controlled vocabularies. These Characteristics can be chained
 * together in complex ways.
 * </p>
 * <p>
 * A Characteristic can form an RDF-style triple, with a Term (the subject) a CharacteristicProperty (the predicate) and
 * an object (either another Characteristic or a DataProperty to hold a literal value).
 * </p>
 */
public class VocabCharacteristic extends Characteristic {

    private static final long serialVersionUID = 9108913504702857653L;

    @Override
    public boolean equals( Object object ) {
        if ( !super.equals( object ) )
            return false;
        if ( !( object instanceof VocabCharacteristic ) )
            return false;
        VocabCharacteristic that = ( VocabCharacteristic ) object;
        return Objects.equals( this.getCategoryUri(), that.getCategoryUri() ) && Objects
                .equals( this.getValueUri(), that.getValueUri() );
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder( 17, 3 ).appendSuper( super.hashCode() ).append( this.getCategoryUri() )
                .append( this.getValueUri() ).toHashCode();
    }

    @Override
    public String toString() {
        // return toString( 0 );
        return super.toString() + " categoryUri=" + this.getCategoryUri() + " valueUri=" + this.getValueUri();
    }

    public static final class Factory {

        public static VocabCharacteristic newInstance() {
            return new VocabCharacteristic();
        }

        public static VocabCharacteristic newInstance( String name, String description,
                String value, String valueUri, String category, String categoryUri, GOEvidenceCode evidenceCode ) {
            final VocabCharacteristic entity = new VocabCharacteristic();
            entity.setName( name );
            entity.setDescription( description );
            entity.setCategoryUri( categoryUri );
            entity.setValueUri( valueUri );
            entity.setValue( value );
            entity.setCategory( category );
            entity.setEvidenceCode( evidenceCode );
            return entity;
        }

    }

}