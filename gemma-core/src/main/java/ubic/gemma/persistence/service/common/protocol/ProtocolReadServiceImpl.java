/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.protocol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.protocol.Protocol;

import java.util.List;

/**
 * Implementation of {@link ProtocolReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL
 * enforcement is the responsibility of the facade {@link ProtocolService}
 * interface -- this class is unsecured at the AOP boundary on purpose, so
 * intra-{@code gemma-core} callers can bypass duplicate ACL checks once
 * authenticated.
 *
 * @see ProtocolService
 */
@Service("protocolReadService")
public class ProtocolReadServiceImpl implements ProtocolReadService {

    private final ProtocolDao protocolDao;

    @Autowired
    public ProtocolReadServiceImpl( ProtocolDao protocolDao ) {
        this.protocolDao = protocolDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Protocol findByName( String protocolName ) {
        return protocolDao.findByName( protocolName );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Protocol> loadAllUniqueByName() {
        return protocolDao.loadAllUniqueByName();
    }
}
