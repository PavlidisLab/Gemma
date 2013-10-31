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
package ubic.gemma.model.common.description;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.BrowsingDao;

/**
 * @see BibliographicReference
 */
public interface BibliographicReferenceDao extends BrowsingDao<BibliographicReference> {

    /**
     * 
     */
    public BibliographicReference find( BibliographicReference bibliographicReference );

    /**
     * 
     */
    public BibliographicReference findByExternalId( java.lang.String id, java.lang.String databaseName );

    /**
     * <p>
     * Find by the external database id, such as for PubMed
     * </p>
     */
    public BibliographicReference findByExternalId( DatabaseEntry externalId );

    /**
     * 
     */
    public BibliographicReference findOrCreate( BibliographicReference bibliographicReference );

    /**
     * 
     */
    public java.util.Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences();

    /**
     * 
     */
    public java.util.Collection<ExpressionExperiment> getRelatedExperiments(
            BibliographicReference bibliographicReference );

    @Override
    public Collection<BibliographicReference> load( Collection<Long> ids );

    /**
     * @param bibliographicReference
     * @return
     */
    public BibliographicReference thaw( BibliographicReference bibliographicReference );

    /**
     * @param bibliographicReferences
     * @return
     */
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences );

    /**
     * @param records
     * @return
     */
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records );

    /**
     * @return
     */
    public Collection<Long> listAll();

}
