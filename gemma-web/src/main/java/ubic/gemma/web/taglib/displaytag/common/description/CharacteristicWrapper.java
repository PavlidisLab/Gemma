/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.taglib.displaytag.common.description;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * Decorator for displaying characteristics in a list view.
 * <p>
 * 
 * @author luke
 * @version $Id$
 */
public class CharacteristicWrapper extends TableDecorator {
    
    private Log log = LogFactory.getLog( this.getClass() );

    public String getDescriptionString() {
        Characteristic c = (Characteristic)getCurrentRowObject();
        
        if ( !StringUtils.isEmpty( c.getValue() ) )
            return c.getValue();
        
        StringBuilder buf = new StringBuilder();
        if ( !StringUtils.isEmpty( c.getName() ) ) {
            buf.append( c.getName() );
            buf.append( ": " );
        }
        if (c instanceof VocabCharacteristic) {
            VocabCharacteristic vc = (VocabCharacteristic)c;
            buf.append( vc.getClassUri() );
            if ( vc.getTermUri() != null && !vc.getTermUri().equals( vc.getClassUri() ) ) {
                buf.append( " = " );
                buf.append( vc.getTermUri() );
                if ( vc.getValue() != null && vc.getValue().length() > 0 ) {
                    buf.append( "; " );
                    buf.append( vc.getValue() );
                }        
            } else if ( !StringUtils.isEmpty( vc.getValue() ) ) {
                buf.append( "; " );
                buf.append( vc.getValue() );
            }
        } else {
            return "unknown characteristic type with no value";
        }
        return buf.toString();
    }
    
}
