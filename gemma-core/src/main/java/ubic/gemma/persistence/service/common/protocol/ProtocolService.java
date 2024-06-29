/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.common.protocol;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.BaseImmutableService;

import java.util.Collection;

/**
 * @author kelsey
 */
public interface ProtocolService extends BaseImmutableService<Protocol> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    Protocol find( Protocol protocol );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    Protocol findOrFail( Protocol protocol );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    Protocol findOrCreate( Protocol protocol );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<Protocol> loadAll();

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( Protocol protocol );
}
