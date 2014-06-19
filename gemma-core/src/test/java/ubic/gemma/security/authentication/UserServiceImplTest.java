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
package ubic.gemma.security.authentication;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserDao;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.model.common.auditAndSecurity.UserGroupDao;

/**
 * @author pavlidis
 * @version $Id$
 */
public class UserServiceImplTest {
    private UserServiceImpl userService = new UserServiceImpl();
    private UserDao userDaoMock;
    private User testUser = User.Factory.newInstance();
    private Collection<UserGroup> userGroups;

    /*
     * @see TestCase#setUp()
     */
    @Before
    public void setUp() {
        userDaoMock = createMock( UserDao.class );
        userService.userDao = userDaoMock;

        UserGroupDao userGroupDaoMock = createMock( UserGroupDao.class );
        userService.userGroupDao = userGroupDaoMock;
        testUser.setEmail( "foo@bar" );
        testUser.setName( "Foo" );
        testUser.setLastName( "Bar" );
        testUser.setUserName( "foobar" );
        testUser.setPassword( "aija" );
        testUser.setPasswordHint( "I am an idiot" );

        UserGroup group = UserGroup.Factory.newInstance();
        group.setName( "users" );
        group.getGroupMembers().add( testUser );
        userGroups = new HashSet<UserGroup>();
        userGroups.add( group );

    }

    @Test
    public void testHandleGetUser() {
        userDaoMock.findByUserName( "foobar" );
        expectLastCall().andReturn( testUser );
        replay( userDaoMock );
        userService.findByUserName( "foobar" );
        verify( userDaoMock );
    }

    @Test
    public void testHandleSaveUser() throws Exception {
        userDaoMock.findByUserName( "foobar" );
        expectLastCall().andReturn( null );
        userDaoMock.findByEmail( "foo@bar" );
        expectLastCall().andReturn( null );
        userDaoMock.create( testUser );
        expectLastCall().andReturn( testUser );
        replay( userDaoMock );
        userService.create( testUser );
        verify( userDaoMock );
    }

    @Test
    public void testHandleRemoveUser() {

        userDaoMock.loadGroups( testUser );
        expectLastCall().andReturn( userGroups );
        userDaoMock.remove( testUser );
        expectLastCall().once();
        replay( userDaoMock );
        userService.delete( testUser );
        verify( userDaoMock );
    }

}
