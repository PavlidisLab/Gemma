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
package ubic.gemma.util;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;

/**
 * A utility class to hold methods that generate human-readable strings from
 * objects in the Gemma data model.  This class was specifically created because
 * I couldn't find anywhere to put a toString method in the AuditEvent class
 * that wasn't destroyed every time Gemma was built.
 * 
 * @author luke
 */
public class ToStringUtil {
    
    public static String toString( AuditEvent event ) {
        StringBuffer buf = new StringBuffer();
        buf.append( event.getDate() );
        buf.append( " by " );
        buf.append( event.getPerformer().getUserName() );
        if ( !StringUtils.isEmpty( event.getNote() ) ) {
            buf.append( "\n" );
            buf.append( event.getNote() );
        }
        if ( !StringUtils.isEmpty( event.getDetail() ) ) {
            buf.append( "\n" );
            buf.append( event.getDetail() );
        }
        return buf.toString();
    }
}