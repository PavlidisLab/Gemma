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
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.AbstractDescribable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Instances of this are used to describe other entities. This base class is just a characteristic that is simply a
 * 'tag' of free text.
 * <p>
 * Characteristics can have an associated URI from an ontology from {@link #getCategoryUri()} and {@link #getValueUri()},
 * but not necessarily since there might not be an adequate term to represent the conveyed concept. These properties are
 * marked with {@link Nullable} and should always be handled with care.
 *
 * @author Paul
 */
@Indexed
public class Characteristic extends AbstractDescribable implements Serializable, Comparable<Characteristic> {

    private static final long serialVersionUID = -7242166109264718620L;

    private static final Comparator<Characteristic> BY_CATEGORY_COMPARATOR = ( c1, c2 ) -> CharacteristicUtils.compareTerm( c1.category, c1.categoryUri, c2.category, c2.categoryUri );

    private static final Comparator<Characteristic> BY_CATEGORY_AND_VALUE_COMPARATOR = BY_CATEGORY_COMPARATOR
            .thenComparing( c -> c, ( c1, c2 ) -> CharacteristicUtils.compareTerm( c1.value, c1.valueUri, c2.value, c2.valueUri ) );

    private static final Comparator<Characteristic> COMPARATOR = BY_CATEGORY_AND_VALUE_COMPARATOR
            .thenComparing( Characteristic::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    /**
     * Obtain a full comparator for characteristics that fallbacks on the ID if everything else is equal.
     * <p>
     * The following fields are compared: category, value, ID.
     */
    public static Comparator<Characteristic> getComparator() {
        return COMPARATOR;
    }


    /**
     * Obtain a comparator to compare terms by category URI (or category if null) in a case-insensitive manner.
     * <p>
     * Two terms that are equal in terms of category will be collapsed if using a {@link java.util.TreeSet}.
     * <p>
     * Use this if you want to get a summary of the categories used by a collection of terms irrespective of their IDs.
     */
    public static Comparator<Characteristic> getByCategoryComparator() {
        return BY_CATEGORY_COMPARATOR;
    }

    /**
     * Obtain a comparator to order terms by value URI (or value if null) in a case-insensitive manner.
     * <p>
     * Two terms that are equal in terms of category and value (i.e. sharing the same ID) will be collapsed if using a
     * {@link java.util.TreeSet}.
     * <p>
     * Use this if you want to get a summary of the annotations used by a collection of terms irrespective of their IDs.
     */
    public static Comparator<Characteristic> getByCategoryAndValueComparator() {
        return BY_CATEGORY_AND_VALUE_COMPARATOR;
    }

    private String category;
    @Nullable
    private String categoryUri;
    private GOEvidenceCode evidenceCode;
    /**
     * Stores the value this characteristic had before it was assigned a URI for the term.
     */
    private String originalValue = null;
    private String value;
    @Nullable
    private String valueUri;
    /**
     * Indicate if this "old-style" characteristic has been migrated to a {@link ubic.gemma.model.expression.experiment.Statement}.
     * This is only meaningful for {@link ubic.gemma.model.expression.experiment.FactorValue} characteristics.
     * @deprecated do not rely on this field, it will be removed once the migration is completed.
     */
    @Deprecated
    private boolean migratedToStatement;


    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Characteristic() {
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    /**
     * @return either the human readable form of the classUri or a free text version if no classUri exists
     */
    @Field
    public String getCategory() {
        return this.category;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    /**
     * @return The URI of the class that this is an instance of. Will only be different from the termUri when the class
     * is
     * effectively abstract, and this is a concrete instance. By putting the abstract class URI in the object we
     * can
     * more readily group together Characteristics that are instances of the same class. For example: If the
     * classUri is
     * "Sex", then the termUri might be "male" or "female" for various instances. Otherwise, the classUri and
     * the
     * termUri can be the same; for example, for "Age", if the "Age" is defined through its properties declared
     * as
     * associations with this.
     */
    @Nullable
    @Field(analyze = Analyze.NO)
    public String getCategoryUri() {
        return this.categoryUri;
    }

    public void setCategoryUri( @Nullable String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public GOEvidenceCode getEvidenceCode() {
        return this.evidenceCode;
    }

    public void setEvidenceCode( GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    /**
     * @return the originalValue
     */
    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue( String originalValue ) {
        this.originalValue = originalValue;
    }

    /**
     * @return The human-readable term (e.g., "OrganismPart"; "kinase")
     */
    @Field
    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    /**
     * @return This can be a URI to any resources that describes the characteristic. Often it might be a URI to an OWL
     * ontology
     * term. If the URI is an instance of an abstract class, the classUri should be filled in with the URI for
     * the
     * abstract class.
     */
    @Nullable
    @Field(analyze = Analyze.NO)
    public String getValueUri() {
        return this.valueUri;
    }

    public void setValueUri( @Nullable String uri ) {
        this.valueUri = uri;
    }

    @Deprecated
    public boolean isMigratedToStatement() {
        return migratedToStatement;
    }

    @Deprecated
    public void setMigratedToStatement( boolean migratedToStatement ) {
        this.migratedToStatement = migratedToStatement;
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
        if ( !( object instanceof Characteristic ) )
            return false;
        Characteristic that = ( Characteristic ) object;
        if ( this.getId() != null && that.getId() != null )
            return super.equals( object );

        /*
         * at this point, we know we have two Characteristics, at least one of which is transient, so we have to look at
         * the fields; we can't just compare the hashcodes because they also look at the id, so comparing one transient
         * and one persistent would always fail...
         */
        return CharacteristicUtils.equals( category, categoryUri, that.category, that.categoryUri )
                && CharacteristicUtils.equals( value, valueUri, that.value, that.valueUri );
    }

    @Override
    public int compareTo( @Nonnull Characteristic characteristic ) {
        return COMPARATOR.compare( this, characteristic );
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder( super.toString() );
        if ( category != null ) {
            b.append( " Category=" ).append( category );
            if ( categoryUri != null ) {
                b.append( " [" ).append( categoryUri ).append( "]" );
            }
        } else if ( categoryUri != null ) {
            b.append( " Category URI=" ).append( categoryUri );
        } else {
            b.append( " [No Category]" );
        }
        if ( value != null ) {
            b.append( " Value=" ).append( value );
            if ( valueUri != null ) {
                b.append( " [" ).append( valueUri ).append( "]" );
            }
        } else if ( valueUri != null ) {
            b.append( " Value URI=" ).append( valueUri );
        }
        return b.toString();
    }

    public static final class Factory {

        public static Characteristic newInstance() {
            return new Characteristic();
        }

        public static Characteristic newInstance( String name, String description, String value, @Nullable String valueUri,
                String category, @Nullable String categoryUri, GOEvidenceCode evidenceCode ) {
            final Characteristic entity = new Characteristic();
            entity.setName( name );
            entity.setDescription( description );
            entity.setValue( value );
            entity.setValueUri( StringUtils.stripToNull( valueUri ) );
            entity.setCategory( category );
            entity.setCategoryUri( StringUtils.stripToNull( categoryUri ) );
            entity.setEvidenceCode( evidenceCode );
            return entity;
        }

        public static Characteristic newInstance( Category category ) {
            Characteristic entity = new Characteristic();
            entity.setCategory( category.getCategory() );
            entity.setCategoryUri( category.getCategoryUri() );
            return entity;
        }

        public static Characteristic newInstance( Category category, String value, @Nullable String valueUri ) {
            Characteristic entity = new Characteristic();
            entity.setCategory( category.getCategory() );
            entity.setCategoryUri( category.getCategoryUri() );
            entity.setValue( value );
            entity.setValueUri( StringUtils.stripToNull( valueUri ) );
            return entity;
        }
    }

}