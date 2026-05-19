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
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CuratableDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by tesarst on 13/03/17.
 * ArrayDesignDao interface
 */
@Repository
public interface ArrayDesignDao extends CuratableDao<ArrayDesign>,
        CachedFilteringVoEnabledDao<ArrayDesign, ArrayDesignValueObject> {

    String OBJECT_ALIAS = "ad";

    Collection<ArrayDesign> loadAllGenericGenePlatforms();

    void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes );

    void deleteAlignmentData( ArrayDesign arrayDesign );

    void deleteGeneProductAssociations( ArrayDesign arrayDesign );

    @Nullable
    ArrayDesign findByShortName( String shortName );

    Collection<ArrayDesign> findByName( String name );

    @Nullable
    ArrayDesign findOneByName( String name );

    Collection<ArrayDesign> findByCompositeSequenceName( String name );

    Collection<ArrayDesign> findByAlternateName( String queryString );

    @Nullable
    ArrayDesign findOneByAlternateName( String name );

    Collection<ArrayDesign> findByManufacturer( String queryString );

    Collection<ArrayDesign> findByTaxon( Taxon taxon );

    Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign );

    Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids );

    Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign );

    long countBioSequences( ArrayDesign arrayDesign );

    /**
     * Obtain all the genes associated to the platform.
     */
    Collection<Gene> getGenes( ArrayDesign arrayDesign, boolean useGene2Cs );

    long countGenes( boolean useGene2Cs );

    long countGenes( ArrayDesign arrayDesign, boolean useGene2Cs );

    /**
     * Obtain all the genes associated to the platform organized by corresponding design elements.
     */
    Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( ArrayDesign arrayDesign, boolean useGene2Cs );

    Map<CompositeSequence, Set<Gene>> getGenesByCompositeSequence( Collection<ArrayDesign> arrayDesign, boolean useGene2Cs );

    Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign );

    /**
     * Obtain the number of associated expression experiments.
     * <p>
     * This is much faster than looking up the size of {@link #getExpressionExperiments(ArrayDesign)}.
     *
     * @see #getExpressionExperiments(ArrayDesign)
     */
    long countExpressionExperiments( ArrayDesign arrayDesign );

    Map<Taxon, Long> getPerTaxonCount();

    /**
     * Obtain a collection of {@link ExpressionExperiment} identifiers that have been switched from a given platform.
     * <p>
     * If you only need to count them, consider using the more performant {@link #countSwitchedExpressionExperiments(ArrayDesign)}
     * instead.
     */
    Collection<ExpressionExperiment> getSwitchedExpressionExperiments( ArrayDesign arrayDesign );

    /**
     * Count the number of switched {@link ExpressionExperiment} from a given platform.
     *
     * @see #getSwitchedExpressionExperiments(ArrayDesign)
     */
    long countSwitchedExpressionExperiments( ArrayDesign arrayDesign );

    /**
     * Obtain all the taxa associated to the {@link BioSequence} of the given platform.
     */
    Collection<Taxon> getTaxaFromBioSequences( ArrayDesign arrayDesign );

    Map<Long, Boolean> isMerged( Collection<Long> ids );

    Map<Long, Boolean> isMergee( Collection<Long> ids );

    Map<Long, Boolean> isSubsumed( Collection<Long> ids );

    Map<Long, Boolean> isSubsumer( Collection<Long> ids );

    Map<CompositeSequence, Collection<BlatResult>> loadAlignments( ArrayDesign arrayDesign );

    Collection<CompositeSequence> loadCompositeSequences( ArrayDesign arrayDesign, int limit, int offset );

    List<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId );

    long countCompositeSequencesWithBioSequences();

    long countCompositeSequencesWithBlatResults();

    long countCompositeSequencesWithGenes( boolean useGene2Cs );

    long countBlatResults( ArrayDesign arrayDesign );

    long countCompositeSequences( ArrayDesign id );

    long countCompositeSequencesWithBioSequences( ArrayDesign arrayDesign );

    long countCompositeSequencesWithBlatResults( ArrayDesign arrayDesign );

    long countCompositeSequencesWithGenes( ArrayDesign arrayDesign, boolean useGene2Cs );

    long countCompositeSequencesWithGenes( Collection<ArrayDesign> arrayDesign, boolean useGene2Cs );

    void removeBiologicalCharacteristics( ArrayDesign arrayDesign );

    /**
     * Lightly thaw the given platform.
     * <p>
     * This includes all the to-one relations, but not the design elements.
     */
    void thawLite( ArrayDesign arrayDesign );

    /**
     * Thaw the given platform as per {@link #thawLite(ArrayDesign)} with its probes and genes.
     */
    void thaw( ArrayDesign arrayDesign );

    /**
     * Only thaw the design elements of a given platform.
     */
    void thawCompositeSequences( ArrayDesign arrayDesign );

    boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee );

    void deleteGeneProductAlignmentAssociations( ArrayDesign arrayDesign );

    void deleteGeneProductAnnotationAssociations( ArrayDesign arrayDesign );

    Slice<ArrayDesignValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * Cursor-mode counterpart to {@link #loadBlacklistedValueObjects(Filters, Sort, int, int)}:
     * keyset pagination over the blacklisted platforms, applying the same shortName/accession
     * blacklist filter that the offset-mode variant composes — see
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md} step 1h.
     */
    CursorPage<ArrayDesignValueObject> loadBlacklistedValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit );
}
