/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.experiment;

import org.hibernate.proxy.HibernateProxyHelper;

/**
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentImpl extends ubic.gemma.model.expression.experiment.ExpressionExperiment {

    /**
     *  
     */
    private static final long serialVersionUID = -1342753625018841735L;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperiment#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object object ) {
        if ( object == null ) return false;
        if ( this.getClass().equals( object.getClass() )
                || HibernateProxyHelper.getClassWithoutInitializingProxy( this.getClass() ).equals(
                        HibernateProxyHelper.getClassWithoutInitializingProxy( object.getClass() ) ) ) {

            ExpressionExperiment that = ( ExpressionExperiment ) object;
            if ( this.getId() != null && that.getId() != null ) {
                return this.getId().equals( that.getId() );
            } else if ( this.getShortName() != null && that.getShortName() != null ) {
                return this.getShortName().equals( that.getShortName() );
            }
            return false;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperiment#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 1;
        if ( this.getId() != null ) {
            return this.getId().hashCode();
        } else if ( this.getShortName() != null ) {
            return this.getShortName().hashCode();
        }
        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.DescribableImpl#toString()
     */
    @Override
    public String toString() {
        return super.toString() + " (" + this.getShortName() + ")";
    }

}