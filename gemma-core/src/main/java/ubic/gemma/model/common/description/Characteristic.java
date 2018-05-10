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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.Describable;

import java.util.Objects;

/**
 * Instances of this are used to describe other entities. This base class is just a characteristic that is simply a
 * 'tag' of free text.
 *
 * @author Paul
 */
public class Characteristic extends Describable {

    private static final long serialVersionUID = -7242166109264718620L;
    private String category;
    private String categoryUri;
    private GOEvidenceCode evidenceCode;
    private String value;
    private String valueUri;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Characteristic() {
    }

    @Override
    public boolean equals( Object object ) {
        if ( object == null )
            return false;
        if ( this == object )
            return true;
        if ( !( object instanceof Characteristic ) )
            return false;
        Characteristic that = ( Characteristic ) object;
        if ( this.getId() != null && that.getId() != null )
            return this.getId().equals( that.getId() );

        /*
         * at this point, we know we have two Characteristics, at least one of which is transient, so we have to look at
         * the fields; we can't just compare the hashcodes because they also look at the id, so comparing one transient
         * and one persistent would always fail...
         */
        return Objects.equals( this.getCategory(), that.getCategory() ) && Objects
                .equals( this.getValue(), that.getValue() );
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder( 17, 1 ).append( this.getId() ).append( this.getCategory() )
                .append( this.getValue() ).toHashCode();
    }

    @Override
    public String toString() {
        if ( StringUtils.isBlank( this.getCategory() ) ) {
            return "[No category] Value = " + this.getValue();
        }
        return "Category = " + this.getCategory() + " Value = " + this.getValue();
    }

    /**
     * @return This can be a URI to any resources that describes the characteristic. Often it might be a URI to an OWL
     *         ontology
     *         term. If the URI is an instance of an abstract class, the classUri should be filled in with the URI for
     *         the
     *         abstract class.
     */
    public String getValueUri() {
        return this.valueUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

    /**
     * @return either the human readable form of the classUri or a free text version if no classUri exists
     */
    public String getCategory() {
        return this.category;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    /**
     * @return The URI of the class that this is an instance of. Will only be different from the termUri when the class
     *         is
     *         effectively abstract, and this is a concrete instance. By putting the abstract class URI in the object we
     *         can
     *         more readily group together Characteristics that are instances of the same class. For example: If the
     *         classUri is
     *         "Sex", then the termUri might be "male" or "female" for various instances. Otherwise, the classUri and
     *         the
     *         termUri can be the same; for example, for "Age", if the "Age" is defined through its properties declared
     *         as
     *         associations with this.
     */
    public String getCategoryUri() {
        return this.categoryUri;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public GOEvidenceCode getEvidenceCode() {
        return this.evidenceCode;
    }

    public void setEvidenceCode( GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    /**
     * @return The human-readable term (e.g., "OrganismPart"; "kinase")
     */
    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public static final class Factory {

        public static Characteristic newInstance() {
            return new Characteristic();
        }

        public static Characteristic newInstance( String name, String description, String value,
                String category, String categoryUri, GOEvidenceCode evidenceCode ) {
            final Characteristic entity = new Characteristic();
            entity.setName( name );
            entity.setDescription( description );
            entity.setValue( value );
            entity.setCategory( category );
            entity.setCategoryUri( categoryUri );
            entity.setEvidenceCode( evidenceCode );
            return entity;
        }
    }

}