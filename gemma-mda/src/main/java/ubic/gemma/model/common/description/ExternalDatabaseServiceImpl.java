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
package ubic.gemma.model.common.description;

import java.util.Collection;

import org.springframework.stereotype.Service;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.ExternalDatabaseService
 */
@Service
public class ExternalDatabaseServiceImpl extends ubic.gemma.model.common.description.ExternalDatabaseServiceBase {

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseService#find(java.lang.String)
     */
    @Override
    protected ExternalDatabase handleFind( java.lang.String name ) {
        return this.getExternalDatabaseDao().findByName( name );
    }

    @Override
    protected ExternalDatabase handleFindOrCreate( ExternalDatabase externalDatabase ) {
        return this.getExternalDatabaseDao().findOrCreate( externalDatabase );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExternalDatabase> handleLoadAll() {
        return ( Collection<ExternalDatabase> ) this.getExternalDatabaseDao().loadAll();
    }

    @Override
    protected void handleRemove( ExternalDatabase externalDatabase ) {
        this.getExternalDatabaseDao().remove( externalDatabase );

    }

}