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
package edu.columbia.gemma.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.StringUtils;

/**
 * This class is converts a java.util.Date to a String and a String to a java.util.Date. It is used by BeanUtils when
 * copying properties. Registered for use in BaseAction.
 * <p>
 * <a href="DateConverter.java.html"><i>View Source</i></a>
 * </p>
 * 
 * @author pavlidis
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @version $Id$
 */
public class DateConverter implements Converter {
    private static DateFormat df = new SimpleDateFormat( DateUtil.getDatePattern() );

    public Object convert( Class type, Object value ) {
        if ( value == null ) {
            return null;
        } else if ( type == Date.class ) {
            return convertToDate( value );
        } else if ( type == String.class ) {
            return convertToString( value );
        }

        throw new ConversionException( "Could not convert " + value.getClass().getName() + " to " + type.getName() );
    }

    protected Object convertToDate( Object value ) {
        if ( value instanceof String ) {
            try {
                if ( StringUtils.isEmpty( value.toString() ) ) {
                    return null;
                }

                return df.parse( ( String ) value );
            } catch ( Exception pe ) {
                throw new ConversionException( "Error converting String to Date" );
            }
        }

        throw new ConversionException( "Could not convert " + value.getClass().getName() + " to Date " );
    }

    protected Object convertToString( Object value ) {
        if ( value instanceof Date ) {
            try {
                return df.format( value );
            } catch ( Exception e ) {
                throw new ConversionException( "Error converting Date to String" );
            }
        }

        return value.toString();
    }
}
