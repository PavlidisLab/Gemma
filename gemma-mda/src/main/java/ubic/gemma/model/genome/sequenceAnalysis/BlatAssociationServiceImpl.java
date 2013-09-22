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
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @version $Id$
 * @see BlatAssociationService
 */
@Service
public class BlatAssociationServiceImpl extends BlatAssociationServiceBase {

    /**
     * @see BlatAssociationService#create(BlatAssociation)
     */
    @Override
    protected BlatAssociation handleCreate( BlatAssociation blatAssociation ) {
        return this.getBlatAssociationDao().create( blatAssociation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see BlatAssociationServiceBase#handleFind(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection<BlatAssociation> handleFind( Gene gene ) {
        return this.getBlatAssociationDao().find( gene );
    }

    /**
     * @see BlatAssociationService#find(BioSequence)
     */
    @Override
    protected Collection<BlatAssociation> handleFind( BioSequence bioSequence ) {
        return this.getBlatAssociationDao().find( bioSequence );
    }

    @Override
    protected void handleThaw( BlatAssociation blatAssociation ) {
        this.getBlatAssociationDao().thaw( blatAssociation );
    }

    @Override
    protected void handleThaw( Collection<BlatAssociation> blatAssociations ) {
        this.getBlatAssociationDao().thaw( blatAssociations );
    }

    @Override
    protected void handleUpdate( BlatAssociation blatAssociation ) {
        this.getBlatAssociationDao().update( blatAssociation );
    }

    @Override
    public void remove( BlatAssociation blatAssociation ) {
        this.getBlatAssociationDao().remove( blatAssociation );
    }

}