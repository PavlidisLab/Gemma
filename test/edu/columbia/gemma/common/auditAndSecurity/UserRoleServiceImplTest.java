package edu.columbia.gemma.common.auditAndSecurity;

import java.util.Collection;
import java.util.HashSet;

import org.easymock.MockControl;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserRoleServiceImplTest extends BaseServiceTestCase {

    private UserRoleServiceImpl userRoleService = new UserRoleServiceImpl();
    private UserRoleDao userRoleDaoMock;
    private MockControl control;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        control = MockControl.createControl( UserRoleDao.class );
        userRoleDaoMock = ( UserRoleDao ) control.getMock();
        userRoleService.setUserRoleDao(userRoleDaoMock);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHandleGetRoles() {
        Collection allRoles = new HashSet();
        for(int i = 0; i < 5; i++ ) {
            UserRole ur = UserRole.Factory.newInstance();
            ur.setName("admin" + i);
            ur.setUserName("paul" + i);
            allRoles.add(ur);
        }
        
        userRoleDaoMock.findAllRoles();
        control.setReturnValue(allRoles);
        
        control.replay();
        userRoleService.getRoles();
        control.verify();
        
    }

    public void testHandleGetRole() {
        UserRole ur = UserRole.Factory.newInstance();
        ur.setName("admin");
        ur.setUserName("paul");
        Collection allRoles = new HashSet();
        allRoles.add(ur);
        userRoleDaoMock.findRolesByRoleName("admin");
        control.setReturnValue(allRoles);
        
        control.replay();
        userRoleService.getRole("admin");
        control.verify();
    }

    public void testHandleSaveRole() {
        UserRole ur = UserRole.Factory.newInstance();
        ur.setName("admin");
        ur.setUserName("paul");
        userRoleDaoMock.create(ur);
        control.setReturnValue(ur);
        
        control.replay();
        userRoleService.saveRole(ur);
        control.verify();
    }

    public void testHandleRemoveRole() {
    }

}
