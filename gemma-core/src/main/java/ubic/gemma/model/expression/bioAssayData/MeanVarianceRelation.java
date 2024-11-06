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

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.persistence.Transient;
import java.util.Arrays;

/**
 * @author Patrick
 */
public class MeanVarianceRelation extends AbstractIdentifiable implements SecuredChild {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1442923993171126882L;
    private double[] means;
    private double[] variances;

    private Securable securityOwner;

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

    @Transient
    @Override
    public Securable getSecurityOwner() {
        return this.securityOwner;
    }

    @SuppressWarnings("unused") // used via reflection
    public void setSecurityOwner( ExpressionExperiment ee ) {
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
        // hashing would be to costly
        return 0;
    }

    public static final class Factory {

        public static MeanVarianceRelation newInstance() {
            return new MeanVarianceRelation();
        }

    }

}