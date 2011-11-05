/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.description;

import org.apache.commons.lang.StringUtils;

/**
 * @see ubic.gemma.model.common.description.DataProperty
 */
public class DataPropertyImpl extends ubic.gemma.model.common.description.DataProperty {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5966661249969195674L;

    @Override
    public java.lang.String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.getValue() );

        buf.append( " = " );
        buf.append( this.getData().toString() );
        return buf.toString();
    }

    @Override
    protected String toString( int indent ) {
        String ind = StringUtils.repeat( "   ", indent );
        StringBuilder buf = new StringBuilder();
        buf.append( ind + this.getValue() + " = " + this.getData().toString() + "\n" );
        return buf.toString();
    }
}