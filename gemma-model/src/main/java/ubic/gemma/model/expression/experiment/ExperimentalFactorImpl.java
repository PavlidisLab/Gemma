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

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactor
 * @version $Id$
 */
public class ExperimentalFactorImpl extends ExperimentalFactor {

    private static final long serialVersionUID = 9095212348088245443L;

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( this == obj ) return true;

        if ( !( obj instanceof ExperimentalFactor ) ) return false;

        ExperimentalFactor other = ( ExperimentalFactor ) obj;

        if ( getId() == null ) {
            if ( other.getId() != null ) return false;
        } else if ( !getId().equals( other.getId() ) ) return false;

        if ( this.getCategory() == null ) {
            if ( other.getCategory() != null ) return false;
        } else if ( !this.getCategory().equals( other.getCategory() ) ) return false;

        if ( getName() == null ) {
            if ( other.getName() != null ) return false;
        } else if ( !getName().equals( other.getName() ) ) return false;

        if ( this.getDescription() == null ) {
            if ( other.getDescription() != null ) return false;
        } else if ( !getDescription().equals( other.getDescription() ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null ) {
            return super.hashCode();
        }

        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( this.getName() == null ) ? 0 : this.getName().hashCode() );
        result = prime * result + ( ( this.getDescription() == null ) ? 0 : this.getDescription().hashCode() );
        return result;
    }
}