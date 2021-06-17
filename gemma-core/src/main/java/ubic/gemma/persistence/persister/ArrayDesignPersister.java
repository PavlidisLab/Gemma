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
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class handles persisting array designs. This is a bit of a special case, because ArrayDesigns are very large
 * (with associated reporters, CompositeSequences, and BioSequences), and also very likely to be submitted more than
 * once to the system. Therefore we want to take care not to get multiple slightly different copies of them, but we also
 * don't want to have to spend an inordinate amount of time checking a submitted version against the database.
 * The association between ArrayDesign and DesignElement is compositional - the lifecycle of a designelement is tied to
 * the arraydesign. However, designelements have associations with biosequence, which have their own lifecycle, in
 * general.
 *
 * @author pavlidis
 */
@Service
public class ArrayDesignPersister extends AuditablePersister<ArrayDesign> {

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Autowired
    private Persister<Contact> contactPersister;
    @Autowired
    private Persister<CompositeSequence> compositeSequencePersister;
    @Autowired
    private Persister<BioSequence> bioSequencePersister;
    @Autowired
    private Persister<Taxon> taxonPersister;
    @Autowired
    private Persister<ExternalDatabase> externalDatabasePersister;
    @Autowired
    private Persister<AuditTrail> auditTrailPersister;

    @Autowired
    public ArrayDesignPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public ArrayDesign persistAuditable( ArrayDesign entity ) {
        if ( entity == null )
            return null;

        if ( !this.isTransient( entity ) )
            return entity;

        /*
         * Note we don't do a full find here.
         */
        ArrayDesign existing = arrayDesignDao.find( entity );

        if ( existing == null ) {

            /*
             * Try less stringent search.
             */
            existing = arrayDesignDao.findByShortName( entity.getShortName() );

            if ( existing == null ) {
                AbstractPersister.log.info( entity + " is new, processing..." );
                return this.persistNewArrayDesign( entity );
            }

            AbstractPersister.log
                    .info( "Platform exactly matching " + entity + " doesn't exist, but found " + existing
                            + "; returning" );

        } else {
            AbstractPersister.log.info( "Platform " + entity + " already exists, returning..." );
        }

        return existing;
    }

    /**
     * Persist an entirely new array design, including composite sequences and any associated new sequences.
     */
    private ArrayDesign persistNewArrayDesign( ArrayDesign arrayDesign ) {

        if ( arrayDesign == null )
            return null;

        AbstractPersister.log.info( "Persisting new platform " + arrayDesign.getName() );

        try {
            this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.COMMIT );

            if ( arrayDesign.getDesignProvider() != null )
                arrayDesign.setDesignProvider( contactPersister.persist( arrayDesign.getDesignProvider() ) );

            if ( arrayDesign.getPrimaryTaxon() == null ) {
                throw new IllegalArgumentException( "Primary taxon cannot be null" );
            }

            arrayDesign.setPrimaryTaxon( this.taxonPersister.persist( arrayDesign.getPrimaryTaxon() ) );

            for ( DatabaseEntry externalRef : arrayDesign.getExternalReferences() ) {
                externalRef.setExternalDatabase( this.externalDatabasePersister.persist( externalRef.getExternalDatabase() ) );
            }

            AbstractPersister.log.info( "Persisting " + arrayDesign );

            if ( arrayDesign.getAuditTrail() != null && this.auditTrailPersister.isTransient( arrayDesign.getAuditTrail() ) )
                arrayDesign.getAuditTrail().setId( null );

            Collection<CompositeSequence> scs = new ArrayList<>( arrayDesign.getCompositeSequences() );
            arrayDesign.getCompositeSequences().clear();
            arrayDesign = arrayDesignDao.create( arrayDesign );
            arrayDesign.getCompositeSequences().addAll( scs );
            arrayDesign = this.persistArrayDesignCompositeSequenceAssociations( arrayDesign );
            arrayDesignDao.update( arrayDesign );

        } finally {
            this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.AUTO );
        }
        return arrayDesign;
    }

    private ArrayDesign persistArrayDesignCompositeSequenceAssociations( ArrayDesign arrayDesign ) {
        int numElements = arrayDesign.getCompositeSequences().size();
        if ( numElements == 0 )
            return arrayDesign;
        AbstractPersister.log.info( "Filling in or updating sequences in composite seqences for " + arrayDesign );

        int persistedBioSequences = 0;
        int numElementsPerUpdate = this.numElementsPerUpdate( arrayDesign.getCompositeSequences() );
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            if ( !compositeSequencePersister.isTransient( compositeSequence ) ) {
                // in case of retry (not used?)
                continue;
            }
            compositeSequence.setId( null );

            compositeSequence.setArrayDesign( arrayDesign );

            BioSequence biologicalCharacteristic = compositeSequence.getBiologicalCharacteristic();

            BioSequence persistedBs = this.bioSequencePersister.persist( biologicalCharacteristic );

            compositeSequence.setBiologicalCharacteristic( persistedBs );

            if ( ++persistedBioSequences % numElementsPerUpdate == 0 && numElements > 1000 ) {
                AbstractPersister.log
                        .info( persistedBioSequences + "/" + numElements + " compositeSequence sequences examined for "
                                + arrayDesign );
            }

        }

        if ( persistedBioSequences > 0 ) {
            AbstractPersister.log
                    .info( "Total of " + persistedBioSequences + " compositeSequence sequences examined for "
                            + arrayDesign );
        }

        return arrayDesign;
    }

}
