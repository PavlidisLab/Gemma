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
package ubic.gemma.model.genome;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import ubic.gemma.model.common.AbstractDescribable;

/**
 * Some part of a chromosome
 */
@Entity
@Table(name = "CHROMOSOME_FEATURE", indexes = {
        @Index(name = "CHROMOSOME_FEATURE_NAME", columnList = "NAME"),
        @Index(name = "NCBI_GI", columnList = "NCBI_GI"),
        @Index(name = "NCBI_GENE_ID", columnList = "NCBI_GENE_ID"),
        @Index(name = "CHROMOSOME_FEATURE_PREVIOUS_NCBI_ID", columnList = "PREVIOUS_NCBI_ID"),
        @Index(name = "CHROMOSOME_FEATURE_ENSEMBL_ID", columnList = "ENSEMBL_ID")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "class")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public abstract class ChromosomeFeature extends AbstractDescribable {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "PHYSICAL_LOCATION_FK", unique = true, columnDefinition = "BIGINT")
    private PhysicalLocation physicalLocation;

    public PhysicalLocation getPhysicalLocation() {
        return this.physicalLocation;
    }

    public void setPhysicalLocation( PhysicalLocation physicalLocation ) {
        this.physicalLocation = physicalLocation;
    }
}
