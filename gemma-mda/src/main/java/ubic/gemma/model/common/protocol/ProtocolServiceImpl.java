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
package ubic.gemma.model.common.protocol;

import java.util.Collection;

import org.springframework.stereotype.Service;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.protocol.ProtocolService
 */
@Service
public class ProtocolServiceImpl extends ubic.gemma.model.common.protocol.ProtocolServiceBase {

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#find(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    protected ubic.gemma.model.common.protocol.Protocol handleFind( ubic.gemma.model.common.protocol.Protocol protocol )
            throws java.lang.Exception {
        return this.getProtocolDao().find( protocol );
    }

    @Override
    protected Protocol handleFindOrCreate( Protocol protocol ) throws Exception {
        return this.getProtocolDao().findOrCreate( protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#remove(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.common.protocol.Protocol protocol ) throws java.lang.Exception {
        this.getProtocolDao().remove( protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#update(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.protocol.Protocol protocol ) throws java.lang.Exception {
        this.getProtocolDao().update( protocol );
    }

    public Collection<Protocol> loadAll() {
        return ( Collection<Protocol> ) this.getProtocolDao().loadAll();
    }

}