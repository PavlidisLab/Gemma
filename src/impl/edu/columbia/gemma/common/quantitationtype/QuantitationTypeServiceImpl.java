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
package edu.columbia.gemma.common.quantitationtype;

/**
 * <hr>
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.quantitationtype.QuantitationTypeService
 */
public class QuantitationTypeServiceImpl extends edu.columbia.gemma.common.quantitationtype.QuantitationTypeServiceBase {

    /**
     * @see edu.columbia.gemma.common.quantitationtype.QuantitationTypeService#find(edu.columbia.gemma.common.quantitationtype.QuantitationType)
     */
    protected edu.columbia.gemma.common.quantitationtype.QuantitationType handleFind(
            edu.columbia.gemma.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception {
        return this.getQuantitationTypeDao().find( quantitationType );
    }

    /**
     * @see edu.columbia.gemma.common.quantitationtype.QuantitationTypeService#create(edu.columbia.gemma.common.quantitationtype.QuantitationType)
     */
    protected edu.columbia.gemma.common.quantitationtype.QuantitationType handleCreate(
            edu.columbia.gemma.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception {
        return ( QuantitationType ) this.getQuantitationTypeDao().create( quantitationType );
    }

    /**
     * @see edu.columbia.gemma.common.quantitationtype.QuantitationTypeService#update(edu.columbia.gemma.common.quantitationtype.QuantitationType)
     */
    protected void handleUpdate( edu.columbia.gemma.common.quantitationtype.QuantitationType quantitationType )
            throws java.lang.Exception {
        this.getQuantitationTypeDao().update( quantitationType );
    }

    /**
     * @see edu.columbia.gemma.common.quantitationtype.QuantitationTypeService#remove(edu.columbia.gemma.common.quantitationtype.QuantitationType)
     */
    protected void handleRemove( edu.columbia.gemma.common.quantitationtype.QuantitationType quantitationType )
            throws java.lang.Exception {
        this.getQuantitationTypeDao().remove( quantitationType );
    }

    @Override
    protected QuantitationType handleFindOrCreate( QuantitationType quantitationType ) throws Exception {
        return this.getQuantitationTypeDao().findOrCreate( quantitationType );
    }

}