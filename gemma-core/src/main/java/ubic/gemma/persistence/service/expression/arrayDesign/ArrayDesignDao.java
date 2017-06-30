package ubic.gemma.persistence.service.expression.arrayDesign;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CuratableDao;

import java.util.Collection;
import java.util.Map;

/**
 * Created by tesarst on 13/03/17.
 * ArrayDesignDao interface
 */
@Repository
public interface ArrayDesignDao extends InitializingBean, CuratableDao<ArrayDesign, ArrayDesignValueObject> {

    Integer countNotTroubled();

    Map<Taxon, Long> getPerTaxonCount();

    void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes );

    ArrayDesign find( String queryString, ArrayDesign entity );

    Collection<ArrayDesign> findByManufacturer( String queryString );

    Collection<ArrayDesign> findByTaxon( Taxon taxon );

    Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    Map<CompositeSequence, Collection<BlatResult>> loadAlignments( ArrayDesign arrayDesign );

    int numExperiments( ArrayDesign arrayDesign );

    ArrayDesign thawLite( ArrayDesign arrayDesign );

    Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns );

    Collection<ArrayDesignValueObject> listFilter( int offset, int limit, String orderBy, boolean asc);

    Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign );

    Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign );

    Collection<CompositeSequence> compositeSequenceWithoutGenes( ArrayDesign arrayDesign );

    void deleteAlignmentData( ArrayDesign arrayDesign );

    void deleteGeneProductAssociations( ArrayDesign arrayDesign );

    Collection<ArrayDesign> findByAlternateName( String queryString );

    Collection<BioAssay> getAllAssociatedBioAssays( Long id );

    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    Collection<Taxon> getTaxa( Long id );

    Taxon getTaxon( Long id );

    Map<Long, Boolean> isMerged( Collection<Long> ids );

    Map<Long, Boolean> isMergee( Collection<Long> ids );

    Map<Long, Boolean> isSubsumed( Collection<Long> ids );

    Map<Long, Boolean> isSubsumer( Collection<Long> ids );

    Collection<ArrayDesignValueObject> loadValueObjectsByIds( Collection<Long> ids );

    Collection<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId );

    Collection<CompositeSequence> loadCompositeSequences( Long id );

    long numAllCompositeSequenceWithBioSequences();

    long numAllCompositeSequenceWithBioSequences( Collection<Long> ids );

    long numAllCompositeSequenceWithBlatResults();

    long numAllCompositeSequenceWithBlatResults( Collection<Long> ids );

    long numAllCompositeSequenceWithGenes();

    long numAllCompositeSequenceWithGenes( Collection<Long> ids );

    long numAllGenes();

    long numAllGenes( Collection<Long> ids );

    long numBioSequences( ArrayDesign arrayDesign );

    long numBlatResults( ArrayDesign arrayDesign );

    long numCompositeSequences( Long id );

    long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign );

    long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign );

    long numCompositeSequenceWithGenes( ArrayDesign arrayDesign );

    long numGenes( ArrayDesign arrayDesign );

    void removeBiologicalCharacteristics( ArrayDesign arrayDesign );

    ArrayDesign thaw( ArrayDesign arrayDesign );

    Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );
}
