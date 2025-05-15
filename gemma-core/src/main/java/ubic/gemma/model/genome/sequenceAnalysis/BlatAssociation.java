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
package ubic.gemma.model.genome.sequenceAnalysis;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.persistence.util.IdentifiableUtils;

public class BlatAssociation extends BioSequence2GeneProduct {

    private BlatResult blatResult;

    public BlatResult getBlatResult() {
        return this.blatResult;
    }

    public void setBlatResult( BlatResult blatResult ) {
        this.blatResult = blatResult;
    }

    @Override
    public int hashCode() {
        return IdentifiableUtils.hash( getBioSequence(), getGeneProduct() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof BlatAssociation ) ) {
            return false;
        }
        BlatAssociation other = ( BlatAssociation ) object;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        } else {
            return IdentifiableUtils.equals( getBioSequence(), other.getBioSequence() )
                    && IdentifiableUtils.equals( getGeneProduct(), other.getGeneProduct() )
                    && IdentifiableUtils.equals( getBlatResult(), other.getBlatResult() );
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append( this.getClass().getSimpleName() );

        if ( this.getId() != null ) {
            buf.append( " Id=" ).append( this.getId() );
        } else {
            buf.append( " Score=" ).append( this.getScore() ).append( " Specific=" ).append( this.getSpecificity() )
                    .append( " Between " );
        }

        buf.append( this.getBioSequence() ).append( " ---> " ).append( this.getGeneProduct() );

        return buf.toString();
    }

    public static final class Factory {
        public static BlatAssociation newInstance() {
            return new BlatAssociation();
        }

    }
}