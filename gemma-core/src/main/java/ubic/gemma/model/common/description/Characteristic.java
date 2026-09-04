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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.AbstractDescribable;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.Comparator;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.stripToNull;

/**
 * Instances of this are used to describe other entities. This base class is just a characteristic that is simply a
 * 'tag' of free text.
 * <p>
 * Characteristics can have an associated URI from an ontology from {@link #getCategoryUri()} and {@link #getValueUri()},
 * but not necessarily since there might not be an adequate term to represent the conveyed concept. These properties are
 * marked with {@link Nullable} and should always be handled with care.
 * <p>
 * Hibernate Search 7 mapping: indexed root and a critical {@code @IndexedEmbedded} contributor —
 * the {@code value/valueUri} pair is what makes "free-text-over-ontology-terms-as-they-appear-on-datasets"
 * work (see SEARCH_RECCE.md Section 5.1).
 *
 * @author Paul
 */
@Entity
@Table(name = "CHARACTERISTIC")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "class", discriminatorType = DiscriminatorType.STRING, length = 255)
// Hbm had `<class discriminator-value="null"><discriminator not-null="false"/>` — i.e., the
// root Characteristic rows persist with SQL NULL in the `class` column, and Statement rows
// persist with "Statement". JPA's @DiscriminatorValue("null") is the special token Hibernate
// reads as "match SQL NULL" (not the literal 4-char string "null"). Without this annotation
// JPA defaulted to the simple class name "Characteristic", which doesn't match any row, and
// Hibernate failed mapping rows with "EntityPersister null" during result hydration.
@DiscriminatorValue("null")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Indexed
public class Characteristic extends AbstractDescribable implements Comparable<Characteristic> {

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

    @Nullable
    @Column(name = "CATEGORY", columnDefinition = "VARCHAR(255)")
    private String category;
    @Nullable
    @Column(name = "CATEGORY_URI", columnDefinition = "VARCHAR(255)")
    private String categoryUri;
    @Enumerated(EnumType.STRING)
    @Column(name = "EVIDENCE_CODE", columnDefinition = "VARCHAR(255)")
    private GOEvidenceCode evidenceCode;
    /**
     * Stores the value this characteristic had before it was assigned a URI for the term.
     */
    @Nullable
    @Column(name = "ORIGINAL_VALUE", columnDefinition = "VARCHAR(255)")
    private String originalValue = null;
    @Column(name = "`VALUE`", columnDefinition = "VARCHAR(255)")
    private String value;
    @Nullable
    @Column(name = "VALUE_URI", columnDefinition = "VARCHAR(255)")
    private String valueUri;
    /**
     * Indicate if this "old-style" characteristic has been migrated to a {@link ubic.gemma.model.expression.experiment.Statement}.
     * This is only meaningful for {@link ubic.gemma.model.expression.experiment.FactorValue} characteristics.
     * @deprecated do not rely on this field, it will be removed once the migration is completed.
     */
    @Deprecated
    @Column(name = "MIGRATED_TO_STATEMENT", nullable = false, columnDefinition = "TINYINT")
    private boolean migratedToStatement;

    /**
     * Opaque JSON array of supporting-evidence items ({@code [{"quote":...,"source":...,"location":...}, ...]})
     * backing a curated tag — the verbatim provenance the curation agents emit (the agents-side
     * {@code FindingEvidence} shape). Stored as-is; Gemma does not parse or query it, so the agents repo
     * owns the evidence schema. Null on tags with no recorded evidence.
     */
    @Nullable
    @Column(name = "SUPPORTING_EVIDENCE", columnDefinition = "TEXT")
    private String supportingEvidence;


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
    @Nullable
    @FullTextField
    public String getCategory() {
        return this.category;
    }

    public void setCategory( @Nullable String category ) {
        this.category = normalizeTermText( category );
    }

    /**
     * Collapse whitespace on a free-text term field: strip the ends, and reduce internal runs to a
     * single space. Null survives as null.
     * <p>
     * These fields carry third-party text — GEO submitters write {@code "cancer cell line "} and
     * {@code "high  fat  diet"}, and 12,861 of the 13,179 double-spaced values in production had the
     * run in the submitter's own {@code originalValue}, so the input reproduces them on every
     * import. The cost is not cosmetic: MySQL's PAD SPACE collation hides a TRAILING space from
     * {@code =} but gives internal runs no such cover, so two spellings of one value split under
     * {@code GROUP BY}, joins, and every exact-label comparison. Normalizing at the setter is the
     * one point every writer passes through — the GEO converter's seventeen call sites, the
     * curation API, agent writes and the CLI — and {@link #getOriginalValue()} still holds the
     * submitter's string verbatim, so nothing is lost.
     * <p>
     * Safe against Hibernate: the mapping annotates the FIELDS, so hydration assigns them directly
     * and never calls a setter. A loaded entity is therefore not silently rewritten (and not marked
     * dirty) by this.
     */
    @Nullable
    protected static String normalizeTermText( @Nullable String s ) {
        if ( s == null ) {
            return null;
        }
        // The no-break spaces have to go first, for two separate reasons. Java does not classify
        // U+202F or U+2007 as whitespace at all, so normalizeSpace leaves them untouched; and
        // while it does map U+00A0 to a plain space, it does so WITHOUT re-collapsing, so
        // "x  y" comes back as "x  y" -- a normalizer emitting the very double space it
        // exists to remove. Mapping them to a plain space up front lets the single collapse below
        // see them as the whitespace they are. Production carries 2,392 values with U+00A0 and 5
        // with U+202F.
        String t = s.replace( '\u00A0', ' ' ).replace( '\u202F', ' ' ).replace( '\u2007', ' ' );
        return StringUtils.normalizeSpace( t );
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
    @KeywordField
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
    @Nullable
    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue( @Nullable String originalValue ) {
        this.originalValue = originalValue;
    }

    /**
     * @return The human-readable term (e.g., "OrganismPart"; "kinase")
     */
    @FullTextField
    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = normalizeTermText( value );
    }

