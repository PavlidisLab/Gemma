/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import java.text.DateFormat;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 */
public class AuditEventImpl extends AuditEvent {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6713721089643871509L;

    private static final String TROUBLE_UNKWNOWN_NAME = "Unknown performer";

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if ( this.getPerformer().getUserName() == null ) {
            buf.append( TROUBLE_UNKWNOWN_NAME );
        } else {
            buf.append( this.getPerformer().getUserName() );
        }

        try {
            buf.append( " on "
                    + DateFormat.getDateInstance( DateFormat.LONG, Locale.getDefault() ).format( this.getDate() ) );
        } catch ( Exception ex ) {
            System.err.println( "AuditEventImpl toString problem." );
            System.err.println( ex );
        }
        buf.append( ": " );

        boolean hasNote = false;

        if ( !StringUtils.isEmpty( this.getNote() ) ) {
            buf.append( this.getNote() );
            hasNote = true;
        }
        if ( !StringUtils.isEmpty( this.getDetail() ) ) {
            if ( hasNote ) {
                buf.append( " - " );
            }
            buf.append( this.getDetail() );
        }
        return buf.toString();
    }

}