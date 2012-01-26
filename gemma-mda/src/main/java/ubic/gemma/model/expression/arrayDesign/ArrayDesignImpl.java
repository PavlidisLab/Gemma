/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.expression.arrayDesign;

/**
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesign {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3705444380575919171L;

    @Override
    public boolean equals( Object object ) {
        if ( !( object instanceof ArrayDesign ) ) return false;
        ArrayDesign that = ( ArrayDesign ) object;
        if ( this.getId() != null && that.getId() != null ) return this.getId().equals( that.getId() );

        if ( this.getName() != null && that.getName() != null ) {
            return this.getName().equals( that.getName() );
        }

        return false;
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null ) return 29 * getId().hashCode();
        if ( this.getName() != null ) return 29 * getName().hashCode();
        return super.hashCode();
    }

    @Override
    public java.lang.String toString() {
        return super.toString() + ( this.getShortName() == null ? "" : " (" + this.getShortName() + ")" );
    }

}