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

import java.util.Collection;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acl.basic.AbstractBasicAclEntry;
import org.springframework.security.acl.basic.BasicAclEntry;

import ubic.gemma.model.common.Securable;
import ubic.gemma.util.EntityUtils;

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
     * Update the control node this recipient points to. That is, for each user in the system there is an acl_permission
     * that does not have an associated acl_object_identity that is a {@link Securable}. Instead, the
     * acl_object_identity in the acl_permission table points to one of the control nodes in the acl_object_identity
     * table (like CustomAclDao.ADMIN_CONTROL_NODE or CustomAclDao.USER_CONTROL_NODE).
     * <p>
     * This is called, for example, after changing the role of a user to make the pertinent changes to the acl
     * permission table.
     * <p>
     * For example, a user="foo", has an acl_permission with an acl_object_identity=2. When changing this from a "user"
     * to an "admin", we must set the acl_object_identity=1 (and mask=6).
     * 
     * @param newAclObjectIdentity The new control node to use (the new acl object identity id to use).
     * @param oldAclObjectIdentity The old control node to use (the old acl object identity id to use).
     * @param mask
     * @param recipient
     */
    public void updateControlNodeForRecipient( int newAclObjectIdentity, int oldObjectIdentity, int mask,
            String recipient ) {
        String queryString = "update acl_permission set acl_object_identity=" + newAclObjectIdentity + ", mask=" + mask
                + " where recipient=\"" + recipient + "\" && acl_object_identity=" + oldObjectIdentity;
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.execute( queryString );
    }

    /**
     * @param dataSource
     */
    public void setDataSource( DataSource dataSource ) {
        this.dataSource = dataSource;
    }

    /**
     * Creates the object_identity to be used in the acl_object_identity table. NOTE copied from SecurableDao.
     * 
     * @param target
     * @return
     */
    private String createObjectIdentityFromObject( Securable target ) {

        Securable implementation = ( Securable ) EntityUtils.getImplementationForProxy( target );
        Long id = implementation.getId();

        return implementation.getClass().getName() + ":" + id;
    }

}
