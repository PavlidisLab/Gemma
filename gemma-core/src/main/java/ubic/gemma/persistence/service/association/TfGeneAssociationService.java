/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.association;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.association.TfGeneAssociation;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;

/**
 * @author paul
 */
public interface TfGeneAssociationService extends BaseService<TfGeneAssociation> {

    @Override
    @Secured({ "GROUP_ADMIN" })
    TfGeneAssociation create( TfGeneAssociation entity );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Collection<TfGeneAssociation> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( TfGeneAssociation entity );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void update( Collection<TfGeneAssociation> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void update( TfGeneAssociation entity );

    @Secured({ "GROUP_ADMIN" })
    void removeAll();
}
