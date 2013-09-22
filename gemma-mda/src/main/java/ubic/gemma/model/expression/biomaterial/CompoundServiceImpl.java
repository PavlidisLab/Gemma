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
 * @see CompoundService
 */
@Service
public class CompoundServiceImpl extends CompoundServiceBase {

    /**
     * @see CompoundService#createFromValueObject(Compound)
     */
    protected Compound handleCreate( Compound compound ) {
        return this.getCompoundDao().create( compound );
    }

    /**
     * @see CompoundService#find(Compound)
     */
    @Override
    protected Compound handleFind( Compound compound ) {
        return this.getCompoundDao().find( compound );
    }

    @Override
    protected Compound handleFindOrCreate( Compound compound ) {
        return this.getCompoundDao().findOrCreate( compound );
    }

    /**
     * @see CompoundService#remove(Compound)
     */
    @Override
    protected void handleRemove( Compound compound ) {
        this.getCompoundDao().remove( compound );
    }

    /**
     * @see CompoundService#update(Compound)
     */
    @Override
    protected void handleUpdate( Compound compound ) {
        this.getCompoundDao().update( compound );
    }

}