    /**
     * @return This can be a URI to any resources that describes the characteristic. Often it might be a URI to an OWL
     * ontology
     * term. If the URI is an instance of an abstract class, the classUri should be filled in with the URI for
     * the
     * abstract class.
     */
    @Nullable
    @KeywordField
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

    @Nullable
    public String getSupportingEvidence() {
        return supportingEvidence;
    }

    public void setSupportingEvidence( @Nullable String supportingEvidence ) {
        this.supportingEvidence = supportingEvidence;
    }

    /**
     * Constant, deliberately.
     *
     * <p>🛑 This used to hash {@code category}/{@code categoryUri} and
     * {@code value}/{@code valueUri} — exactly the fields curation MUTATES. A
     * Characteristic lives in {@link ubic.gemma.model.analysis.Investigation}'s
     * {@code HashSet}, so re-terming one in place moved it to a bucket computed
     * from its old value while {@link #equals} still matched it by id:
     * {@code contains()} and {@code remove()} then answered false for an element
     * that was demonstrably in the set. The same hash also broke the
     * equals/hashCode contract outright, because two instances sharing an id but
     * differing in content are equal by the id branch below and hashed
     * differently.</p>
     *
     * <p>The cost is that a hash collection of Characteristics degrades to a
     * linear scan of one bucket. That is invisible for the per-entity sets this
     * class actually lives in (tens of elements), and it is NOT invisible for
     * bulk keying: building a 5000-entry {@code HashMap} keyed by transient
     * Characteristics measured 1141 ms this way versus 42 ms before.
     * ⇒ <b>Do not key a large map by Characteristic.</b> Use a
     * {@link java.util.TreeMap}, or key by id. The annotation usage-frequency
     * aggregations in {@code ExpressionExperimentDaoImpl} are the precedent.</p>
     *
     * <p>🛑 Pick the comparator by what the keys are. {@link #getComparator()}
     * breaks the tie on id, so a map built from TRANSIENT keys (a GROUP BY
     * projection, say) and probed with a persisted characteristic misses:
     * id-vs-null is never zero, even though {@link #equals} calls the two equal.
     * For those maps use {@link #getByCategoryAndValueComparator()} (or
     * {@link #getByCategoryComparator()} when the value is not part of the key),
     * which collapses exactly the pairs {@link #equals} considers equal. Reserve
     * {@link #getComparator()} for maps whose keys are all persisted and where
     * two distinct rows sharing a term must stay distinct.</p>
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
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
            return getId().equals( that.getId() );

        /*
         * at this point, we know we have two Characteristics, at least one of which is transient, so we have to look at
         * the fields; we can't just compare the hashcodes because they also look at the id, so comparing one transient
         * and one persistent would always fail...
         */
        return CharacteristicUtils.equals( category, categoryUri, that.category, that.categoryUri )
                && CharacteristicUtils.equals( value, valueUri, that.value, that.valueUri );
    }

    @Override
    public int compareTo( @NonNull Characteristic characteristic ) {
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

        /**
         * Create a copy from the given characteristic.
         */
        public static Characteristic newInstance( Characteristic from ) {
            final Characteristic entity = newInstance();
            entity.setName( from.getName() );
            entity.setDescription( from.getDescription() );
            entity.setCategory( from.getCategory() );
            entity.setCategoryUri( from.getCategoryUri() );
            entity.setValue( from.getValue() );
            entity.setValueUri( from.getValueUri() );
            entity.setEvidenceCode( from.getEvidenceCode() );
            // no need to copy originalValue, this is only relevant for historical reasons
            return entity;
        }

        public static Characteristic newInstance( String name, String description, String value, @Nullable String valueUri,
                String category, @Nullable String categoryUri, GOEvidenceCode evidenceCode ) {
            final Characteristic entity = new Characteristic();
            entity.setName( name );
            entity.setDescription( description );
            entity.setValue( value );
            entity.setValueUri( stripToNull( valueUri ) );
            entity.setCategory( category );
            entity.setCategoryUri( stripToNull( categoryUri ) );
            entity.setEvidenceCode( evidenceCode );
            return entity;
        }

        public static Characteristic newInstance( String category, @Nullable String categoryUri ) {
            Characteristic entity = new Characteristic();
            entity.setCategory( category );
            entity.setCategoryUri( stripToNull( categoryUri ) );
            entity.setValue( category );
            entity.setValueUri( stripToNull( categoryUri ) );
            return entity;
        }

        public static Characteristic newInstance( Category category ) {
            return newInstance( category.getCategory(), category.getCategoryUri() );
        }

        public static Characteristic newInstance( String category, @Nullable String categoryUri, String value, @Nullable String valueUri ) {
            final Characteristic entity = new Characteristic();
            entity.setCategory( category );
            entity.setCategoryUri( stripToNull( categoryUri ) );
            entity.setValue( value );
            entity.setValueUri( stripToNull( valueUri ) );
            return entity;
        }

        public static Characteristic newInstance( Category category, String value, @Nullable String valueUri ) {
            return newInstance( category.getCategory(), category.getCategoryUri(), value, valueUri );
        }

        public static Characteristic newInstance( Category category, Value value ) {
            return newInstance( category.getCategory(), category.getCategoryUri(), value.getValue(), value.getValueUri() );
        }
    }

}