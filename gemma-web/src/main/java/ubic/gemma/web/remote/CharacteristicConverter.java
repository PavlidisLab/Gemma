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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.convert.BeanConverter;
import org.directwebremoting.dwrp.ParseUtil;
import org.directwebremoting.dwrp.ProtocolConstants;
import org.directwebremoting.extend.InboundContext;
import org.directwebremoting.extend.InboundVariable;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.Property;
import org.directwebremoting.extend.TypeHintContext;
import org.directwebremoting.util.LocalUtil;
import org.directwebremoting.util.Messages;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author kelsey
 * @version $Id$
 */
public class CharacteristicConverter extends BeanConverter {
    private static Log log = LogFactory.getLog( CharacteristicConverter.class.getName() );

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

            Object bean;
            if ( instanceType != null ) {

                log.info( instanceType );

                if ( tokens.containsKey( "valueUri" ) )
                    bean = VocabCharacteristic.Factory.newInstance();
                else
                    bean = Characteristic.Factory.newInstance();

                inctx.addConverted( iv, instanceType, bean );
            } else {
                if ( tokens.containsKey( "valueUri" ) )
                    bean = ubic.gemma.model.common.description.VocabCharacteristic.Factory.newInstance();
                else
                    bean = ubic.gemma.model.common.description.Characteristic.Factory.newInstance();

                inctx.addConverted( iv, paramType, bean );
            }

            Map<String, Property> properties = getPropertyMapFromObject( bean, false, true );

            // Loop through the properties passed in

            for ( Iterator<Entry<String, String>> it = tokens.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, String> entry = it.next();
                String key = entry.getKey();
                String val = entry.getValue();

                Property property = properties.get( key );
                if ( property == null ) {
                    log.warn( "Missing java bean property to match javascript property: " + key
                            + ". For causes see debug level logs:" );

                    log.debug( "- The javascript may be refer to a property that does not exist" );
                    log.debug( "- You may be missing the correct setter: set" + Character.toTitleCase( key.charAt( 0 ) )
                            + key.substring( 1 ) + "()" );
                    log.debug( "- The property may be excluded using include or exclude rules." );

                    StringBuffer all = new StringBuffer();
                    for ( Iterator<String> pit = properties.keySet().iterator(); pit.hasNext(); ) {
                        all.append( pit.next() );
                        if ( pit.hasNext() ) {
                            all.append( ',' );
                        }
                    }
                    log.debug( "Fields exist for (" + all + ")." );
                    continue;
                }

                Class<?> propType = property.getPropertyType();

                String[] split = ParseUtil.splitInbound( val );
                String splitValue = split[LocalUtil.INBOUND_INDEX_VALUE];
                String splitType = split[LocalUtil.INBOUND_INDEX_TYPE];

                InboundVariable nested = new InboundVariable( iv.getLookup(), null, splitType, splitValue );
                TypeHintContext incc = createTypeHintContext( inctx, property );

                Object output = converterManager.convertInbound( propType, nested, inctx, incc );

                // TODO: Total hack. Change the properties association to be a SET instead of a Collection in the model
                // Model think this is a collection, hibernate thinks its a set. DWR converts collections to
                // ArrayLists... *sigh* Hibernate then dies of a class cast exception. All because of a general type of
                // Collection
                if ( ( key.equals( "properties" ) ) && ( output instanceof ArrayList ) ) {
                    ArrayList<Object> propertyList = ( ArrayList<Object> ) output;
                    output = new HashSet<Object>( propertyList );
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
