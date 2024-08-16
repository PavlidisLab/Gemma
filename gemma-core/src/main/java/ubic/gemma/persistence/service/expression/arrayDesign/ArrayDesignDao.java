package ubic.gemma.persistence.service.expression.arrayDesign;

import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.CachedFilteringVoEnabledDao;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CuratableDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by tesarst on 13/03/17.
 * ArrayDesignDao interface
 */
@Repository
public interface ArrayDesignDao extends CuratableDao<ArrayDesign>,
        CachedFilteringVoEnabledDao<ArrayDesign, ArrayDesignValueObject> {

    String OBJECT_ALIAS = "ad";

    void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes );

    void deleteAlignmentData( ArrayDesign arrayDesign );

    void deleteGeneProductAssociations( ArrayDesign arrayDesign );

    ArrayDesign findByShortName( String shortName );

    Collection<ArrayDesign> findByName( String name );

    Collection<ArrayDesign> findByAlternateName( String queryString );

    Collection<ArrayDesign> findByManufacturer( String queryString );

    Collection<ArrayDesign> findByTaxon( Taxon taxon );

    Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign );

    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    Collection<Gene> getGenes( ArrayDesign arrayDesign );

    Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    /**
     * Obtain the number of associated expression experiments.
     * <p>
     * This is much faster than looking up the size of {@link #getExpressionExperiments(ArrayDesign)}.
     */
    long getExpressionExperimentsCount( ArrayDesign arrayDesign );

    Map<Taxon, Long> getPerTaxonCount();

    /**
     * Obtain a collection of {@link ExpressionExperiment} identifiers that have been switched from a given platform.
     * <p>
     * If you only need to count them, consider using the more performant {@link #getSwitchedExpressionExperimentsCount(ArrayDesign)}
     * instead.
     */
    Collection<ExpressionExperiment> getSwitchedExpressionExperiments( ArrayDesign arrayDesign );

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

    long numExperiments( ArrayDesign arrayDesign );

    long numGenes( ArrayDesign arrayDesign );

    void removeBiologicalCharacteristics( ArrayDesign arrayDesign );

    /**
     * Lightly thaw the given platform.
     */
    void thawLite( ArrayDesign arrayDesign );

    /**
     * Thaw the given platform as per {@link #thawLite(ArrayDesign)} with its probes and genes.
     */
    void thaw( ArrayDesign arrayDesign );

    Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );

    void deleteGeneProductAlignmentAssociations( ArrayDesign arrayDesign );

    void deleteGeneProductAnnotationAssociations( ArrayDesign arrayDesign );

    Slice<ArrayDesignValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );
}
