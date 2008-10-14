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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.convert.BeanConverter;
import org.directwebremoting.dwrp.ObjectOutboundVariable;
import org.directwebremoting.dwrp.SimpleOutboundVariable;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;
import org.directwebremoting.extend.Property;

import ubic.basecode.dataStructure.DoublePoint;

/**
 * Ther 8 decimal percission of a double is just a waste of bandwidth in most cases. This converter just trunks the
 * percission to 3 when the double gets converted to a string.
 * 
 * @author kelsey
 * @version $Id$
 */
public class DoublePointConverter extends BeanConverter {

    private static Log log = LogFactory.getLog( DoublePointConverter.class.getName() );
    private static int PERCISSION = 4;

    public OutboundVariable convertOutbound( Object data, OutboundContext outctx ) throws MarshallException {

        if ( !( data instanceof DoublePoint ) ) return super.convertOutbound( data, outctx );

        // Where we collect out converted children
        Map ovs = new TreeMap();

        // We need to do this before collecting the children to save recurrsion
        ObjectOutboundVariable ov = new ObjectOutboundVariable(outctx);
        outctx.put(data, ov);

        try
        {
            Map properties = getPropertyMapFromObject(data, true, false);
            for (Iterator it = properties.entrySet().iterator(); it.hasNext();)
            {
                Map.Entry entry = (Map.Entry) it.next();
                String name = (String) entry.getKey();
                Property property = (Property) entry.getValue();

                Object value = property.getValue(data);
                OutboundVariable nested;
                if ( value instanceof Double ){
                    
                    String valueString = Double.toString( (Double)value );
                    if ( valueString.length() > PERCISSION ) valueString = valueString.substring( 0, PERCISSION );
                    
                    nested = new SimpleOutboundVariable(valueString, outctx, true);

                }
                else{
                 nested = getConverterManager().convertOutbound(value, outctx);
                }
                ovs.put(name, nested);
            }
        }
        catch (MarshallException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new MarshallException(data.getClass(), ex);
        }

        ov.init(ovs, getJavascript());

        return ov;
    }
    
}    

