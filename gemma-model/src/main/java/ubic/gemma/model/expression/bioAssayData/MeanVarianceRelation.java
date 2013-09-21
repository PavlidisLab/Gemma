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

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import gemma.gsec.model.Securable;

/**
 * 
 */
public abstract class MeanVarianceRelation implements java.io.Serializable, gemma.gsec.model.SecuredChild {

    /**
     * Constructs new instances of {@link MeanVarianceRelation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link MeanVarianceRelation}.
         */
        public static MeanVarianceRelation newInstance() {
            return new MeanVarianceRelationImpl();
        }

    }

    private Securable securityOwner;

    public void setSecurityOwner( ExpressionExperiment ee ) {
        this.securityOwner = ee;
    }

    /**
     * @see MeanVarianceRelation#getSecurityOwner()
     */
    @Override
    public gemma.gsec.model.Securable getSecurityOwner() {
        return this.securityOwner;
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1442923993171126882L;
    private byte[] means;

    private byte[] variances;

    private byte[] lowessX;

    private byte[] lowessY;

    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public MeanVarianceRelation() {
    }

    /**
     * Returns <code>true</code> if the argument is an MeanVarianceRelation instance and all identifiers for this entity
     * equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof MeanVarianceRelation ) ) {
            return false;
        }
        final MeanVarianceRelation that = ( MeanVarianceRelation ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public byte[] getLowessX() {
        return this.lowessX;
    }

    /**
     * 
     */
    public byte[] getLowessY() {
        return this.lowessY;
    }

    /**
     * 
     */
    public byte[] getMeans() {
        return this.means;
    }

    /**
     * 
     */
    public byte[] getVariances() {
        return this.variances;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setLowessX( byte[] lowessX ) {
        this.lowessX = lowessX;
    }

    public void setLowessY( byte[] lowessY ) {
        this.lowessY = lowessY;
    }

    public void setMeans( byte[] means ) {
        this.means = means;
    }

    public void setVariances( byte[] variances ) {
        this.variances = variances;
    }

}