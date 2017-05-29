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
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.hibernate.SessionFactory;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>BlatAssociation</code>.
 * </p>
 *
 * @see BlatAssociation
 */
public abstract class BlatAssociationDaoBase extends AbstractDao<BlatAssociation> implements BlatAssociationDao {

    public BlatAssociationDaoBase( SessionFactory sessionFactory ) {
        super( BlatAssociation.class, sessionFactory );
    }

    /**
     * @see BlatAssociationDao#thaw(Collection)
     */
    @Override
    public void thaw( final Collection<BlatAssociation> blatAssociations ) {
        try {
            this.handleThaw( blatAssociations );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'BlatAssociationDao.thaw(Collection blatAssociations)' --> " + th, th );
        }
    }

    /**
     * @see BlatAssociationDao#thaw(BlatAssociation)
     */
    @Override
    public void thaw( final BlatAssociation blatAssociation ) {
        this.handleThaw( blatAssociation );
    }

    /**
     * Performs the core logic for {@link #thaw(Collection)}
     */
    protected abstract void handleThaw( Collection<BlatAssociation> blatAssociations );

    /**
     * Performs the core logic for {@link #thaw(BlatAssociation)}
     */
    protected abstract void handleThaw( BlatAssociation blatAssociation );

}