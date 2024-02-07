package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataRemovedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import javax.annotation.Nullable;
import java.util.Collection;
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
    private AuditTrailService auditTrailService;

    /**
     * @deprecated do not use this, it's only meant as a workaround for deleting single-cell vectors
     */
    @Autowired
    @Deprecated
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    public void addSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( !ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s already have vectors for the quantitation type: %s; use replaceSingleCellDataVectors() to replace existing vectors.",
                        ee, quantitationType ) );
        validateSingleCellDataVectors( ee, quantitationType, vectors );
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        if ( scd.getId() == null ) {
            log.info( "Creating a new single-cell dimension for " + ee + ": " + scd );
            expressionExperimentDao.createSingleCellDimension( ee, scd );
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
        auditTrailService.addUpdateEvent( ee, DataAddedEvent.class,
                String.format( "Added %d vectors for %s with dimension %s", numVectorsAdded, quantitationType, scd ) );
    }

    @Override
    @Transactional
    public void replaceSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors ) {
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
        int previousSize = ee.getSingleCellExpressionDataVectors().size();
        if ( !vectorsToBeReplaced.isEmpty() ) {
            // if the SCD was created, we do not need to check additional vectors for removing the existing one
            removeSingleCellVectorsAndDimensionIfNecessary( ee, vectorsToBeReplaced, scdCreated ? null : vectors );
        } else {
            log.warn( "No vectors with the quantitation type: " + quantitationType );
        }
        int numVectorsRemoved = ee.getSingleCellExpressionDataVectors().size() - previousSize;
        log.info( String.format( "Adding %d single-cell vectors to %s for %s", vectors.size(), ee, quantitationType ) );
        ee.getSingleCellExpressionDataVectors().addAll( vectors );
        int numVectorsAdded = ee.getSingleCellExpressionDataVectors().size() - ( previousSize - numVectorsRemoved );
        expressionExperimentDao.update( ee );
        auditTrailService.addUpdateEvent( ee, DataReplacedEvent.class,
                String.format( "Replaced %d vectors with %d vectors for %s with dimension %s.", numVectorsRemoved, numVectorsAdded, quantitationType, scd ) );
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
            Assert.isTrue( vector.getData().length < maximumDataLength,
                    String.format( "All vector must have at most %d bytes.", maximumDataLength ) );
            // 1. monotonous, 2. distinct, 3. within the range of the cell IDs
            int lastI = -1;
            for ( int i : vector.getDataIndices() ) {
                Assert.isTrue( i > lastI );
                Assert.isTrue( i < maximumDataLength );
            }
        }
    }

    /**
     * Validate single-cell dimension.
     */
    private void validateSingleCellDimension( ExpressionExperiment ee, SingleCellDimension scbad ) {
        Assert.isTrue( scbad.getCellIds().size() == scbad.getNumberOfCells(),
                "The number of cell IDs must match the number of cells." );
        if ( scbad.getCellTypes() != null ) {
            Assert.notNull( scbad.getNumberOfCellTypeLabels() );
            Assert.notNull( scbad.getCellTypeLabels() );
            Assert.isTrue( scbad.getCellTypes().length == scbad.getCellIds().size(),
                    "The number of cell types must match the number of cell IDs." );
            int numberOfCellTypeLabels = scbad.getCellTypeLabels().size();
            Assert.isTrue( numberOfCellTypeLabels > 0,
                    "There must be at least one cell type label declared in the cellTypeLabels collection." );
            Assert.isTrue( numberOfCellTypeLabels == scbad.getNumberOfCellTypeLabels(),
                    "The number of cell types must match the number of values the cellTypeLabels collection." );
            for ( int k : scbad.getCellTypes() ) {
                Assert.isTrue( k >= 0 && k < numberOfCellTypeLabels,
                        String.format( "Cell type vector values must be within the [%d, %d[ range.", 0, numberOfCellTypeLabels ) );
            }
        } else {
            Assert.isNull( scbad.getCellTypeLabels() );
            Assert.isNull( scbad.getNumberOfCellTypeLabels(), "There is no cell types assigned, the number of cell types must be null." );
        }
        Assert.isTrue( ee.getBioAssays().containsAll( scbad.getBioAssays() ), "Not all supplied BioAssays belong to " + ee );
        validateSparseRangeArray( scbad.getBioAssays(), scbad.getBioAssaysOffset(), scbad.getNumberOfCells() );
    }

    @Override
    @Transactional
    public void removeSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType ) {
        Assert.notNull( ee.getId(), "The dataset must be persistent." );
        Assert.notNull( quantitationType.getId(), "The quantitation type must be persistent." );
        Assert.isTrue( ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s does not have the quantitation type %s.", ee, quantitationType ) );
        Set<SingleCellExpressionDataVector> vectors = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( quantitationType ) ).collect( Collectors.toSet() );
        SingleCellDimension scd;
        if ( !vectors.isEmpty() ) {
            scd = vectors.iterator().next().getSingleCellDimension();
            removeSingleCellVectorsAndDimensionIfNecessary( ee, vectors, null );
        } else {
            scd = null;
            log.warn( "No vectors with the quantitation type: " + quantitationType );
        }
        ee.getQuantitationTypes().remove( quantitationType );
        expressionExperimentDao.update( ee );
        if ( !vectors.isEmpty() ) {
            auditTrailService.addUpdateEvent( ee, DataRemovedEvent.class,
                    String.format( "Removed %d vectors for %s with dimension %s.", vectors.size(), quantitationType, scd ) );
        }
    }

    /**
     * Remove the given single-cell vectors and their corresponding single-cell dimension if necessary.
     * @param ee the experiment to remove the vectors from.
     * @param additionalVectors additional vectors to check if the single-cell dimension is still in use (i.e. vectors that are in the process of being added).
     */
    private void removeSingleCellVectorsAndDimensionIfNecessary( ExpressionExperiment ee,
            Collection<SingleCellExpressionDataVector> vectors,
            @Nullable Collection<SingleCellExpressionDataVector> additionalVectors ) {
        log.info( String.format( "Removing %d single-cell vectors for %s...", vectors.size(), ee ) );
        ee.getSingleCellExpressionDataVectors().removeAll( vectors );
        // FIXME: flushing shouldn't be necessary here, but Hibernate does appear to cascade vectors removal prior to removing the SCD or QT...
        sessionFactory.getCurrentSession().flush();
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
    }

    @Override
    @Transactional(readOnly = true)
    public List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee ) {
        return expressionExperimentDao.getSingleCellDimensions( ee );
    }
}
