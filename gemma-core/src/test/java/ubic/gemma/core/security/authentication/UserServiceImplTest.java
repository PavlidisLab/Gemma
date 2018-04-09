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
package ubic.gemma.core.security.authentication;

import org.junit.Before;
import org.junit.Test;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDao;

import java.util.Collection;
import java.util.HashSet;

import static org.easymock.EasyMock.*;

/**
 * @author pavlidis
 */
public class UserServiceImplTest {
    private final UserServiceImpl userService = new UserServiceImpl();
    private final User testUser = User.Factory.newInstance();
    private UserDao userDaoMock;
    private Collection<UserGroup> userGroups;

    @Before
    public void setUp() {
        userDaoMock = createMock( UserDao.class );
        userService.userDao = userDaoMock;

        userService.userGroupDao = createMock( UserGroupDao.class );
        testUser.setEmail( "foo@bar" );
        testUser.setName( "Foo" );
        testUser.setLastName( "Bar" );
        testUser.setUserName( "foobar" );
        testUser.setPassword( "aija" );
        testUser.setPasswordHint( "I am an idiot" );

        UserGroup group = UserGroup.Factory.newInstance();
        group.setName( "users" );
        group.getGroupMembers().add( testUser );
        userGroups = new HashSet<>();
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
