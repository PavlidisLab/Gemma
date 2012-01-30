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
package ubic.gemma.model.expression.biomaterial;

import org.springframework.stereotype.Service;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.biomaterial.CompoundService
 */
@Service
public class CompoundServiceImpl extends ubic.gemma.model.expression.biomaterial.CompoundServiceBase {

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundService#createDatabaseEntity(ubic.gemma.model.expression.biomaterial.Compound)
     */
    protected ubic.gemma.model.expression.biomaterial.Compound handleCreate(
            ubic.gemma.model.expression.biomaterial.Compound compound ) throws java.lang.Exception {
        return this.getCompoundDao().create( compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundService#find(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    protected ubic.gemma.model.expression.biomaterial.Compound handleFind(
            ubic.gemma.model.expression.biomaterial.Compound compound ) throws java.lang.Exception {
        return this.getCompoundDao().find( compound );
    }

    @Override
    protected Compound handleFindOrCreate( Compound compound ) throws Exception {
        return this.getCompoundDao().findOrCreate( compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundService#remove(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.expression.biomaterial.Compound compound ) throws java.lang.Exception {
        this.getCompoundDao().remove( compound );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.CompoundService#update(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.expression.biomaterial.Compound compound ) throws java.lang.Exception {
        this.getCompoundDao().update( compound );
    }

}