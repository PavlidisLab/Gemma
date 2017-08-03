/*
 * The Gemma project.
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
package ubic.gemma.persistence.service.common.description;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.description.ExternalDatabase;

import java.util.Collection;

/**
 * @author pavlidis
 * @see ExternalDatabaseService
 */
@Service
public class ExternalDatabaseServiceImpl extends ExternalDatabaseServiceBase {

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    /**
     * @see ExternalDatabaseService#find(java.lang.String)
     */
    @Override
    protected ExternalDatabase handleFind( java.lang.String name ) {
        return this.externalDatabaseDao.findByName( name );
    }

    @Override
    protected ExternalDatabase handleFindOrCreate( ExternalDatabase externalDatabase ) {
        return this.externalDatabaseDao.findOrCreate( externalDatabase );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExternalDatabase> handleLoadAll() {
        return this.externalDatabaseDao.loadAll();
    }

    @Override
    protected void handleRemove( ExternalDatabase externalDatabase ) {
        this.externalDatabaseDao.remove( externalDatabase );

    }

}