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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#remove(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public void remove( final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            this.getQuantitationTypeDao().remove( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.remove(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    
    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#update(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    protected void handleUpdate( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType )
            throws java.lang.Exception {
        this.getQuantitationTypeDao().update( quantitationType );
    }
    
    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#create(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public QuantitationType create( final QuantitationType quantitationType ) {
        try {
            return this.getQuantitationTypeDao().create( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.create(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#find(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public QuantitationType find( final QuantitationType quantitationType ) {
        try {
            return this.getQuantitationTypeDao().find( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.find(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#findOrCreate(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType findOrCreate(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.getQuantitationTypeDao().findOrCreate( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.findOrCreate(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#load(java.lang.Long)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType load( final java.lang.Long id ) {
        try {
            return this.getQuantitationTypeDao().load( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#loadAll()
     */
    public java.util.Collection loadAll() {
        try {
            return this.getQuantitationTypeDao().loadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.loadAll()' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>quantitationType</code>'s DAO.
     */
    public void setQuantitationTypeDao( ubic.gemma.model.common.quantitationtype.QuantitationTypeDao quantitationTypeDao ) {
        this.quantitationTypeDao = quantitationTypeDao;
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#update(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public void update( final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            this.handleUpdate( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.update(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>quantitationType</code>'s DAO.
     */
    protected ubic.gemma.model.common.quantitationtype.QuantitationTypeDao getQuantitationTypeDao() {
        return this.quantitationTypeDao;
    }


}