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

package edu.columbia.gemma.common.protocol;

/**
 * 
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.common.protocol.ProtocolService
 */
public class ProtocolServiceImpl
    extends edu.columbia.gemma.common.protocol.ProtocolServiceBase
{

    /**
     * @see edu.columbia.gemma.common.protocol.ProtocolService#find(edu.columbia.gemma.common.protocol.Protocol)
     */
    protected edu.columbia.gemma.common.protocol.Protocol handleFind(edu.columbia.gemma.common.protocol.Protocol protocol)
        throws java.lang.Exception
    {
        //@todo implement protected edu.columbia.gemma.common.protocol.Protocol handleFind(edu.columbia.gemma.common.protocol.Protocol protocol)
        return null;
    }

    /**
     * @see edu.columbia.gemma.common.protocol.ProtocolService#update(edu.columbia.gemma.common.protocol.Protocol)
     */
    protected void handleUpdate(edu.columbia.gemma.common.protocol.Protocol protocol)
        throws java.lang.Exception
    {
        //@todo implement protected void handleUpdate(edu.columbia.gemma.common.protocol.Protocol protocol)
        throw new java.lang.UnsupportedOperationException("edu.columbia.gemma.common.protocol.ProtocolService.handleUpdate(edu.columbia.gemma.common.protocol.Protocol protocol) Not implemented!");
    }

    /**
     * @see edu.columbia.gemma.common.protocol.ProtocolService#remove(edu.columbia.gemma.common.protocol.Protocol)
     */
    protected void handleRemove(edu.columbia.gemma.common.protocol.Protocol protocol)
        throws java.lang.Exception
    {
        this.getProtocolDao().remove(protocol);
    }

    @Override
    protected Protocol handleFindOrCreate( Protocol protocol ) throws Exception {
        return this.getProtocolDao().findOrCreate( protocol );
    }

}