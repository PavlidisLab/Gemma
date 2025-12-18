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

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.DatabaseEntry;
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
 * once to the system. Therefore, we want to take care not to get multiple slightly different copies of them, but we also
 * don't want to have to spend an inordinate amount of time checking a submitted version against the database.
 * The association between ArrayDesign and DesignElement is compositional - the lifecycle of a {@link ubic.gemma.model.expression.bioAssayData.DesignElementDataVector}
 * is tied to the {@link ArrayDesign}. However, {@link ubic.gemma.model.expression.bioAssayData.DesignElementDataVector}
 * have associations with {@link BioSequence}, which have their own lifecycle, in general.
 *
 * @author pavlidis
 */
public abstract class ArrayDesignPersister extends GenomePersister {

    @Autowired
    private ArrayDesignDao arrayDesignDao;

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
     * Persist an array design.
     */
    private ArrayDesign findOrPersistArrayDesign( ArrayDesign arrayDesign, Caches caches ) {
        if ( arrayDesign.getId() != null ) {
            AbstractPersister.log.debug( "Platform " + arrayDesign + " already exists, returning..." );
            return arrayDesign;
        }

        // Try less stringent search using the short name
        ArrayDesign existing = arrayDesignDao.findByShortName( arrayDesign.getShortName() );
        if ( existing != null ) {
            AbstractPersister.log.info( String.format( "Platform exactly matching %s doesn't exist, but found %s; returning",
                    arrayDesign, existing ) );
            return existing;
        }

        AbstractPersister.log.debug( arrayDesign + " is new, processing..." );
        return this.persistNewArrayDesign( arrayDesign, caches );
    }

    /**
     * Persist an entirely new array design, including composite sequences and any associated new sequences.
     */
    private ArrayDesign persistNewArrayDesign( ArrayDesign arrayDesign, Caches caches ) {
        AbstractPersister.log.debug( "Persisting new platform " + arrayDesign.getName() );

        if ( arrayDesign.getDesignProvider() != null )
            arrayDesign.setDesignProvider( this.persistContact( arrayDesign.getDesignProvider() ) );

        if ( arrayDesign.getPrimaryTaxon() == null ) {
            throw new IllegalArgumentException( "Primary taxon cannot be null" );
        }

        arrayDesign.setPrimaryTaxon( this.doPersist( arrayDesign.getPrimaryTaxon(), caches ) );

        for ( DatabaseEntry externalRef : arrayDesign.getExternalReferences() ) {
            externalRef.setExternalDatabase( this.persistExternalDatabase( externalRef.getExternalDatabase(), caches ) );
        }

        AbstractPersister.log.debug( "Persisting " + arrayDesign );

        Collection<CompositeSequence> scs = new ArrayList<>( arrayDesign.getCompositeSequences() );
        arrayDesign.getCompositeSequences().clear();
        arrayDesign = arrayDesignDao.create( arrayDesign );
        arrayDesign.getCompositeSequences().addAll( scs );
        this.persistArrayDesignCompositeSequenceAssociations( arrayDesign, caches );
        arrayDesignDao.update( arrayDesign );

        return arrayDesign;
    }

    private void persistArrayDesignCompositeSequenceAssociations( ArrayDesign arrayDesign, Caches caches ) {
        int numElements = arrayDesign.getCompositeSequences().size();
        if ( numElements == 0 )
            return;
        AbstractPersister.log.debug( "Filling in or updating sequences in composite sequences for " + arrayDesign );

        int persistedBioSequences = 0;
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            compositeSequence.setArrayDesign( arrayDesign );

            BioSequence biologicalCharacteristic = compositeSequence.getBiologicalCharacteristic();

            if ( compositeSequence.getBiologicalCharacteristic() != null ) {
                compositeSequence.setBiologicalCharacteristic( this.persistBioSequence( biologicalCharacteristic, caches ) );
            }

            if ( ++persistedBioSequences % REPORT_BATCH_SIZE == 0 ) {
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
    }

}
