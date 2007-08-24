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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.description.Characteristic;

/**
 * @author pavlidis
 * @version $Id$
 */
public class FactorValueImpl extends ubic.gemma.model.expression.experiment.FactorValue {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5395878022298281346L;

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValue#toString()
     */
    @Override
    public java.lang.String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.getClass().getSimpleName() );
        if ( this.getId() != null ) {
            buf.append( "Id:" + this.getId() );
        } else {
            buf.append( " " );
        }
        if ( this.getCharacteristics().size() > 0 ) {
            for ( Characteristic c : this.getCharacteristics() ) {
                buf.append( c );
            }
        } else if ( this.getMeasurement() != null ) {
            buf.append( this.getMeasurement() );
        } else if ( StringUtils.isNotBlank( this.getValue() ) ) {
            buf.append( "Value: '" + this.getValue() + "'" );
        }
        return buf.toString();
    }
}