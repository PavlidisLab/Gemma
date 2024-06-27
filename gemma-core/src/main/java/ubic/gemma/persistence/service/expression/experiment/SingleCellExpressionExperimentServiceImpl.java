package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.DoubleSingleCellExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataRemovedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ubic.gemma.core.util.ListUtils.validateSparseRangeArray;

@Service
@CommonsLog
public class SingleCellExpressionExperimentServiceImpl implements SingleCellExpressionExperimentService {

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional(readOnly = true)
    public SingleCellExpressionDataMatrix<Double> getSingleCellExpressionDataMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {
        return new DoubleSingleCellExpressionDataMatrix( expressionExperimentDao.getSingleCellDataVectors( expressionExperiment, quantitationType ) );
    }

    @Override
    @Transactional
    public int addSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( !ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s already have vectors for the quantitation type: %s; use replaceSingleCellDataVectors() to replace existing vectors.",
                        ee, quantitationType ) );
        validateSingleCellDataVectors( ee, quantitationType, vectors );
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
        // make all other single-cell QTs non-preferred
        if ( quantitationType.getIsPreferred() ) {
            for ( QuantitationType qt : ee.getQuantitationTypes() ) {
                if ( qt.getIsPreferred() ) {
                    log.info( "Setting " + qt + " to non-preferred since we're adding a new set of preferred vectors to " + ee );
                    qt.setIsPreferred( false );
                    break; // there is at most 1 set of preferred vectors
                }
            }
        }
        ee.getQuantitationTypes().add( quantitationType );
        expressionExperimentDao.update( ee ); // will take care of creating vectors
        if ( quantitationType.getIsPreferred() && scdCreated ) {
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
                String.format( "Added %d vectors for %s with dimension %s", numVectorsAdded, quantitationType, scd ) );
        return numVectorsAdded;
    }

    @Override
    @Transactional
    public int replaceSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
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
            numVectorsRemoved = removeSingleCellVectorsAndDimensionIfNecessary( ee, quantitationType, scdCreated ? null : vectors );
        } else {
            log.warn( "No vectors with the quantitation type: " + quantitationType );
            numVectorsRemoved = 0;
        }
        log.info( String.format( "Adding %d single-cell vectors to %s for %s", vectors.size(), ee, quantitationType ) );
        ee.getSingleCellExpressionDataVectors().addAll( vectors );
        expressionExperimentDao.update( ee );
        if ( quantitationType.getIsPreferred() && scdCreated ) {
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

    private void validateSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
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
        validateSingleCellDimension( ee, singleCellDimension );
        Assert.isTrue( vectors.stream().allMatch( v -> v.getSingleCellDimension() == singleCellDimension ),
                "All vectors must share the same dimension: " + singleCellDimension );
        Assert.isTrue( singleCellDimension.getId() == null
                        || ee.getSingleCellExpressionDataVectors().stream().map( SingleCellExpressionDataVector::getSingleCellDimension ).anyMatch( singleCellDimension::equals ),
                singleCellDimension + " is persistent, but does not belong any single-cell vector of this dataset: " + ee );
        // we only support double for storage
        // TODO: support counting data using integers too
        Assert.isTrue( quantitationType.getRepresentation() == PrimitiveType.DOUBLE,
                "Only double is supported for single-cell data vector storage." );
        int maximumDataLength = 8 * singleCellDimension.getCellIds().size();
        for ( SingleCellExpressionDataVector vector : vectors ) {
            Assert.isTrue( vector.getData().length <= maximumDataLength,
                    String.format( "All vector must have at most %d bytes.", maximumDataLength ) );
            // 1. monotonous, 2. distinct, 3. within the range of the cell IDs
            int lastI = -1;
            for ( int i : vector.getDataIndices() ) {
                Assert.isTrue( i > lastI );
                Assert.isTrue( i < maximumDataLength );
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
            removedVectors = removeSingleCellVectorsAndDimensionIfNecessary( ee, quantitationType, null );
        } else {
            scd = null;
            log.warn( "No vectors with the quantitation type: " + quantitationType );
            removedVectors = 0;
        }
        ee.getQuantitationTypes().remove( quantitationType );
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
     */
    private int removeSingleCellVectorsAndDimensionIfNecessary( ExpressionExperiment ee,
            QuantitationType quantitationType,
            @Nullable Collection<SingleCellExpressionDataVector> additionalVectors ) {
        Set<SingleCellExpressionDataVector> vectors = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( quantitationType ) ).collect( Collectors.toSet() );
        log.info( String.format( "Removing %d single-cell vectors for %s...", vectors.size(), ee ) );
        int removedVectors = expressionExperimentDao.removeSingleCellDataVectors( ee, quantitationType, false );
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
    public List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee ) {
        return expressionExperimentDao.getSingleCellDimensions( ee );
    }

    @Override
    @Transactional
    public CellTypeAssignment relabelCellTypes( ExpressionExperiment ee, SingleCellDimension dimension, List<String> newCellTypeLabels, @Nullable Protocol protocol, @Nullable String description ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( dimension.getId(), "Single-cell dimension must be persistent." );
        Assert.isTrue( ee.getBioAssays().containsAll( dimension.getBioAssays() ), "Single-cell dimension does not belong to the dataset." );
        CellTypeAssignment labelling = new CellTypeAssignment();
        labelling.setPreferred( true );
        labelling.setProtocol( protocol );
        labelling.setDescription( description );
        int[] ct = new int[dimension.getCellIds().size()];
        List<String> labels = newCellTypeLabels.stream().sorted().distinct().collect( Collectors.toList() );
        for ( int i = 0; i < ct.length; i++ ) {
            ct[i] = Collections.binarySearch( labels, newCellTypeLabels.get( i ) );
        }
        labelling.setCellTypeIndices( ct );
        labelling.setCellTypes( labels.stream()
                .map( l -> Characteristic.Factory.newInstance( Categories.CELL_TYPE, l, null ) )
                .collect( Collectors.toList() ) );
        labelling.setNumberOfCellTypes( labels.size() );
        expressionExperimentDao.addCellTypeAssignment( ee, dimension, labelling );
        validateSingleCellDimension( ee, dimension );
        log.info( "Relabelled single-cell vectors for " + ee + " with: " + labelling );

        // checking labelling.isPreferred() is not enough, the labelling might apply to non-preferred vectors
        if ( labelling.equals( getPreferredCellTypeAssignment( ee ) ) ) {
            log.info( "New labels are preferred and also apply to preferred single-cell vectors, recreating the cell type factor..." );
            recreateCellTypeFactor( ee, labelling );
        }

        return labelling;
    }

    @Override
    @Transactional
    public void removeCellTypeLabels( ExpressionExperiment ee, SingleCellDimension dimension, CellTypeAssignment cellTypeAssignment ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( dimension.getId(), "Single-cell dimension must be persistent." );
        Assert.isTrue( ee.getBioAssays().containsAll( dimension.getBioAssays() ), "Single-cell dimension does not belong to the dataset." );
        Assert.isTrue( dimension.getCellTypeAssignments().contains( cellTypeAssignment ),
                "The supplied labelling does not belong to the dimension." );
        boolean alsoRemoveFactor = cellTypeAssignment.equals( getPreferredCellTypeAssignment( ee ) );
        dimension.getCellTypeAssignments().remove( cellTypeAssignment );
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
    public CellTypeAssignment getPreferredCellTypeAssignment( ExpressionExperiment ee ) {
        return expressionExperimentDao.getPreferredCellTypeAssignment( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> getCellTypes( ExpressionExperiment ee ) {
        return expressionExperimentDao.getCellTypes( ee );
    }

    /**
     * Validate single-cell dimension.
     */
    private void validateSingleCellDimension( ExpressionExperiment ee, SingleCellDimension scbad ) {
        Assert.isTrue( !scbad.getCellIds().isEmpty(), "There must be at least one cell ID." );
        for ( int i = 0; i < scbad.getBioAssays().size(); i++ ) {
            List<String> sampleCellIds = scbad.getCellIdsBySample( i );
            Assert.isTrue( sampleCellIds.stream().distinct().count() == sampleCellIds.size(),
                    "Cell IDs must be unique for each sample." );
        }
        Assert.isTrue( scbad.getCellIds().size() == scbad.getNumberOfCells(),
                "The number of cell IDs must match the number of cells." );
        Assert.isTrue( scbad.getCellTypeAssignments().stream().filter( CellTypeAssignment::isPreferred ).count() <= 1,
                "There must be at most one preferred cell type labelling." );
        for ( CellTypeAssignment labelling : scbad.getCellTypeAssignments() ) {
            Assert.notNull( labelling.getNumberOfCellTypes() );
            Assert.notNull( labelling.getCellTypes() );
            Assert.isTrue( labelling.getCellTypeIndices().length == scbad.getCellIds().size(),
                    "The number of cell types must match the number of cell IDs." );
            int numberOfCellTypeLabels = labelling.getCellTypes().size();
            Assert.isTrue( numberOfCellTypeLabels > 0,
                    "There must be at least one cell type label declared in the cellTypeLabels collection." );
            Assert.isTrue( labelling.getCellTypes().stream().distinct().count() == labelling.getCellTypes().size(),
                    "Cell type labels must be unique." );
            Assert.isTrue( numberOfCellTypeLabels == labelling.getNumberOfCellTypes(),
                    "The number of cell types must match the number of values the cellTypeLabels collection." );
            for ( int k : labelling.getCellTypeIndices() ) {
                Assert.isTrue( k >= 0 && k < numberOfCellTypeLabels,
                        String.format( "Cell type vector values must be within the [%d, %d[ range.", 0, numberOfCellTypeLabels ) );
            }
        }
        Assert.isTrue( !scbad.getBioAssays().isEmpty(), "There must be at least one BioAssay." );
        Assert.isTrue( ee.getBioAssays().containsAll( scbad.getBioAssays() ), "Not all supplied BioAssays belong to " + ee );
        validateSparseRangeArray( scbad.getBioAssays(), scbad.getBioAssaysOffset(), scbad.getNumberOfCells() );
    }


    @Override
    @Transactional
    public ExperimentalFactor recreateCellTypeFactor( ExpressionExperiment ee ) {
        CellTypeAssignment ctl = getPreferredCellTypeAssignment( ee );
        Assert.notNull( ctl, "There must be a preferred cell type labelling for " + ee + " to update the cell type factor." );
        return recreateCellTypeFactor( ee, ctl );
    }

    private ExperimentalFactor recreateCellTypeFactor( ExpressionExperiment ee, CellTypeAssignment ctl ) {
        removeCellTypeFactorIfExists( ee );
        // create a new cell type factor
        ExperimentalFactor cellTypeFactor = ExperimentalFactor.Factory.newInstance();
        cellTypeFactor.setType( FactorType.CATEGORICAL );
        cellTypeFactor.setCategory( Characteristic.Factory.newInstance( Categories.CELL_TYPE ) );
        cellTypeFactor.setExperimentalDesign( ee.getExperimentalDesign() );
        ee.getExperimentalDesign().getExperimentalFactors().add( cellTypeFactor );
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

        return experimentalFactorService.create( cellTypeFactor );
    }

    private void removeCellTypeFactorIfExists( ExpressionExperiment ee ) {
        ExperimentalFactor existingCellTypeFactor = ee.getExperimentalDesign().getExperimentalFactors().stream()
                .filter( ef -> ef.getCategory() != null )
                .filter( ef -> CharacteristicUtils.equals( ef.getCategory().getCategory(), ef.getCategory().getCategoryUri(),
                        Categories.CELL_TYPE.getCategory(), Categories.CELL_TYPE.getCategoryUri() ) )
                .findFirst()
                .orElse( null );
        if ( existingCellTypeFactor != null ) {
            // this will remove analysis involving the factor and also sample-fv associations
            log.info( "Removing existing cell type factor for " + ee );
            experimentalFactorService.remove( existingCellTypeFactor );
        } else {
            log.info( "There's no cell type factor for " + ee );
        }
    }
}
