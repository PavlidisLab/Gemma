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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles persisting array designs.
 * <p>
 * Persister-shrink S2c: lifted out of the {@link GenomePersister} inheritance chain
 * into a concrete {@code @Component}. Genome and Common collaboration is via
 * {@code @Autowired} fields ({@link #genome}, {@link #common}); the protected
 * {@code doPersist} entry point remains so the still-extant {@code RelationshipPersister}
 * chain keeps compiling (delegates to {@link #persistArrayDesign} for the AD arm,
 * falls through to {@code genome.doGenome} / {@code common.doCommon}, then throws).
 * <p>
 * Phase 3 persister retirement: rewired to {@link BusinessKey#find(Session, ArrayDesign)}
 * for the root-level "find existing" lookup, with the {@code compositeSequences} and
 * {@code externalReferences} collections flowing through Hibernate's {@code cascade="all"}
 * on {@link ArrayDesign#getCompositeSequences()} and {@link ArrayDesign#getExternalReferences()}
 * (see {@code ArrayDesign.hbm.xml}). The remaining responsibilities here are:
 * <ul>
 *   <li>Pre-resolve {@link BioSequence} per composite sequence — BioSequences have their own
 *       lifecycle (shared across array designs) and are <strong>not</strong> cascaded from
 *       CompositeSequence; they must be looked up / created before {@code dao.create}.</li>
 *   <li>Pre-resolve the design provider, primary taxon, and the external database for each
 *       external reference — these are {@code many-to-one} associations without cascade.</li>
 * </ul>
 * <p>
 * The {@link ArrayDesignsForExperimentCache} contract is unaffected: that cache is populated
 * explicitly by {@code ExpressionExperimentPrePersistServiceImpl} and {@code GeoServiceImpl},
 * never as a side effect of this persister.
 *
 * @author pavlidis
 */
@Component("arrayDesignPersister")
public class ArrayDesignPersister {

    /**
     * Local copy of {@code REPORT_BATCH_SIZE} so {@code persistNewArrayDesign}'s
     * progress-log loop keeps compiling now that AD no longer inherits the constant
     * from {@link GenomePersister}.
     */
    private static final int REPORT_BATCH_SIZE = 100;

    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private CommonPersister common;
    @Autowired
    private GenomePersister genome;
    @Autowired
    private ArrayDesignDao arrayDesignDao;
    @Autowired
    private ContactDao contactDao;

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Polymorphic dispatch entry point reached by {@code RelationshipPersister} via the
     * still-extant inheritance chain. Handles the AD arm itself; everything else falls
     * through to {@link GenomePersister#doGenome} then {@link CommonPersister#doCommon},
     * else throws.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Map<String, ExternalDatabase> xdbCache ) {
        if ( entity instanceof ArrayDesign ) {
            return ( T ) this.persistArrayDesign( ( ArrayDesign ) entity, xdbCache, new HashMap<>(), new HashMap<>() );
        }
        T genomeHandled = genome.doGenome( entity, xdbCache );
        if ( genomeHandled != null || entity instanceof Taxon ) {
            return genomeHandled;
        }
        T commonHandled = ( T ) common.doCommon( entity, xdbCache );
        if ( commonHandled != null || entity instanceof ubic.gemma.model.common.description.Characteristic
                || entity instanceof ubic.gemma.model.common.auditAndSecurity.User ) {
            return commonHandled;
        }
        throw new UnsupportedOperationException( String.format( "Don't know how to persist a %s.", entity.getClass().getSimpleName() ) );
    }

    /**
     * Polymorphic persist-or-update dispatch entry point reached by the chain. AD does
     * not own any update arm of its own; everything routes through Genome.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersistOrUpdate( T entity, Map<String, ExternalDatabase> xdbCache ) {
        T genomeHandled = genome.doGenomeUpdate( entity, xdbCache );
        if ( genomeHandled != null ) {
            return genomeHandled;
        }
        throw new UnsupportedOperationException( String.format( "Don't know how to persist or update a %s.", entity.getClass().getSimpleName() ) );
    }

    /**
     * Persister-shrink S2c typed dispatch: returns {@code null} if the entity is not
     * an {@link ArrayDesign}. Used by the S2e dispatcher.
     */
    @Nullable
    public ArrayDesign doArrayDesign( Object entity, Map<String, ExternalDatabase> xdbCache ) {
        if ( entity instanceof ArrayDesign ) {
            return persistArrayDesign( ( ArrayDesign ) entity, xdbCache, new HashMap<>(), new HashMap<>() );
        }
        return null;
    }

    /**
     * Persister-shrink S3 public entry point: find an existing ArrayDesign by business
     * key, else create. Owns the {@link FlushMode#MANUAL} window formerly carried by
     * {@link PersisterHelperImpl#persist(ubic.gemma.model.common.Identifiable)}; caches
     * are fresh per call (matches prior PHI semantics).
     */
    @Transactional
    public ArrayDesign persistArrayDesign( ArrayDesign arrayDesign ) {
        try {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.MANUAL );
            ArrayDesign result = this.persistArrayDesign( arrayDesign, new HashMap<>(), new HashMap<>(), new HashMap<>() );
            sessionFactory.getCurrentSession().flush();
            return result;
        } finally {
            sessionFactory.getCurrentSession().setHibernateFlushMode( FlushMode.AUTO );
        }
    }

    /**
     * Look up an existing ArrayDesign by business key (shortName, alternate names, or name —
     * see {@link BusinessKey#matches}); otherwise create a new one along with its full graph.
     * Public after S2c so the S2e dispatcher (and future direct callers) can reach it.
     */
    public ArrayDesign persistArrayDesign( ArrayDesign arrayDesign, Map<String, ExternalDatabase> xdbCache, Map<Object, Taxon> taxonCache, Map<Integer, Chromosome> chromosomeCache ) {
        if ( arrayDesign.getId() != null ) {
            CommonPersister.log.debug( "Platform " + arrayDesign + " already exists, returning..." );
            return arrayDesign;
        }

        Session session = getSessionFactory().getCurrentSession();
        ArrayDesign existing = BusinessKey.find( session, arrayDesign );
        if ( existing != null ) {
            CommonPersister.log.info( String.format( "Platform exactly matching %s doesn't exist, but found %s; returning",
                    arrayDesign, existing ) );
            return existing;
        }

        CommonPersister.log.debug( arrayDesign + " is new, processing..." );
        return this.persistNewArrayDesign( arrayDesign, xdbCache, taxonCache, chromosomeCache );
    }

    /**
     * Persist an entirely new array design, including composite sequences and any associated
     * new sequences. CompositeSequences and externalReferences flow through Hibernate cascade
     * ({@code cascade="all"} declared in ArrayDesign.hbm.xml); only their non-cascaded
     * many-to-one associations (BioSequence, ExternalDatabase) and the AD's own owning
     * associations (designProvider, primaryTaxon) need explicit BK resolution here.
     */
    private ArrayDesign persistNewArrayDesign( ArrayDesign arrayDesign, Map<String, ExternalDatabase> xdbCache, Map<Object, Taxon> taxonCache, Map<Integer, Chromosome> chromosomeCache ) {
        CommonPersister.log.debug( "Persisting new platform " + arrayDesign.getName() );

        if ( arrayDesign.getDesignProvider() != null ) {
            // BK lookup covers Person (Person extends Contact) — see BusinessKey.find(Session, Contact).
            Contact designProvider = arrayDesign.getDesignProvider();
            Contact existing = contactDao.find( designProvider );
            arrayDesign.setDesignProvider( existing != null ? existing : contactDao.create( designProvider ) );
        }

        if ( arrayDesign.getPrimaryTaxon() == null ) {
            throw new IllegalArgumentException( "Primary taxon cannot be null" );
        }
        // Phase 3 lift: was doPersist (instanceof Taxon arm); now a direct call to persistTaxon
        // so the threaded taxonCache stays alive across the AD graph.
        // S2b lead-in: routed through @Autowired GenomePersister rather than this.persistTaxon
        // (which used to inherit through CommonPersister).
        arrayDesign.setPrimaryTaxon( genome.persistTaxon( arrayDesign.getPrimaryTaxon(), taxonCache ) );

        for ( DatabaseEntry externalRef : arrayDesign.getExternalReferences() ) {
            // Phase 3 lift: helper takes the per-call Map<String, ExternalDatabase>
            // threaded through this persist (formerly carried on Caches).
            // S2b lead-in: routed through @Autowired CommonPersister.
            externalRef.setExternalDatabase( common.persistExternalDatabase( externalRef.getExternalDatabase(), xdbCache ) );
        }

        // Resolve BioSequence for each CompositeSequence before saving the AD.
        // BioSequence has its own lifecycle (shared across array designs) and is NOT
        // cascaded from CompositeSequence; CompositeSequence itself IS cascaded from
        // ArrayDesign (cascade="all" on ArrayDesign.compositeSequences in HBM), so a
        // single dao.create(ad) persists the whole probe set in one go.
        int numElements = arrayDesign.getCompositeSequences().size();
        int examined = 0;
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {
            compositeSequence.setArrayDesign( arrayDesign );
            BioSequence biologicalCharacteristic = compositeSequence.getBiologicalCharacteristic();
            if ( biologicalCharacteristic != null ) {
                // S2b lead-in: routed through @Autowired GenomePersister.
                compositeSequence.setBiologicalCharacteristic( genome.persistBioSequence( biologicalCharacteristic, xdbCache, taxonCache, chromosomeCache ) );
            }
            if ( ++examined % REPORT_BATCH_SIZE == 0 ) {
                CommonPersister.log.info( examined + "/" + numElements
                        + " compositeSequence sequences examined for " + arrayDesign );
            }
        }
        if ( examined > 0 ) {
            CommonPersister.log.info( "Total of " + examined
                    + " compositeSequence sequences examined for " + arrayDesign );
        }

        CommonPersister.log.debug( "Persisting " + arrayDesign );
        return arrayDesignDao.create( arrayDesign );
    }

}
