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
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.util.BusinessKey;

/**
 * Handles persisting array designs.
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
public abstract class ArrayDesignPersister extends GenomePersister {

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Autowired
    private ContactDao contactDao;

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Caches caches ) {
        if ( entity instanceof ArrayDesign ) {
            return ( T ) this.findOrPersistArrayDesign( ( ArrayDesign ) entity, caches );
        } else {
            return super.doPersist( entity, caches );
        }
    }

    /**
     * Look up an existing ArrayDesign by business key (shortName, alternate names, or name —
     * see {@link BusinessKey#matches}); otherwise create a new one along with its full graph.
     */
    private ArrayDesign findOrPersistArrayDesign( ArrayDesign arrayDesign, Caches caches ) {
        if ( arrayDesign.getId() != null ) {
            AbstractPersister.log.debug( "Platform " + arrayDesign + " already exists, returning..." );
            return arrayDesign;
        }

        Session session = getSessionFactory().getCurrentSession();
        ArrayDesign existing = BusinessKey.find( session, arrayDesign );
        if ( existing != null ) {
            AbstractPersister.log.info( String.format( "Platform exactly matching %s doesn't exist, but found %s; returning",
                    arrayDesign, existing ) );
            return existing;
        }

        AbstractPersister.log.debug( arrayDesign + " is new, processing..." );
        return this.persistNewArrayDesign( arrayDesign, caches );
    }

    /**
     * Persist an entirely new array design, including composite sequences and any associated
     * new sequences. CompositeSequences and externalReferences flow through Hibernate cascade
     * ({@code cascade="all"} declared in ArrayDesign.hbm.xml); only their non-cascaded
     * many-to-one associations (BioSequence, ExternalDatabase) and the AD's own owning
     * associations (designProvider, primaryTaxon) need explicit BK resolution here.
     */
    private ArrayDesign persistNewArrayDesign( ArrayDesign arrayDesign, Caches caches ) {
        AbstractPersister.log.debug( "Persisting new platform " + arrayDesign.getName() );

        if ( arrayDesign.getDesignProvider() != null ) {
            // BK lookup covers Person (Person extends Contact) — see BusinessKey.find(Session, Contact).
            Contact designProvider = arrayDesign.getDesignProvider();
            Contact existing = contactDao.find( designProvider );
            arrayDesign.setDesignProvider( existing != null ? existing : contactDao.create( designProvider ) );
        }

        if ( arrayDesign.getPrimaryTaxon() == null ) {
            throw new IllegalArgumentException( "Primary taxon cannot be null" );
        }
        arrayDesign.setPrimaryTaxon( this.doPersist( arrayDesign.getPrimaryTaxon(), caches ) );

        for ( DatabaseEntry externalRef : arrayDesign.getExternalReferences() ) {
            externalRef.setExternalDatabase( this.persistExternalDatabase( externalRef.getExternalDatabase(), caches ) );
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
                compositeSequence.setBiologicalCharacteristic( this.persistBioSequence( biologicalCharacteristic, caches ) );
            }
            if ( ++examined % REPORT_BATCH_SIZE == 0 ) {
                AbstractPersister.log.info( examined + "/" + numElements
                        + " compositeSequence sequences examined for " + arrayDesign );
            }
        }
        if ( examined > 0 ) {
            AbstractPersister.log.info( "Total of " + examined
                    + " compositeSequence sequences examined for " + arrayDesign );
        }

        AbstractPersister.log.debug( "Persisting " + arrayDesign );
        return arrayDesignDao.create( arrayDesign );
    }

}
