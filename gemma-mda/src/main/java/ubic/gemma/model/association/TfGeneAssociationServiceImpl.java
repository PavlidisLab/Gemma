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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class TfGeneAssociationServiceImpl implements TfGeneAssociationService {

    @Autowired
    private TfGeneAssociationDao tfGeneAssociationDao;

    @Override
    public Collection<? extends TfGeneAssociation> create( Collection<? extends TfGeneAssociation> entities ) {
        return tfGeneAssociationDao.create( entities );
    }

    @Override
    public TfGeneAssociation create( TfGeneAssociation entity ) {
        return tfGeneAssociationDao.create( entity );
    }

    @Override
    public Collection<? extends TfGeneAssociation> findByTargetGene( Gene gene ) {
        return tfGeneAssociationDao.findByTargetGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends TfGeneAssociation> findByTf( Gene tf ) {
        return tfGeneAssociationDao.findByTf( tf );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends TfGeneAssociation> load( Collection<Long> ids ) {
        return tfGeneAssociationDao.load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public TfGeneAssociation load( Long id ) {
        return tfGeneAssociationDao.load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends TfGeneAssociation> loadAll() {
        return tfGeneAssociationDao.loadAll();
    }

    @Override
    @Transactional
    public void remove( Collection<? extends TfGeneAssociation> entities ) {
        tfGeneAssociationDao.remove( entities );
    }

    @Override
    @Transactional
    public void remove( Long id ) {
        tfGeneAssociationDao.remove( id );
    }

    @Override
    @Transactional
    public void remove( TfGeneAssociation entity ) {
        tfGeneAssociationDao.remove( entity );
    }

    @Override
    @Transactional
    public void removeAll() {
        tfGeneAssociationDao.removeAll();
    }

}
