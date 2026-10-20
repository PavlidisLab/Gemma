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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.hibernate.ByteArrayType;

import java.util.Arrays;

/**
 * @author Patrick
 */
@Entity
@Table(name = "MEAN_VARIANCE_RELATION")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class MeanVarianceRelation extends AbstractIdentifiable implements SecuredChild<ExpressionExperiment> {

    @Type(value = ByteArrayType.class, parameters = @Parameter(name = "arrayType", value = "double"))
    @Column(name = "MEANS", nullable = false, columnDefinition = "MEDIUMBLOB")
    private double[] means;
    @Type(value = ByteArrayType.class, parameters = @Parameter(name = "arrayType", value = "double"))
    @Column(name = "VARIANCES", nullable = false, columnDefinition = "MEDIUMBLOB")
    private double[] variances;

    @Nullable
    @Transient
    private ExpressionExperiment securityOwner;

    public double[] getMeans() {
        return this.means;
    }

    public void setMeans( double[] means ) {
        this.means = means;
    }

    public double[] getVariances() {
        return this.variances;
    }

    public void setVariances( double[] variances ) {
        this.variances = variances;
    }

    @Nullable
    @Transient
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return this.securityOwner;
    }

    @SuppressWarnings("unused") // used via reflection
    public void setSecurityOwner( @Nullable ExpressionExperiment ee ) {
        this.securityOwner = ee;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof MeanVarianceRelation ) ) {
            return false;
        }
        final MeanVarianceRelation that = ( MeanVarianceRelation ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Arrays.equals( means, that.means )
                    && Arrays.equals( variances, that.variances );
        }
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException( "MeanVarianceRelation cannot be hashed." );
    }

    public static final class Factory {

        public static MeanVarianceRelation newInstance() {
            return new MeanVarianceRelation();
        }

        public static MeanVarianceRelation newInstance( double[] means, double[] variances ) {
            MeanVarianceRelation mvr = newInstance();
            mvr.setMeans( means );
            mvr.setVariances( variances );
            return mvr;
        }
    }
}