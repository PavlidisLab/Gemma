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

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.TfGeneAssociation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;

/**
 * @author paul
 */
@Service
public class TfGeneAssociationServiceImpl extends AbstractService<TfGeneAssociation>
        implements TfGeneAssociationService {

    private final TfGeneAssociationDao tfGeneAssociationDao;

    @Autowired
    public TfGeneAssociationServiceImpl( TfGeneAssociationDao tfGeneAssociationDao ) {
        super(tfGeneAssociationDao);
        this.tfGeneAssociationDao = tfGeneAssociationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TfGeneAssociation> findByTargetGene( Gene gene ) {
        return tfGeneAssociationDao.findByTargetGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TfGeneAssociation> findByTf( Gene tf ) {
        return tfGeneAssociationDao.findByTf( tf );
    }

    @Override
    @Transactional
    public void removeAll() {
        tfGeneAssociationDao.removeAll();
    }

}
