/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.security.authorization.acl;

import javax.sql.DataSource;

import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.Sid;

/**
 * Subclass to support some additional functionality we need that JdbcMutableAclService does not implement.
 * 
 * @author paul
 * @version $Id$
 */
public class AclServiceImpl extends JdbcMutableAclService implements AclService {

    /*
     * This is declared as a bean in applicationContext-security.xml. I had trouble using the @Component annotation
     * without doing a lot of other configuration changes (passing arguments to the constructor ...)
     */

    /**
     * @param dataSource
     * @param lookupStrategy
     * @param aclCache
     */
    public AclServiceImpl( DataSource dataSource, LookupStrategy lookupStrategy, AclCache aclCache ) {
        super( dataSource, lookupStrategy, aclCache );
    }

    /**
     * Remove a sid and all associated ACEs.
     * 
     * @param sid
     */
    @Override
    public void deleteSid( Sid sid ) {

        Long sidId = super.createOrRetrieveSidPrimaryKey( sid, false );

        // note: this version failed to delete all relevant acl_entry rows
        //String deleteAces = "delete e from acl_entry e inner join acl_sid s on s.id=e.sid where s.sid = ?";
        
        String deleteAces = "delete from acl_entry where sid = ?";
        int rowsAff = jdbcTemplate.update( deleteAces, new Object[] { sidId } );
        
        log.debug( "Deleted "+rowsAff+" entries from the acl_entry table." );

        String deleteSid = "delete from acl_sid where id = ?";
        rowsAff = jdbcTemplate.update( deleteSid, new Object[] { sidId } );
        
        log.debug( "Deleted "+rowsAff+" entries from the acl_sid table." );

    }

}
