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
package ubic.gemma.web.remote;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.convert.BeanConverter;
import org.directwebremoting.dwrp.ObjectOutboundVariable;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;
import org.directwebremoting.extend.Property;

import ubic.basecode.ontology.model.OntologyClassRestriction;
import ubic.basecode.ontology.model.OntologyTerm;

/**
 * @author kelsey
 * @version $Id$
 */
public class OntologyTermConverter extends BeanConverter {

    private static Log log = LogFactory.getLog( OntologyTermConverter.class.getName() );

    @Override
    public OutboundVariable convertOutbound( Object data, OutboundContext outctx ) throws MarshallException {
        // Where we collect out converted children
        Map<String, OutboundVariable> ovs = new TreeMap<String, OutboundVariable>();

        // We need to do this before collecting the children to save recursion
        ObjectOutboundVariable ov = new ObjectOutboundVariable( outctx );
        outctx.put( data, ov );

        try {
            Map<String, Property> properties = getPropertyMapFromObject( data, true, false );
            props: for ( Iterator<Map.Entry<String, Property>> it = properties.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Property> entry = it.next();
                String name = entry.getKey();
                Property property = entry.getValue();

                Object value = property.getValue( data );

                // Loop detection:
                // Detects self referential loops
                //
                if ( data instanceof OntologyTerm && value instanceof Collection ) {// Look for loops in the class
                    // restrictions of the ontology term
                    OntologyTerm term = ( OntologyTerm ) data;
                    for ( Object o : ( Collection<?> ) value ) {

                        if ( o instanceof OntologyClassRestriction ) {

                            OntologyClassRestriction restriction = ( OntologyClassRestriction ) o;
                            OntologyTerm restrictedTo = restriction.getRestrictedTo();

                            if ( restrictedTo != null && restrictedTo.equals( term ) ) { // is
                                // it a
                                // self
                                // referential
                                // loop?
                                if ( log.isDebugEnabled() ) log.debug( "Loop detected" );
                                // todo: put something special in the returned object so that the java scrip gui knows
                                // and displays the loop correctly
                                continue props; // if a loop is detected don't try to convert it!
                            }
                        }
                    }
                }
                OutboundVariable nested = getConverterManager().convertOutbound( value, outctx );

                ovs.put( name, nested );
            }
        } catch ( MarshallException ex ) {
            throw ex;
        } catch ( Exception ex ) {
            throw new MarshallException( data.getClass(), ex );
        }

        ov.init( ovs, getJavascript() );

        return ov;
    }

}
