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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the Contact, Person, User interitance hierarchy and the association between User and UserRole.
 * 
 * @author keshav
 * @version $Id$
 */
public class UserDaoImplTest extends BaseSpringContextTest {
    String email = null;

    private UserDao userDao;
    private final Log log = LogFactory.getLog( UserDaoImplTest.class );
    private User testUser;
    private UserRole ur;

    public final void testCreateUser() throws Exception {
        assert testUser != null;
        User f = ( User ) userDao.create( testUser );
        assertNotNull( f );
        assertNotNull( f.getId() );
    }

    public final void testFindByEmail() throws Exception {
        userDao.create( testUser );

        Contact u = userDao.findByEmail( email );
        assertNotNull( u );
        assertEquals( email, u.getEmail() );
        assertTrue( u instanceof User );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        email = RandomStringUtils.randomAlphabetic( 16 ) + "@bar.com";
        // User Object
        testUser = User.Factory.newInstance();
        // UserRole Object
        ur = UserRole.Factory.newInstance();

        String rand = ( new Date() ).toString();

        String adminName = RandomStringUtils.randomAlphabetic( 16 );
        String userName = RandomStringUtils.randomAlphabetic( 16 );
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
        testUser.setPasswordHint( "test hint" );
        testUser.setEmail( email );
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