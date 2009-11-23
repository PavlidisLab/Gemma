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

import org.springframework.stereotype.Service;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.protocol.SoftwareService
 */
@Service
public class SoftwareServiceImpl extends ubic.gemma.model.common.protocol.SoftwareServiceBase {

    /**
     * @see ubic.gemma.model.common.protocol.SoftwareService#find(ubic.gemma.model.common.protocol.Software)
     */
    @Override
    protected ubic.gemma.model.common.protocol.Software handleFind( ubic.gemma.model.common.protocol.Software software )
            throws java.lang.Exception {
        return this.getSoftwareDao().find( software );
    }

    @Override
    protected Software handleFindOrCreate( Software software ) throws Exception {
        return this.getSoftwareDao().findOrCreate( software );
    }

    /**
     * @see ubic.gemma.model.common.protocol.SoftwareService#remove(ubic.gemma.model.common.protocol.Software)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.common.protocol.Software software ) throws java.lang.Exception {
        this.getSoftwareDao().remove( software );
    }

    /**
     * @see ubic.gemma.model.common.protocol.SoftwareService#update(ubic.gemma.model.common.protocol.Software)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.protocol.Software software ) throws java.lang.Exception {
        this.getSoftwareDao().update( software );
    }

}