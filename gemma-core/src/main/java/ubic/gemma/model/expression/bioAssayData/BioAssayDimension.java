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
package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores the order of BioAssays referred to in DataVectors.
 * Note: Not a SecuredChild - maybe should be?
 */
@Entity
@Table(name = "BIO_ASSAY_DIMENSION")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class BioAssayDimension extends AbstractIdentifiable {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "BIO_ASSAY_DIMENSIONS2BIO_ASSAYS",
            joinColumns = @JoinColumn(name = "BIO_ASSAY_DIMENSIONS_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "BIO_ASSAYS_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "BIO_ASSAY_BIO_ASSAY_DIMENSIONS_FKC"),
            inverseForeignKey = @ForeignKey(name = "BIO_ASSAYS_FKC"))
    @OrderColumn(name = "ORDERING")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<BioAssay> bioAssays = new ArrayList<>();

    /**
     * Indicate if this BioAssayDimension resulting from merging other BioAssayDimensions.
     * TODO: switch to a regular boolean once all the entities have been migrated to the new schema.
     */
    @Nullable
    @Column(name = "IS_MERGED", columnDefinition = "TINYINT")
    private Boolean merged;

    public List<BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    public void setBioAssays( List<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    @Nullable
    public Boolean getMerged() {
        return merged;
    }

    public void setMerged( @Nullable Boolean merged ) {
        this.merged = merged;
    }

    @Override
    public int hashCode() {
        return bioAssays.hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof BioAssayDimension ) )
            return false;
        BioAssayDimension that = ( BioAssayDimension ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return Objects.equals( getBioAssays(), that.getBioAssays() );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( bioAssays != null ? " Number of Assays=" + bioAssays.size() : "" )
                + ( merged != null && merged ? " [Merged]" : "" );
    }

    public static final class Factory {

        public static BioAssayDimension newInstance() {
            return new BioAssayDimension();
        }

        public static BioAssayDimension newInstance( List<BioAssay> bioAssays ) {
            final BioAssayDimension entity = newInstance();
            entity.setBioAssays( bioAssays );
            return entity;
        }
    }
}