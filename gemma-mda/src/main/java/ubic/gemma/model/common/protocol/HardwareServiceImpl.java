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

/**
 * <hr>
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.protocol.HardwareService
 */
public class HardwareServiceImpl extends ubic.gemma.model.common.protocol.HardwareServiceBase {

    /**
     * @see ubic.gemma.model.common.protocol.HardwareService#find(ubic.gemma.model.common.protocol.Hardware)
     */
    @Override
    protected ubic.gemma.model.common.protocol.Hardware handleFind( ubic.gemma.model.common.protocol.Hardware hardware )
            throws java.lang.Exception {
        return this.getHardwareDao().find( hardware );
    }

    /**
     * @see ubic.gemma.model.common.protocol.HardwareService#update(ubic.gemma.model.common.protocol.Hardware)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.protocol.Hardware hardware ) throws java.lang.Exception {
        this.getHardwareDao().update( hardware );
    }

    /**
     * @see ubic.gemma.model.common.protocol.HardwareService#remove(ubic.gemma.model.common.protocol.Hardware)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.common.protocol.Hardware hardware ) throws java.lang.Exception {
        this.getHardwareDao().remove( hardware );
    }

    @Override
    protected Hardware handleFindOrCreate( Hardware hardware ) throws Exception {
        return this.getHardwareDao().findOrCreate( hardware );
    }

}