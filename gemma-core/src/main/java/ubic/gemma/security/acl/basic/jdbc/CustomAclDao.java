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

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acl.basic.AbstractBasicAclEntry;
import org.springframework.security.acl.basic.BasicAclEntry;

/**
 * This Dao should be used to do things to the acl_permission and acl_object_identity tables that are not included as
 * part of the Acegi API. For example, {@link BasicAclEntry} (and the implementing {@link AbstractBasicAclEntry} do not
 * provide a way to modify the acl_object_identity of the acl_permission table).
 * 
 * @author keshav
 * @version $Id$
 */
@SuppressWarnings("deprecation")
public class CustomAclDao {

    /*
     * Objects are grouped in a hierarchy. A default 'parent' is defined in the database. This must match an entry in
     * the ACL_OBJECT_IDENTITY table. In Gemma this is added as part of database initialization (see mysql-acegy-acl.sql
     * for MySQL version)
     */

    public static final String ADMIN_CONTROL_NODE = "adminControlNode";

    public static final String PUBLIC_CONTROL_NODE = "publicControlNode";

    public static final String ADMIN_CONTROL_NODE_PARENT_ID = "1";

    public static final String PUBLIC_CONTROL_NODE_PARENT_ID = "2";

    private DataSource dataSource = null;

    /**
     * Updates the acl_object_identity parent to parent.
     * 
     * @param id
     * @param parent
     */
    public void updateAclObjectIdentityParent( Long id, int parent ) {
        String queryString = "update acl_object_identity set parent_object=" + parent + " where id=" + id;
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.execute( queryString );
    }

    /**
     * Inserts a "control node" (row in acl_permission table) giving the recipient access to the public data. Call this
     * when adding a new user to the database.
     * 
     * @param recipient
     * @param mask
     */
    public void insertPublicAccessControlNodeForRecipient( String recipient, int mask ) {
        String queryString = "insert into acl_permission values(" + null + "," + PUBLIC_CONTROL_NODE_PARENT_ID + ","
                + "\'" + recipient + "\'" + "," + mask + ")";
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.execute( queryString );
    }

    /**
     * @param dataSource
     */
    public void setDataSource( DataSource dataSource ) {
        this.dataSource = dataSource;
    }

}
