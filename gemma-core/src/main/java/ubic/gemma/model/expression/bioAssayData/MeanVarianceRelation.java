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

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author Patrick
 */
public class MeanVarianceRelation implements SecuredChild, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1442923993171126882L;
    private Securable securityOwner;
    private double[] means;
    private double[] variances;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public MeanVarianceRelation() {
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public double[] getMeans() {
        return this.means;
    }

    public void setMeans( double[] means ) {
        this.means = means;
    }

    @Transient
    @Override
    public Securable getSecurityOwner() {
        return this.securityOwner;
    }

    public void setSecurityOwner( ExpressionExperiment ee ) {
        this.securityOwner = ee;
    }

    public double[] getVariances() {
        return this.variances;
    }

    public void setVariances( double[] variances ) {
        this.variances = variances;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
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
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    public static final class Factory {

        public static MeanVarianceRelation newInstance() {
            return new MeanVarianceRelation();
        }

    }

}