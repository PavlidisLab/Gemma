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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.protocol.ProtocolService
 */
@Service
public class ProtocolServiceImpl implements ProtocolService {

    @Autowired
    private ProtocolDao protocolDao;

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<Protocol> loadAll() {
        return ( Collection<Protocol> ) this.protocolDao.loadAll();
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#find(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    @Transactional(readOnly = true)
    public Protocol find( final Protocol protocol ) {
        try {
            return this.protocolDao.find( protocol );
        } catch ( Throwable th ) {
            throw new ProtocolServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.ProtocolService.find(ubic.gemma.model.common.protocol.Protocol protocol)' --> "
                            + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#findOrCreate(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    @Transactional
    public Protocol findOrCreate( final Protocol protocol ) {
        try {
            return this.protocolDao.findOrCreate( protocol );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.ProtocolServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.ProtocolService.findOrCreate(ubic.gemma.model.common.protocol.Protocol protocol)' --> "
                            + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#remove(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    @Transactional
    public void remove( final ubic.gemma.model.common.protocol.Protocol protocol ) {
        try {
            this.protocolDao.remove( protocol );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.ProtocolServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.ProtocolService.remove(ubic.gemma.model.common.protocol.Protocol protocol)' --> "
                            + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolService#update(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    @Transactional
    public void update( final ubic.gemma.model.common.protocol.Protocol protocol ) {
        try {
            this.protocolDao.update( protocol );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.protocol.ProtocolServiceException(
                    "Error performing 'ubic.gemma.model.common.protocol.ProtocolService.update(ubic.gemma.model.common.protocol.Protocol protocol)' --> "
                            + th,
                    th );
        }
    }

}