package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.singleCell.SingleCellSlicerUtils;
import ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataIntMatrix;
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
import ubic.gemma.persistence.util.Thaws;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.core.analysis.preprocess.convert.RepresentationConversionUtils.convertVectors;
import static ubic.gemma.core.analysis.singleCell.SingleCellSlicerUtils.createSlicer;
import static ubic.gemma.core.analysis.singleCell.SingleCellSlicerUtils.sliceCellIds;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.createStreamMonitor;

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

    @Autowired
    private SingleCellSparsityMetrics metrics;

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
    public Collection<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, SingleCellVectorInitializationConfig config ) {
        if ( config.isIncludeCellIds() && config.isIncludeData() && config.isIncludeDataIndices() ) {
            return getSingleCellDataVectors( ee, quantitationType );
        }
        return expressionExperimentDao.getSingleCellDataVectors( ee, quantitationType, config.isIncludeCellIds(), config.isIncludeData(), config.isIncludeDataIndices() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType ) {
        SingleCellDimension scd = getSingleCellDimension( ee, quantitationType );
        if ( scd == null ) {
            return null;
        }
        if ( scd.getBioAssays().equals( samples ) ) {
            log.info( "The requested slices is the same as the original dimension, returning the original vectors." );
            return getSingleCellDataVectors( ee, quantitationType );
        }
        int[] sampleStarts = new int[samples.size()];
        int[] sampleEnds = new int[samples.size()];
        int totalCells = 0;
        for ( int i = 0; i < samples.size(); i++ ) {
            BioAssay bioAssay = samples.get( i );
            int sampleIndex = scd.getBioAssays().indexOf( bioAssay );
            if ( sampleIndex == -1 ) {
                throw new IllegalArgumentException( bioAssay + " is not a sample of " + scd );
            }
            sampleStarts[i] = scd.getBioAssaysOffset()[sampleIndex];
            sampleEnds[i] = sampleStarts[i] + scd.getNumberOfCellsBySample( sampleIndex );
            totalCells += sampleEnds[i] - sampleStarts[i];
        }
        return expressionExperimentDao.getSingleCellDataVectors( ee, quantitationType ).stream()
                .map( createSlicer( samples,
                        sliceCellIds( scd, samples, sampleStarts, sampleEnds, totalCells ),
                        sliceCtas( scd, samples, sampleStarts, sampleEnds, totalCells ),
                        sliceClcs( scd, samples, sampleStarts, sampleEnds, totalCells ) ) )
                .collect( Collectors.toList() );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SingleCellExpressionDataVector> getSingleCellDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType, SingleCellVectorInitializationConfig config ) {
        SingleCellDimension scd = config.isIncludeCellIds() ? getSingleCellDimension( ee, quantitationType ) : getSingleCellDimensionWithoutCellIds( ee, quantitationType );
        if ( scd == null ) {
            return null;
        }
        if ( scd.getBioAssays().equals( samples ) ) {
            log.info( "The requested slices is the same as the original dimension, returning the original vectors." );
            return getSingleCellDataVectors( ee, quantitationType, config );
        }
        int[] sampleStarts = new int[samples.size()];
        int[] sampleEnds = new int[samples.size()];
        int totalCells = 0;
        for ( int i = 0; i < samples.size(); i++ ) {
            BioAssay bioAssay = samples.get( i );
            int sampleIndex = scd.getBioAssays().indexOf( bioAssay );
            if ( sampleIndex == -1 ) {
                throw new IllegalArgumentException( bioAssay + " is not a sample of " + scd );
            }
            sampleStarts[i] = scd.getBioAssaysOffset()[sampleIndex];
            sampleEnds[i] = sampleStarts[i] + scd.getNumberOfCellsBySample( sampleIndex );
            totalCells += sampleEnds[i] - sampleStarts[i];
        }
        return expressionExperimentDao.getSingleCellDataVectors( ee, quantitationType, config.isIncludeCellIds(), config.isIncludeData(), config.isIncludeDataIndices() ).stream()
                .map( createSlicer( samples,
                        config.isIncludeCellIds() ? sliceCellIds( scd, samples, sampleStarts, sampleEnds, totalCells ) : null,
                        sliceCtas( scd, samples, sampleStarts, sampleEnds, totalCells ),
                        sliceClcs( scd, samples, sampleStarts, sampleEnds, totalCells ) ) )
                .collect( Collectors.toList() );
    }

    /**
     * In-database version of {@link SingleCellSlicerUtils#sliceCtas(SingleCellDimension, List, int[], int[], int)}.
     */
    private Set<CellTypeAssignment> sliceCtas( SingleCellDimension scd, List<BioAssay> bioAssays, int[] sampleStarts, int[] sampleEnds, int totalCells ) {
        Set<CellTypeAssignment> slicedCtas = new HashSet<>();
        for ( CellTypeAssignment cta : scd.getCellTypeAssignments() ) {
            Characteristic[] csi = new Characteristic[totalCells];
            int offset = 0;
            for ( int i = 0; i < bioAssays.size(); i++ ) {
                System.arraycopy( expressionExperimentDao.getCellTypeAt( cta, sampleStarts[i], sampleEnds[i] ), 0,
                        csi, offset, sampleEnds[i] - sampleStarts[i] );
                offset += sampleEnds[i] - sampleStarts[i];
            }
            List<Characteristic> c = new ArrayList<>();
            int[] i = new int[csi.length];
            populate( csi, c, i );
            slicedCtas.add( CellTypeAssignment.Factory.newInstance( cta.getName(), c, i ) );
        }
        return slicedCtas;
    }

    /**
     * In-database version of {@link SingleCellSlicerUtils#sliceClcs(SingleCellDimension, List, int[], int[], int)}.
     */
    private Set<CellLevelCharacteristics> sliceClcs( SingleCellDimension scd, List<BioAssay> bioAssays, int[] sampleStarts, int[] sampleEnds, int totalCells ) {
        Set<CellLevelCharacteristics> slicedClcs = new HashSet<>();
        for ( CellLevelCharacteristics clc : scd.getCellLevelCharacteristics() ) {
            Characteristic[] csi = new Characteristic[totalCells];
            int offset = 0;
            for ( int i = 0; i < bioAssays.size(); i++ ) {
                System.arraycopy( expressionExperimentDao.getCellLevelCharacteristicAt( clc, sampleStarts[i], sampleEnds[i] ),
                        0, csi, offset, sampleEnds[i] - sampleStarts[i] );
                offset += sampleEnds[i] - sampleStarts[i];
            }
            List<Characteristic> c = new ArrayList<>();
            int[] i = new int[csi.length];
            populate( csi, c, i );
            slicedClcs.add( CellLevelCharacteristics.Factory.newInstance( clc.getName(), clc.getDescription(), c, i ) );
        }
        return slicedClcs;
    }

    private void populate( Characteristic[] cs, List<Characteristic> c, int[] i ) {
        for ( int j = 0; j < i.length; j++ ) {
            if ( cs[j] == null ) {
                i[j] = -1;
            } else if ( c.contains( cs[j] ) ) {
                i[j] = c.indexOf( cs[j] );
            } else {
                i[j] = c.size();
                c.add( cs[j] );
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession ) {
        return expressionExperimentDao.streamSingleCellDataVectors( ee, quantitationType, fetchSize, useCursorFetchIfSupported, createNewSession );
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession, SingleCellVectorInitializationConfig config ) {
        if ( config.isIncludeCellIds() && config.isIncludeData() && config.isIncludeDataIndices() ) {
            return expressionExperimentDao.streamSingleCellDataVectors( ee, quantitationType, fetchSize, useCursorFetchIfSupported, createNewSession );
        }
        return expressionExperimentDao.streamSingleCellDataVectors( ee, quantitationType, fetchSize, useCursorFetchIfSupported, createNewSession, config.isIncludeCellIds(), config.isIncludeData(), config.isIncludeDataIndices() );
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession, SingleCellVectorInitializationConfig config ) {
        if ( config.isIncludeCellIds() && config.isIncludeData() && config.isIncludeDataIndices() ) {
            return expressionExperimentDao.streamSingleCellDataVectors( ee, quantitationType, fetchSize, useCursorFetchIfSupported, createNewSession )
                    .map( createSlicer( samples ) );
        }
        return expressionExperimentDao.streamSingleCellDataVectors( ee, quantitationType, fetchSize, useCursorFetchIfSupported, createNewSession, config.isIncludeCellIds(), config.isIncludeData(), config.isIncludeDataIndices() )
                .map( createSlicer( samples ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<SingleCellExpressionDataVector> streamSingleCellDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType quantitationType, int fetchSize, boolean useCursorFetchIfSupported, boolean createNewSession ) {
        return expressionExperimentDao.streamSingleCellDataVectors( ee, quantitationType, fetchSize, useCursorFetchIfSupported, createNewSession )
                .map( createSlicer( samples ) );
    }

    @Override
    @Transactional(readOnly = true)
    public SingleCellExpressionDataVector getSingleCellDataVectorWithoutCellIds( ExpressionExperiment ee, QuantitationType quantitationType, CompositeSequence designElement ) {
        return expressionExperimentDao.getSingleCellDataVectorWithoutCellIds( ee, quantitationType, designElement );
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
    public Map<BioAssay, Long> getNumberOfNonZeroesBySample( ExpressionExperiment ee, QuantitationType qt, int fetchSize, boolean useCursorFetchIfSupported ) {
        return expressionExperimentDao.getNumberOfNonZeroesBySample( ee, qt, fetchSize, useCursorFetchIfSupported );
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
    public SingleCellExpressionDataMatrix<?> getSingleCellExpressionDataMatrix( ExpressionExperiment expressionExperiment, List<BioAssay> samples, QuantitationType quantitationType ) {
        Collection<SingleCellExpressionDataVector> vectors = getSingleCellDataVectors( expressionExperiment, samples, quantitationType );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vector for " + quantitationType + " in " + expressionExperiment );
        }
        if ( quantitationType.getRepresentation() == PrimitiveType.DOUBLE ) {
            return new SingleCellExpressionDataDoubleMatrix( vectors );
        } else if ( quantitationType.getRepresentation() == PrimitiveType.INT ) {
            return new SingleCellExpressionDataIntMatrix( vectors );
        } else {
            log.warn( "Data for " + quantitationType + " will be converted from " + quantitationType.getRepresentation() + " to " + PrimitiveType.DOUBLE + "." );
            try {
                return new SingleCellExpressionDataDoubleMatrix( convertVectors( vectors, PrimitiveType.DOUBLE, SingleCellExpressionDataVector.class ) );
            } catch ( QuantitationTypeConversionException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SingleCellExpressionDataMatrix<?> getSingleCellExpressionDataMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {
        Collection<SingleCellExpressionDataVector> vectors = getSingleCellDataVectors( expressionExperiment, quantitationType );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vector for " + quantitationType + " in " + expressionExperiment );
        }
        if ( quantitationType.getRepresentation() == PrimitiveType.DOUBLE ) {
            return new SingleCellExpressionDataDoubleMatrix( vectors );
        } else if ( quantitationType.getRepresentation() == PrimitiveType.INT ) {
            return new SingleCellExpressionDataIntMatrix( vectors );
        } else {
            log.warn( "Data for " + quantitationType + " will be converted from " + quantitationType.getRepresentation() + " to " + PrimitiveType.DOUBLE + "." );
            try {
                return new SingleCellExpressionDataDoubleMatrix( convertVectors( vectors, PrimitiveType.DOUBLE, SingleCellExpressionDataVector.class ) );
            } catch ( QuantitationTypeConversionException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    @Override
    @Transactional
    public int addSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors, @Nullable String details ) {
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
            log.info( "Recomputing single-cell sparsity metrics for " + quantitationType + "..." );
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
    public int replaceSingleCellDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<SingleCellExpressionDataVector> vectors, @Nullable String details ) {
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
            log.info( "Recomputing single-cell sparsity metrics for " + quantitationType + "..." );
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

    @Override
    @Transactional
    public void updateSparsityMetrics( ExpressionExperiment ee ) {
        ee = expressionExperimentDao.reload( ee );
        Optional<QuantitationType> preferredQt = getPreferredSingleCellQuantitationType( ee );
        if ( preferredQt.isPresent() ) {
            SingleCellDimension dimension = expressionExperimentDao.getSingleCellDimensionWithoutCellIds( ee, preferredQt.get() );
            if ( dimension == null ) {
                throw new IllegalStateException( "There is not single-cell dimension associated to " + preferredQt + "." );
            }
            long numVecs = expressionExperimentDao.getNumberOfSingleCellDataVectors( ee, preferredQt.get() );
            try ( Stream<SingleCellExpressionDataVector> vecs = expressionExperimentDao.streamSingleCellDataVectors( ee, preferredQt.get(), 30, false, false ) ) {
                log.info( "Recomputing single-cell sparsity metrics for " + preferredQt.get() + "..." );
                applyBioAssaySparsityMetrics( ee, dimension, vecs.peek( createStreamMonitor( ee, preferredQt.get(), SingleCellExpressionExperimentServiceImpl.class.getName(), 100, numVecs ) ) );
            }
        } else {
            // reset the metrics to null since there is no preferred vectors
            log.info( "There is no preferred single-cell vectors for " + ee + ", clearing any sparsity metrics..." );
            clearBioAssaySparsityMetrics( ee );
        }
        expressionExperimentDao.update( ee );
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
        boolean isSupported = metrics.isSupported( vectors.iterator().next() );
        if ( !isSupported ) {
            log.warn( "Sparsity metrics cannot be computed for " + ee + ", they will be all set to null." );
        }
        for ( BioAssay ba : ee.getBioAssays() ) {
            if ( !isSupported ) {
                ba.setNumberOfCells( null );
                ba.setNumberOfDesignElements( null );
                ba.setNumberOfCellsByDesignElements( null );
                continue;
            }
            int sampleIndex = dimension.getBioAssays().indexOf( ba );
            if ( sampleIndex == -1 ) {
                log.warn( ba + " is not used in " + dimension + ", the single-cell sparsity metrics will be set to null." );
                ba.setNumberOfCells( null );
                ba.setNumberOfDesignElements( null );
                ba.setNumberOfCellsByDesignElements( null );
                continue;
            }
            ba.setNumberOfCells( metrics.getNumberOfCells( vectors, sampleIndex, null, -1 ) );
            ba.setNumberOfDesignElements( metrics.getNumberOfDesignElements( vectors, sampleIndex, null, -1 ) );
            ba.setNumberOfCellsByDesignElements( metrics.getNumberOfCellsByDesignElements( vectors, sampleIndex, null, -1 ) );
            log.info( String.format( "Sparsity metrics for %s: %d cells, %d design elements, %d cells by design elements.",
                    ba, ba.getNumberOfCells(), ba.getNumberOfDesignElements(), ba.getNumberOfCellsByDesignElements() ) );
        }
    }

    /**
     * Streaming variant of {@link #applyBioAssaySparsityMetrics(ExpressionExperiment, SingleCellDimension, Collection)}
     */
    @SuppressWarnings("DuplicatedCode")
    private void applyBioAssaySparsityMetrics( ExpressionExperiment ee, SingleCellDimension dimension, Stream<SingleCellExpressionDataVector> vectors ) {
        SingleCellSparsityMetrics metrics = new SingleCellSparsityMetrics();
        int numberOfSamples = dimension.getBioAssays().size();
        boolean[] isExpressed = new boolean[dimension.getNumberOfCells()];
        int[] numberOfDesignElements = new int[dimension.getBioAssays().size()];
        int[] numberOfCellByDesignElements = new int[dimension.getBioAssays().size()];
        boolean alreadyCheckedForSupport = false;
        boolean isSupported = true;
        Iterator<SingleCellExpressionDataVector> it = vectors.iterator();
        while ( it.hasNext() ) {
            SingleCellExpressionDataVector vec = it.next();
            if ( !alreadyCheckedForSupport ) {
                if ( !metrics.isSupported( vec ) ) {
                    isSupported = false;
                    break;
                }
                alreadyCheckedForSupport = true;
            }
            for ( int sampleIndex = 0; sampleIndex < numberOfSamples; sampleIndex++ ) {
                metrics.addExpressedCells( vec, sampleIndex, null, -1, isExpressed );
                numberOfDesignElements[sampleIndex] += metrics.getNumberOfDesignElements( vec, sampleIndex, null, -1 );
                numberOfCellByDesignElements[sampleIndex] += metrics.getNumberOfCellsByDesignElements( vec, sampleIndex, null, -1 );
                sampleIndex++;
            }
        }
        for ( BioAssay ba : ee.getBioAssays() ) {
            if ( !isSupported ) {
                ba.setNumberOfCells( null );
                ba.setNumberOfDesignElements( null );
                ba.setNumberOfCellsByDesignElements( null );
                continue;
            }
            int sampleIndex = dimension.getBioAssays().indexOf( ba );
            if ( sampleIndex == -1 ) {
                log.warn( ba + " is not used in " + dimension + ", the single-cell sparsity metrics will be set to null." );
                ba.setNumberOfCells( null );
                ba.setNumberOfDesignElements( null );
                ba.setNumberOfCellsByDesignElements( null );
                continue;

            }
            int numberOfCells = 0;
            int start = dimension.getBioAssaysOffset()[sampleIndex];
            int end = start + dimension.getNumberOfCellsBySample( sampleIndex );
            for ( int i = start; i < end; i++ ) {
                if ( isExpressed[i] ) {
                    numberOfCells++;
                }
            }
            ba.setNumberOfCells( numberOfCells );
            ba.setNumberOfDesignElements( numberOfDesignElements[sampleIndex] );
            ba.setNumberOfCellsByDesignElements( numberOfCellByDesignElements[sampleIndex] );
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
        int numCells = singleCellDimension.getNumberOfCells();
        int sizeInBytes = quantitationType.getRepresentation().getSizeInBytes();
        for ( SingleCellExpressionDataVector vector : vectors ) {
            if ( sizeInBytes != -1 ) {
                Assert.isTrue( vector.getData().length == sizeInBytes * vector.getDataIndices().length );
            } else {
                // all our variable-length representations used at least 1 byte per element
                Assert.isTrue( vector.getData().length >= vector.getDataIndices().length );
            }
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
        // make sure that the EE is in the session so that the vectors can be loaded
        ee = expressionExperimentDao.reload( ee );
        // this ensures that the same entity is used in ee.getQuantitationTypes()
        quantitationType = quantitationTypeService.reload( quantitationType );
        Assert.isTrue( ee.getQuantitationTypes().contains( quantitationType ),
                String.format( "%s does not have the quantitation type %s.", ee, quantitationType ) );
        QuantitationType finalQuantitationType = quantitationType;
        Set<SingleCellExpressionDataVector> vectors = ee.getSingleCellExpressionDataVectors().stream()
                .filter( v -> v.getQuantitationType().equals( finalQuantitationType ) )
                .collect( Collectors.toSet() );
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
        Thaws.thawSingleCellDimension( scd );
        return scd;
    }

    @Override
    @Transactional(readOnly = true)
    public SingleCellDimension getSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, QuantitationType qt, SingleCellDimensionInitializationConfig config ) {
        return expressionExperimentDao.getSingleCellDimensionWithoutCellIds( ee, qt, config.isIncludeBioAssays(), config.isIncludeCtas(), config.isIncludeClcs(), config.isIncludeProtocol(), config.isIncludeCharacteristics(), config.isIncludeIndices() );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SingleCellDimension> getSingleCellDimensions( ExpressionExperiment ee ) {
        return expressionExperimentDao.getSingleCellDimensions( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SingleCellDimension> getSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee ) {
        return expressionExperimentDao.getSingleCellDimensionsWithoutCellIds( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SingleCellDimension> getSingleCellDimensionsWithoutCellIds( ExpressionExperiment ee, SingleCellDimensionInitializationConfig config ) {
        return expressionExperimentDao.getSingleCellDimensionsWithoutCellIds( ee, config.isIncludeBioAssays(), config.isIncludeCtas(), config.isIncludeClcs(), config.isIncludeProtocol(), config.isIncludeCharacteristics(), config.isIncludeIndices() );
    }

    @Override
    @Transactional(readOnly = true)
    public SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getSingleCellDimension( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public SingleCellDimension getSingleCellDimensionWithoutCellIds( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getSingleCellDimensionWithoutCellIds( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SingleCellDimension> getPreferredSingleCellDimension( ExpressionExperiment ee ) {
        return Optional.ofNullable( expressionExperimentDao.getPreferredSingleCellDimension( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SingleCellDimension> getPreferredSingleCellDimensionWithoutCellIds( ExpressionExperiment ee ) {
        return Optional.ofNullable( expressionExperimentDao.getPreferredSingleCellDimensionWithoutCellIds( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SingleCellDimension> getPreferredSingleCellDimensionWithCellLevelCharacteristics( ExpressionExperiment ee ) {
        return getPreferredSingleCellDimension( ee )
                .map( scd -> {
                    Thaws.thawSingleCellDimension( scd );
                    return scd;
                } );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stream<String>> streamCellIds( ExpressionExperiment ee, boolean createNewSession ) {
        return getPreferredSingleCellDimensionWithoutCellIds( ee )
                .map( ( SingleCellDimension dimension ) -> expressionExperimentDao.streamCellIds( dimension, createNewSession ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<String> streamCellIds( ExpressionExperiment ee, QuantitationType qt, boolean createNewSession ) {
        SingleCellDimension scd = getSingleCellDimensionWithoutCellIds( ee, qt );
        if ( scd != null ) {
            return expressionExperimentDao.streamCellIds( scd, createNewSession );
        } else {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Characteristic> streamCellTypes( ExpressionExperiment ee, CellTypeAssignment cta, boolean createNewSession ) {
        return expressionExperimentDao.streamCellTypes( cta, createNewSession );
    }

    @Override
    @Transactional(readOnly = true)
    public Characteristic getCellTypeAt( ExpressionExperiment ee, QuantitationType qt, Long ctaId, int cellIndex ) {
        CellTypeAssignment cta = requireNonNull( expressionExperimentDao.getCellTypeAssignmentWithoutIndices( ee, qt, ctaId ),
                "No cell type assignment with ID " + ctaId + " found." );
        return expressionExperimentDao.getCellTypeAt( cta, cellIndex );
    }

    @Override
    @Transactional(readOnly = true)
    public Characteristic[] getCellTypeAt( ExpressionExperiment ee, QuantitationType qt, Long ctaId, int startIndex, int endIndexExclusive ) {
        CellTypeAssignment cta = expressionExperimentDao.getCellTypeAssignmentWithoutIndices( ee, qt, ctaId );
        if ( cta == null ) {
            return null;
        }
        return expressionExperimentDao.getCellTypeAt( cta, startIndex, endIndexExclusive );
    }

    @Override
    @Transactional(readOnly = true)
    public Characteristic getCellTypeAt( ExpressionExperiment ee, QuantitationType qt, String ctaName, int cellIndex ) {
        CellTypeAssignment cta = requireNonNull( expressionExperimentDao.getCellTypeAssignmentWithoutIndices( ee, qt, ctaName ),
                "No cell type assignment with name " + ctaName + " found." );
        return expressionExperimentDao.getCellTypeAt( cta, cellIndex );
    }

    @Override
    @Transactional(readOnly = true)
    public Characteristic[] getCellTypeAt( ExpressionExperiment ee, QuantitationType qt, String ctaName, int startIndex, int endIndexExclusive ) {
        CellTypeAssignment cta = expressionExperimentDao.getCellTypeAssignmentWithoutIndices( ee, qt, ctaName );
        if ( cta == null ) {
            return null;
        }
        return expressionExperimentDao.getCellTypeAt( cta, startIndex, endIndexExclusive );
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCellLevelCharacteristicsCategory( ExpressionExperiment ee, CellLevelCharacteristics clc ) {
        return expressionExperimentDao.getCellLevelCharacteristicsCategory( clc );
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<Characteristic> streamCellLevelCharacteristics( ExpressionExperiment ee, CellLevelCharacteristics clc, boolean createNewSession ) {
        return expressionExperimentDao.streamCellLevelCharacteristics( clc, createNewSession );
    }

    @Override
    @Transactional
    public CellTypeAssignment relabelCellTypes( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension dimension, List<String> newCellTypeLabels, @Nullable Protocol protocol, @Nullable String description ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( dimension.getId(), "Single-cell dimension must be persistent." );
        Assert.isTrue( ee.getBioAssays().containsAll( dimension.getBioAssays() ), "Single-cell dimension does not belong to the dataset." );
        CellTypeAssignment cta = new CellTypeAssignment();
        cta.setPreferred( true );
        cta.setProtocol( protocol );
        cta.setDescription( description );
        int[] ct = new int[dimension.getNumberOfCells()];
        List<String> labels = newCellTypeLabels.stream().sorted().distinct().collect( Collectors.toList() );
        int N = 0;
        for ( int i = 0; i < ct.length; i++ ) {
            int k = Collections.binarySearch( labels, newCellTypeLabels.get( i ) );
            if ( k >= 0 ) {
                ct[i] = k;
                N++;
            } else {
                ct[i] = CellTypeAssignment.UNKNOWN_CELL_TYPE;
            }
        }
        cta.setCellTypeIndices( ct );
        cta.setNumberOfAssignedCells( N );
        cta.setCellTypes( labels.stream()
                .map( l -> Characteristic.Factory.newInstance( Categories.CELL_TYPE, l, null ) )
                .collect( Collectors.toList() ) );
        cta.setNumberOfCellTypes( labels.size() );
        return createCellTypeAssignment( ee, qt, dimension, cta );
    }

    @Override
    @Transactional
    public CellTypeAssignment addCellTypeAssignment( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension dimension, CellTypeAssignment cta ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( dimension.getId(), "Single-cell dimension must be persistent." );
        Assert.isTrue( ee.getBioAssays().containsAll( dimension.getBioAssays() ), "Single-cell dimension does not belong to the dataset." );
        Assert.isNull( cta.getId(), "Cell type assignment must be non-persistent." );
        return createCellTypeAssignment( ee, qt, dimension, cta );
    }

    private CellTypeAssignment createCellTypeAssignment( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension dimension, CellTypeAssignment cta ) {
        if ( cta.getName() != null ) {
            for ( CellTypeAssignment e : dimension.getCellTypeAssignments() ) {
                if ( cta.getName().equalsIgnoreCase( e.getName() ) ) {
                    throw new IllegalArgumentException( "There is already a cell type assignment named '" + cta.getName() + "'." );
                }
            }
        }
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
            if ( qt.getIsSingleCellPreferred() ) {
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
        Assert.notNull( cellTypeAssignment.getId(), "The cell type assignment must be persistent." );
        Assert.isTrue( ee.getBioAssays().containsAll( dimension.getBioAssays() ), "Single-cell dimension does not belong to the dataset." );
        boolean alsoRemoveFactor = getPreferredCellTypeAssignment( ee ).map( cellTypeAssignment::equals ).orElse( false );
        removeCellTypeAssignment( ee, dimension, cellTypeAssignment, alsoRemoveFactor );
    }

    @Override
    @Transactional
    public void removeCellTypeAssignment( ExpressionExperiment ee, QuantitationType qt, CellTypeAssignment cellTypeAssignment ) {
        Assert.notNull( ee.getId(), "Dataset must be persistent." );
        Assert.notNull( qt.getId(), "Quantitation type must be persistent." );
        Assert.notNull( cellTypeAssignment.getId(), "The cell type assignment must be persistent." );
        SingleCellDimension dim = getSingleCellDimension( ee, qt );
        if ( dim == null ) {
            throw new IllegalStateException( "There is no single-cell dimension for " + qt + " in " + ee + "." );
        }
        // since we have the QT, we can check if the labelling is preferred without querying the database
        boolean alsoRemoveFactor = qt.getIsSingleCellPreferred() && cellTypeAssignment.isPreferred();
        removeCellTypeAssignment( ee, dim, cellTypeAssignment, alsoRemoveFactor );
    }

    private void removeCellTypeAssignment( ExpressionExperiment ee, SingleCellDimension dimension, CellTypeAssignment cellTypeAssignment, boolean alsoRemoveFactor ) {
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
    public List<CellTypeAssignment> getCellTypeAssignments( ExpressionExperiment expressionExperiment, QuantitationType qt ) {
        return expressionExperimentDao.getCellTypeAssignments( expressionExperiment, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, Long ctaId ) {
        return expressionExperimentDao.getCellTypeAssignment( expressionExperiment, qt, ctaId );
    }

    @Override
    @Transactional(readOnly = true)
    public CellTypeAssignment getCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String ctaName ) {
        return expressionExperimentDao.getCellTypeAssignment( expressionExperiment, qt, ctaName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CellTypeAssignment> getCellTypeAssignmentByProtocol( ExpressionExperiment ee, QuantitationType qt, String protocolName ) {
        return expressionExperimentDao.getCellTypeAssignmentByProtocol( ee, qt, protocolName );
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
        if ( clc.getName() != null ) {
            for ( CellLevelCharacteristics e : scd.getCellLevelCharacteristics() ) {
                if ( clc.getName().equalsIgnoreCase( e.getName() ) ) {
                    throw new IllegalArgumentException( "There is already a cell-level characteristics named '" + e.getName() + "'." );
                }
            }
        }
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
    @Transactional
    public void removeCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt, CellLevelCharacteristics clc ) {
        SingleCellDimension dim = getSingleCellDimension( ee, qt );
        if ( dim == null ) {
            throw new IllegalStateException( "There is no single-cell dimension for " + qt + " in " + ee + "." );
        }
        removeCellLevelCharacteristics( ee, dim, clc );
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

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public CellLevelCharacteristics getCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt, Long id ) {
        return expressionExperimentDao.getCellLevelCharacteristics( expressionExperiment, qt, id );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public CellLevelCharacteristics getCellLevelCharacteristics( ExpressionExperiment ee, QuantitationType qt, String name ) {
        return expressionExperimentDao.getCellLevelCharacteristics( ee, qt, name );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CellLevelCharacteristics> getCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt ) {
        return expressionExperimentDao.getCellLevelCharacteristics( expressionExperiment, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CellLevelCharacteristics> getCellLevelMask( ExpressionExperiment expressionExperiment, QuantitationType qt ) {
        List<CellLevelCharacteristics> candidates = expressionExperimentDao.getCellLevelCharacteristics( expressionExperiment, qt, Categories.MASK );
        if ( candidates.size() == 1 ) {
            return Optional.of( candidates.iterator().next() );
        } else if ( candidates.isEmpty() ) {
            return Optional.empty();
        } else {
            log.warn( expressionExperiment + " " + qt + " has more than one cell-level masks." );
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> getCellTypes( ExpressionExperiment ee ) {
        return expressionExperimentDao.getCellTypes( ee );
    }

    @Override
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
            log.warn( "There is more than one cell type factor in " + ee + "." );
            return Optional.empty();
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
