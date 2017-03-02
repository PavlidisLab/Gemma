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
package ubic.gemma.model.common.quantitationtype;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService
 */
@Service
public class QuantitationTypeServiceImpl implements QuantitationTypeService {

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#create(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    @Transactional
    public QuantitationType create( final QuantitationType quantitationType ) {
        return this.getQuantitationTypeDao().create( quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#find(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    @Transactional(readOnly = true)
    public QuantitationType find( final QuantitationType quantitationType ) {
        return this.getQuantitationTypeDao().find( quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#findOrCreate(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    @Transactional
    public QuantitationType findOrCreate( final QuantitationType quantitationType ) {
        return this.getQuantitationTypeDao().findOrCreate( quantitationType );

    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public QuantitationType load( final java.lang.Long id ) {
        return this.getQuantitationTypeDao().load( id );

    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#loadAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<QuantitationType> loadAll() {
        return ( Collection<QuantitationType> ) this.getQuantitationTypeDao().loadAll();

    }

    @Override
    @Transactional(readOnly = true)
    public List<QuantitationType> loadByDescription( String description ) {
        return this.quantitationTypeDao.loadByDescription( description );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#remove(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    @Transactional
    public void remove( final QuantitationType quantitationType ) {
        try {
            this.getQuantitationTypeDao().remove( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.remove(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#update(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    @Transactional
    public void update( final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        this.handleUpdate( quantitationType );

    }

    /**
     * Gets the reference to <code>quantitationType</code>'s DAO.
     */
    QuantitationTypeDao getQuantitationTypeDao() {
        return this.quantitationTypeDao;
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#update(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    protected void handleUpdate( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        this.getQuantitationTypeDao().update( quantitationType );
    }

}