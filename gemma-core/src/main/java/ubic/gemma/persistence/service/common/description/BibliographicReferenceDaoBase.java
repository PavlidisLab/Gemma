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
package ubic.gemma.persistence.service.common.description;

import org.hibernate.SessionFactory;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.Map;

/**
 * Base DAO Class: is able to create, update, remove, load, and find objects of type <code>BibliographicReference</code>
 * .
 *
 * @see BibliographicReference
 */
public abstract class BibliographicReferenceDaoBase
        extends VoEnabledDao<BibliographicReference, BibliographicReferenceValueObject>
        implements BibliographicReferenceDao {

    public BibliographicReferenceDaoBase( SessionFactory sessionFactory ) {
        super( BibliographicReference.class, sessionFactory );
    }

    @Override
    public BibliographicReference findByExternalId( final String id, final String databaseName ) {
        //noinspection unchecked
        return ( BibliographicReference ) this.getSessionFactory().getCurrentSession().createQuery(
                "from BibliographicReference b where b.pubAccession.accession=:id AND b.pubAccession.externalDatabase.name=:databaseName" )
                .setParameter( "id", id ).setParameter( "databaseName", databaseName ).uniqueResult();
    }

    @Override
    public BibliographicReference findByExternalId( final DatabaseEntry externalId ) {
        //noinspection unchecked
        return ( BibliographicReference ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from BibliographicReference b where b.pubAccession=:externalId" )
                .setParameter( "externalId", externalId ).uniqueResult();
    }

    /**
     * @see BibliographicReferenceDao#getAllExperimentLinkedReferences()
     */
    @Override
    public Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences() {
        return this.handleGetAllExperimentLinkedReferences();
    }

    /**
     * Performs the core logic for {@link #getAllExperimentLinkedReferences()}
     */
    protected abstract Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences();

}