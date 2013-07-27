/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.security.audit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.AuditableAccessControlEntry;

import ubic.gemma.util.Settings;

/**
 * Wire into the {@link org.springframework.security.acls.jdbc.LookupStrategy} to enable logging of object access.
 * 
 * @author paul
 * @version $Id$
 * @see org.springframework.security.acls.jdbc.LookupStrategy
 */
public class AclAuditLogger implements AuditLogger {

    private static Log log = LogFactory.getLog( AclAuditLogger.class );

    private static boolean needToLog = Settings.getBoolean( "gemma.acl.audit" );

    @Override
    public void logIfNeeded( boolean granted, AccessControlEntry ace ) {

        if ( !needToLog ) return;

        if ( ace instanceof AuditableAccessControlEntry ) {
            AuditableAccessControlEntry auditableAce = ( AuditableAccessControlEntry ) ace;

            if ( granted && auditableAce.isAuditSuccess() ) {
                log.info( "GRANTED due to ACE: " + ace + " ObjectIdentity=" + ace.getAcl().getObjectIdentity() );
            } else if ( !granted && auditableAce.isAuditFailure() ) {
                log.warn( "DENIED due to ACE: " + ace + " ObjectIdentity=" + ace.getAcl().getObjectIdentity() );
            }
        }

    }

}
