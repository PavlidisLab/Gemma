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

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.architecture.SuppressArchUnit;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.persistence.service.association.Gene2GOAssociationDao;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Persist objects like Gene2GOAssociation.
 * <p>
 * Persister-shrink S2d: lifted out of the {@link ArrayDesignPersister} inheritance
 * chain into a concrete {@code @Component}. Genome and AD collaboration is via
 * {@code @Autowired} fields; the recursive {@code persistExpressionExperimentSet}
 * → EE-dispatch path uses an {@code @Lazy @Autowired PersisterHelperImpl dispatcher}
 * to break the Spring DI cycle (see {@code PERSISTER_SHRINK_S2_DETAIL.md} §5.3).
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
@Component("relationshipPersister")
public class RelationshipPersister {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private CommonPersister common;

    @Autowired
    private GenomePersister genome;

    @Autowired
    private ArrayDesignPersister arrayDesign;

    /**
     * Persister-shrink S2d: {@link #persistExpressionExperimentSet} recursively dispatches
     * its set members (transient {@link ExpressionExperiment}s) back to the EE arm on
     * {@link PersisterHelperImpl}. Spring would see {@code Relationship → PHI → Relationship}
     * as a DI cycle; {@code @Lazy} resolves with a proxy and breaks the eager loop.
     * In production no caller routes a raw {@code ExpressionExperimentSet} through
     * {@code persist} (see recce §2.2); this field exists for the test-fixture path
     * that does.
     *
     * <p>The {@code Impl} type is required: {@code PersisterHelperImpl} exposes
     * the protected {@code doPersist} dispatch method not present on any
     * narrower interface, and the {@code @Lazy} proxy is what lets Spring
     * tolerate the {@code Relationship ↔ PHI} DI cycle. The
     * {@link SuppressArchUnit} marker tells {@code AutowireImplRuleTest} this
     * exception is intentional.
     */
    @SuppressArchUnit("AutowireImpl")
    @Lazy
    @Autowired
    private PersisterHelperImpl dispatcher;

