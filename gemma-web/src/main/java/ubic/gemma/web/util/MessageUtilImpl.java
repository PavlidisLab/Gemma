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
package ubic.gemma.web.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author pavlidis
 */
@Component
@CommonsLog
public class MessageUtilImpl implements MessageUtil {

    public static final String MESSAGES_ATTRIBUTE = "messages";

    @Autowired
    private MessageSource messageSource;

    @Override
    public void saveMessage( String key, String defaultMessage ) {
        saveMessage( key, new Object[] {}, defaultMessage );
    }

    @Override
    public void saveMessage( String key, Object parameter, String defaultMessage ) {
        saveMessage( key, new Object[] { parameter }, defaultMessage );
    }

    @Override
    public void saveMessage( String key, Object[] parameters, String defaultMessage ) {
        saveMessage( messageSource.getMessage( key, parameters, defaultMessage, LocaleContextHolder.getLocale() ) );
    }

    @Override
    public void saveMessage( String msg ) {
        Object sessAttr = RequestContextHolder.getRequestAttributes()
                .getAttribute( MESSAGES_ATTRIBUTE, RequestAttributes.SCOPE_SESSION );

        List<Object> messages;
        if ( sessAttr == null ) {
            messages = new ArrayList<>();
        } else {
            if ( sessAttr instanceof String ) {
                messages = new ArrayList<>();
                messages.add( sessAttr );
            } else if ( sessAttr instanceof Collection ) {
                messages = new ArrayList<>( ( Collection<?> ) sessAttr );
            } else {
                log.warn( "Unexpected type of messages attribute: " + sessAttr.getClass().getName() + ", it will be overwritten with an ArrayList." );
                messages = new ArrayList<>();
            }
        }
        messages.add( msg );

        RequestContextHolder.getRequestAttributes()
                .setAttribute( MESSAGES_ATTRIBUTE, messages, RequestAttributes.SCOPE_SESSION );
    }
}
