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
package ubic.gemma.model.association;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;

import org.springframework.lang.Nullable;
import java.util.Objects;

@Entity
@Table(name = "GENE2GO_ASSOCIATION")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Gene2GOAssociation extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GENE_FK", nullable = false, columnDefinition = "BIGINT")
    private Gene gene;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "ONTOLOGY_ENTRY_FK", nullable = false, unique = true, columnDefinition = "BIGINT")
    private Characteristic ontologyEntry;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "EVIDENCE_CODE", columnDefinition = "VARCHAR(255)")
    private GOEvidenceCode evidenceCode;

    public Gene getGene() {
        return this.gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    public Characteristic getOntologyEntry() {
        return this.ontologyEntry;
    }

    public void setOntologyEntry( Characteristic ontologyEntry ) {
        this.ontologyEntry = ontologyEntry;
    }

    @Nullable
    public GOEvidenceCode getEvidenceCode() {
        return this.evidenceCode;
    }

    public void setEvidenceCode( @Nullable GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash( gene, ontologyEntry, evidenceCode );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Gene2GOAssociation ) ) {
            return false;
        }
        Gene2GOAssociation that = ( Gene2GOAssociation ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( gene, that.gene )
                    && Objects.equals( ontologyEntry, that.ontologyEntry )
                    && Objects.equals( evidenceCode, that.evidenceCode );
        }
    }

    @Override
    public String toString() {
        if ( gene == null || ontologyEntry == null ) return "?";
        return gene + " ---> " + ontologyEntry.getValue() + " [" + ontologyEntry.getValueUri() + "]";
    }

    public static final class Factory {
        public static Gene2GOAssociation newInstance( Gene gene, Characteristic ontologyEntry,
                GOEvidenceCode evidenceCode ) {
            final Gene2GOAssociation entity = new Gene2GOAssociation();
            entity.gene = gene;
            entity.ontologyEntry = ontologyEntry;
            entity.evidenceCode = evidenceCode;
            return entity;
        }
    }
}