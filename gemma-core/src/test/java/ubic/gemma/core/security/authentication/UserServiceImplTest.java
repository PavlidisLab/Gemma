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

import gemma.gsec.SecurityService;
import gemma.gsec.acl.domain.AclService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDao;

import java.util.Collection;
import java.util.HashSet;

import static org.mockito.Mockito.*;

/**
 * @author pavlidis
 */
@ContextConfiguration
public class UserServiceImplTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class UserServiceImplTestContextConfiguration {

        @Bean
        public UserService userService() {
            return new UserServiceImpl();
        }

        @Bean
        public UserDao userDao() {
            return mock();
        }

        @Bean
        public UserGroupDao userGroupDao() {
            return mock();
        }

        @Bean
        public AclService aclService() {
            return mock();
        }

        @Bean
        public SecurityService securityService() {
            return mock();
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDaoMock;

    private final User testUser = User.Factory.newInstance( "foobar" );

    private Collection<UserGroup> userGroups;

    @Before
    public void setUp() {
        testUser.setId( 1L );
        testUser.setEmail( "foo@bar" );
        testUser.setName( "Foo" );
        testUser.setLastName( "Bar" );
        testUser.setPassword( "aija" );
        testUser.setPasswordHint( "I am an idiot" );

        UserGroup group = UserGroup.Factory.newInstance();
        group.setName( "users" );
        group.getGroupMembers().add( testUser );
        userGroups = new HashSet<>();
        userGroups.add( group );
    }

    @After
    public void resetMocks() {
        reset( userDaoMock );
    }

    @Test
    public void testHandleGetUser() {
        when( userDaoMock.findByUserName( "foobar" ) ).thenReturn( testUser );
        userService.findByUserName( "foobar" );
        verify( userDaoMock ).findByUserName( "foobar" );
    }

    @Test
    public void testHandleSaveUser() throws Exception {
        userService.create( testUser );
        verify( userDaoMock ).findByUserName( "foobar" );
        verify( userDaoMock ).findByEmail( "foo@bar" );
        verify( userDaoMock ).create( testUser );
    }

    @Test
    public void testHandleRemoveUser() {
        when( userDaoMock.load( testUser.getId() ) ).thenReturn( testUser );
        when( userDaoMock.loadGroups( testUser ) ).thenReturn( userGroups );
        userService.delete( testUser );
        verify( userDaoMock ).remove( testUser );
    }
}
