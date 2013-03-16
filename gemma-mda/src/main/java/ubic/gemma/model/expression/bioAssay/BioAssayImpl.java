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
package ubic.gemma.model.expression.bioAssay;

import ubic.gemma.model.common.auditAndSecurity.Securable;

/**
 * @see ubic.gemma.model.expression.bioAssay.BioAssay
 */
public class BioAssayImpl extends ubic.gemma.model.expression.bioAssay.BioAssay {

    /**
     * 
     */
    private static final long serialVersionUID = -984217953142208083L;

    @Override
    public boolean equals( Object object ) {

        if ( !( object instanceof BioAssay ) ) {
            return false;
        }
        final BioAssay that = ( BioAssay ) object;
        if ( this.getId() != null && that.getId() != null ) return this.getId().equals( that.getId() );

        if ( this.getName() != null && that.getName() != null && !this.getName().equals( that.getName() ) )
            return false;

        if ( this.getDescription() != null && that.getDescription() != null
                && !this.getDescription().equals( that.getDescription() ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        if ( this.getId() != null ) {
            return 29 * getId().hashCode();
        }
        int nameHash = this.getName() == null ? 0 : getName().hashCode();

        int descHash = this.getDescription() == null ? 0 : getDescription().hashCode();
        hashCode = 29 * nameHash + descHash;

        return hashCode;
    }

    @Override
    public Securable getSecurityOwner() {
        throw new UnsupportedOperationException( "Sorry, " + this.getClass().getSimpleName()
                + " cannot identify their own security owner in this scope" );
    }
}