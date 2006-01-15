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
package edu.columbia.gemma.expression.biomaterial;

/**
 * 
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2006 University of British Columbia
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.expression.biomaterial.CompoundService
 */
public class CompoundServiceImpl
    extends edu.columbia.gemma.expression.biomaterial.CompoundServiceBase
{

    /**
     * @see edu.columbia.gemma.expression.biomaterial.CompoundService#find(edu.columbia.gemma.expression.biomaterial.Compound)
     */
    protected edu.columbia.gemma.expression.biomaterial.Compound handleFind(edu.columbia.gemma.expression.biomaterial.Compound compound)
        throws java.lang.Exception
    {
        //@todo implement protected edu.columbia.gemma.expression.biomaterial.Compound handleFind(edu.columbia.gemma.expression.biomaterial.Compound compound)
        return null;
    }

    /**
     * @see edu.columbia.gemma.expression.biomaterial.CompoundService#create(edu.columbia.gemma.expression.biomaterial.Compound)
     */
    protected edu.columbia.gemma.expression.biomaterial.Compound handleCreate(edu.columbia.gemma.expression.biomaterial.Compound compound)
        throws java.lang.Exception
    {
        //@todo implement protected edu.columbia.gemma.expression.biomaterial.Compound handleCreate(edu.columbia.gemma.expression.biomaterial.Compound compound)
        return null;
    }

    /**
     * @see edu.columbia.gemma.expression.biomaterial.CompoundService#update(edu.columbia.gemma.expression.biomaterial.Compound)
     */
    protected void handleUpdate(edu.columbia.gemma.expression.biomaterial.Compound compound)
        throws java.lang.Exception
    {
        //@todo implement protected void handleUpdate(edu.columbia.gemma.expression.biomaterial.Compound compound)
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.expression.biomaterial.CompoundService.handleUpdate(edu.columbia.gemma.expression.biomaterial.Compound compound) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.expression.biomaterial.CompoundService#remove(edu.columbia.gemma.expression.biomaterial.Compound)
     */
    protected void handleRemove(edu.columbia.gemma.expression.biomaterial.Compound compound)
        throws java.lang.Exception
    {
        this.getCompoundDao().remove(compound);
    }

    @Override
    protected Compound handleFindOrCreate( Compound compound ) throws Exception {
        return this.getCompoundDao().findOrCreate(compound);
    }

}