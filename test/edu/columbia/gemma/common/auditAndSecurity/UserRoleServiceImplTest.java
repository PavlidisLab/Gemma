package edu.columbia.gemma.common.auditAndSecurity;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserRoleServiceImplTest extends TestCase {

    private UserRoleServiceImpl userRoleService = new UserRoleServiceImpl();
    private UserRoleDao userRoleDaoMock;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        userRoleDaoMock = createMock( UserRoleDao.class );
        userRoleService.setUserRoleDao( userRoleDaoMock );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHandleGetRoles() {
        Collection<UserRole> allRoles = new HashSet<UserRole>();
        for ( int i = 0; i < 5; i++ ) {
            UserRole ur = UserRole.Factory.newInstance();
            ur.setName( "admin" + i );
            ur.setUserName( "paul" + i );
            allRoles.add( ur );
        }

        userRoleDaoMock.loadAll();
        expectLastCall().andReturn( allRoles );

        replay( userRoleDaoMock );
        userRoleService.getRoles();
        verify( userRoleDaoMock );

    }

    // this commented out because the getRole method no longer calls the Dao.
    // public void testHandleGetRole() {
    // UserRole ur = UserRole.Factory.newInstance();
    // ur.setName("admin");
    // // ur.setUserName("paul");
    // Collection allRoles = new HashSet();
    // allRoles.add(ur);
    // userRoleDaoMock.findRolesByRoleName("admin");
    // control.setReturnValue(allRoles);
    //        
    // control.replay();
    // userRoleService.getRole("admin");
    // control.verify();
    // }

    public void testHandleSaveRole() {
        UserRole ur = UserRole.Factory.newInstance();
        ur.setName( "admin" );
        ur.setUserName( "paul" );
        userRoleDaoMock.create( ur );
        expectLastCall().andReturn( ur );

        replay( userRoleDaoMock );
        userRoleService.saveRole( ur );
        verify( userRoleDaoMock );
    }

    public void testHandleRemoveRole() {
    }

}
