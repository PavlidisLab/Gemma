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
package ubic.gemma.model.association;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
public interface TfGeneAssociationService {

    @Secured( { "GROUP_ADMIN" })
    public Collection<? extends TfGeneAssociation> create( Collection<? extends TfGeneAssociation> entities );

    @Secured( { "GROUP_ADMIN" })
    public TfGeneAssociation create( TfGeneAssociation entity );

    public Collection<? extends TfGeneAssociation> findByTargetGene( Gene gene );

    public Collection<? extends TfGeneAssociation> findByTf( Gene tf );

    public Collection<? extends TfGeneAssociation> load( Collection<Long> ids );

    public TfGeneAssociation load( Long id );

    public Collection<? extends TfGeneAssociation> loadAll();

    @Secured( { "GROUP_ADMIN" })
    public void remove( Collection<? extends TfGeneAssociation> entities );

    @Secured( { "GROUP_ADMIN" })
    public void remove( Long id );

    @Secured( { "GROUP_ADMIN" })
    public void remove( TfGeneAssociation entity );

    @Secured( { "GROUP_ADMIN" })
    public void removeAll();

    @Secured( { "GROUP_ADMIN" })
    public void update( Collection<? extends TfGeneAssociation> entities );

    @Secured( { "GROUP_ADMIN" })
    public void update( TfGeneAssociation entity );

}
