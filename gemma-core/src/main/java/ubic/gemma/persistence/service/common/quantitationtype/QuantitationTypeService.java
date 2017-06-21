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
package ubic.gemma.persistence.service.common.quantitationtype;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;
import java.util.List;

/**
 * @author kelsey
 */
@Service
public interface QuantitationTypeService extends BaseVoEnabledService<QuantitationType, QuantitationTypeValueObject> {

    @Secured({ "GROUP_USER" })
    QuantitationType create( QuantitationType quantitationType );

    @Secured({ "GROUP_USER" })
    QuantitationType findOrCreate( QuantitationType quantitationType );

    @Secured({ "GROUP_USER" })
    void remove( QuantitationType quantitationType );

    @Secured({ "GROUP_USER" })
    void update( QuantitationType quantitationType );

    @Secured({ "GROUP_USER" })
    Collection<QuantitationType> create( Collection<QuantitationType> entities );

    @Secured({ "GROUP_USER" })
    void remove( Collection<QuantitationType> entities );

    @Secured({ "GROUP_USER" })
    void remove( Long id );

    @Secured({ "GROUP_USER" })
    void update( Collection<QuantitationType> entities );

    @Secured({ "GROUP_USER" })
    List<QuantitationType> loadByDescription( String description );

}
