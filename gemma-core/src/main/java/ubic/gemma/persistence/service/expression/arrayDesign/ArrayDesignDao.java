package ubic.gemma.persistence.service.expression.arrayDesign;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CuratableDao;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by tesarst on 13/03/17.
 * ArrayDesignDao interface
 */
@Repository
public interface ArrayDesignDao extends CuratableDao<ArrayDesign, ArrayDesignValueObject>,
        FilteringVoEnabledDao<ArrayDesign, ArrayDesignValueObject> {

    String OBJECT_ALIAS = "ad";

    void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes );

    Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign );

    Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign );

    Collection<CompositeSequence> compositeSequenceWithoutGenes( ArrayDesign arrayDesign );

    void deleteAlignmentData( ArrayDesign arrayDesign );

    void deleteGeneProductAssociations( ArrayDesign arrayDesign );

    Collection<ArrayDesign> findByAlternateName( String queryString );

    Collection<ArrayDesign> findByManufacturer( String queryString );

    Collection<ArrayDesign> findByTaxon( Taxon taxon );

    Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign );

    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    Map<Taxon, Long> getPerTaxonCount();

    /**
     * Obtain a collection of {@link ExpressionExperiment} identifiers that have been switched from a given platform.
     *
     * If you only need to count them, consider using the more performant {@link #getSwitchedExpressionExperimentsCount(ArrayDesign)}
     * instead.
     */
    Collection<Long> getSwitchedExpressionExperimentIds( ArrayDesign arrayDesign );

    /**
     * Count the number of switched {@link ExpressionExperiment} from a given platform.
     */
    Long getSwitchedExpressionExperimentsCount( ArrayDesign arrayDesign );

    Collection<Taxon> getTaxa( ArrayDesign arrayDesign );

    Map<Long, Boolean> isMerged( Collection<Long> ids );

    Map<Long, Boolean> isMergee( Collection<Long> ids );

    Map<Long, Boolean> isSubsumed( Collection<Long> ids );

    Map<Long, Boolean> isSubsumer( Collection<Long> ids );

    Map<CompositeSequence, Collection<BlatResult>> loadAlignments( ArrayDesign arrayDesign );

    Collection<CompositeSequence> loadCompositeSequences( ArrayDesign arrayDesign, int limit, int offset );

    List<ArrayDesignValueObject> loadValueObjectsByIds( Collection<Long> ids );

    List<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId );

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

    long numCompositeSequences( ArrayDesign id );

    long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign );

    long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign );

    long numCompositeSequenceWithGenes( ArrayDesign arrayDesign );

    int numExperiments( ArrayDesign arrayDesign );

    long numGenes( ArrayDesign arrayDesign );

    void removeBiologicalCharacteristics( ArrayDesign arrayDesign );

    ArrayDesign thaw( ArrayDesign arrayDesign );

    ArrayDesign thawLite( ArrayDesign arrayDesign );

    Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns );

    Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );
}
