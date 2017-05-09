/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.persistence.service.common.auditAndSecurity;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserQuery;

import java.sql.Date;
import java.util.Collection;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public interface UserQueryDao {

    public UserQuery create( UserQuery userQuery );

    public Collection<UserQuery> findByUser( User user );

    public UserQuery findMostRecentForUser( User user );

    public UserQuery load( Long id );

    public Collection<? extends UserQuery> loadAll();

    public void remove( UserQuery userQuery );

    public void removeAllForUser( User user );

    public void removeOldForUser( User user, Date staleDate );

}
