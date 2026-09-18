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
package ubic.gemma.model.analysis.expression.coexpression;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds the data of the sample coexpression matrix
 */
/*
 * 🛑 NOT second-level cached, and it must not become so again.
 *
 * Every other L2 region is sized in ENTRIES -- L2_READ_ONLY is 1,000 of them -- which is a sensible budget
 * for a Chromosome or a Unit and a catastrophic one here. This entity's single meaningful field is an
 * n-squared LONGBLOB: 9.5 MB at 1,090 samples, and 110 MB at the largest row on production, across 47,532
 * rows. "1,000 entries" was therefore a heap budget of tens of gigabytes written as a small number.
 *
 * It was not theoretical. corrMat -force on GSE260875 died against a 30 GiB heap with 29.7 GiB of it in
 * 1,901 retained copies of that experiment's 9.5 MB matrix -- measured from a heap dump by frb, 2026-09-17,
 * as 1,901 byte[9,505,52x] paired with 1,901 int[1,817,91x] and no double[] in the heap at all. Hibernate's
 * L2 runs through JSR-107 ehcache, whose stores are by VALUE, so each entry is a serialized copy rather than
 * a reference -- which is why the histogram shows byte arrays of exactly 1090 x 1090 x 8 plus a serialization
 * header, and no live matrix objects.
 *
 * The database is where 2.7 GB of correlation matrices belong. A caller that wants one repeatedly should hold
 * it, or cache the response, not ask the ORM to keep serialized copies of blobs.
 */
@Entity
@Table(name = "SAMPLE_COEXPRESSION_MATRIX")
@Immutable
public class SampleCoexpressionMatrix extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "BIO_ASSAY_DIMENSION_FK", nullable = false, columnDefinition = "BIGINT")
    private BioAssayDimension bioAssayDimension;
    @Lob
    @Column(name = "COEXPRESSION_MATRIX", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] coexpressionMatrix;

    public BioAssayDimension getBioAssayDimension() {
        return this.bioAssayDimension;
    }

    public void setBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public byte[] getCoexpressionMatrix() {
        return this.coexpressionMatrix;
    }

    public void setCoexpressionMatrix( byte[] coexpressionMatrix ) {
        this.coexpressionMatrix = coexpressionMatrix;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        return Objects.hash( bioAssayDimension );
    }

    /**
     * Returns <code>true</code> if the argument is an SampleCoexpressionMatrix instance and all identifiers for this
     * entity equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof SampleCoexpressionMatrix ) ) {
            return false;
        }
        final SampleCoexpressionMatrix that = ( SampleCoexpressionMatrix ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( getBioAssayDimension(), that.getBioAssayDimension() )
                    && Arrays.equals( getCoexpressionMatrix(), that.getCoexpressionMatrix() );
        }
    }

    public static class Factory {

        public static SampleCoexpressionMatrix newInstance( BioAssayDimension bioAssayDimension, byte[] coexpressionMatrix ) {
            SampleCoexpressionMatrix matrix = new SampleCoexpressionMatrix();
            matrix.setBioAssayDimension( bioAssayDimension );
            matrix.setCoexpressionMatrix( coexpressionMatrix );
            return matrix;
        }
    }
}