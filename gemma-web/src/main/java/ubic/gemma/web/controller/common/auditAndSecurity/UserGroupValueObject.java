/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.web.controller.common.auditAndSecurity;

/**
 * @author paul
 * @version $Id$
 */
public class UserGroupValueObject {

    /**
     * Can the current user edit the group - e.g., add users.
     */
    private boolean canEdit;

    /**
     * If the current user is a member of the group.
     */
    private boolean isMember;

    private String groupName;

    public UserGroupValueObject() {
        super();
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public boolean isMember() {
        return isMember;
    }

    public void setCanEdit( boolean canEdit ) {
        this.canEdit = canEdit;
    }

    public void setGroupName( String groupName ) {
        this.groupName = groupName;
    }

    public void setMember( boolean isMember ) {
        this.isMember = isMember;
    }

}
