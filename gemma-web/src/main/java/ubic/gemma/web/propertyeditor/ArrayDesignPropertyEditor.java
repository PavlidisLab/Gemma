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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;

/**
 * Used to convert ArrayDesigns from and into strings for display in forms.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignPropertyEditor extends PropertyEditorSupport {

    ArrayDesignService arrayDesignService;

    private static Log log = LogFactory.getLog( ArrayDesignPropertyEditor.class.getName() );

    public ArrayDesignPropertyEditor( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    @Override
    public String getAsText() {
        if ( this.getValue() == null || ( ( ArrayDesign ) this.getValue() ).getId() == null ) {
            return " --- ";
        }
        return ( ( ArrayDesign ) this.getValue() ).getName();
    }

    @Override
    public void setAsText( String text ) throws IllegalArgumentException {
        if ( log.isDebugEnabled() ) log.debug( "Transforming " + text + " to a platform ..." );
        Object ad = arrayDesignService.findByName( text );
        this.setValue( ad ); // okay to be null.F
    }
}
