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

package ubic.gemma.model.analysis.expression;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.HashSet;
import java.util.Set;

/**
 * A grouping of expression studies.
 * <p>
 * Hibernate Search 7 indexed root. Section 2.1 of SEARCH_RECCE.md notes that the pre-strip
 * code had a TODO to include {@code experiments.*} in this document; that gap is preserved.
 *
 * @author Paul
 */
@Entity
@Table(name = "EXPRESSION_EXPERIMENT_SET")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Indexed
public class ExpressionExperimentSet extends AbstractAuditable implements Securable {

    @Nullable
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCESSION_FK", unique = true, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "EXPRESSION_EXPERIMENT_SET_ACCESSION_FKC"))
    private DatabaseEntry accession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TAXON_FK", columnDefinition = "BIGINT")
    private Taxon taxon;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "EXPERIMENTS2EXPRESSION_EXPERIMENT_SETS",
            joinColumns = @JoinColumn(name = "EXPRESSION_EXPERIMENT_SETS_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "EXPERIMENTS_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "EXPRESSION_EXPERIMENTS_EXPRESSION_EXPERIMENT_SETS_FKC"))
    private Set<ExpressionExperiment> experiments = new HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public ExpressionExperimentSet() {
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof ExpressionExperimentSet ) )
            return false;
        ExpressionExperimentSet that = ( ExpressionExperimentSet ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return DescribableUtils.equalsByName( this, that );
    }

    @Override
    @FullTextField
    public String getName() {
        return super.getName();
    }

    @Override
    @FullTextField(projectable = Projectable.YES)
    public String getDescription() {
        return super.getDescription();
    }

    @Nullable
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public DatabaseEntry getAccession() {
        return accession;
    }

    public void setAccession( @Nullable DatabaseEntry accession ) {
        this.accession = accession;
    }

    public Set<ExpressionExperiment> getExperiments() {
        return this.experiments;
    }

    public void setExperiments( Set<ExpressionExperiment> experiments ) {
        this.experiments = experiments;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public static final class Factory {
        public static ExpressionExperimentSet newInstance() {
            return new ExpressionExperimentSet();
        }
    }

}
