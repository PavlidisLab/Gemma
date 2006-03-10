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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * Tests the Contact, Person, User interitance hierarchy and the association between User and UserRole.
 * 
 * @author keshav
 * @version $Id$
 */
public class UserDaoImplTest extends BaseTransactionalSpringContextTest {

    private UserDao userDao;
    private final Log log = LogFactory.getLog( UserDaoImplTest.class );
    private User testUser;
    private UserRole ur;
    private PersisterHelper persisterHelper;
    
    public final void testCreateUser() throws Exception {
        assert testUser != null;
        userDao.create( testUser );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onSetUpInTransaction() throws Exception {
        persisterHelper = ( PersisterHelper )this.getBean( "persisterHelper" );
        super.onSetUpInTransaction();

        // User Object
        testUser = User.Factory.newInstance();
        // UserRole Object
        ur = UserRole.Factory.newInstance();

        String rand = ( new Date() ).toString();

        String adminName = "admin";
        String userName = "user";
        User checkUser = userDao.findByUserName( adminName );

        if ( ( checkUser == null ) ) {
            // User Object
            testUser.setUserName( adminName );
            // UserRole Object
            ur.setUserName( adminName );
            ur.setName( adminName );
        } else {
            testUser.setUserName( rand );
            log.info( rand );
            ur.setUserName( rand );
            ur.setName( userName );
        }

        testUser.setPassword( "root" );
        testUser.setConfirmPassword( "root" );
        testUser.setPasswordHint( "test hint" );
        AuditTrail ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        testUser.setAuditTrail( ad );
        ad = ( AuditTrail ) persisterHelper.persist( ad );
        testUser.getRoles().add( ur );

        // dao.create( testUser );
    }

    /**
     * @param useDao The useDao to set.
     */
    public void setUserDao( UserDao useDao ) {
        this.userDao = useDao;
    }
}