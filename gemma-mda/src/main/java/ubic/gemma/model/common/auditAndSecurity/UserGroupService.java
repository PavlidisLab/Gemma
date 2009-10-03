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
package ubic.gemma.model.common.auditAndSecurity;

/**
 * 
 */
public interface UserGroupService {

    /**
     * 
     */
    public ubic.gemma.model.common.Securable create( java.lang.String name, java.lang.String description );

    /**
     * 
     */
    public void update( java.lang.String groupName, ubic.gemma.model.common.auditAndSecurity.User groupMember );

    public void update( UserGroup group );
}
