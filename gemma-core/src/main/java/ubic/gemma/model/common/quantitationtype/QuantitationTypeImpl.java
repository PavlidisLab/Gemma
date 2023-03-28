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
package ubic.gemma.model.common.quantitationtype;

/**
 * @see ubic.gemma.model.common.quantitationtype.QuantitationType
 */
public class QuantitationTypeImpl extends ubic.gemma.model.common.quantitationtype.QuantitationType {

    private static final long serialVersionUID = -352202738189491165L;

    public QuantitationTypeImpl() {
        super();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof QuantitationType ) ) {
            return false;
        }
        final QuantitationType that = ( QuantitationType ) object;

        if ( that.getName() != null && this.getName() != null && !this.getName().equals( that.getName() ) ) {
            return false;
        }

        if ( this.getScale() != null && that.getScale() != null && !this.getScale().equals( that.getScale() ) ) {
            return false;
        }

        if ( this.getIsPreferred() != that.getIsPreferred() ) {
            return false;
        }

        if ( this.getIsRatio() != that.getIsRatio() ) {
            return false;
        }

        if ( this.getIsNormalized() != that.getIsNormalized() ) {
            return false;
        }

        if ( this.getIsBackground() != that.getIsBackground() ) {
            return false;
        }

        if ( this.getIsBackgroundSubtracted() != that.getIsBackgroundSubtracted() ) {
            return false;
        }

        if ( this.getGeneralType() != null && that.getGeneralType() != null && !this.getGeneralType()
                .equals( that.getGeneralType() ) ) {
            return false;
        }

        //noinspection SimplifiableIfStatement // Better readability
        if ( this.getRepresentation() != null && that.getRepresentation() != null && !this.getRepresentation()
                .equals( that.getRepresentation() ) ) {
            return false;
        }

        return this.getType() == null || that.getRepresentation() == null || this.getType().equals( that.getType() );
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? this.computeHashCode() : this.getId().hashCode() );
        return hashCode;
    }

    private int computeHashCode() {
        int hashCode = 0;
        if ( this.getName() != null ) {
            hashCode = hashCode + this.getName().hashCode();
        }
        if ( this.getType() != null ) {
            hashCode = hashCode + this.getType().hashCode();
        }
        if ( this.getRepresentation() != null ) {
            hashCode = hashCode + this.getRepresentation().hashCode();
        }
        if ( this.getGeneralType() != null ) {
            hashCode = hashCode + this.getGeneralType().hashCode();
        }
        if ( this.getScale() != null ) {
            hashCode = hashCode + this.getScale().hashCode();
        }
        hashCode += Boolean.hashCode( this.getIsBackground() );
        hashCode += Boolean.hashCode( this.getIsBackgroundSubtracted() );
        hashCode += Boolean.hashCode( this.getIsNormalized() );
        hashCode += Boolean.hashCode( this.getIsPreferred() );
        hashCode += Boolean.hashCode( this.getIsRatio() );

        return hashCode;
    }
}