/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.description;

import org.springframework.security.access.annotation.Secured;

/**
 * @author kelsey
 * @version $Id$
 */
public interface DatabaseEntryService {

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    public DatabaseEntry find( DatabaseEntry databaseEntry );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public void remove( ubic.gemma.model.common.description.DatabaseEntry databaseEntry );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public void update( ubic.gemma.model.common.description.DatabaseEntry databaseEntry );

}
