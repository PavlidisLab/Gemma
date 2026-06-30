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
package ubic.gemma.model.expression.experiment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The value for a ExperimentalFactor, representing a specific instance of the factor, such as "10 ug/kg" or "mutant"
 * <p>
 * Hibernate Search 7 mapping: indexed root and embedded contributor to
 * {@link ExperimentalFactor#getFactorValues()}. Pulls each {@link Statement} characteristic
 * (a {@link Characteristic} subtype with predicate + object slots) into the EE document.
 */
@Indexed
@Entity
@Table(name = "FACTOR_VALUE")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FactorValue extends AbstractIdentifiable implements SecuredChild<ExpressionExperiment> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXPERIMENTAL_FACTOR_FK", nullable = false, columnDefinition = "BIGINT")
    private ExperimentalFactor experimentalFactor;

    @Nullable
    @Deprecated
    @Column(name = "`VALUE`", columnDefinition = "VARCHAR(255)")
    private String value;

    @Nullable
    @Column(name = "IS_BASELINE", columnDefinition = "TINYINT")
    private Boolean isBaseline;

    // assumed readily available in FactorValueValueObject
    @Nullable
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "MEASUREMENT_FK", columnDefinition = "BIGINT", unique = true)
    private Measurement measurement;

    // assumed readily available in FactorValueValueObject
    // remove the where clause when old-style characteristics have been removed (see https://github.com/PavlidisLab/Gemma/issues/929 for details)
    //
    // @Fetch(SUBSELECT) is deliberate. This is a Set<Statement>, and Statement is a SINGLE_TABLE subclass of
    // Characteristic (@DiscriminatorValue("Statement")). Hibernate 6 renders the subclass discriminator of a
    // *join-fetched* subclass collection as a derived table:
    //   left join (select * from CHARACTERISTIC t where t.class='Statement') c on fv.ID=c.FACTOR_VALUE_FK
    // On MySQL 5.7 that derived table can stop being merged once several such prepared statements coexist on a
    // pooled connection (Hibernate + HikariCP cachePrepStmts) and be materialized in full (~178k Statement rows)
    // on execute -- reproduced at ~3.5s in a combined fetch. SUBSELECT keeps the collection eager but loads it in
    // its own statement so it never lands inside a larger combined fetch (e.g. initializing
    // ExperimentalDesign.experimentalFactors for /datasets/{id}/design). NB: the dominant /design first-contact
    // cost was the ExperimentalFactor.annotations join, not this -- see ExperimentalFactor#annotations -- but
    // keeping these statements out of combined fetches removes the materialization hazard as well.
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @JoinColumn(name = "FACTOR_VALUE_FK", columnDefinition = "BIGINT",
            foreignKey = @jakarta.persistence.ForeignKey(name = "CHARACTERISTIC_FACTOR_VALUE_FKC"))
    @SQLRestriction("class = 'Statement'")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Statement> characteristics = new HashSet<>();

    // non-migrated characteristics
    // remove this mapping once all old-style characteristics have been migrated (see https://github.com/PavlidisLab/Gemma/issues/929 for details)
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "FACTOR_VALUE_FK", columnDefinition = "BIGINT",
            foreignKey = @jakarta.persistence.ForeignKey(name = "CHARACTERISTIC_FACTOR_VALUE_FKC"))
    @SQLRestriction("class is null")
    @Immutable
    @Cascade({ org.hibernate.annotations.CascadeType.DETACH, org.hibernate.annotations.CascadeType.REMOVE })
    @Deprecated
    private Set<Characteristic> oldStyleCharacteristics = new HashSet<>();

    @Column(name = "NEEDS_ATTENTION", nullable = false, columnDefinition = "TINYINT")
    private boolean needsAttention;

    @Transient
    private ExpressionExperiment securityOwner = null;

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    public ExperimentalFactor getExperimentalFactor() {
        return this.experimentalFactor;
    }

    public void setExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        this.experimentalFactor = experimentalFactor;
    }

    /**
     * Indicate if this factor value is a "forced" baseline or non-baseline condition.
     * <p>
     * This is ignored if the factor is continuous.
     */
    @Nullable
    public Boolean getIsBaseline() {
        return this.isBaseline;
    }

    public void setIsBaseline( @Nullable Boolean isBaseline ) {
        this.isBaseline = isBaseline;
    }

    /**
     * @deprecated use {@link #getMeasurement()} or {@link #getCharacteristics()} to retrieve the value.
     */
    @Nullable
    @Deprecated
    public String getValue() {
        return this.value;
    }

    @Deprecated
    public void setValue( @Nullable String value ) {
        this.value = value;
    }

    /**
     * If this is a continuous factor, a measurement representing its value.
     */
    @Nullable
    public Measurement getMeasurement() {
        return this.measurement;
    }

    public void setMeasurement( @Nullable Measurement measurement ) {
        this.measurement = measurement;
    }

    /**
     * Collection of {@link Statement} describing this factor value.
     */
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<Statement> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Set<Statement> characteristics ) {
        this.characteristics = characteristics;
    }

    /**
     * Old-style characteristics from the 1.30 series.
     * <p>
     * This will be removed when all the characteristics are ported to the new style using {@link Statement}.
     */
    @Deprecated
    public Set<Characteristic> getOldStyleCharacteristics() {
        return oldStyleCharacteristics;
    }

    @Deprecated
    public void setOldStyleCharacteristics( Set<Characteristic> oldCharacteristics ) {
        this.oldStyleCharacteristics = oldCharacteristics;
    }

    /**
     * Indicate if this factor value needs attention.
     * <p>
     * If this is the case, there might be a {@link ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent}
     * event attached to the owning {@link ExpressionExperiment} with additional details.
     */
    public boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( boolean troubled ) {
        this.needsAttention = troubled;
    }

    @Transient
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return securityOwner;
    }

    public void setSecurityOwner( ExpressionExperiment ee ) {
        this.securityOwner = ee;
    }

    @Override
    public int hashCode() {
        // experimentalFactor is lazy-loaded, so it cannot be used in the hashCode() implementation
        return Objects.hash( getMeasurement(), getCharacteristics() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof FactorValue ) )
            return false;
        FactorValue that = ( FactorValue ) object;
        if ( this.getId() != null && that.getId() != null )
            return this.getId().equals( that.getId() );
        /*
         * at this point, we know we have two FactorValues, at least one of which is transient, so we have to look at
         * the fields; pain in butt
         */
        return IdentifiableUtils.equals( getExperimentalFactor(), that.getExperimentalFactor() )
                && Objects.equals( getMeasurement(), that.getMeasurement() )
                && Objects.equals( getCharacteristics(), that.getCharacteristics() )
                && Objects.equals( getValue(), that.getValue() );
    }

    @Override
    public String toString() {
        return String.format( "FactorValue%s%s%s%s%s%s",
                getId() != null ? " Id=" + getId() : "",
                value != null ? " Value=" + value : "",
                measurement != null ? " Measurement=" + measurement : "",
                !characteristics.isEmpty() ? " Characteristics=[" + characteristics.stream().sorted().map( Statement::toString ).collect( Collectors.joining( ", " ) ) + "]" : "",
                isBaseline != null ? " Baseline=" + isBaseline : "",
                needsAttention ? " [Needs Attention]" : ""
        );
    }

    public static final class Factory {

        public static FactorValue newInstance() {
            return new FactorValue();
        }

        public static FactorValue newInstance( ExperimentalFactor experimentalFactor ) {
            final FactorValue entity = new FactorValue();
            entity.setExperimentalFactor( experimentalFactor );
            return entity;
        }

        /**
         * Create a FactorValue with a single characteristic.
         */
        public static FactorValue newInstance( ExperimentalFactor factor, Characteristic c ) {
            return newInstance( factor, c instanceof Statement ? ( Statement ) c : Statement.Factory.newInstance( c ) );
        }

        /**
         * Create a FactorValue with a single statement.
         */
        public static FactorValue newInstance( ExperimentalFactor factor, Statement c ) {
            Assert.isTrue( factor.getType() == FactorType.CATEGORICAL,
                    "Only categorical factors can be created with a single characteristic." );
            FactorValue entity = newInstance( factor );
            entity.getCharacteristics().add( c );
            return entity;
        }


        /**
         * Create a FactorValue with a measurement.
         */
        public static FactorValue newInstance( ExperimentalFactor factor, Measurement measurement ) {
            Assert.isTrue( factor.getType() == FactorType.CONTINUOUS, "Only continuous factors can have a measurement." );
            FactorValue entity = newInstance( factor );
            entity.setMeasurement( measurement );
            return entity;
        }
    }

}
