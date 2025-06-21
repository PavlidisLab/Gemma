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
import org.directwebremoting.dwrp.ParseUtil;
import org.directwebremoting.dwrp.ProtocolConstants;
import org.directwebremoting.extend.*;
import org.directwebremoting.util.LocalUtil;
import org.directwebremoting.util.Messages;
import ubic.gemma.model.common.description.Characteristic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author kelsey
 *
 */
public class CharacteristicConverter extends BeanConverter {

    @SuppressWarnings("unchecked")
    @Override
    public Object convertInbound( Class paramType, InboundVariable iv, InboundContext inctx ) throws MarshallException {
        String value = iv.getValue();

        // If the text is null then the whole bean is null
        if ( value.trim().equals( ProtocolConstants.INBOUND_NULL ) ) {
            return null;
        }

        if ( !value.startsWith( ProtocolConstants.INBOUND_MAP_START ) ) {
            throw new MarshallException( paramType, Messages.getString( "BeanConverter.FormatError",
                    ProtocolConstants.INBOUND_MAP_START ) );
        }

        if ( !value.endsWith( ProtocolConstants.INBOUND_MAP_END ) ) {
            throw new MarshallException( paramType, Messages.getString( "BeanConverter.FormatError",
                    ProtocolConstants.INBOUND_MAP_START ) );
        }

        value = value.substring( 1, value.length() - 1 );

        try {
            Map<String, String> tokens = extractInboundTokens( paramType, value );

            Object bean = Characteristic.Factory.newInstance();

            if ( instanceType != null ) {
                inctx.addConverted( iv, instanceType, bean );
            } else {
                inctx.addConverted( iv, paramType, bean );
            }

            Map<String, Property> properties = getPropertyMapFromObject( bean, false, true );

            // Loop through the properties passed in

            for ( Entry<String, String> entry : tokens.entrySet() ) {
                String key = entry.getKey();
                String val = entry.getValue();

                Property property = properties.get( key );
                if ( property == null ) {
                    continue;
                }

                Class<?> propType = property.getPropertyType();

                String[] split = ParseUtil.splitInbound( val );
                String splitValue = split[LocalUtil.INBOUND_INDEX_VALUE];
                String splitType = split[LocalUtil.INBOUND_INDEX_TYPE];

                InboundVariable nested = new InboundVariable( iv.getLookup(), null, splitType, splitValue );
                TypeHintContext incc = createTypeHintContext( inctx, property );

                Object output = converterManager.convertInbound( propType, nested, inctx, incc );

                // Unfortunate hack. Change the properties association to be a Set instead of a Collection in the
                // model; Model think this is a generic Collection, Hibernate thinks its a Set. DWR converts collections
                // to ArrayLists... *sigh* Hibernate then dies of a class cast exception. All because of a general type
                // of Collection
                if ( ( key.equals( "properties" ) ) && ( output instanceof ArrayList ) ) {
                    ArrayList<Object> propertyList = ( ArrayList<Object> ) output;
                    output = new HashSet<>( propertyList );
                }

                property.setValue( bean, output );
            }

            return bean;
        } catch ( MarshallException ex ) {
            throw ex;
        } catch ( Exception ex ) {
            throw new MarshallException( paramType, ex );
        }
    }
}
