/*
 * The Gemma project
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
package ubic.gemma.persistence.persister;

import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.persistence.service.association.Gene2GOAssociationDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Persist objects like Gene2GOAssociation.
 * <p>
 * Phase-2 note: the gene-gene coexpression subsystem was removed, so {@code CoexpressionAnalysis}
 * handling is gone too.
 * <p>
 * Phase 3 persister retirement: methods here are being rewired to delegate to
 * {@link BusinessKey#find(Session, Object)} (where a static resolver exists) followed by a
 * direct {@code dao.create()} on miss, so the whole persister can eventually be deleted in
 * favour of either the DAO {@code findOrCreate} call or a JPA cascade declared in the
 * parent's HBM mapping.
 *
 * @author pavlidis
 */
public abstract class RelationshipPersister extends ExpressionPersister {

    @Autowired
    private Gene2GOAssociationDao gene2GoAssociationDao;

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Caches caches, Map<String, ExternalDatabase> xdbCache ) {
        if ( entity instanceof Gene2GOAssociation ) {
            return ( T ) this.persistGene2GOAssociation( ( Gene2GOAssociation ) entity, caches, xdbCache );
        } else if ( entity instanceof ExpressionExperimentSet ) {
            return ( T ) this.persistExpressionExperimentSet( ( ExpressionExperimentSet ) entity, caches, xdbCache );
        } else {
            return super.doPersist( entity, caches, xdbCache );
        }
    }

    private ExpressionExperimentSet persistExpressionExperimentSet( ExpressionExperimentSet entity, Caches caches, Map<String, ExternalDatabase> xdbCache ) {
        // No static BusinessKey.find for ExpressionExperimentSet (the DAO-level find()
        // takes an ExpressionExperiment with different semantics — "sets containing this
        // EE"), so we keep the explicit member-persistence + create flow. Members
        // (ExpressionExperiments) are resolved by id through doPersist; their own BK
        // handling is upstream in ExpressionPersister.
        Collection<ExpressionExperiment> setMembers = new HashSet<>();

        for ( ExpressionExperiment baSet : entity.getExperiments() ) {
            if ( baSet.getId() == null ) {
                baSet = this.doPersist( baSet, caches, xdbCache );
            }
            setMembers.add( baSet );
        }
        entity.getExperiments().clear();
        entity.getExperiments().addAll( setMembers );

        return expressionExperimentSetDao.create( entity );
    }

    private Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association, Caches caches, Map<String, ExternalDatabase> xdbCache ) {
        // Gene first — Gene2GOAssociation BK matches on gene + ontologyEntry, so the
        // gene side must be resolved to a managed instance before the lookup.
        association.setGene( this.persistGene( association.getGene(), caches, xdbCache ) );
        Session session = getSessionFactory().getCurrentSession();
        Gene2GOAssociation existing = BusinessKey.find( session, association );
        return existing != null ? existing : gene2GoAssociationDao.create( association );
    }

}
