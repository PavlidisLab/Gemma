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
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PostAuthorize;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.common.auditAndSecurity.SecurableBaseImmutableService;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author kelsey
 */
public interface ProtocolService extends SecurableBaseImmutableService<Protocol> {

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    @PostAuthorize("returnObject == null or hasPermission(returnObject, 'READ') or hasPermission(returnObject, 'ADMINISTRATION')")
    Protocol findByName( String protocolName );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    @PostFilter("hasPermission(filterObject, 'READ') or hasPermission(filterObject, 'ADMINISTRATION')")
    List<Protocol> loadAllUniqueByName();
}
