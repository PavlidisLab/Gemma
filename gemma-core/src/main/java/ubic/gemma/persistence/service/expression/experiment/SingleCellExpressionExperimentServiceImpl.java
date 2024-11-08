package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.DoubleSingleCellExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ubic.gemma.persistence.service.expression.experiment.SingleCellSparsityMetrics.*;

@Service
@CommonsLog
public class SingleCellExpressionExperimentServiceImpl implements SingleCellExpressionExperimentService {

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithSingleCellVectors( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            expressionExperimentDao.thawLite( ee );
            Hibernate.initialize( ee.getSingleCellExpressionDataVectors() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType ) {
        return expressionExperimentDao.getSingleCellDataVectors( ee, quantitationType );
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize ) {
        return expressionExperimentDao.streamSingleCellDataVectors( ee, quantitationType, fetchSize );
    }

    @Override
    @Transactional(readOnly = true)
    public long getNumberOfSingleCellDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getNumberOfSingleCellDataVectors( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public long getNumberOfNonZeroes( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getNumberOfNonZeroes( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssay, Long> getNumberOfNonZeroesBySample( ExpressionExperiment ee, QuantitationType qt, int fetchSize ) {
        return expressionExperimentDao.getNumberOfNonZeroesBySample( ee, qt, fetchSize );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection<SingleCellExpressionDataVector>> getPreferredSingleCellDataVectors( ExpressionExperiment ee ) {
        QuantitationType qt = expressionExperimentDao.getPreferredSingleCellQuantitationType( ee );
        if ( qt == null ) {
            return Optional.empty();
        }
        return Optional.of( expressionExperimentDao.getSingleCellDataVectors( ee, qt ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getSingleCellQuantitationTypes( ExpressionExperiment ee ) {
        return expressionExperimentDao.getSingleCellQuantitationTypes( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuantitationType> getPreferredSingleCellQuantitationType( ExpressionExperiment ee ) {
        return Optional.ofNullable( expressionExperimentDao.getPreferredSingleCellQuantitationType( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public SingleCellExpressionDataMatrix<Double> getSingleCellExpressionDataMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {
        List<SingleCellExpressionDataVector> vectors = expressionExperimentDao.getSingleCellDataVectors( expressionExperiment, quantitationType );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vector for " + quantitationType + " in " + expressionExperiment );
        }
        return new DoubleSingleCellExpressionDataMatrix( vectors );
    }

    @Override
    @Transactional
    public int addSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors, String details ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.isTrue( !ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s already have vectors for the quantitation type: %s; use replaceSingleCellDataVectors() to replace existing vectors.",
                        ee, quantitationType ) );
        validateSingleCellDataVectors( ee, quantitationType, vectors );
        if ( quantitationType.getId() == null ) {
            log.info( "Creating " + quantitationType + "..." );
            quantitationType = quantitationTypeService.create( quantitationType, SingleCellExpressionDataVector.class );
        }
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        boolean scdCreated = false;
        if ( scd.getId() == null ) {
            log.info( "Creating a new single-cell dimension for " + ee + ": " + scd );
            expressionExperimentDao.createSingleCellDimension( ee, scd );
            scdCreated = true;
        }
        for ( SingleCellExpressionDataVector v : vectors ) {
            v.setExpressionExperiment( ee );
        }
        int previousSize = ee.getSingleCellExpressionDataVectors().size();
        log.info( String.format( "Adding %d single-cell vectors to %s for %s", vectors.size(), ee, quantitationType ) );
        ee.getSingleCellExpressionDataVectors().addAll( vectors );
        int numVectorsAdded = ee.getSingleCellExpressionDataVectors().size() - previousSize;
        ee.getQuantitationTypes().add( quantitationType );
        applyPreferredSingleCellVectors( ee, quantitationType );
        if ( quantitationType.getIsSingleCellPreferred() ) {
            applyBioAssaySparsityMetrics( ee, scd, vectors );
        }
        expressionExperimentDao.update( ee ); // will take care of creating vectors
        if ( quantitationType.getIsSingleCellPreferred() && scdCreated ) {
            CellTypeAssignment preferredLabelling = scd.getCellTypeAssignments().stream().filter( CellTypeAssignment::isPreferred ).findFirst().orElse( null );
            if ( preferredLabelling != null ) {
                log.info( "New single-cell preferred vectors were added, recreating the cell type factor." );
                recreateCellTypeFactor( ee, preferredLabelling );
            } else {
                log.info( "New single-cell preferred vectors do not have cell type labelling, removing any existing cell type factor..." );
                removeCellTypeFactorIfExists( ee );
            }
        }
        auditTrailService.addUpdateEvent( ee, DataAddedEvent.class,
                String.format( "Added %d vectors for %s with dimension %s.", numVectorsAdded, quantitationType, scd ), details );
        return numVectorsAdded;
    }

    @Override
    @Transactional
    public int replaceSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors, String details ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s does not have the quantitation type: %s; use addSingleCellDataVectors() to add new vectors instead.",
                        ee, quantitationType ) );
        validateSingleCellDataVectors( ee, quantitationType, vectors );
        boolean scdCreated = false;
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        if ( scd.getId() == null ) {
            log.info( "Creating a new single-cell dimension for " + ee + ": " + scd );
            expressionExperimentDao.createSingleCellDimension( ee, scd );
            scdCreated = true;
        }
        Set<SingleCellExpressionDataVector> vectorsToBeReplaced = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( quantitationType ) ).collect( Collectors.toSet() );
        for ( SingleCellExpressionDataVector v : vectors ) {
            v.setExpressionExperiment( ee );
        }
        int numVectorsRemoved;
        if ( !vectorsToBeReplaced.isEmpty() ) {
            // if the SCD was created, we do not need to check additional vectors for removing the existing one
            numVectorsRemoved = removeSingleCellVectorsAndDimensionIfNecessary( ee, quantitationType, scdCreated ? null : vectors, false );
        } else {
            log.warn( "No vectors with the quantitation type: " + quantitationType );
            numVectorsRemoved = 0;
        }
        log.info( String.format( "Adding %d single-cell vectors to %s for %s", vectors.size(), ee, quantitationType ) );
        ee.getSingleCellExpressionDataVectors().addAll( vectors );
        applyPreferredSingleCellVectors( ee, quantitationType );
        if ( quantitationType.getIsSingleCellPreferred() ) {
            applyBioAssaySparsityMetrics( ee, scd, vectors );
        }
        expressionExperimentDao.update( ee );
        if ( quantitationType.getIsSingleCellPreferred() && scdCreated ) {
            CellTypeAssignment preferredLabelling = scd.getCellTypeAssignments().stream().filter( CellTypeAssignment::isPreferred ).findFirst().orElse( null );
            if ( preferredLabelling != null ) {
                log.info( "Preferred single-cell vectors were replaced, recreating the cell type factor." );
                recreateCellTypeFactor( ee, preferredLabelling );
            } else {
                log.info( "Preferred single-cell vectors do not have cell type labelling, removing any existing cell type factor..." );
                removeCellTypeFactorIfExists( ee );
            }
        }
        auditTrailService.addUpdateEvent( ee, DataReplacedEvent.class,
                String.format( "Replaced %d vectors with %d vectors for %s with dimension %s.", numVectorsRemoved, vectors.size(), quantitationType, scd ) );
        return numVectorsRemoved;
    }

    /**
     * Make all other single-cell QTs non-preferred
     */
    private void applyPreferredSingleCellVectors( ExpressionExperiment ee, QuantitationType quantitationType ) {
        if ( !quantitationType.getIsSingleCellPreferred() ) {
            return;
        }
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
            if ( qt.getIsSingleCellPreferred() && !qt.equals( quantitationType ) ) {
                log.info( "Setting " + qt + " to non-preferred since we're adding a new set of preferred vectors to " + ee );
                qt.setIsSingleCellPreferred( false );
                break; // there is at most one set of preferred SC vectors
            }
        }
    }

    /**
     * Apply various single-cell sparsity metrics to the bioassays.
     */
    private void applyBioAssaySparsityMetrics( ExpressionExperiment ee, SingleCellDimension dimension, Collection<SingleCellExpressionDataVector> vectors ) {
        for ( BioAssay ba : ee.getBioAssays() ) {
            int sampleIndex = dimension.getBioAssays().indexOf( ba );
            if ( sampleIndex != -1 ) {
                ba.setNumberOfCells( getNumberOfCells( vectors, sampleIndex, null ) );
                ba.setNumberOfDesignElements( getNumberOfDesignElements( vectors, sampleIndex, null ) );
                ba.setNumberOfCellsByDesignElements( getNumberOfCellsByDesignElements( vectors, sampleIndex, null ) );
                log.info( String.format( "Sparsity metrics for %s: %d cells, %d design elements, %d cells by design elements.",
                        ba, ba.getNumberOfCells(), ba.getNumberOfDesignElements(), ba.getNumberOfCellsByDesignElements() ) );
            } else {
                log.warn( ba + " is not used in " + dimension + ", the single-cell sparsity metrics will be set to null." );
                ba.setNumberOfCells( null );
                ba.setNumberOfDesignElements( null );
                ba.setNumberOfCellsByDesignElements( null );
            }
        }
    }

    private void clearBioAssaySparsityMetrics( ExpressionExperiment ee ) {
        for ( BioAssay ba : ee.getBioAssays() ) {
            ba.setNumberOfCells( null );
            ba.setNumberOfDesignElements( null );
            ba.setNumberOfCellsByDesignElements( null );
        }
    }

    private void validateSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.isTrue( !vectors.isEmpty(), "At least one single-cell vector has to be supplied; use removeSingleCellDataVectors() to remove vectors instead." );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getExpressionExperiment() == null || v.getExpressionExperiment().equals( ee ) ),
                "Some of the vectors belong to other expression experiments." );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getQuantitationType() == quantitationType ),
                "All vectors must have the same quantitation type: " + quantitationType );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getDesignElement() != null && v.getDesignElement().getId() != null ),
                "All vectors must have a persistent design element." );
        // TODO: allow vectors from multiple platforms
        CompositeSequence element = vectors.iterator().next().getDesignElement();
        ArrayDesign platform = element.getArrayDesign();
        Assert.isTrue( vectors.stream().allMatch( v -> v.getDesignElement().getArrayDesign().equals( platform ) ),
                "All vectors must have a persistent design element from the same platform." );
        SingleCellDimension singleCellDimension = vectors.iterator().next().getSingleCellDimension();
        Assert.isTrue( vectors.stream().allMatch( v -> v.getSingleCellDimension() == singleCellDimension ),
                "All vectors must share the same dimension: " + singleCellDimension );
        Assert.isTrue( singleCellDimension.getId() == null
                        || ee.getSingleCellExpressionDataVectors().stream().map( SingleCellExpressionDataVector::getSingleCellDimension ).anyMatch( singleCellDimension::equals ),
                singleCellDimension + " is persistent, but does not belong any single-cell vector of this dataset: " + ee );
        // make sure that the platform used by the BAs is the same as the platform for the design elements
        Assert.isTrue( singleCellDimension.getBioAssays().stream().allMatch( ba -> ba.getArrayDesignUsed().equals( platform ) ),
                "All the BioAssays must use a platform that match that of the vectors: " + platform );
        // we only support double for storage
        // TODO: support counting data using integers too
        Assert.isTrue( quantitationType.getRepresentation() == PrimitiveType.DOUBLE,
                "Only double is supported for single-cell data vector storage." );
        int numCells = singleCellDimension.getCellIds().size();
        int maximumDataLength = 8 * numCells;
        for ( SingleCellExpressionDataVector vector : vectors ) {
            Assert.isTrue( vector.getData().length <= maximumDataLength,
                    String.format( "All vector must have at most %d bytes.", maximumDataLength ) );
            // 1. monotonous, 2. distinct, 3. within the range of the cell IDs
            int lastI = -1;
            for ( int i : vector.getDataIndices() ) {
                Assert.isTrue( i > lastI );
                Assert.isTrue( i < numCells );
            }
        }
    }

    @Override
    @Transactional
    public int removeSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s does not have the quantitation type %s.", ee, quantitationType ) );
        Set<SingleCellExpressionDataVector> vectors = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( quantitationType ) ).collect( Collectors.toSet() );
        SingleCellDimension scd;
        int removedVectors;
        if ( !vectors.isEmpty() ) {
            scd = vectors.iterator().next().getSingleCellDimension();
            removedVectors = removeSingleCellVectorsAndDimensionIfNecessary( ee, quantitationType, null, true );
        } else {
            scd = null;
            log.warn( "No vectors with the quantitation type: " + quantitationType );
            removedVectors = 0;
        }
        if ( quantitationType.getIsSingleCellPreferred() ) {
            log.info( "Removing preferred single-cell vectors, clearing sparsity metrics..." );
            clearBioAssaySparsityMetrics( ee );
        }
        expressionExperimentDao.update( ee );
        if ( removedVectors > 0 ) {
            auditTrailService.addUpdateEvent( ee, DataRemovedEvent.class,
                    String.format( "Removed %d vectors for %s with dimension %s.", removedVectors, quantitationType, scd ) );
        }
        return removedVectors;
    }

    /**
     * Remove the given single-cell vectors and their corresponding single-cell dimension if necessary.
     *
     * @param ee                the experiment to remove the vectors from.
     * @param additionalVectors additional vectors to check if the single-cell dimension is still in use (i.e. vectors that are in the process of being added).
     * @param deleteQt          if true, also detach and delete the QT
     */
    private int removeSingleCellVectorsAndDimensionIfNecessary( ExpressionExperiment ee,
            QuantitationType quantitationType,
            @Nullable Collection<SingleCellExpressionDataVector> additionalVectors,
            boolean deleteQt ) {
        Set<SingleCellExpressionDataVector> vectors = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( quantitationType ) ).collect( Collectors.toSet() );
        log.info( String.format( "Removing %d single-cell vectors for %s...", vectors.size(), ee ) );
        int removedVectors = expressionExperimentDao.removeSingleCellDataVectors( ee, quantitationType, deleteQt );
        // check if SCD is still in use else remove it
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        boolean scdStillUsed = false;
        for ( SingleCellExpressionDataVector v : ee.getSingleCellExpressionDataVectors() ) {
            if ( v.getSingleCellDimension().equals( scd ) ) {
                scdStillUsed = true;
                break;
            }
        }
        if ( !scdStillUsed && additionalVectors != null ) {
            for ( SingleCellExpressionDataVector v : additionalVectors ) {
                if ( v.getSingleCellDimension().equals( scd ) ) {
                    scdStillUsed = true;
                    break;
                }
            }
        }
        if ( !scdStillUsed ) {
            log.info( "Removing unused single-cell dimension " + scd + " for " + ee );
            expressionExperimentDao.deleteSingleCellDimension( ee, scd );

        }
        return removedVectors;
    }

    @Override
    @Transactional(readOnly = true)
    public SingleCellDimension getSingleCellDimensionWithCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt ) {
        SingleCellDimension scd = expressionExperimentDao.getSingleCellDimension( ee, qt );
        if ( scd == null ) {
            throw new IllegalStateException( qt + " does not have an associated single-cell dimension." );
        }
        Hibernate.initialize( scd.getCellTypeAssignments() );
        Hibernate.initialize( scd.getCellLevelCharacteristics() );
        return scd;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee ) {
        return expressionExperimentDao.getSingleCellDimensions( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SingleCellDimension> getPreferredSingleCellDimension( ExpressionExperiment ee ) {
        return Optional.ofNullable( expressionExperimentDao.getPreferredSingleCellDimension( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SingleCellDimension> getPreferredSingleCellDimensionWithCellLevelCharacteristics( ExpressionExperiment ee ) {
        return getPreferredSingleCellDimension( ee )
                .map( scd -> {
                    Hibernate.initialize( scd.getCellTypeAssignments() );
                    Hibernate.initialize( scd.getCellLevelCharacteristics() );
                    return scd;
                } );
    }

    @Override
    @Transactional
    public CellTypeAssignment relabelCellTypes( ExpressionExperiment ee, SingleCellDimension dimension, List<String> newCellTypeLabels, @Nullable Protocol protocol, @Nullable String description ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( dimension.getId(), "Single-cell dimension must be persistent." );
        Assert.isTrue( ee.getBioAssays().containsAll( dimension.getBioAssays() ), "Single-cell dimension does not belong to the dataset." );
        CellTypeAssignment cta = new CellTypeAssignment();
        cta.setPreferred( true );
        cta.setProtocol( protocol );
        cta.setDescription( description );
        int[] ct = new int[dimension.getCellIds().size()];
        List<String> labels = newCellTypeLabels.stream().sorted().distinct().collect( Collectors.toList() );
        for ( int i = 0; i < ct.length; i++ ) {
            ct[i] = Collections.binarySearch( labels, newCellTypeLabels.get( i ) );
        }
        cta.setCellTypeIndices( ct );
        cta.setCellTypes( labels.stream()
                .map( l -> Characteristic.Factory.newInstance( Categories.CELL_TYPE, l, null ) )
                .collect( Collectors.toList() ) );
        cta.setNumberOfCellTypes( labels.size() );
        return createCellTypeAssignment( ee, dimension, cta );
    }

    @Override
    @Transactional
    public CellTypeAssignment addCellTypeAssignment( ExpressionExperiment ee, SingleCellDimension dimension, CellTypeAssignment cta ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( dimension.getId(), "Single-cell dimension must be persistent." );
        Assert.isTrue( ee.getBioAssays().containsAll( dimension.getBioAssays() ), "Single-cell dimension does not belong to the dataset." );
        Assert.isNull( cta.getId(), "Cell type assignment must be non-persistent." );
        return createCellTypeAssignment( ee, dimension, cta );
    }

    private CellTypeAssignment createCellTypeAssignment( ExpressionExperiment ee, SingleCellDimension dimension, CellTypeAssignment cta ) {
        Assert.isTrue( !dimension.getCellTypeAssignments().contains( cta ), dimension + " already has a cell type assignment matching " + cta + "." );
        if ( cta.isPreferred() ) {
            for ( CellTypeAssignment a : dimension.getCellTypeAssignments() ) {
                if ( a.isPreferred() ) {
                    log.info( "Marking existing cell type assignment as non-preferred, a new preferred assignment will be added." );
                    a.setPreferred( false );
                    break;
                }
            }
        }
        dimension.getCellTypeAssignments().add( cta );
        expressionExperimentDao.updateSingleCellDimension( ee, dimension );
        auditTrailService.addUpdateEvent( ee, CellTypeAssignmentAddedEvent.class, "Added " + cta + " to " + dimension + "." );
        log.info( "Relabelled single-cell vectors for " + ee + " with: " + cta );

        if ( cta.isPreferred() ) {
            // checking labelling.isPreferred() is not enough, the labelling might apply to non-preferred vectors
            if ( cta.equals( expressionExperimentDao.getPreferredCellTypeAssignment( ee ) ) ) {
                log.info( "New labels are preferred and also apply to preferred single-cell vectors, recreating the cell type factor..." );
                recreateCellTypeFactor( ee, cta );
            } else {
                log.info( "New labels are preferred but do not apply to preferred single-cell vectors, the cell type factor will not be recreated." );
            }
        }

        return cta;
    }

    @Override
    @Transactional
    public void removeCellTypeAssignment( ExpressionExperiment ee, SingleCellDimension dimension, CellTypeAssignment cellTypeAssignment ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( dimension.getId(), "Single-cell dimension must be persistent." );
        Assert.isTrue( ee.getBioAssays().containsAll( dimension.getBioAssays() ), "Single-cell dimension does not belong to the dataset." );
        Assert.isTrue( dimension.getCellTypeAssignments().contains( cellTypeAssignment ),
                "The supplied labelling does not belong to the dimension." );
        boolean alsoRemoveFactor = getPreferredCellTypeAssignment( ee ).map( cellTypeAssignment::equals ).orElse( false );
        if ( !dimension.getCellTypeAssignments().remove( cellTypeAssignment ) ) {
            throw new IllegalArgumentException( cellTypeAssignment + " is not associated to " + dimension );
        }
        expressionExperimentDao.updateSingleCellDimension( ee, dimension );
        auditTrailService.addUpdateEvent( ee, CellTypeAssignmentRemovedEvent.class, "Removed " + cellTypeAssignment + " from " + dimension + "." );
        if ( alsoRemoveFactor ) {
            log.info( "The preferred cell type labels have been removed, removing the cell type factor..." );
            removeCellTypeFactorIfExists( ee );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment ee ) {
        return expressionExperimentDao.getCellTypeAssignments( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CellTypeAssignment> getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId ) {
        return Optional.ofNullable( expressionExperimentDao.getCellTypeAssignment( expressionExperiment, qt, ctaId ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CellTypeAssignment> getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName ) {
        return Optional.ofNullable( expressionExperimentDao.getCellTypeAssignment( expressionExperiment, qt, ctaName ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CellTypeAssignment> getPreferredCellTypeAssignment( ExpressionExperiment ee ) {
        return Optional.ofNullable( expressionExperimentDao.getPreferredCellTypeAssignment( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CellTypeAssignment> getPreferredCellTypeAssignment( ExpressionExperiment ee, QuantitationType qt ) {
        return Optional.ofNullable( expressionExperimentDao.getPreferredCellTypeAssignment( ee ) );
    }

    @Override
    @Transactional
    public CellLevelCharacteristics addCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDimension scd, CellLevelCharacteristics clc ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( scd.getId(), "Dimension must be persistent." );
        Assert.isNull( clc.getId(), "Cell-level characteristics must be transient." );
        Assert.isTrue( !scd.getCellLevelCharacteristics().contains( clc ), scd + " already has a cell-level characteristics matching " + clc + "." );
        scd.getCellLevelCharacteristics().add( clc );
        expressionExperimentDao.updateSingleCellDimension( ee, scd );
        auditTrailService.addUpdateEvent( ee, CellLevelCharacteristicsAddedEvent.class, "Added " + clc + " to " + scd + "." );
        return clc;
    }

    @Override
    @Transactional
    public void removeCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDimension scd, CellLevelCharacteristics clc ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( scd.getId(), "Dimension must be persistent." );
        Assert.isNull( clc.getId(), "Cell-level characteristics must be persistent." );
        if ( !scd.getCellLevelCharacteristics().remove( clc ) ) {
            throw new IllegalArgumentException( clc + " is not associated to " + scd );
        }
        expressionExperimentDao.updateSingleCellDimension( ee, scd );
        auditTrailService.addUpdateEvent( ee, CellLevelCharacteristicsRemovedEvent.class, "Removed " + clc + " from " + scd + "." );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee ) {
        return expressionExperimentDao.getCellLevelCharacteristics( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment ee, Category category ) {
        return expressionExperimentDao.getCellLevelCharacteristics( ee, category );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> getCellTypes( ExpressionExperiment ee ) {
        return expressionExperimentDao.getCellTypes( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentalFactor> getCellTypeFactor( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() == null ) {
            log.warn( ee + " does not have an experimental design, returning null for the cell type factor." );
            return Optional.empty();
        }
        Set<ExperimentalFactor> candidates = ee.getExperimentalDesign().getExperimentalFactors().stream()
                .filter( ef -> ef.getCategory() != null )
                .filter( ef -> CharacteristicUtils.hasCategory( ef.getCategory(), Categories.CELL_TYPE ) )
                .collect( Collectors.toSet() );
        if ( candidates.isEmpty() ) {
            return Optional.empty();
        } else if ( candidates.size() > 1 ) {
            throw new IllegalStateException( "There is more than one cell type factor in " + ee + "." );
        } else {
            return Optional.of( candidates.iterator().next() );
        }
    }

    @Override
    @Transactional
    public ExperimentalFactor recreateCellTypeFactor( ExpressionExperiment ee ) {
        return getPreferredCellTypeAssignment( ee )
                .map( ctl -> recreateCellTypeFactor( ee, ctl ) )
                .orElseThrow( () -> new IllegalStateException( "There must be a preferred cell type labelling for " + ee + " to update the cell type factor." ) );
    }

    private ExperimentalFactor recreateCellTypeFactor( ExpressionExperiment ee, CellTypeAssignment ctl ) {
        Assert.notNull( ee.getExperimentalDesign(), ee + " does not have an experimental design, cannot re-create the cell type factor." );
        removeCellTypeFactorIfExists( ee );
        // create a new cell type factor
        ExperimentalFactor cellTypeFactor = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL, Categories.CELL_TYPE );
        cellTypeFactor.setDescription( "Cell type factor pre-populated from " + ctl + "." );
        cellTypeFactor.setExperimentalDesign( ee.getExperimentalDesign() );
        for ( Characteristic ct : ctl.getCellTypes() ) {
            FactorValue fv = new FactorValue();
            Statement s = new Statement();
            s.setCategory( ct.getCategory() );
            s.setCategoryUri( ct.getCategoryUri() );
            s.setSubject( ct.getValue() );
            s.setSubjectUri( ct.getValueUri() );
            fv.getCharacteristics().add( s );
            fv.setExperimentalFactor( cellTypeFactor );
            cellTypeFactor.getFactorValues().add( fv );
        }
        cellTypeFactor = experimentalFactorService.create( cellTypeFactor );
        log.info( "Created cell type factor " + cellTypeFactor + " from " + ctl );
        ee.getExperimentalDesign().getExperimentalFactors().add( cellTypeFactor );
        experimentalDesignService.update( ee.getExperimentalDesign() );
        auditTrailService.addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class,
                String.format( "Created a cell type factor %s from preferred cell type assignment %s.", cellTypeFactor, ctl ) );
        return cellTypeFactor;
    }

    private void removeCellTypeFactorIfExists( ExpressionExperiment ee ) {
        ExperimentalFactor existingCellTypeFactor = getCellTypeFactor( ee ).orElse( null );
        if ( existingCellTypeFactor != null ) {
            // this will remove analysis involving the factor and also sample-fv associations
            log.info( "Removing existing cell type factor for " + ee + ": " + existingCellTypeFactor );
            experimentalFactorService.remove( existingCellTypeFactor );
            auditTrailService.addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class,
                    String.format( "Removed the cell type factor %s.", existingCellTypeFactor ) );
        } else {
            log.info( "There's no cell type factor for " + ee );
        }
    }
}
