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

import org.springframework.stereotype.Service;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService
 */
@Service
public class QuantitationTypeServiceImpl extends ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceBase {

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#create(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected ubic.gemma.model.common.quantitationtype.QuantitationType handleCreate(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception {
        return this.getQuantitationTypeDao().create( quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#find(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected ubic.gemma.model.common.quantitationtype.QuantitationType handleFind(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception {
        return this.getQuantitationTypeDao().find( quantitationType );
    }

    @Override
    protected QuantitationType handleFindOrCreate( QuantitationType quantitationType ) throws Exception {
        return this.getQuantitationTypeDao().findOrCreate( quantitationType );
    }

    @Override
    protected QuantitationType handleLoad( Long id ) throws Exception {
        return this.getQuantitationTypeDao().load( id );
    }

    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getQuantitationTypeDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#remove(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType )
            throws java.lang.Exception {
        this.getQuantitationTypeDao().remove( quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#update(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType )
            throws java.lang.Exception {
        this.getQuantitationTypeDao().update( quantitationType );
    }

}