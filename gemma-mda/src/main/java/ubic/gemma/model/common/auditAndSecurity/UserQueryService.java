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
package ubic.gemma.model.common.auditAndSecurity;

import java.sql.Date;
import java.util.Collection;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public interface UserQueryService {

    public UserQuery create( UserQuery userQuery );

    public void remove( UserQuery userQuery );

    public void removeAllForUser( User user );

    public void removeOldForUser( User user, Date staleDate );

    public Collection<UserQuery> loadAll();

    public Collection<UserQuery> findByUser( User user );

    public UserQuery load( Long id );

    public UserQuery findMostRecentForUser( User user );

}
