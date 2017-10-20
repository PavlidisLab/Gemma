/*
 * The Gemma project
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
package ubic.gemma.web.propertyeditor;

import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * @author pavlidis
 *
 */
public class QuantitationTypePropertyEditor extends PropertyEditorSupport {
    private static Log log = LogFactory.getLog( QuantitationTypePropertyEditor.class.getName() );

    private Map<String, QuantitationType> map = new HashMap<String, QuantitationType>();

    public QuantitationTypePropertyEditor( Collection<QuantitationType> types ) {
        for ( QuantitationType type : types ) {
            map.put( type.getName(), type );
        }
    }

    @Override
    public String getAsText() {
        if ( this.getValue() == null || ( ( QuantitationType ) this.getValue() ).getId() == null ) {
            return "---";
        }
        return ( ( QuantitationType ) this.getValue() ).getName();
    }

    @Override
    public void setAsText( String text ) throws IllegalArgumentException {
        if ( log.isDebugEnabled() ) log.debug( "Transforming " + text + " to a quantitation type..." );
        Object ad = this.map.get( text );
        this.setValue( ad ); // okay to be null
    }
}
