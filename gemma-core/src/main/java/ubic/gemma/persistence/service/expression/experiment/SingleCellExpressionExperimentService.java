package ubic.gemma.persistence.service.expression.experiment;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public interface SingleCellExpressionExperimentService {

    /**
     * Load an experiment with its single-cell data vectors initialized.
     * <p>
     * The rest of the experiment is also initialized as per {@link ExpressionExperimentDao#thawLite(ExpressionExperiment)}.
     */
    @Nullable
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    ExpressionExperiment loadWithSingleCellVectors( Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<QuantitationType> getSingleCellQuantitationTypes( ExpressionExperiment ee );

    /**
     * Obtain the preferred single-cell quantitation type.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<QuantitationType> getPreferredSingleCellQuantitationType( ExpressionExperiment ee );

    /**
     * Obtain the single-cell quantitation types by single-cell dimension.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Map<SingleCellDimension, Set<QuantitationType>> getSingleCellQuantitationTypesBySingleCellDimensionWithoutCellIds( ExpressionExperiment ee, SingleCellDimensionInitializationConfig config );

    /**
     * Obtain preferred single-cell vectors.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<Collection<SingleCellExpressionDataVector>> getPreferredSingleCellDataVectors( ExpressionExperiment ee );

    @Getter
    @Builder
    class SingleCellVectorInitializationConfig {
        /**
         * Initialize {@link CompositeSequence#getBiologicalCharacteristic()}.
         */
        private boolean includeBiologicalCharacteristics;
        private boolean includeCellIds;
        private boolean includeData;
        private boolean includeDataIndices;
    }

    /**
     * Obtain single-cell vectors for a given quantitation type.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, SingleCellVectorInitializationConfig config );

    /**
     * Obtain single-cell vectors for a particular sample.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType, SingleCellVectorInitializationConfig config );

    /**
     * Obtain a stream over single-cell vectors for a given quantitation type.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession, SingleCellVectorInitializationConfig config );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession, SingleCellVectorInitializationConfig config );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession );

    /**
     * Obtain a single single-cell vector without initializing cell IDs.
     *
     * @see #getSingleCellDimensionWithoutCellIds(ExpressionExperiment, QuantitationType)
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellExpressionDataVector getSingleCellDataVectorWithoutCellIds( ExpressionExperiment ee, QuantitationType quantitationType, CompositeSequence designElement );

    /**
     * Obtain the number of single-cell vectors for a given quantitation type.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long getNumberOfSingleCellDataVectors( ExpressionExperiment ee, QuantitationType qt );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    long getNumberOfNonZeroes( ExpressionExperiment ee, QuantitationType qt );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Map<BioAssay, Long> getNumberOfNonZeroesBySample( ExpressionExperiment ee, QuantitationType qt, int fetchSize, boolean useCursorFetchIfSupported );

    /**
     * Obtain a single-cell expression data matrix for the given quantitation type.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellExpressionDataMatrix<?> getSingleCellExpressionDataMatrix( ExpressionExperiment expressionExperiment, List<BioAssay> samples, QuantitationType quantitationType );

    /**
     * Obtain a single-cell expression data matrix for the given quantitation type.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellExpressionDataMatrix<?> getSingleCellExpressionDataMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType );

    /**
     * Add single-cell data vectors.
     *
     * @param recrateCellTypeFactorIfNecessary re-create the cell type factor if necessary (i.e. a new set of preferred single-cell vectors are added)
     * @param ignoreCompatibleFactor
     * @return the number of vectors that were added
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int addSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType,
            Collection<SingleCellExpressionDataVector> vectors, @Nullable String details, boolean recrateCellTypeFactorIfNecessary, boolean ignoreCompatibleFactor );

    /**
     * Replace existing single-cell data vectors for the given quantitation type.
     *
     * @param details                           additional details to include in the audit event
     * @param recreateCellTypeFactorIfNecessary re-create the cell type factor if necessary (i.e. if the preferred
     *                                          single-cell vectors are being replaced)
     * @param ignoreCompatibleFactor            ignore an existing compatible cell type factor and re-create it anyway
     * @return the number of vectors that were replaced
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int replaceSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType,
            Collection<SingleCellExpressionDataVector> vectors, @Nullable String details, boolean recreateCellTypeFactorIfNecessary, boolean ignoreCompatibleFactor );

    /**
     * Update the sparsity metrics.
     * <p>
     * If no preferred single-cell vectors are present, the sparsity metrics will be cleared.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void updateSparsityMetrics( ExpressionExperiment ee );

    /**
     * Remove single-cell data vectors for the given quantitation type.
     *
     * @return the number of vectors that were removed
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType );

    /**
     * Obtain all the single-cell dimensions used by a given dataset.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<SingleCellDimension> getSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee );

    @Getter
    @Builder
    class SingleCellDimensionInitializationConfig {
        private boolean includeBioAssays;
        private boolean includeCtas;
        private boolean includeClcs;
        private boolean includeProtocol;
        private boolean includeCharacteristics;
        private boolean includeIndices;
    }

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<SingleCellDimension> getSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee, SingleCellDimensionInitializationConfig singleCellDimensionInitializationConfig );

    /**
     * Obtain a single-cell dimension used for a given dataset and QT.
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, QuantitationType qt );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellDimension getSingleCellDimensionWithCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Retrieve a single-cell dimension with its bioassays and cell-level  characteristics initialized.
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellDimension getSingleCellDimensionWithAssaysAndCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellDimension getSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, QuantitationType qt );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellDimension getSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, QuantitationType qt, SingleCellDimensionInitializationConfig singleCellDimensionInitializationConfig );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    SingleCellDimension getSingleCellDimensionByIdWithoutCellIds( ExpressionExperiment expressionExperiment, Long id, SingleCellDimensionInitializationConfig config );

    /**
     * Obtain the preferred single-cell dimension.
     * <p>
     * Cell type assignments and other cell-level characteristics are eagerly initialized.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<SingleCellDimension> getPreferredSingleCellDimension( ExpressionExperiment ee );

    /**
     * Obtain the preferred single-cell dimension without its cell IDs.
     * <p>
     * The returned object is not persistent since it's a projection.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<SingleCellDimension> getPreferredSingleCellDimensionWithoutCellIds( ExpressionExperiment ee );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<SingleCellDimension> getPreferredSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, SingleCellDimensionInitializationConfig singleCellDimensionInitializationConfig );

    /**
     * Obtain the preferred single-cell dimension.
     * <p>
     * Cell type assignments and other cell-level characteristics are eagerly initialized.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<SingleCellDimension> getPreferredSingleCellDimensionWithCellLevelCharacteristics( ExpressionExperiment ee );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<Stream<String>> streamCellIds( ExpressionExperiment ee, boolean createNewSession );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Stream<String> streamCellIds( ExpressionExperiment ee, QuantitationType qt, boolean createNewSession );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Stream<Characteristic> streamCellTypes( ExpressionExperiment ee, CellTypeAssignment cta, boolean createNewSession );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Characteristic getCellTypeAt( ExpressionExperiment ee, QuantitationType qt, Long ctaId, int cellIndex );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Characteristic[] getCellTypeAt( ExpressionExperiment ee, QuantitationType qt, Long ctaId, int startIndex, int endIndexExclusive );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Characteristic getCellTypeAt( ExpressionExperiment ee, QuantitationType qt, String ctaName, int cellIndex );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Characteristic[] getCellTypeAt( ExpressionExperiment ee, QuantitationType qt, String ctaName, int startIndex, int endIndexExclusive );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Category getCellLevelCharacteristicsCategory( ExpressionExperiment ee, CellLevelCharacteristics clc );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Stream<Characteristic> streamCellLevelCharacteristics( ExpressionExperiment ee, CellLevelCharacteristics clc, boolean createNewSession );

    /**
     * Relabel the cell types of an existing set of single-cell vectors.
     *
     * @param newCellTypeLabels                 the new cell types labels, must match the number of cells
     * @param labellingProtocol                 the protocol used to generate the new labelling, or null if unknown
     * @param recreateCellTypeFactorIfNecessary
     * @param ignoreCompatibleFactor
     * @return a new, preferred cell type labelling
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    CellTypeAssignment relabelCellTypes( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension dimension, List<String> newCellTypeLabels, @Nullable Protocol labellingProtocol, @Nullable String description, boolean recreateCellTypeFactorIfNecessary, boolean ignoreCompatibleFactor );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    CellTypeAssignment addCellTypeAssignment( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension dimension, CellTypeAssignment cellTypeAssignment, boolean recreateCellTypeFactorIfNecessary, boolean ignoreCompatibleFactor );

    enum PreferredCellTypeAssignmentChangeOutcome {
        /**
         * No change were made.
         */
        UNCHANGED,
        CELL_TYPE_FACTOR_UNCHANGED,
        /**
         * The factor was left unchanged, but is now misaligned with the preferred cell type assignment.
         */
        CELL_TYPE_FACTOR_UNCHANGED_BUT_MISALIGNED,
        CELL_TYPE_FACTOR_RECREATED,
        CELL_TYPE_FACTOR_REMOVED
    }

    /**
     * @see #changePreferredCellTypeAssignment(ExpressionExperiment, SingleCellDimension, CellTypeAssignment, boolean, boolean)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    PreferredCellTypeAssignmentChangeOutcome changePreferredCellTypeAssignment( ExpressionExperiment ee, QuantitationType quantitationType, CellTypeAssignment newPreferredCta, boolean recreateCellTypeFactorIfNecessary, boolean ignoreCompatibleFactor );

    /**
     * Change the preferred cell type assignment to the given one.
     *
     * @param recreateCellTypeFactorIfNecessary re-create the cell type factor; this is only done if the preferred cell
     *                                          type assignment applies to the preferred single-cell vectors and the
     *                                          current cell type factor is incompatible.
     * @param ignoreCompatibleFactor
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    PreferredCellTypeAssignmentChangeOutcome changePreferredCellTypeAssignment( ExpressionExperiment ee, SingleCellDimension dimension, CellTypeAssignment newPreferredCta, boolean recreateCellTypeFactorIfNecessary, boolean ignoreCompatibleFactor );

    /**
     * @see #clearPreferredCellTypeAssignment(ExpressionExperiment, SingleCellDimension)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    PreferredCellTypeAssignmentChangeOutcome clearPreferredCellTypeAssignment( ExpressionExperiment ee, QuantitationType quantitationType );

    /**
     * Clear the preferred cell type assignment.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    PreferredCellTypeAssignmentChangeOutcome clearPreferredCellTypeAssignment( ExpressionExperiment ee, SingleCellDimension dimension );

    /**
     * Remove the given cell type assignment.
     * <p>
     * If the cell type labelling is preferred and applies to the preferred vectors as per {@link #getPreferredCellTypeAssignment(ExpressionExperiment)}, the cell type factor will be removed.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeCellTypeAssignment( ExpressionExperiment ee, SingleCellDimension scd, CellTypeAssignment cellTypeAssignment );

    /**
     * Remove the given cell type assignment by QT.
     *
     * @see #removeCellTypeAssignment(ExpressionExperiment, SingleCellDimension, CellTypeAssignment)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeCellTypeAssignment( ExpressionExperiment ee, QuantitationType qt, CellTypeAssignment cellTypeAssignment );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeCellTypeAssignmentById( ExpressionExperiment ee, Long ctaId );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeCellTypeAssignmentById( ExpressionExperiment ee, SingleCellDimension dimension, Long ctaId );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    boolean removeCellTypeAssignmentByName( ExpressionExperiment ee, SingleCellDimension dimension, String name );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    long removeAllCellTypeAssignments( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Obtain all the cell type labellings from all single-cell vectors.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment ee );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment expressionExperiment, QuantitationType qt );

    /**
     * Obtain a cell type assignment by ID.
     *
     * @return that cell type assignmente, or null if none is found
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId );


    /**
     * Obtain a cell type assignment by name.
     *
     * @return that cell type assignmente, or null if none is found
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CellTypeAssignment getCellTypeAssignmentWithoutIndices( ExpressionExperiment ee, QuantitationType qt, Long ctaId );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CellTypeAssignment getCellTypeAssignmentWithoutIndices( ExpressionExperiment ee, QuantitationType qt, String ctaName );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<CellTypeAssignment> getCellTypeAssignmentsWithoutIndices( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Obtain a collection of protocols used to assign cell types to single-cell vectors.
     */
    Collection<Protocol> getCellTypeAssignmentProtocols();

    /**
     * Obtain a cell type assignment by a protocol identifier.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<CellTypeAssignment> getCellTypeAssignmentByProtocol( ExpressionExperiment ee, QuantitationType qt, String protocolName );

    /**
     * Obtain the preferred cell type labelling from the preferred single-cell vectors.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<CellTypeAssignment> getPreferredCellTypeAssignment( ExpressionExperiment ee );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<CellTypeAssignment> getPreferredCellTypeAssignment( ExpressionExperiment ee, QuantitationType qt );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<CellTypeAssignment> getPreferredCellTypeAssignmentWithoutIndices( ExpressionExperiment ee );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<CellTypeAssignment> getPreferredCellTypeAssignmentWithoutIndices( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Add new cell-level characteristics.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    CellLevelCharacteristics addCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDimension scd, CellLevelCharacteristics clc );

    /**
     * Remove existing cell-level characteristics.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDimension scd, CellLevelCharacteristics clc );

    /**
     * Remove existing cell-level characteristics by QT.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt, CellLevelCharacteristics clc );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeCellLevelCharacteristicsById( ExpressionExperiment ee, Long clcId );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeCellLevelCharacteristicsById( ExpressionExperiment ee, SingleCellDimension dimension, Long clcId );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    boolean removeCellLevelCharacteristicsByName( ExpressionExperiment ee, SingleCellDimension dimension, String name );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    long removeAllCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt );

    /**
     * @see ExpressionExperimentDao#getCellLevelCharacteristics(ExpressionExperiment)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee );

    /**
     * Obtain CLC for given category.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee, Category category );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CellLevelCharacteristics getCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt, Long id );

    /**
     * Obtain a CLC by name.
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CellLevelCharacteristics getCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt, String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CellLevelCharacteristics getCellLevelCharacteristicsWithoutIndices( ExpressionExperiment ee, QuantitationType qt, Long ctaId );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    CellLevelCharacteristics getCellLevelCharacteristicsWithoutIndices( ExpressionExperiment ee, QuantitationType qt, String clcName );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<CellLevelCharacteristics> getCellLevelCharacteristicsWithoutIndices( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Obtain a mask if one is unambiguously defined for the given experiment and quantitation type.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<CellLevelCharacteristics> getCellLevelMask( ExpressionExperiment expressionExperiment, QuantitationType qt );

    /**
     * Obtain the cell types of a given single-cell dataset.
     * <p>
     * Only the cell types applicable to the preferred single-cell vectors and cell type assignment are returned.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<Characteristic> getCellTypes( ExpressionExperiment ee );

    /**
     * Obtain the cell type factor.
     *
     * @return a cell type factor, or {@link Optional#empty()} if none exist or if there is more than one cell type
     * factor.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Optional<ExperimentalFactor> getCellTypeFactor( ExpressionExperiment ee );

    /**
     * Recreate the cell type factor based on the preferred cell type assignment of the preferred single-cell vectors.
     * <p>
     * Analyses involving the factor are removed and samples mentioning the factor values are updated as per
     * {@link ExperimentalFactorService#remove(ExperimentalFactor)}.
     * <p>
     * Note that the cell type factor will not be deleted if there is more than one such factor present as per
     * {@link #getCellTypeFactor(ExpressionExperiment)}.
     *
     * @param removeExistingIfNecessary if there is already a cell type factor that is incompatible with the preferred
     *                                  cell type assignment, remove it.
     * @param ignoreCompatibleFactor    remove an existing cell type factor even if a compatible cell type factor is
     *                                  found, requires {@code removeExistingIfNecessary} to be true.
     * @return the created cell type factor, or a compatible one if found, or {@code null} if no factor was created or no compatible one was found
     * @throws IllegalStateException if the dataset does not have a preferred cell type labelling for its preferred set
     *                               of single-cell vectors.
     */
    @Nullable
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    ExperimentalFactor createCellTypeFactor( ExpressionExperiment ee, boolean removeExistingIfNecessary, boolean ignoreCompatibleFactor );
}
