/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.common.Auditable;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class StatusServiceImpl implements StatusService {

    @Autowired
    StatusDao statusDao;

    @Override
    public void initializeStatus( Auditable auditable ) {
        if ( auditable.getStatus() != null ) return;
        this.statusDao.initializeStatus( auditable );

    }

    @Override
    public Status create() {
        return this.statusDao.create();
    }

    @Override
    public void update( Status s ) {
        this.statusDao.update( s );      
    }

}
