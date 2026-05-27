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

package ubic.gemma.model.genome.gene;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.SecuredNotChild;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;

import java.util.HashSet;
import java.util.Set;

/**
 * A grouping of genes that share a common relationship.
 * <p>
 * Hibernate Search 7 indexed root. Embeds {@link Characteristic} on each member of
 * {@link #getCharacteristics()}, source accession, literature references, and members
 * (which in turn embed each {@link Gene}).
 */
@Entity
@Table(name = "GENE_SET", indexes = @Index(name = "GENE_SET_NAME", columnList = "NAME"))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Indexed
public class GeneSet extends AbstractAuditable implements SecuredNotChild {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "GENE_SET_FK", columnDefinition = "BIGINT", foreignKey = @ForeignKey(name = "CHARACTERISTIC_GENE_SET_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Characteristic> characteristics = new HashSet<>();

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "SOURCE_ACCESSION_FK", unique = true, columnDefinition = "BIGINT")
    private DatabaseEntry sourceAccession;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "GENE_SETS2LITERATURE_SOURCES",
            joinColumns = @JoinColumn(name = "GENE_SETS_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "LITERATURE_SOURCES_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "BIBLIOGRAPHIC_REFERENCE_GENE_SETS_FKC"))
    private Set<BibliographicReference> literatureSources = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "GENE_SET_FK", columnDefinition = "BIGINT", foreignKey = @ForeignKey(name = "GENE_SET_MEMBER_GENE_SET_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<GeneSetMember> members = new HashSet<>();

    static public GeneSetMember containsGene( Gene g, GeneSet gs ) {
        for ( GeneSetMember gm : gs.getMembers() ) {
            if ( gm.getGene().equals( g ) )
                return gm;
        }
        return null;
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
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

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Set<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }


    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<BibliographicReference> getLiteratureSources() {
        return this.literatureSources;
    }

    public void setLiteratureSources( Set<BibliographicReference> literatureSources ) {
        this.literatureSources = literatureSources;
    }

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<GeneSetMember> getMembers() {
        return this.members;
    }

    public void setMembers( Set<GeneSetMember> members ) {
        this.members = members;
    }

    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public DatabaseEntry getSourceAccession() {
        return this.sourceAccession;
    }

    public void setSourceAccession( DatabaseEntry sourceAccession ) {
        this.sourceAccession = sourceAccession;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof GeneSet ) )
            return false;
        GeneSet that = ( GeneSet ) object;
        if ( getId() != null && that.getId() != null )
            return getId().equals( that.getId() );
        return false;
    }

    public static final class Factory {
        public static GeneSet newInstance() {
            return new GeneSet();
        }

    }

}