    @Autowired
    private Gene2GOAssociationDao gene2GoAssociationDao;

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Polymorphic dispatch entry point reached by {@link PersisterHelperImpl} via its
     * (still-extant) {@code extends RelationshipPersister}. Handles the
     * Gene2GOAssociation and ExpressionExperimentSet arms; everything else falls through
     * the AD/Genome/Common chain via {@code arrayDesign.doPersist}.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Map<String, ExternalDatabase> xdbCache ) {
        if ( entity instanceof Gene2GOAssociation ) {
            return ( T ) this.persistGene2GOAssociation( ( Gene2GOAssociation ) entity, xdbCache );
        } else if ( entity instanceof ExpressionExperimentSet ) {
            return ( T ) this.persistExpressionExperimentSet( ( ExpressionExperimentSet ) entity, xdbCache );
        }
        // Delegate to the AD layer (protected method on a same-package bean) which itself
        // falls through to Genome/Common. Keeps the S2c chain semantics intact post-S2d.
        return arrayDesign.doPersist( entity, xdbCache );
    }

    /**
     * Polymorphic persist-or-update dispatch entry point. Relationship has no update
     * arm of its own; delegate downward.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersistOrUpdate( T entity, Map<String, ExternalDatabase> xdbCache ) {
        return arrayDesign.doPersistOrUpdate( entity, xdbCache );
    }

    /**
     * Typed dispatch surface for the S2e dispatcher: returns {@code null} when the
     * entity is neither {@link Gene2GOAssociation} nor {@link ExpressionExperimentSet}.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Identifiable> T doRelationship( T entity, Map<String, ExternalDatabase> xdbCache ) {
        if ( entity instanceof Gene2GOAssociation ) {
            return ( T ) this.persistGene2GOAssociation( ( Gene2GOAssociation ) entity, xdbCache );
        } else if ( entity instanceof ExpressionExperimentSet ) {
            return ( T ) this.persistExpressionExperimentSet( ( ExpressionExperimentSet ) entity, xdbCache );
        }
        return null;
    }

    /**
     * Persist an {@link ExpressionExperimentSet}. Members with null id are recursively
     * dispatched through the EE arm on {@link PersisterHelperImpl} — reached via the
     * {@code @Lazy} dispatcher proxy so Spring tolerates the {@code Relationship ↔ PHI}
     * DI cycle. Public after S2d.
     */
    public ExpressionExperimentSet persistExpressionExperimentSet( ExpressionExperimentSet entity, Map<String, ExternalDatabase> xdbCache ) {
        // No static BusinessKey.find for ExpressionExperimentSet (the DAO-level find()
        // takes an ExpressionExperiment with different semantics — "sets containing this
        // EE"), so we keep the explicit member-persistence + create flow. Members
        // (ExpressionExperiments) are resolved by id through the dispatcher; their own
        // BK handling is upstream in PersisterHelperImpl's EE arm.
        Collection<ExpressionExperiment> setMembers = new HashSet<>();

        for ( ExpressionExperiment baSet : entity.getExperiments() ) {
            if ( baSet.getId() == null ) {
                // S2d: route through the @Lazy PHI dispatcher rather than this.doPersist
                // (Relationship's doPersist would loop back to itself for non-EE
                // entities, and the EE arm lives on PHI). The proxy breaks the
                // eager DI cycle.
                baSet = dispatcher.doPersist( baSet, xdbCache );
            }
            setMembers.add( baSet );
        }
        entity.getExperiments().clear();
        entity.getExperiments().addAll( setMembers );

        return expressionExperimentSetDao.create( entity );
    }

    /**
     * Persister-shrink S3 public entry point: persist a single
     * {@link Gene2GOAssociation}. Owns the {@link FlushMode#MANUAL} window
     * formerly carried by {@link PersisterHelperImpl#persist(ubic.gemma.model.common.Identifiable)}.
     */
    @Transactional
    public Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            Gene2GOAssociation result = this.persistGene2GOAssociation( association, new HashMap<>() );
            sessionFactory.getCurrentSession().flush();
            return result;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    /**
     * Persister-shrink S3 public entry point: persist a batch of
     * {@link Gene2GOAssociation}s. Equivalent to the {@code persist(Collection)}
     * arm formerly routed through {@link PersisterHelperImpl#persist(Collection)}
     * with each element dispatched through the Gene2GOAssociation arm.
     * Shares a single xdbCache across the batch for efficiency.
     */
    @Transactional
    public List<Gene2GOAssociation> persistGene2GOAssociations( Collection<Gene2GOAssociation> associations ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            Map<String, ExternalDatabase> xdbCache = new HashMap<>();
            List<Gene2GOAssociation> result = new ArrayList<>( associations.size() );
            for ( Gene2GOAssociation a : associations ) {
                result.add( this.persistGene2GOAssociation( a, xdbCache ) );
            }
            sessionFactory.getCurrentSession().flush();
            return result;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    /**
     * Public after S2d so the S2e dispatcher can reach it.
     */
    public Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association, Map<String, ExternalDatabase> xdbCache ) {
        // Gene first — Gene2GOAssociation BK matches on gene + ontologyEntry, so the
        // gene side must be resolved to a managed instance before the lookup.
        // Phase 3 lift: taxonCache and chromosomeCache are per-call; allocate fresh
        // maps here since the Gene2GO path persists one gene at a time and there's
        // no shared cache to thread.
        Map<Object, Taxon> taxonCache = new HashMap<>();
        Map<Integer, Chromosome> chromosomeCache = new HashMap<>();
        // S2c lead-in: routed through @Autowired GenomePersister.
        association.setGene( genome.persistGene( association.getGene(), xdbCache, taxonCache, chromosomeCache ) );
        Session session = getSessionFactory().getCurrentSession();
        Gene2GOAssociation existing = BusinessKey.find( session, association );
        return existing != null ? existing : gene2GoAssociationDao.create( association );
    }

}
