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
package ubic.gemma.web.controller.dwr;

import org.directwebremoting.convert.BeanConverter;
import org.directwebremoting.dwrp.ObjectOutboundVariable;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;
import org.directwebremoting.extend.Property;
import ubic.basecode.dataStructure.DoublePoint;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * The 8 decimal precision of a double is just a waste of bandwidth in most cases. This converter just truncs the
 * precision to 3 when the double gets converted to a string.
 *
 * @author kelsey
 *
 */
public class DoublePointConverter extends BeanConverter {

    @Override
    public OutboundVariable convertOutbound( Object data, OutboundContext outctx ) throws MarshallException {

        if ( !( data instanceof DoublePoint ) ) return super.convertOutbound( data, outctx );

        // Where we collect out converted children
        Map<String, OutboundVariable> ovs = new TreeMap<>();

        // We need to do this before collecting the children to save recursion
        ObjectOutboundVariable ov = new ObjectOutboundVariable( outctx );
        outctx.put( data, ov );

        try {
            //noinspection unchecked
            Map<String, Property> properties = getPropertyMapFromObject( data, true, false );
            for ( Entry<String, Property> entry : properties.entrySet() ) {
                String name = entry.getKey();
                Property property = entry.getValue();

                Object value = property.getValue( data );
                OutboundVariable nested;
                if ( value instanceof Double ) {

                    // Reduce precision to save bandwidth
                    Double v = Double.parseDouble( String.format( "%.3f", value ) );

                    nested = getConverterManager().convertOutbound( v, outctx );

                } else {
                    nested = getConverterManager().convertOutbound( value, outctx );
                }
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
