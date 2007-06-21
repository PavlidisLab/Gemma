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
package ubic.gemma.security.acl.basic.jdbc;

import org.acegisecurity.acl.basic.AbstractBasicAclEntry;
import org.acegisecurity.acl.basic.BasicAclEntry;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This Dao should be used to do things to the acl_permission and acl_object_identity tables that are not included as
 * part of the Acegi API. For example, {@link BasicAclEntry} (and the implementing {@link AbstractBasicAclEntry} do not
 * provide a way to modify the acl_object_identity of the acl_permission table).
 * 
 * @author keshav
 * @version $Id$
 */
public class CustomAclDao {

    private BasicDataSource dataSource = null;

    /**
     * Updates the acl_object_identity of the acl_permission table.
     * 
     * @param recipient
     */
    public void updateAclObjectIdentityInAclPermission( String recipient ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.execute( "update acl_permission set acl_object_identity=1 where recipient=" + "\'" + recipient
                + "\'" );
    }

    /**
     * @param dataSource
     */
    public void setDataSource( BasicDataSource dataSource ) {
        this.dataSource = dataSource;
    }

}
