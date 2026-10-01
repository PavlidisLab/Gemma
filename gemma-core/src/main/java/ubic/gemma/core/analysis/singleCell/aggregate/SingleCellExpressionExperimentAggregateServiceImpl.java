package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.singleCell.SingleCellMaskUtils;
import ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.core.security.audit.payload.SingleCellAggregationPayload;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentServiceImpl;

import org.springframework.lang.Nullable;
import java.io.Console;
import java.nio.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ubic.gemma.core.analysis.singleCell.CellLevelCharacteristicsMappingUtils.createMappingByFactorValueCharacteristics;
import static ubic.gemma.model.common.DescribableUtils.getNextAvailableName;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.*;

@Service
@Slf4j
public class SingleCellExpressionExperimentAggregateServiceImpl implements SingleCellExpressionExperimentAggregateService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private SingleCellExpressionExperimentAggregateAuditService aggregateAuditService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Override
    @Transactional
    public QuantitationType aggregateVectorsByCellType( ExpressionExperiment ee, List<BioAssay> cellBAs, SingleCellAggregationConfig config ) {
        QuantitationType qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred set of single-cell vectors." ) );
        CellTypeAssignment cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred cell type assignment." ) );
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a cell type factor." ) );
        Map<Characteristic, FactorValue> cellType2Factor = createMappingByFactorValueCharacteristics( cta, cellTypeFactor );
        return aggregateVectors( ee, qt, cellBAs, cta, cellTypeFactor, cellType2Factor, config );
    }

    @Override
    @Transactional
    public QuantitationType aggregateVectors( ExpressionExperiment ee, QuantitationType qt, List<BioAssay> cellBAs, CellLevelCharacteristics cellLevelCharacteristics, ExperimentalFactor factor, Map<Characteristic, FactorValue> cellType2Factor, SingleCellAggregationConfig config ) throws UnsupportedScaleTypeForSingleCellAggregationException {
        // FIXME: this is needed because if EE is not in the session, getSingleCellDataVectors() will retrieve
        //        a distinct QT than that of ee.getQuantitationTypes()
        ee = expressionExperimentService.reload( ee );
        qt = quantitationTypeService.reload( qt );
        SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig vectorInitConfig = SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig.builder()
                .includeCellIds( false )
                .includeData( true )
                .includeDataIndices( true )
                .build();
        log.info( "Loading single-cell data vectors for aggregation for " + qt + "..." );
        long numVecs = singleCellExpressionExperimentService.getNumberOfSingleCellDataVectors( ee, qt );
        // With a positive fetch size, the vectors are streamed from the database instead of being materialized:
        // once for computeLibrarySize (log2cpm only) and once for the aggregation loop. Only the aggregated vectors
        // (one double per pseudo-bulk assay) are held in memory, so heap use no longer scales with the number of
        // cells. Both streams run in this method's transaction, which keeps the session open for their lifetime.
        final boolean useStreaming = config.getFetchSize() > 0;
        Collection<SingleCellExpressionDataVector> vectors;
        SingleCellDimension scd;
        if ( useStreaming ) {
            // the streamed vectors use a dimension without cell IDs, so that is what we retrieve here too
            scd = numVecs > 0 ? singleCellExpressionExperimentService.getSingleCellDimensionWithoutCellIds( ee, qt ) : null;
            if ( scd == null ) {
                throw new IllegalStateException( ee + " does not have single-cell vectors for " + qt + "." );
            }
            vectors = null;
        } else {
            // the in-memory path holds every vector's data and indices at once
            if ( numVecs > SingleCellExpressionExperimentServiceImpl.SC_MATRIX_VECTOR_COUNT_LIMIT ) {
                throw new IllegalStateException( String.format(
                        "Refusing to aggregate single-cell vectors for %s in %s in memory: %d vectors exceeds the %d ceiling. "
                                + "Use a positive fetch size to stream the vectors instead.",
                        qt, ee, numVecs, SingleCellExpressionExperimentServiceImpl.SC_MATRIX_VECTOR_COUNT_LIMIT ) );
            }
            vectors = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt, vectorInitConfig );
            if ( vectors.isEmpty() ) {
                throw new IllegalStateException( ee + " does not have single-cell vectors for " + qt + "." );
            }
            scd = vectors.iterator().next().getSingleCellDimension();
            numVecs = vectors.size();
        }

        // check the QT and determine how to aggregate its data
        // TODO: support other types and representations for aggregation
        Assert.isTrue( qt.getGeneralType().equals( GeneralType.QUANTITATIVE ), "Only quantitative data can be aggregated." );
        Assert.isTrue( qt.getType().equals( StandardQuantitationType.COUNT ) || qt.getType().equals( StandardQuantitationType.AMOUNT ),
                "Only counts or amounts can be aggregated." );
        SingleCellExpressionAggregationMethod method;
        switch ( qt.getScale() ) {
            case LINEAR:
            case COUNT:
                method = SingleCellExpressionAggregationMethod.SUM;
                break;
            case LOG1P:
                method = SingleCellExpressionAggregationMethod.LOG1P_SUM;
                break;
            case LN:
            case LOG2:
            case LOG10:
            case LOGBASEUNKNOWN:
                method = SingleCellExpressionAggregationMethod.LOG_SUM;
                break;
            default:
                throw new UnsupportedScaleTypeForSingleCellAggregationException( qt.getScale() );
        }

        log.info( "Aggregating single-cell data with scale " + qt.getScale() + " using " + method + "." );

        // map subpopulation bioassay to their sample
        Map<BioAssay, BioAssay> sourceBioAssayMap = createSourceBioAssayMap( ee, cellBAs );

        // assigne sample to cell types
        Map<BioAssay, Integer> cellTypeIndices = assignSampleToCellTypeIndex( cellBAs, cellLevelCharacteristics, cellType2Factor );

        String cellTypeFactorName;
        if ( factor.getName() != null ) {
            cellTypeFactorName = factor.getName();
        } else if ( factor.getCategory() != null ) {
            cellTypeFactorName = factor.getCategory().getCategory();
        } else {
            log.warn( "Could not find a suitable name for " + factor + ", will default to 'cell type'." );
            cellTypeFactorName = "cell type";
        }

        // this is aligned with how other dimensions are named
        BioAssayDimension newBad = BioAssayDimension.Factory.newInstance( new ArrayList<>( cellBAs ) );
        newBad = bioAssayDimensionService.findOrCreate( newBad );

        boolean canLog2cpm = qt.getType() == StandardQuantitationType.COUNT
                && ( qt.getScale() == ScaleType.LOG2
                || qt.getScale() == ScaleType.LN
                || qt.getScale() == ScaleType.LOG10
                || qt.getScale() == ScaleType.LOG1P
                || qt.getScale() == ScaleType.LINEAR
                || qt.getScale() == ScaleType.COUNT );

        // create vectors now
        QuantitationType newQt = QuantitationType.Factory.newInstance( qt );
        newQt.setName( getNextAvailableName( ee.getQuantitationTypes(), qt.getName() + " aggregated by " + cellTypeFactorName + ( canLog2cpm ? " (log2cpm)" : "" ) ) );
        newQt.setDescription( ( StringUtils.isNotBlank( qt.getDescription() ) ? qt.getDescription() + "\n" : "" )
                + "Expression data has been aggregated by " + cellTypeFactorName + " using " + method + "."
                + ( canLog2cpm ? " The data was subsequently converted to log2cpm." : "" ) );
        newQt.setIsPreferred( config.isMakePreferred() );
        // we're always aggregating into doubles, regardless of the input representation
        newQt.setRepresentation( PrimitiveType.DOUBLE );
        newQt.setIsAggregated( true );

        boolean[] mask;
        if ( config.getMask() != null ) {
            mask = SingleCellMaskUtils.parseMask( config.getMask() );
        } else {
            mask = null;
        }

        Map<BioAssay, Integer> sourceSampleToIndex = ListUtils.indexOfElements( scd.getBioAssays() );
        int numSourceSamples = sourceSampleToIndex.size();

        // Group the pseudo-bulk output columns (cellBAs) by (source sample, cell type) so that each vector's
        // per-source-sample cell range is scanned once, not once per cell type sharing that sample. A source
        // sample split into K cell types previously had its data for every gene scanned K times over (and,
        // with sparsity metrics on, 4K times -- see aggregateData). Almost always one column per cell per
        // (source sample, cell type) pair; the list accommodates more without changing behaviour if that ever
        // isn't true.
        int numCellTypes = cellLevelCharacteristics.getCharacteristics().size();
        @SuppressWarnings("unchecked")
        List<Integer>[][] columnsBySourceSampleAndCellType = new List[numSourceSamples][numCellTypes];
        for ( int i = 0; i < cellBAs.size(); i++ ) {
            BioAssay sample = cellBAs.get( i );
            int sourceSampleIndex = sourceSampleToIndex.get( sourceBioAssayMap.get( sample ) );
            int cellTypeIndex = cellTypeIndices.get( sample );
            List<Integer>[] row = columnsBySourceSampleAndCellType[sourceSampleIndex];
            if ( row[cellTypeIndex] == null ) {
                row[cellTypeIndex] = new ArrayList<>( 1 );
            }
            row[cellTypeIndex].add( i );
        }

        double[] normalizationFactor;
        double[] librarySize;
        Map<BioAssay, Double> sourceSampleLibrarySizeAdjustments = new HashMap<>();
        if ( canLog2cpm ) {
            log.info( "Original data uses the COUNT type, but a log2cpm transformation will be performed, the resulting type for the aggregate will be AMOUNT." );
            newQt.setType( StandardQuantitationType.AMOUNT );
            newQt.setScale( ScaleType.LOG2 );
            // TODO: compute normalization factors from data
            normalizationFactor = new double[cellBAs.size()];
            Arrays.fill( normalizationFactor, 1.0 );
            try ( Stream<SingleCellExpressionDataVector> libStream = useStreaming
                    ? streamVectors( ee, qt, vectorInitConfig, config )
                    : vectors.stream() ) {
                librarySize = computeLibrarySize( libStream::iterator, numVecs, newBad, cellLevelCharacteristics,
                        // when including masked cells, do not allow the calculation to consider the mask
                        config.isIncludeMaskedCellsInLibrarySize() ? null : mask,
                        sourceBioAssayMap, sourceSampleToIndex, sourceSampleLibrarySizeAdjustments,
                        method, config.isAdjustLibrarySizes(), columnsBySourceSampleAndCellType, config.getConsole() );
            }
            for ( int i = 0; i < librarySize.length; i++ ) {
                if ( librarySize[i] == 0 ) {
                    log.warn( "Library size for " + cellBAs.get( i ) + " is zero, this will cause NaN values in the log2cpm transformation." );
                }
            }
        } else {
            normalizationFactor = null;
            librarySize = null;
        }

        // update sequencing metadata
        if ( librarySize != null ) {
            updateSequenceReadCounts( newBad, librarySize );
        }

        // sparsity metrics, only needed for preferred QTs
        boolean[] expressedCells;
        Map<BioAssay, Integer> designElementsByBioAssay;
        Map<BioAssay, Integer> cellByDesignElementByBioAssay;
        if ( config.isMakePreferred() ) {
            expressedCells = new boolean[scd.getNumberOfCellIds()];
            designElementsByBioAssay = new HashMap<>();
            cellByDesignElementByBioAssay = new HashMap<>();
        } else {
            expressedCells = null;
            designElementsByBioAssay = null;
            cellByDesignElementByBioAssay = null;
        }

        StopWatch timer = StopWatch.createStarted();
        Collection<RawExpressionDataVector> rawVectors = new ArrayList<>( ( int ) numVecs );
        try ( Stream<SingleCellExpressionDataVector> aggStream = useStreaming
                ? streamVectors( ee, qt, vectorInitConfig, config )
                .peek( createStreamMonitor( ee, qt, SingleCellExpressionExperimentAggregateServiceImpl.class.getName(), 100, numVecs, config.getConsole() ) )
                : vectors.stream() ) {
            for ( SingleCellExpressionDataVector v : ( Iterable<SingleCellExpressionDataVector> ) aggStream::iterator ) {
                RawExpressionDataVector rawVector = new RawExpressionDataVector();
                rawVector.setExpressionExperiment( ee );
                rawVector.setQuantitationType( newQt );
                rawVector.setBioAssayDimension( newBad );
                rawVector.setDesignElement( v.getDesignElement() );
                int[] numberOfCells = new int[cellBAs.size()];
                rawVector.setDataAsDoubles( aggregateData( v, newBad, cellLevelCharacteristics, mask, columnsBySourceSampleAndCellType,
                        method, expressedCells, designElementsByBioAssay,
                        cellByDesignElementByBioAssay, canLog2cpm, normalizationFactor, librarySize, numberOfCells ) );
                rawVector.setNumberOfCells( numberOfCells );
                rawVectors.add( rawVector );
                if ( rawVectors.size() % 100 == 0 ) {
                    if ( config.getConsole() != null ) {
                        config.getConsole().printf( "Aggregating single-cell vectors [%d/%d] @ %.2f vectors/sec.\r",
                                rawVectors.size(), numVecs, 1000.0 * rawVectors.size() / timer.getTime() );
                    } else {
                        log.info( String.format( "Aggregating single-cell vectors [%d/%d] @ %.2f vectors/sec.",
                                rawVectors.size(), numVecs, 1000.0 * rawVectors.size() / timer.getTime() ) );
                    }
                }
            }
        }

        log.info( String.format( "Aggregated %d single-cell vectors @ %.2f vectors/sec.",
                rawVectors.size(), 1000.0 * rawVectors.size() / timer.getTime() ) );

        int[] maskedCells = new int[cellBAs.size()];
        int[] totalCells = new int[cellBAs.size()];
        if ( config.isMakePreferred() ) {
            log.info( "Applying single-cell sparsity metrics to the aggregated assays..." );
            for ( int j = 0; j < cellBAs.size(); j++ ) {
                BioAssay ba = cellBAs.get( j );
                assert expressedCells != null;
                int sourceSampleIndex = sourceSampleToIndex.get( sourceBioAssayMap.get( ba ) );
                int cellTypeIndex = cellTypeIndices.get( ba );
                int count = 0;
                for ( int i = scd.getBioAssaysOffset()[sourceSampleIndex]; i < scd.getBioAssaysOffset()[sourceSampleIndex] + scd.getNumberOfCellIdsBySample( sourceSampleIndex ); i++ ) {
                    if ( cellLevelCharacteristics.getIndices()[i] == cellTypeIndex ) {
                        if ( expressedCells[i] ) {
                            count++;
                        }
                        if ( mask != null && mask[i] ) {
                            maskedCells[j]++;
                        }
                        totalCells[j]++;
                    }
                }
                ba.setNumberOfCells( count );
                ba.setNumberOfDesignElements( designElementsByBioAssay.getOrDefault( ba, 0 ) );
                ba.setNumberOfCellsByDesignElements( cellByDesignElementByBioAssay.getOrDefault( ba, 0 ) );
            }
            bioAssayService.update( cellBAs );
        }

        int newVecs = expressionExperimentService.addRawDataVectors( ee, newQt, rawVectors );
        String note = String.format( Locale.ENGLISH, "Created %d aggregated raw vectors for %s.", newVecs, newQt );
        // Phase C bucket 2f: typed payload via the AuditedAspect. The audit row is
        // written by the @Audited annotation on
        // SingleCellExpressionExperimentAggregateAuditService#recordAggregateCreated
        // — the co-bean hop is required because Spring AOP can't intercept
        // self-invocations on this service.
        List<SingleCellAggregationPayload.AggregatedAssay> aggregatedAssays = new ArrayList<>( cellBAs.size() );
        StringBuilder details = new StringBuilder();
        details.append( "Single-cell quantitation type: " ).append( qt ).append( "\n" );
        details.append( "Single-cell dimension: " ).append( scd ).append( "\n" );
        details.append( "Aggregated assays:" );
        for ( int i = 0; i < cellBAs.size(); i++ ) {
            BioAssay cellBa = cellBAs.get( i );
            details.append( "\n" ).append( "\t" ).append( cellBa );
            Integer pNumberOfCells = null;
            Integer pNumberOfDesignElements = null;
            Integer pNumberOfCellsByDesignElements = null;
            Integer pMaskedCells = null;
            Integer pTotalCells = null;
            Double pLibrarySize = null;
            Double pUnadjustedLibrarySize = null;
            if ( config.isMakePreferred() ) {
                pNumberOfCells = cellBa.getNumberOfCells();
                pNumberOfDesignElements = cellBa.getNumberOfDesignElements();
                pNumberOfCellsByDesignElements = cellBa.getNumberOfCellsByDesignElements();
                details.append( " Number of cells=" ).append( cellBa.getNumberOfCells() );
                details
                        .append( " Number of design elements=" ).append( cellBa.getNumberOfDesignElements() )
                        .append( " Number of cells x design elements=" ).append( cellBa.getNumberOfCellsByDesignElements() );
            }
            if ( mask != null ) {
                pMaskedCells = maskedCells[i];
                pTotalCells = totalCells[i];
                details.append( " Number of masked cells=" ).append( maskedCells[i] ).append( "/" ).append( totalCells[i] );
            }
            if ( librarySize != null ) {
                if ( librarySize[i] == 0 ) {
                    pLibrarySize = 0d;
                    details.append( " Library Size is zero, the aggregate is filled with NAs" );
                } else {
                    pLibrarySize = librarySize[i];
                    details.append( " Library Size=" ).append( String.format( Locale.ENGLISH, "%.2f", librarySize[i] ) );
                    Double lsa = sourceSampleLibrarySizeAdjustments.get( sourceBioAssayMap.get( cellBa ) );
                    if ( lsa != null && lsa != 1.0 ) {
                        pUnadjustedLibrarySize = librarySize[i] / lsa;
                        details.append( " (adjusted from " ).append( String.format( Locale.ENGLISH, "%.2f", librarySize[i] / lsa ) ).append( " due to unmapped genes)" );
                    }
                }
            }
            aggregatedAssays.add( new SingleCellAggregationPayload.AggregatedAssay(
                    cellBa.toString(),
                    pNumberOfCells, pNumberOfDesignElements, pNumberOfCellsByDesignElements,
                    pMaskedCells, pTotalCells,
                    pLibrarySize, pUnadjustedLibrarySize ) );
        }
        if ( config.getMask() != null ) {
            details.append( "\n" ).append( " Mask: " ).append( config.getMask() );
        }
        log.info( note + "\n" + details );
        SingleCellAggregationPayload payload = new SingleCellAggregationPayload(
                qt.toString(),
                scd.toString(),
                config.getMask() != null ? config.getMask().toString() : null,
                aggregatedAssays );
        aggregateAuditService.recordAggregateCreated( ee, note, payload );

        return newQt;
    }

    private Stream<SingleCellExpressionDataVector> streamVectors( ExpressionExperiment ee, QuantitationType qt,
            SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig vectorInitConfig,
            SingleCellAggregationConfig config ) {
        return singleCellExpressionExperimentService.streamSingleCellDataVectors( ee, qt, config.getFetchSize(),
                config.isUseCursorFetchIfSupported(), false, vectorInitConfig );
    }

    private void updateSequenceReadCounts( BioAssayDimension bad, double[] librarySize ) {
        for ( int i = 0; i < bad.getBioAssays().size(); i++ ) {
            BioAssay ba = bad.getBioAssays().get( i );
            ba.setSequenceReadCount( Math.round( librarySize[i] ) );
        }
        bioAssayService.update( bad.getBioAssays() );
    }

    private Map<BioAssay, BioAssay> createSourceBioAssayMap( ExpressionExperiment ee, Collection<BioAssay> cellBAs ) {
        Map<BioAssay, BioAssay> sourceBioAssayMap = new HashMap<>();
        for ( BioAssay ba : cellBAs ) {
            Assert.notNull( ba.getSampleUsed().getSourceBioMaterial(),
                    ba + "'s sample does not have a source biomaterial." );
            Set<BioAssay> sourceBAs = ee.getBioAssays().stream()
                    .filter( ba.getSampleUsed().getSourceBioMaterial().getBioAssaysUsedIn()::contains )
                    .collect( Collectors.toSet() );
            if ( sourceBAs.isEmpty() ) {
                throw new IllegalStateException( ba + " does not have a source BioAssay in " + ee );
            } else if ( sourceBAs.size() > 1 ) {
                throw new IllegalStateException( ba + " has more than one source BioAssay in " + ee );
            }
            sourceBioAssayMap.put( ba, sourceBAs.iterator().next() );
        }
        return sourceBioAssayMap;
    }

    private Map<BioAssay, Integer> assignSampleToCellTypeIndex( Collection<BioAssay> cellBAs, CellLevelCharacteristics cta, Map<Characteristic, FactorValue> cellType2Factor ) {
        Map<BioAssay, Integer> cellTypes = new HashMap<>();
        for ( BioAssay ba : cellBAs ) {
            boolean found = false;
            List<Characteristic> types = cta.getCharacteristics();
            for ( int i = 0; i < types.size(); i++ ) {
                FactorValue fv = cellType2Factor.get( types.get( i ) );
                if ( fv != null && ba.getSampleUsed().getAllFactorValues().contains( fv ) ) {
                    cellTypes.put( ba, i );
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                throw new IllegalStateException( ba + " does not have an assigned cell type, make sure that its counterpart " + ba.getSampleUsed() + " has a cell type factor assigned." );
            }
        }
        return cellTypes;
    }

    /**
     * Compute the library size for each sample.
     *
     * @param vectors    vectors to iterate, consumed exactly once so that a database stream can be passed
     * @param numVectors expected number of vectors, only used to report progress
     */
    private double[] computeLibrarySize( Iterable<SingleCellExpressionDataVector> vectors, long numVectors,
            BioAssayDimension bad, CellLevelCharacteristics cta,
            @Nullable boolean[] mask,
            Map<BioAssay, BioAssay> sourceBioAssayMap, Map<BioAssay, Integer> sourceSampleToIndex,
            Map<BioAssay, Double> sourceSampleLibrarySizeAdjustments,
            SingleCellExpressionAggregationMethod method,
            boolean adjustLibrarySizes,
            List<Integer>[][] columnsBySourceSampleAndCellType,
            @Nullable Console console ) throws IllegalStateException {
        StopWatch timer = StopWatch.createStarted();
        log.info( "Computing library sizes for " + bad.getBioAssays().size() + " pseudo-bulk assays..." );
        List<BioAssay> samples = bad.getBioAssays();
        int numSamples = samples.size();
        int numSourceSamples = columnsBySourceSampleAndCellType.length;
        // library sizes of the sub-assays
        double[] librarySize = new double[numSamples];
        // library sizes of the source assays
        double[] sourceLibrarySize = new double[numSourceSamples];
        // allocated once, reset per vector below -- one pass per source sample per vector, not one per
        // (source sample, cell type) pseudo-bulk column.
        int[] sourceSampleStarts = new int[numSourceSamples];
        int[] sourceSampleEnds = new int[numSourceSamples];
        int w = 0;
        for ( SingleCellExpressionDataVector scv : vectors ) {
            Arrays.fill( sourceSampleStarts, -1 );
            Arrays.fill( sourceSampleEnds, -1 );
            PrimitiveType representation = scv.getQuantitationType().getRepresentation();
            Buffer scrv = scv.getDataAsBuffer();
            for ( int sourceSampleIndex = 0; sourceSampleIndex < numSourceSamples; sourceSampleIndex++ ) {
                List<Integer>[] byCellType = columnsBySourceSampleAndCellType[sourceSampleIndex];
                int start, end;
                if ( sourceSampleStarts[sourceSampleIndex] != -1 ) {
                    start = sourceSampleStarts[sourceSampleIndex];
                } else {
                    int after;
                    if ( sourceSampleIndex > 0 && sourceSampleStarts[sourceSampleIndex - 1] != -1 ) {
                        after = sourceSampleEnds[sourceSampleIndex - 1];
                    } else {
                        after = 0;
                    }
                    start = sourceSampleStarts[sourceSampleIndex] = getSampleStart( scv, sourceSampleIndex, after );
                }
                if ( sourceSampleEnds[sourceSampleIndex] != -1 ) {
                    end = sourceSampleEnds[sourceSampleIndex];
                } else {
                    end = sourceSampleEnds[sourceSampleIndex] = getSampleEnd( scv, sourceSampleIndex, start );
                }
                for ( int k = start; k < end; k++ ) {
                    int cellIndex = scv.getDataIndices()[k];
                    if ( mask != null && mask[cellIndex] ) {
                        continue;
                    }
                    double unscaledValue;
                    if ( method == SingleCellExpressionAggregationMethod.SUM ) {
                        unscaledValue = getDouble( scrv, k, representation );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                        unscaledValue = Math.exp( getDouble( scrv, k, representation ) );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                        unscaledValue = Math.expm1( getDouble( scrv, k, representation ) );
                    } else {
                        throw new UnsupportedOperationException( "Unsupported aggregation method: " + method );
                    }
                    // Every cell in this source sample's range contributes to the SOURCE sample's total
                    // exactly once per vector, regardless of how many cell-type columns the sample fans out
                    // into. The previous per-column loop added this sample's full per-gene sum once per cell
                    // type sharing it, over-counting sourceLibrarySize by that factor whenever a sample had
                    // more than one cell type -- i.e. essentially always. Only live with -adjustLibrarySizes.
                    sourceLibrarySize[sourceSampleIndex] += unscaledValue;
                    // a cell with no assigned cell type reads as CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC
                    // (-1): it never matches a real column, same as the old per-column `==` comparison skipped it.
                    int cellType = cta.getIndices()[cellIndex];
                    List<Integer> cols = cellType >= 0 && cellType < byCellType.length ? byCellType[cellType] : null;
                    if ( cols != null ) {
                        for ( int col : cols ) {
                            librarySize[col] += unscaledValue;
                        }
                    }
                }
            }
            w++;
            if ( w % 100 == 0 ) {
                if ( console != null ) {
                    console.printf( "Computing library size [%d/%d] @ %.2f vector/sec.\r", w, numVectors,
                            1000.0 * w / timer.getTime() );
                } else {
                    log.info( String.format( "Computing library size [%d/%d] @ %.2f vector/sec.", w, numVectors,
                            1000.0 * w / timer.getTime() ) );
                }
            }
        }
        log.info( String.format( "Computed library size for %d vectors @ %.2f vector/sec.", w,
                1000.0 * w / timer.getTime() ) );
        if ( adjustLibrarySizes ) {
            log.info( "Adjusting library sizes..." );
            for ( Map.Entry<BioAssay, Integer> e : sourceSampleToIndex.entrySet() ) {
                BioAssay sourceSample = e.getKey();
                int sourceSampleIndex = e.getValue();
                if ( sourceSample.getSequenceReadCount() == null )
                    continue;
                if ( sourceSample.getSequenceReadCount() < sourceLibrarySize[sourceSampleIndex] ) {
                    throw new IllegalStateException(
                            String.format( "The library size for %s (%.2f) exceeds the number of reads (%d).",
                                    sourceSample, sourceLibrarySize[sourceSampleIndex], sourceSample.getSequenceReadCount() ) );

                }
                sourceSampleLibrarySizeAdjustments.put( sourceSample, sourceSample.getSequenceReadCount() / sourceLibrarySize[e.getValue()] );
            }
            // adjust library sizes
            for ( int i = 0; i < librarySize.length; i++ ) {
                BioAssay sample = bad.getBioAssays().get( i );
                BioAssay sourceSample = sourceBioAssayMap.get( sample );
                Double adjustment = sourceSampleLibrarySizeAdjustments.get( sourceSample );
                if ( adjustment == null ) {
                    continue;
                }
                // this will scale the library size to the number of reads in the source sample instead of the number of
                // reads that we recorded in the vectors
                librarySize[i] *= adjustment;
            }
        }
        return librarySize;
    }

    /**
     * Aggregate the single-cell data to match the target BAD.
     * <p>
     * One pass per source sample, not one per (source sample, cell type) output column: a cell is read and
     * classified once, and its contribution (sum, expressed-cell count, sparsity bookkeeping) is applied
     * directly to whichever column its cell type maps to. The previous version re-scanned a source sample's
     * entire per-gene range once per cell type sharing it -- and, with sparsity metrics on
     * ({@code cellsByBioAssay}/{@code designElementsByBioAssay}/{@code cellByDesignElementByBioAssay} all
     * non-null, the default for a preferred QT), did so FOUR separate times per column (the sum loop plus
     * three independent {@link SingleCellSparsityMetrics} re-scans that each repeated the same start/end
     * lookup and cell-by-cell filtering). A source sample split into K cell types was ~4K scans of its data
     * per gene instead of one.
     *
     * @param performLog2cpm whether to perform log2cpm transformation or not, if provided librarySize must be set
     * @param librarySize    library size for each sample, used for log2cpm transformation
     */
    private double[] aggregateData(
            SingleCellExpressionDataVector scv,
            BioAssayDimension bad,
            CellLevelCharacteristics cta,
            @Nullable boolean[] mask,
            List<Integer>[][] columnsBySourceSampleAndCellType,
            SingleCellExpressionAggregationMethod method,
            @Nullable boolean[] cellsByBioAssay,
            @Nullable Map<BioAssay, Integer> designElementsByBioAssay,
            @Nullable Map<BioAssay, Integer> cellByDesignElementByBioAssay,
            boolean performLog2cpm,
            @Nullable double[] normalizationFactor,
            @Nullable double[] librarySize,
            int[] numberOfCells ) {
        Assert.isTrue( !performLog2cpm || ( normalizationFactor != null && librarySize != null ),
                "Normalization factors and library size must be provided for log2cpm transformation." );
        ScaleType scaleType = scv.getQuantitationType().getScale();
        List<BioAssay> samples = bad.getBioAssays();
        int numSamples = samples.size();
        double[] rv = new double[numSamples];
        // per-vector, per-column: was at least one expressed cell of this column's type seen for this gene?
        // (designElementsByBioAssay contributes 0 or 1 per vector per column, never a within-vector count)
        boolean[] designElementSeen = new boolean[numSamples];
        // per-vector, per-column: how many of this column's cells expressed this gene
        int[] cellsByDesignElementCount = new int[numSamples];
        Buffer scrv = scv.getDataAsBuffer();
        PrimitiveType representation = scv.getQuantitationType().getRepresentation();

        int numSourceSamples = columnsBySourceSampleAndCellType.length;
        int[] sourceSampleStarts = new int[numSourceSamples];
        int[] sourceSampleEnds = new int[numSourceSamples];
        Arrays.fill( sourceSampleStarts, -1 );
        Arrays.fill( sourceSampleEnds, -1 );
        for ( int sourceSampleIndex = 0; sourceSampleIndex < numSourceSamples; sourceSampleIndex++ ) {
            List<Integer>[] byCellType = columnsBySourceSampleAndCellType[sourceSampleIndex];
            int start, end;
            if ( sourceSampleStarts[sourceSampleIndex] != -1 ) {
                start = sourceSampleStarts[sourceSampleIndex];
            } else {
                int after;
                if ( sourceSampleIndex > 0 && sourceSampleStarts[sourceSampleIndex - 1] != -1 ) {
                    after = sourceSampleEnds[sourceSampleIndex - 1];
                } else {
                    after = 0;
                }
                start = sourceSampleStarts[sourceSampleIndex] = getSampleStart( scv, sourceSampleIndex, after );
            }
            if ( sourceSampleEnds[sourceSampleIndex] != -1 ) {
                end = sourceSampleEnds[sourceSampleIndex];
            } else {
                end = sourceSampleEnds[sourceSampleIndex] = getSampleEnd( scv, sourceSampleIndex, start );
            }
            for ( int k = start; k < end; k++ ) {
                int cellIndex = scv.getDataIndices()[k];
                if ( mask != null && mask[cellIndex] ) {
                    continue;
                }
                // a cell with no assigned cell type reads as CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC
                // (-1): it never matches a real column, same as the old per-column `==` comparison skipped it.
                int cellType = cta.getIndices()[cellIndex];
                List<Integer> cols = cellType >= 0 && cellType < byCellType.length ? byCellType[cellType] : null;
                if ( cols == null ) {
                    // no requested column pulls cells of this type from this sample
                    continue;
                }
                double d = getDouble( scrv, k, representation );
                boolean expressed = SingleCellSparsityMetrics.isExpressed( d, scaleType );
                for ( int col : cols ) {
                    if ( method == SingleCellExpressionAggregationMethod.SUM ) {
                        rv[col] += d;
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                        rv[col] += Math.exp( d );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                        rv[col] += Math.expm1( d );
                    } else {
                        throw new UnsupportedOperationException( "Unsupported aggregation method: " + method );
                    }
                    if ( expressed ) {
                        numberOfCells[col]++;
                        designElementSeen[col] = true;
                        cellsByDesignElementCount[col]++;
                    }
                }
                if ( expressed && cellsByBioAssay != null && !cellsByBioAssay[cellIndex] ) {
                    cellsByBioAssay[cellIndex] = true;
                }
            }
        }

        for ( int i = 0; i < numSamples; i++ ) {
            if ( performLog2cpm ) {
                if ( librarySize[i] == 0 ) {
                    // this is technically a 0/0 situation
                    rv[i] = Double.NaN;
                } else {
                    rv[i] = Math.log( 1e6 * normalizationFactor[i] * ( rv[i] + 0.5 ) / ( librarySize[i] + 1.0 ) ) / Math.log( 2 );
                }
            } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                rv[i] = Math.log( rv[i] );
            } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                rv[i] = Math.log1p( rv[i] );
            } else {
                throw new UnsupportedOperationException( "Unsupported aggregation method: " + method );
            }

            if ( designElementsByBioAssay != null ) {
                BioAssay sample = samples.get( i );
                int seen = designElementSeen[i] ? 1 : 0;
                designElementsByBioAssay.compute( sample, ( k, v ) -> ( v != null ? v : 0 ) + seen );
            }
            if ( cellByDesignElementByBioAssay != null ) {
                BioAssay sample = samples.get( i );
                int count = cellsByDesignElementCount[i];
                cellByDesignElementByBioAssay.compute( sample, ( k, v ) -> ( v != null ? v : 0 ) + count );
            }
        }

        return rv;
    }

    private double getDouble( Buffer buffer, int k, PrimitiveType representation ) {
        switch ( representation ) {
            case FLOAT:
                return ( ( FloatBuffer ) buffer ).get( k );
            case DOUBLE:
                return ( ( DoubleBuffer ) buffer ).get( k );
            case INT:
                return ( ( IntBuffer ) buffer ).get( k );
            case LONG:
                return ( ( LongBuffer ) buffer ).get( k );
            default:
                throw new UnsupportedOperationException( "Unsupported representation " + representation );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAggregated( ExpressionExperiment ee, QuantitationType quantitationType ) {
        if ( quantitationType.getIsAggregated() ) {
            return true;
        }
        // TODO: remove this once all QTs have the isAggregated flag set correctly
        //       processed vectors also contain "aggregated by" in their name, so we also need to check the vector type
        if ( quantitationType.getName().contains( "aggregated by" ) ) {
            Class<? extends DataVector> dataVectorType = quantitationTypeService.getDataVectorType( quantitationType );
            return dataVectorType != null && RawExpressionDataVector.class.isAssignableFrom( dataVectorType );
        }
        return false;
    }

    @Override
    @Transactional
    public int removeAggregatedVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return removeAggregatedVectors( ee, qt, false );
    }

    @Override
    @Transactional
    public int removeAggregatedVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension ) {
        // this is needed because the raw vectors must be loaded
        ee = expressionExperimentService.reload( ee );
        qt = quantitationTypeService.reload( qt );
        // gather all the assays that the aggregated vectors were using
        Collection<BioAssay> bioAssays;
        BioAssayDimension dimension = expressionExperimentService.getBioAssayDimension( ee, qt, RawExpressionDataVector.class );
        if ( dimension != null ) {
            bioAssays = dimension.getBioAssays();
        } else {
            log.warn( "No BioAssayDimension found for " + qt + " in " + ee + "." );
            bioAssays = Collections.emptyList();
        }
        int removedVectors = expressionExperimentService.removeRawDataVectors( ee, qt, keepDimension );
        // clear sparsity metrics if we are removing the preferred QT
        if ( qt.getIsPreferred() ) {
            log.info( "Clearing sparsity metrics on " + bioAssays.size() + " assays since we're removing preferred aggregated vectors..." );
            for ( BioAssay ba : bioAssays ) {
                ba.setNumberOfCells( null );
                ba.setNumberOfDesignElements( null );
                ba.setNumberOfCellsByDesignElements( null );
                bioAssayService.update( ba );
            }
        }
        return removedVectors;
    }

    /**
     * Methods for aggregating single-cell expression data.
     */
    public enum SingleCellExpressionAggregationMethod {
        /**
         * Aggregate data by summing it.
         */
        SUM,
        /**
         * Equivalent to {@link #SUM} for log-transformed data.
         */
        LOG_SUM,
        /**
         * Equivalent to {@link #SUM} for data transformed by {@code log 1 + X}
         */
        LOG1P_SUM
    }
}
