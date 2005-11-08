/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.common.protocol;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.common.protocol.SoftwareService
 */
public class SoftwareServiceImpl extends edu.columbia.gemma.common.protocol.SoftwareServiceBase {

    /**
     * @see edu.columbia.gemma.common.protocol.SoftwareService#find(edu.columbia.gemma.common.protocol.Software)
     */
    protected edu.columbia.gemma.common.protocol.Software handleFind(
            edu.columbia.gemma.common.protocol.Software software ) throws java.lang.Exception {
        // @todo implement protected edu.columbia.gemma.common.protocol.Software
        // handleFind(edu.columbia.gemma.common.protocol.Software software)
        return null;
    }

    /**
     * @see edu.columbia.gemma.common.protocol.SoftwareService#update(edu.columbia.gemma.common.protocol.Software)
     */
    protected void handleUpdate( edu.columbia.gemma.common.protocol.Software software ) throws java.lang.Exception {
        // @todo implement protected void handleUpdate(edu.columbia.gemma.common.protocol.Software software)
        throw new java.lang.UnsupportedOperationException(
                "edu.columbia.gemma.common.protocol.SoftwareService.handleUpdate(edu.columbia.gemma.common.protocol.Software software) Not implemented!" );
    }

    /**
     * @see edu.columbia.gemma.common.protocol.SoftwareService#remove(edu.columbia.gemma.common.protocol.Software)
     */
    protected void handleRemove( edu.columbia.gemma.common.protocol.Software software ) throws java.lang.Exception {
        // @todo implement protected void handleRemove(edu.columbia.gemma.common.protocol.Software software)
        throw new java.lang.UnsupportedOperationException(
                "edu.columbia.gemma.common.protocol.SoftwareService.handleRemove(edu.columbia.gemma.common.protocol.Software software) Not implemented!" );
    }

    @Override
    protected Software handleFindOrCreate( Software software ) throws Exception {
        return this.getSoftwareDao().findOrCreate( software );
    }

}