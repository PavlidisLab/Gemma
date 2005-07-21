package edu.columbia.gemma.common.auditAndSecurity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.SessionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * Tests the Contact, Person, User interitance hierarchy and the association between User and UserRole.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class UserDaoImplTest extends BaseDAOTestCase {

    private UserDao dao = null;
    private final Log log = LogFactory.getLog( UserDaoImplTest.class );
    private SessionFactory sf = null;
    private User testUser = null;
    private UserRole ur = null;

    public final void testCreateUser() throws Exception {
        dao.create( testUser );
    }

    protected void setUp() throws Exception {
        super.setUp();

        sf = ( SessionFactory ) ctx.getBean( "sessionFactory" );
        dao = ( UserDao ) ctx.getBean( "userDao" );

        // User Object
        testUser = User.Factory.newInstance();
        // UserRole Object
        ur = UserRole.Factory.newInstance();

        String rand = ( new Date() ).toString();

        String adminName = "admin";
        String userName = "user";
        User checkUser = dao.findByUserName( adminName );

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

        Set set = new HashSet();
        testUser.setRoles( set );

        testUser.setPassword( "root" );
        testUser.setConfirmPassword( "root" );
        testUser.setPasswordHint( "test hint" );

        testUser.getRoles().add( ur );

        // dao.create( testUser );
    }

    protected void tearDown() throws Exception {
        // dao.remove( testUser );
        dao = null;
    }
}