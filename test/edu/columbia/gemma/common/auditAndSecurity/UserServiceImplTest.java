package edu.columbia.gemma.common.auditAndSecurity;

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
public class UserServiceImplTest extends BaseServiceTestCase {
    private MockControl control;
    private UserServiceImpl userService = new UserServiceImpl();
    private UserDao userDaoMock;
    private User testUser = User.Factory.newInstance();

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        control = MockControl.createControl( UserDao.class );
        userDaoMock = (UserDao)control.getMock();
        userService.setUserDao(userDaoMock);
        testUser.setEmail("foo@bar");
        testUser.setFirstName("Foo");
        testUser.setLastName("Bar");
        testUser.setMiddleName("");
        testUser.setUserName("foobar");
        testUser.setPassword("aija");
        testUser.setConfirmPassword("aija");
        testUser.setPasswordHint("I am an idiot");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHandleGetUser() {
        userDaoMock.findByUserName("foobar");
        control.setReturnValue(testUser);
        control.replay();
        userService.getUser("foobar");
        control.verify();
    }

    public void testHandleGetUsers() {
    }

    public void testHandleSaveUser() throws Exception {
        userDaoMock.create(testUser);
        control.setReturnValue(testUser);
        control.replay();
        userService.saveUser(testUser);
        control.verify();
    }

    public void testHandleRemoveUser() {
        userDaoMock.remove(testUser);
      //  control.setVoidCallable();
        userDaoMock.findByUserName("foobar");
        control.setReturnValue(testUser); // this should get called to find the user to remove.
        control.replay();
        userService.removeUser("foobar");
        control.verify();
    }

    /*
     * Class under test for String handleCheckLoginCookie(java.lang.String)
     */
    public void testHandleCheckLoginCookieString() {
        // TODO
    }

    /*
     * Class under test for String handleCreateLoginCookie(java.lang.String)
     */
    public void testHandleCreateLoginCookieString() {
        // TODO
    }

    /*
     * Class under test for void handleRemoveLoginCookies(String)
     */
    public void testHandleRemoveLoginCookiesString() {
        // TODO
    }

    /*
     * Class under test for void handleAddRole(User, UserRole)
     */
    public void testHandleAddRoleUserUserRole() {
        // TODO
    }

}
