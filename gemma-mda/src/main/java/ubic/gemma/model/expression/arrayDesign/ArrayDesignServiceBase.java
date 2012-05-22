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
package ubic.gemma.model.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.arrayDesign.ArrayDesignService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService
 */
public abstract class ArrayDesignServiceBase implements ubic.gemma.model.expression.arrayDesign.ArrayDesignService {

    Log log = LogFactory.getLog( this.getClass() ); 
    
    @Autowired
    private ubic.gemma.model.expression.arrayDesign.ArrayDesignDao arrayDesignDao;

    @Autowired
    private AuditEventDao auditEventDao;

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCompositeSequenceWithoutBioSequences( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCompositeSequenceWithoutBlatResults( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutGenes(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCompositeSequenceWithoutGenes( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.countAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#create(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign create(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleCreate( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.create(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void deleteAlignmentData( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            this.handleDeleteAlignmentData( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void deleteGeneProductAssociations( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            this.handleDeleteGeneProductAssociations( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign find(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleFind( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.find(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByAlternateName(java.lang.String)
     */
    public java.util.Collection<ArrayDesign> findByAlternateName( final java.lang.String queryString ) {
        try {
            return this.handleFindByAlternateName( queryString );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.findByAlternateName(java.lang.String queryString)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByName(java.lang.String)
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findByShortName(java.lang.String)
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByShortName( final java.lang.String shortName ) {
        try {
            return this.handleFindByShortName( shortName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.findByShortName(java.lang.String shortName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findOrCreate(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleFindOrCreate( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getAllAssociatedBioAssays(java.lang.Long)
     */
    public java.util.Collection<BioAssay> getAllAssociatedBioAssays( final java.lang.Long id ) {
        try {
            return this.handleGetAllAssociatedBioAssays( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getAllAssociatedBioAssays(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @return the auditEventDao
     */
    public AuditEventDao getAuditEventDao() {
        return auditEventDao;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getCompositeSequenceCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.lang.Integer getCompositeSequenceCount(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleGetCompositeSequenceCount( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getCompositeSequenceCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<ExpressionExperiment> getExpressionExperiments(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleGetExpressionExperiments( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastAnnotationFile(java.util.Collection)
     */
    public java.util.Map<Long, AuditEvent> getLastAnnotationFile( final java.util.Collection<Long> ids ) {
        try {
            return this.handleGetLastAnnotationFile( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getLastAnnotationFile(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastGeneMapping(java.util.Collection)
     */
    public java.util.Map<Long, AuditEvent> getLastGeneMapping( final java.util.Collection<Long> ids ) {
        try {
            return this.handleGetLastGeneMapping( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getLastGeneMapping(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastRepeatAnalysis(java.util.Collection)
     */
    public java.util.Map<Long, AuditEvent> getLastRepeatAnalysis( final java.util.Collection<Long> ids ) {
        try {
            return this.handleGetLastRepeatAnalysis( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getLastRepeatAnalysis(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastSequenceAnalysis(java.util.Collection)
     */
    public java.util.Map<Long, AuditEvent> getLastSequenceAnalysis( final java.util.Collection<Long> ids ) {
        try {
            return this.handleGetLastSequenceAnalysis( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getLastSequenceAnalysis(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastSequenceUpdate(java.util.Collection)
     */
    public java.util.Map<Long, AuditEvent> getLastSequenceUpdate( final java.util.Collection<Long> ids ) {
        try {
            return this.handleGetLastSequenceUpdate( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getLastSequenceUpdate(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastTroubleEvent(java.util.Collection)
     */
    public java.util.Map<Long, AuditEvent> getLastTroubleEvent( final java.util.Collection<Long> ids ) {
        try {
            return this.handleGetLastTroubleEvent( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getLastTroubleEvent(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getLastValidationEvent(java.util.Collection)
     */
    public java.util.Map<Long, AuditEvent> getLastValidationEvent( final java.util.Collection<Long> ids ) {
        try {
            return this.handleGetLastValidationEvent( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getLastValidationEvent(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getTaxa(java.lang.Long)
     */
    public java.util.Collection<ubic.gemma.model.genome.Taxon> getTaxa( final java.lang.Long id ) {
        try {
            return this.handleGetTaxa( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getTaxons(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#getTaxon(java.lang.Long)
     */
    public ubic.gemma.model.genome.Taxon getTaxon( final java.lang.Long id ) {
        try {
            return this.handleGetTaxon( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.getTaxon(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#isMerged(java.util.Collection)
     */
    public java.util.Map<Long, Boolean> isMerged( final java.util.Collection<Long> ids ) {
        try {
            return this.handleIsMerged( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.isMerged(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#isMergee(java.util.Collection)
     */
    public java.util.Map<Long, Boolean> isMergee( final java.util.Collection<Long> ids ) {
        try {
            return this.handleIsMergee( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.isMergee(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#isSubsumed(java.util.Collection)
     */
    public java.util.Map<Long, Boolean> isSubsumed( final java.util.Collection<Long> ids ) {
        try {
            return this.handleIsSubsumed( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.isSubsumed(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#isSubsumer(java.util.Collection)
     */
    public java.util.Map<Long, Boolean> isSubsumer( final java.util.Collection<Long> ids ) {
        try {
            return this.handleIsSubsumer( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.isSubsumer(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#load(long)
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign load( final long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.load(long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadAll()
     */
    public java.util.Collection<ArrayDesign> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.loadAll()' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadAllValueObjects()
     */
    public java.util.Collection<ArrayDesignValueObject> loadAllValueObjects() {
        try {
            return this.handleLoadAllValueObjects();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.loadAllValueObjects()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.util.Collection<CompositeSequence> loadCompositeSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleLoadCompositeSequences( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.loadCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadMultiple(java.util.Collection)
     */
    public java.util.Collection<ArrayDesign> loadMultiple( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadValueObjects(java.util.Collection)
     */
    public ArrayDesignValueObject loadValueObject( final Long id ) {
        try {
            Collection<Long> ids = new ArrayList<Long>();
            ids.add( id );
            Collection<ArrayDesignValueObject> advos = this.handleLoadValueObjects( ids );
            if(advos == null || advos.size() < 1)
                throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                        "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.loadValueObject(Long id)' --> "
                                + "no entities found for id = " +  id);
            if( advos.size() > 1){
                // this should never happen
                log.error( "Found more than one ArrayDesign for id = "+id );
            }
            return advos.iterator().next();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.loadValueObject(Long id)' --> "
                            + th, th );
        }
    }
    
    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#loadValueObjects(java.util.Collection)
     */
    public java.util.Collection<ArrayDesignValueObject> loadValueObjects( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadValueObjects( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.loadValueObjects(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithBioSequences()
     */
    public long numAllCompositeSequenceWithBioSequences() {
        try {
            return this.handleNumAllCompositeSequenceWithBioSequences();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numAllCompositeSequenceWithBioSequences()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithBioSequences(java.util.Collection)
     */
    public long numAllCompositeSequenceWithBioSequences( final java.util.Collection<Long> ids ) {
        try {
            return this.handleNumAllCompositeSequenceWithBioSequences( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numAllCompositeSequenceWithBioSequences(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithBlatResults()
     */
    public long numAllCompositeSequenceWithBlatResults() {
        try {
            return this.handleNumAllCompositeSequenceWithBlatResults();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numAllCompositeSequenceWithBlatResults()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithBlatResults(java.util.Collection)
     */
    public long numAllCompositeSequenceWithBlatResults( final java.util.Collection<Long> ids ) {
        try {
            return this.handleNumAllCompositeSequenceWithBlatResults( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numAllCompositeSequenceWithBlatResults(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithGenes()
     */
    public long numAllCompositeSequenceWithGenes() {
        try {
            return this.handleNumAllCompositeSequenceWithGenes();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numAllCompositeSequenceWithGenes()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllCompositeSequenceWithGenes(java.util.Collection)
     */
    public long numAllCompositeSequenceWithGenes( final java.util.Collection<Long> ids ) {
        try {
            return this.handleNumAllCompositeSequenceWithGenes( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numAllCompositeSequenceWithGenes(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllGenes()
     */
    public long numAllGenes() {
        try {
            return this.handleNumAllGenes();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numAllGenes()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numAllGenes(java.util.Collection)
     */
    public long numAllGenes( final java.util.Collection<Long> ids ) {
        try {
            return this.handleNumAllGenes( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numAllGenes(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numBioSequences( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumBioSequences( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numBlatResults( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumBlatResults( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numCompositeSequenceWithBioSequences(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumCompositeSequenceWithBioSequences( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numCompositeSequenceWithBlatResults(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumCompositeSequenceWithBlatResults( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numCompositeSequenceWithGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumCompositeSequenceWithGenes( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public long numGenes( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleNumGenes( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#remove(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void remove( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            this.handleRemove( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.remove(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void removeBiologicalCharacteristics( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            this.handleRemoveBiologicalCharacteristics( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>arrayDesign</code>'s DAO.
     */
    public void setArrayDesignDao( ubic.gemma.model.expression.arrayDesign.ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @param auditEventDao the auditEventDao to set
     */
    public void setAuditEventDao( AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ArrayDesign thaw( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleThaw( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#thawLite(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public ArrayDesign thawLite( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleThawLite( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.thawLite(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#update(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public void update( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            this.handleUpdate( arrayDesign );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.update(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignService#updateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public java.lang.Boolean updateSubsumingStatus(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee ) {
        try {
            return this.handleUpdateSubsumingStatus( candidateSubsumer, candidateSubsumee );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceException(
                    "Error performing 'ubic.gemma.model.expression.arrayDesign.ArrayDesignService.updateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer, ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>arrayDesign</code>'s DAO.
     */
    protected ubic.gemma.model.expression.arrayDesign.ArrayDesignDao getArrayDesignDao() {
        return this.arrayDesignDao;
    }

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #compositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleCompositeSequenceWithoutGenes(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleCreate(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #deleteAlignmentData(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleDeleteAlignmentData( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #deleteGeneProductAssociations(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleDeleteGeneProductAssociations(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFind(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByAlternateName(java.lang.String)}
     */
    protected abstract java.util.Collection<ArrayDesign> handleFindByAlternateName( java.lang.String queryString )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindByName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByShortName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindByShortName(
            java.lang.String shortName ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleFindOrCreate(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAllAssociatedBioAssays(java.lang.Long)}
     */
    protected abstract java.util.Collection<BioAssay> handleGetAllAssociatedBioAssays( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getCompositeSequenceCount(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.lang.Integer handleGetCompositeSequenceCount(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getExpressionExperiments(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleGetExpressionExperiments(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastAnnotationFile(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastAnnotationFile( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastGeneMapping(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastGeneMapping( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastRepeatAnalysis(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastRepeatAnalysis( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastSequenceAnalysis(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastSequenceAnalysis( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastSequenceUpdate(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastSequenceUpdate( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastTroubleEvent(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastTroubleEvent( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastValidationEvent(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, AuditEvent> handleGetLastValidationEvent( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getTaxa(java.lang.Long)} Lmd 29/07/09 Fishmanomics provide support multi
     * taxon arrays
     */
    protected abstract java.util.Collection<ubic.gemma.model.genome.Taxon> handleGetTaxa( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getTaxon(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleGetTaxon( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #isMerged(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsMerged( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #isMergee(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsMergee( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #isSubsumed(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsSubsumed( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #isSubsumer(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Boolean> handleIsSubsumer( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(long)}
     */
    protected abstract ubic.gemma.model.expression.arrayDesign.ArrayDesign handleLoad( long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ArrayDesign> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAllValueObjects()}
     */
    protected abstract java.util.Collection<ArrayDesignValueObject> handleLoadAllValueObjects()
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadCompositeSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleLoadCompositeSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<ArrayDesign> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadValueObjects(java.util.Collection)}
     */
    protected abstract java.util.Collection<ArrayDesignValueObject> handleLoadValueObjects(
            java.util.Collection<Long> ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBioSequences()}
     */
    protected abstract long handleNumAllCompositeSequenceWithBioSequences() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBioSequences(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithBioSequences( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBlatResults()}
     */
    protected abstract long handleNumAllCompositeSequenceWithBlatResults() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithBlatResults(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithBlatResults( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithGenes()}
     */
    protected abstract long handleNumAllCompositeSequenceWithGenes() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllCompositeSequenceWithGenes(java.util.Collection)}
     */
    protected abstract long handleNumAllCompositeSequenceWithGenes( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllGenes()}
     */
    protected abstract long handleNumAllGenes() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numAllGenes(java.util.Collection)}
     */
    protected abstract long handleNumAllGenes( java.util.Collection<Long> ids ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumBioSequences( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumBlatResults( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #numCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumCompositeSequenceWithGenes(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #numGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract long handleNumGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #removeBiologicalCharacteristics(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleRemoveBiologicalCharacteristics(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ArrayDesign handleThaw( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thawLite(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract ArrayDesign handleThawLite( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #updateSubsumingStatus(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract java.lang.Boolean handleUpdateSubsumingStatus(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee ) throws java.lang.Exception;

}