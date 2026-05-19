/*
 * The gemma-mda project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package gemma.gsec.acl.domain;

import org.springframework.security.acls.model.Sid;

/**
 * @author Paul
 * @version $Id: AclSid.java,v 1.1 2013/09/14 16:55:20 paul Exp $
 */
public abstract class AclSid implements Sid {

    /**
     *
     */
    private static final long serialVersionUID = -3256613712125656321L;
    // This ID is just our convention for hibernate. In reality the effective unique primary key is the sid (e.g.
    // username or
    // grantedauthority)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

}
