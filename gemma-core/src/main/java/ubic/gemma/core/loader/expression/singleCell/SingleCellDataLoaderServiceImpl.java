package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoBioAssayMapper;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.util.mapper.*;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.common.quantitationtype.NonUniqueQuantitationTypeByNameException;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentGeoService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.createStreamMonitor;

@Service
@CommonsLog
public class SingleCellDataLoaderServiceImpl implements SingleCellDataLoaderService {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentGeoService expressionExperimentGeoService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Value("${gemma.download.path}/singleCellData")
    private Path singleCellDataBasePath;

    @Value("${cellranger.dir}")
    private Path cellRangerPrefix;

    @Value("${python.exe}")
    private Path pythonExecutable;

    @Value("${gemma.scratch.dir}")
    private Path scratchDir;

    @Override
    @Transactional
    public QuantitationType load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoaderConfig config ) {
        Assert.isNull( config.getDataPath(), "An explicit path cannot be provided when detecting the data type automatically." );
        ee = expressionExperimentService.loadOrFail( ee.getId() );
        try ( SingleCellDataLoader loader = getLoader( ee, config ) ) {
            return load( ee, platform, loader, config );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional
    public QuantitationType load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        ee = expressionExperimentService.loadOrFail( ee.getId() );
        if ( config.getDataPath() != null ) {
            log.info( "Loading single-cell data for " + ee + " from " + config.getDataPath() + "..." );
        }
        try ( SingleCellDataLoader loader = getLoader( ee, dataType, config ) ) {
            return load( ee, platform, loader, config );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional
    public Map<BioAssay, SequencingMetadata> loadSequencingMetadata( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        return loadSequencingMetadata( getLoader( ee, config ), getSingleCellDimension( ee, false, config ) );
    }

    @Override
    @Transactional
    public Collection<CellTypeAssignment> loadCellTypeAssignments( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        ee = expressionExperimentService.loadOrFail( ee.getId() );
        try ( SingleCellDataLoader loader = getLoader( ee, config ) ) {
            return loadCellTypeAssignments( loader, ee, config );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional
    public Collection<CellTypeAssignment> loadCellTypeAssignments( ExpressionExperiment ee, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        ee = expressionExperimentService.loadOrFail( ee.getId() );
        try ( SingleCellDataLoader loader = getLoader( ee, dataType, config ) ) {
            return loadCellTypeAssignments( loader, ee, config );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private Collection<CellTypeAssignment> loadCellTypeAssignments( SingleCellDataLoader loader, ExpressionExperiment ee, SingleCellDataLoaderConfig config ) throws IOException {
        QuantitationType qt = getQuantitationType( ee, config );
        SingleCellDimension dimension = getSingleCellDimension( ee, true, config );
        Set<CellTypeAssignment> ctas = loader.getCellTypeAssignments( dimension );
        applyPreferredCellTypeAssignment( ctas, config );
        return DescribableUtils.addAllByName( dimension.getCellTypeAssignments(), ctas,
                // because we're dealing with a persistent dimension, we need to use the service to add/remove CTAs
                ( ignored, cta ) -> singleCellExpressionExperimentService.addCellTypeAssignment( ee, qt, dimension, cta, config.isRecreateCellTypeFactorIfNecessary(), config.isIgnoreCompatibleCellTypeFactor() ),
                ( ignored, cta ) -> singleCellExpressionExperimentService.removeCellTypeAssignmentByName( ee, dimension, requireNonNull( cta.getName() ) ),
                config.isReplaceExistingCellTypeAssignment(), config.isReplaceExistingCellTypeAssignment() );
    }

    @Override
    @Transactional
    public Collection<CellLevelCharacteristics> loadOtherCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        ee = expressionExperimentService.loadOrFail( ee.getId() );
        try ( SingleCellDataLoader loader = getLoader( ee, config ) ) {
            return loadOtherCellLevelCharacteristics( loader, ee, config );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional
    public Collection<CellLevelCharacteristics> loadOtherCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        ee = expressionExperimentService.loadOrFail( ee.getId() );
        try ( SingleCellDataLoader loader = getLoader( ee, dataType, config ) ) {
            return loadOtherCellLevelCharacteristics( loader, ee, config );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private Collection<CellLevelCharacteristics> loadOtherCellLevelCharacteristics( SingleCellDataLoader loader, ExpressionExperiment ee, SingleCellDataLoaderConfig config ) throws IOException {
        SingleCellDimension dimension = getSingleCellDimension( ee, true, config );
        Set<CellLevelCharacteristics> clcs = loader.getOtherCellLevelCharacteristics( dimension );
        return DescribableUtils.addAllByName( dimension.getCellLevelCharacteristics(), clcs,
                // because we're dealing with a persistent dimension, we need to use the service to add/remove CLCs
                ( ignored, clc ) -> singleCellExpressionExperimentService.addCellLevelCharacteristics( ee, dimension, clc ),
                ( ignored, clc ) -> singleCellExpressionExperimentService.removeCellLevelCharacteristicsByName( ee, dimension, requireNonNull( clc.getName() ) ),
                config.isReplaceExistingOtherCellLevelCharacteristics(), config.isIgnoreExistingOtherCellLevelCharacteristics() );
    }

    private QuantitationType getQuantitationType( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        if ( config.getQuantitationTypeName() != null ) {
            try {
                QuantitationType qt = quantitationTypeService.findByNameAndVectorType( ee, config.getQuantitationTypeName(), SingleCellExpressionDataVector.class );
                if ( qt == null ) {
                    throw new IllegalArgumentException( "No quantitation type with name " + config.getQuantitationTypeName() + " for " + ee + "." );
                }
                return qt;
            } catch ( NonUniqueQuantitationTypeByNameException e ) {
                throw new RuntimeException( e );
            }
        } else {
            return singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                    .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred single-cell dimension." ) );
        }
    }

    /**
     * @param includeCellIds whether to include cell IDs in the dimension. Note that if you choose not to, you will not
     *                       be able to access {@link SingleCellDimension#getCellTypeAssignments()} and
     *                       {@link SingleCellDimension#getCellLevelCharacteristics()} since lazy-initialization hasn't
     *                       been implemented.
     */
    private SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, boolean includeCellIds, SingleCellDataLoaderConfig config ) {
        if ( config.getQuantitationTypeName() != null ) {
            try {
                QuantitationType qt = quantitationTypeService.findByNameAndVectorType( ee, config.getQuantitationTypeName(), SingleCellExpressionDataVector.class );
                if ( qt == null ) {
                    throw new IllegalArgumentException( "No quantitation type with name " + config.getQuantitationTypeName() + " for " + ee + "." );
                }
                if ( includeCellIds ) {
                    return singleCellExpressionExperimentService.getSingleCellDimension( ee, qt );
                } else {
                    return singleCellExpressionExperimentService.getSingleCellDimensionWithoutCellIds( ee, qt );
                }
            } catch ( NonUniqueQuantitationTypeByNameException e ) {
                throw new RuntimeException( e );
            }
        } else {
            if ( includeCellIds ) {
                return singleCellExpressionExperimentService.getPreferredSingleCellDimension( ee )
                        .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred single-cell dimension." ) );
            } else {
                return singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee )
                        .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred single-cell dimension." ) );
            }
        }
    }

    private QuantitationType load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoader loader, SingleCellDataLoaderConfig config ) {
        ee = requireNonNull( singleCellExpressionExperimentService.loadWithSingleCellVectors( ee.getId() ) );
        Assert.isTrue( platform.getPrimaryTaxon().equals( expressionExperimentService.getTaxon( ee ) ),
                "Platform primary taxon does not match dataset." );
        SingleCellDimension dim = loadSingleCellDimension( loader, ee.getBioAssays() );
        QuantitationType qt = loadQuantitationType( loader, ee, config );
        Collection<CellTypeAssignment> loadedCtas = loadCellTypeAssignments( loader, dim, config );
        if ( !loadedCtas.isEmpty() ) {
            log.info( "Loaded " + loadedCtas.size() + " cell type assignments." );
        }
        Collection<CellLevelCharacteristics> loadedClcs = loadOtherCellLevelCharacteristics( loader, dim, config );
        if ( !loadedClcs.isEmpty() ) {
            log.info( "Loaded " + loadedClcs.size() + " cell-level characteristics." );
        }
        Map<BioAssay, SequencingMetadata> loadedSm = loadSequencingMetadata( loader, dim );
        if ( !loadedSm.isEmpty() ) {
            log.info( "Loaded sequencing metadata for " + loadedSm.size() + " assays." );
        }
        loadVectors( loader, ee, dim, qt, platform, config );
        return qt;
    }

    private SingleCellDimension loadSingleCellDimension( SingleCellDataLoader loader, Set<BioAssay> bioAssays ) {
        try {
            return loader.getSingleCellDimension( bioAssays );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private QuantitationType loadQuantitationType( SingleCellDataLoader loader, ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        String qtName = config.getQuantitationTypeName();
        Set<QuantitationType> qts;
        try {
            qts = loader.getQuantitationTypes();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        Collection<QuantitationType> availableQts = qts;
        if ( qtName != null ) {
            qts = qts.stream().filter( q -> q.getName().equals( qtName ) ).collect( Collectors.toSet() );
        }
        QuantitationType qt;
        if ( qts.isEmpty() ) {
            throw new IllegalArgumentException( String.format( "No quantitation available%s. Choose one among:\n\t%s",
                    qtName != null ? " with name " + qtName : "",
                    availableQts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
        } else if ( qts.size() > 1 ) {
            throw new IllegalArgumentException( String.format( "More than one available quantitation type%s. Choose one among:\n%s",
                    qtName != null ? " with name " + qtName : "",
                    availableQts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
        } else {
            qt = qts.iterator().next();
        }

        applyQuantitationTypeOverrides( qt, config );

        if ( config.isReplaceExistingQuantitationType() ) {
            // find the persistent QT matching the data
            QuantitationType existingQt = quantitationTypeService.find( ee, qt, SingleCellExpressionDataVector.class );
            if ( existingQt == null ) {
                log.warn( "Could not find a QT matching " + qt + " in " + ee + ", will attempt to find one by name only." );
                try {
                    existingQt = quantitationTypeService.findByNameAndVectorType( ee, qt.getName(), SingleCellExpressionDataVector.class );
                } catch ( NonUniqueQuantitationTypeByNameException e ) {
                    throw new RuntimeException( e );
                }
            }
            if ( existingQt != null ) {
                log.info( "Data will be replaced for " + existingQt + "..." );
                qt = existingQt;
                // since we're retrieving it by name only, it might not have the desired overrides, so re-apply them
                applyQuantitationTypeOverrides( qt, config );
            } else {
                availableQts = singleCellExpressionExperimentService.getSingleCellQuantitationTypes( ee );
                throw new IllegalArgumentException( String.format( "%s does not match any existing single-cell quantitation type. Choose one among:\n\t%s",
                        qt,
                        availableQts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
            }
        } else {
            if ( ee.getQuantitationTypes().contains( qt ) ) {
                // this check is also done in SingleCellExpressionExperimentService.addSingleCellDataVectors(), but
                // after loading the vectors from disk which is time-consuming
                throw new IllegalArgumentException( ee + " already has a quantitation type matching " + qt + ". Set replaceExistingQuantitationType to replace existing vectors instead or use a different name by setting quantitationTypeNewName." );
            }
            log.info( "Data will be added for " + qt + "..." );
        }

        if ( config.isMarkQuantitationTypeAsRecomputedFromRawData() ) {
            log.info( "Marking " + qt + " as recomputed from raw data." );
            qt.setIsRecomputedFromRawData( true );
        }

        if ( config.isMarkQuantitationTypeAsPreferred() ) {
            log.info( "Marking " + qt + " as preferred for single-cell." );
            qt.setIsSingleCellPreferred( true );
        }

        return qt;
    }

    private void applyQuantitationTypeOverrides( QuantitationType qt, SingleCellDataLoaderConfig config ) {
        // apply any overrides
        if ( config.getQuantitationTypeNewName() != null ) {
            log.info( "Overriding the name to " + config.getQuantitationTypeNewName() + " (was " + qt.getName() + ")." );
            qt.setName( config.getQuantitationTypeNewName() );
        }
        if ( config.getQuantitationTypeNewType() != null ) {
            log.info( "Overriding the type to " + config.getQuantitationTypeNewType() + " (was " + qt.getType() + ")." );
            qt.setType( config.getQuantitationTypeNewType() );
        }
        if ( config.getQuantitationTypeNewScaleType() != null ) {
            log.info( "Overriding the scale type to " + config.getQuantitationTypeNewScaleType() + " (was " + qt.getScale() + ")." );
            qt.setScale( config.getQuantitationTypeNewScaleType() );
        }
        if ( config.isPreferSinglePrecision() ) {
            log.info( "Single-precision is preferred, will adjust the quantitation type accordingly." );
            switch ( qt.getRepresentation() ) {
                case LONG:
                    qt.setRepresentation( PrimitiveType.INT );
                    log.info( "Will use INT instead of LONG for " + qt + "." );
                    break;
                case DOUBLE:
                    qt.setRepresentation( PrimitiveType.FLOAT );
                    log.info( "Will use FLOAT instead of DOUBLE for " + qt + "." );
                    break;
                case FLOAT:
                case INT:
                    log.info( "Already using single-precision for " + qt + "." );
                    break;
                default:
                    log.warn( "The representation of " + qt + " is nether a long or double, will not change it." );
                    break;
            }
        }
    }

    private Collection<CellTypeAssignment> loadCellTypeAssignments( SingleCellDataLoader loader, SingleCellDimension dim, SingleCellDataLoaderConfig config ) {
        Assert.isNull( dim.getId(), "This method can only load a CTA in a non-persistent dimension." );
        try {
            Set<CellTypeAssignment> ctas = loader.getCellTypeAssignments( dim );
            applyPreferredCellTypeAssignment( ctas, config );
            return DescribableUtils.addAllByName( dim.getCellTypeAssignments(), ctas,
                    config.isReplaceExistingCellTypeAssignment(),
                    config.isIgnoreExistingCellTypeAssignment() );
        } catch ( UnsupportedOperationException e ) {
            log.info( e.getMessage() ); // no need for the stacktrace
            return Collections.emptySet();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private void applyPreferredCellTypeAssignment( Collection<CellTypeAssignment> cellTypeAssignments, SingleCellDataLoaderConfig config ) {
        CellTypeAssignment preferredCta;
        if ( config.getPreferredCellTypeAssignmentName() != null ) {
            String name = config.getPreferredCellTypeAssignmentName();
            Set<CellTypeAssignment> preferredCtas = cellTypeAssignments.stream()
                    .filter( cta -> name.equals( cta.getName() ) )
                    .collect( Collectors.toSet() );
            if ( preferredCtas.isEmpty() ) {
                String possibleNames = cellTypeAssignments.stream()
                        .map( CellTypeAssignment::getName )
                        .collect( Collectors.joining( ", " ) );
                throw new IllegalStateException( "No cell type assignment with name " + name + ", possible values are: " + possibleNames + "." );
            } else if ( preferredCtas.size() > 1 ) {
                throw new IllegalStateException( "More than one cell type assignment with name " + name + "." );
            }
            preferredCta = preferredCtas.iterator().next();
            log.info( "Marking " + preferredCta + " as preferred." );
            preferredCta.setPreferred( true );
        } else if ( config.isMarkSingleCellTypeAssignmentAsPreferred() ) {
            if ( cellTypeAssignments.isEmpty() ) {
                throw new IllegalStateException( "No cell type assignment." );
            } else if ( cellTypeAssignments.size() > 1 ) {
                throw new IllegalStateException( "More than one cell type assignment." );
            }
            preferredCta = cellTypeAssignments.iterator().next();
            log.info( "Marking the only cell-type assignment " + preferredCta + " as preferred." );
            preferredCta.setPreferred( true );
        }
    }

    private Collection<CellLevelCharacteristics> loadOtherCellLevelCharacteristics( SingleCellDataLoader loader, SingleCellDimension dim, SingleCellDataLoaderConfig config ) {
        Assert.isNull( dim.getId() );
        try {
            return DescribableUtils.addAllByName( dim.getCellLevelCharacteristics(),
                    loader.getOtherCellLevelCharacteristics( dim ),
                    config.isReplaceExistingOtherCellLevelCharacteristics(),
                    config.isIgnoreExistingOtherCellLevelCharacteristics() );
        } catch ( UnsupportedOperationException e ) {
            log.info( e.getMessage() ); // no need for the stacktrace
            return Collections.emptySet();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private Map<BioAssay, SequencingMetadata> loadSequencingMetadata( SingleCellDataLoader loader, SingleCellDimension dim ) {
        Map<BioAssay, SequencingMetadata> sequencingMetadata;
        try {
            sequencingMetadata = loader.getSequencingMetadata( dim );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        for ( BioAssay ba : dim.getBioAssays() ) {
            SequencingMetadata sm = sequencingMetadata.get( ba );
            if ( sm == null ) {
                log.warn( "There is no sequencing metadata available for " + ba + ", ignoring." );
                continue;
            }
            if ( sm.getReadCount() != null ) {
                if ( sm.getReadCount() <= 0 ) {
                    throw new IllegalStateException( "Read count must be strictly positive." );
                }
                ba.setSequenceReadCount( sm.getReadCount() );
            }
            if ( sm.getReadLength() != null ) {
                if ( sm.getReadLength() <= 0 ) {
                    throw new IllegalStateException( "Read length must be strictly positive." );
                }
                ba.setSequenceReadLength( sm.getReadLength() );
            }
            if ( sm.getIsPaired() != null ) {
                ba.setSequencePairedReads( sm.getIsPaired() );
            }
            bioAssayService.update( ba );
        }
        return sequencingMetadata;
    }

    private void loadVectors( SingleCellDataLoader loader, ExpressionExperiment ee, SingleCellDimension dim, QuantitationType qt, ArrayDesign platform, SingleCellDataLoaderConfig config ) {
        DesignElementMapper mapper;
        Set<SingleCellExpressionDataVector> vectors;
        String mappingDetails;
        try {
            Set<String> genes = loader.getGenes();
            platform = arrayDesignService.thawCompositeSequences( platform );
            mapper = createElementsMapping( platform, genes );
            EntityMapper.MappingStatistics stats = mapper.forCandidates( platform ).getMappingStatistics( genes );
            mappingDetails = String.format( "Genes are mapped by %s. %.2f%% of genes are mapped and %.2f%% of the platform elements are covered.",
                    mapper.getName(), 100.0 * stats.getOverlap(), 100.0 * stats.getCoverage() );
            log.info( mappingDetails );
            loader.setDesignElementToGeneMapper( mapper );
            vectors = loader.loadVectors( platform.getCompositeSequences(), dim, qt )
                    .peek( createStreamMonitor( ee, qt, getClass().getName(), 100, ( long ) ( stats.getCoverage() * platform.getCompositeSequences().size() ), config.getConsole() ) )
                    .collect( Collectors.toSet() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        int switched = switchBioAssaysToTargetPlatform( dim, platform );
        if ( switched > 0 ) {
            log.info( String.format( "Switched %d bioassays to %s.", switched, platform ) );
        }
        if ( config.isReplaceExistingQuantitationType() ) {
            int replacedVectors = singleCellExpressionExperimentService.replaceSingleCellDataVectors( ee, qt, vectors, mappingDetails, config.isRecreateCellTypeFactorIfNecessary(), config.isIgnoreCompatibleCellTypeFactor() );
            log.info( String.format( "Replaced %d single-cell vectors in %s.", replacedVectors, qt ) );
        } else {
            int addedVectors = singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors, mappingDetails, config.isRecreateCellTypeFactorIfNecessary(), config.isIgnoreCompatibleCellTypeFactor() );
            log.info( String.format( "Added %d single-cell vectors to %s in %s.", addedVectors, ee, qt ) );
        }
    }

    /**
     * Default mapper for design elements which implement a few strategies:
     * <ol>
     *     <li>CompositeSequence name</li>
     *     <li>NCBI Gene ID</li>
     *     <li>Ensembl ID -- version numbers are ignored</li>
     *     <li>Official symbols</li>
     *     <li>Gene names</li>
     * </ol>
     *
     * @author poirigui
     */
    private DesignElementMapper createElementsMapping( ArrayDesign platform, Collection<String> geneIdentifiers ) {
        // create mapping by precedence of ID type
        Map<CompositeSequence, Set<Gene>> cs2g = arrayDesignService.getGenesByCompositeSequence( platform );
        List<DesignElementMapper> mappers = new ArrayList<>();
        // highest precedence is the probe name
        Map<String, CompositeSequence> elementsMapping = new HashMap<>();
        for ( CompositeSequence cs : platform.getCompositeSequences() ) {
            elementsMapping.putIfAbsent( cs.getName(), cs );
        }
        mappers.add( new MapBasedDesignElementMapper( "Design Element Name", elementsMapping ) );
        // then look for gene, E
        mappers.add( new NcbiIdDesignElementMapper( cs2g ) );
        mappers.add( new EnsemblIdDesignElementMapper( cs2g ) );
        mappers.add( new OfficialSymbolDesignElementMapper( cs2g ) );
        mappers.add( new GeneNameDesignElementMapper( cs2g ) );
        DesignElementMapper bestMapper = null;
        long numGenes = 0;
        for ( DesignElementMapper mapper : mappers ) {
            if ( log.isDebugEnabled() ) {
                log.debug( mapper.getName() + ": " + mapper.forCandidates( platform ).getMappingStatistics( geneIdentifiers ) );
            }
            long g = geneIdentifiers.stream().filter( mapper.forCandidates( platform )::contains ).count();
            if ( g > numGenes ) {
                bestMapper = mapper;
                numGenes = g;
            }
        }
        if ( bestMapper == null ) {
            throw new IllegalArgumentException( "None of the available gene identifier mapping strategy applies to the given gene identifiers." );
        }
        return bestMapper;
    }

    /**
     * Switch the BioAssays from the single-cell dimension to be onto the given target platform.
     * <p>
     * This must be done prior to adding vector so that the expression data is in a reasonable state (i.e. platforms
     * of a BioAssay matches the platform used by the design elements of the vectors).
     * TODO: unify that logic with DataUpdaterImpl.switchBioAssaysToTargetPlatform()
     */
    private int switchBioAssaysToTargetPlatform( SingleCellDimension dimension, ArrayDesign targetPlatform ) {
        int switched = 0;
        for ( BioAssay ba : dimension.getBioAssays() ) {
            if ( !ba.getArrayDesignUsed().equals( targetPlatform ) ) {
                ArrayDesign previousPlatform = ba.getArrayDesignUsed();
                if ( ba.getOriginalPlatform() == null ) {
                    ba.setOriginalPlatform( previousPlatform );
                }
                ba.setArrayDesignUsed( targetPlatform );
                bioAssayService.update( ba );
                log.info( "Switched " + ba + " from " + previousPlatform + " to " + targetPlatform + "." );
                switched++;
            }
        }
        return switched;
    }

    /**
     * Obtain a loader with an automatically detected data type.
     */
    private SingleCellDataLoader getLoader( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        SingleCellDataType dataType;
        if ( Files.exists( getAnnDataFile( ee ) ) ) {
            dataType = SingleCellDataType.ANNDATA;
        } else if ( Files.exists( getSeuratDiskFile( ee ) ) ) {
            dataType = SingleCellDataType.SEURAT_DISK;
        } else if ( Files.exists( getMexDir( ee ) ) ) {
            dataType = SingleCellDataType.MEX;
        } else if ( Files.exists( getLoomFile( ee ) ) ) {
            dataType = SingleCellDataType.LOOM;
        } else {
            dataType = SingleCellDataType.NULL;
            log.warn( "No single-cell data found for " + ee + " in " + singleCellDataBasePath + ", using the null loader." );
        }
        return getLoader( ee, dataType, config );
    }

    /**
     * Obtain a loader for a specific data type.
     */
    private SingleCellDataLoader getLoader( ExpressionExperiment ee, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        switch ( dataType ) {
            case ANNDATA:
                return getAnnDataLoader( ee, config );
            case SEURAT_DISK:
                return getSeuratDiskLoader();
            case MEX:
                return getMexLoader( ee, config );
            case LOOM:
                return getLoomLoader();
            case NULL:
                return getNullLoader( ee, config );
            default:
                throw new IllegalArgumentException( "Unknown single-cell data type " + dataType + "." );
        }
    }

    private SingleCellDataLoader getAnnDataLoader( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        BioAssayMapper bioAssayMapper = getBioAssayMapper( ee, config );
        Path p = config.getDataPath() != null ? config.getDataPath() : getAnnDataFile( ee );
        AnnDataSingleCellDataLoaderConfigurer annDataConfigurer = new AnnDataSingleCellDataLoaderConfigurer( p, ee.getBioAssays(), bioAssayMapper );
        annDataConfigurer.setPythonExecutable( pythonExecutable );
        annDataConfigurer.setScratchDir( scratchDir );
        return configureLoader( annDataConfigurer, bioAssayMapper, config );
    }

    private SingleCellDataLoader getSeuratDiskLoader() {
        throw new UnsupportedOperationException( "Seurat Disk is not directly supported, convert it to AnnData first." );
    }

    private SingleCellDataLoader getMexLoader( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        BioAssayMapper bioAssayMapper = getBioAssayMapper( ee, config );
        Path dir;
        if ( config.getDataPath() != null ) {
            dir = config.getDataPath();
        } else {
            dir = getMexDir( ee );
        }
        GeoSeries geoSeries;
        if ( ee.getAccession() != null && ee.getAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            log.info( ee + " originates from GEO, will include its series metadata." );
            geoSeries = expressionExperimentGeoService.getGeoSeries( ee );
        } else {
            geoSeries = null;
        }

        return configureLoader( new MexSingleCellDataLoaderConfigurer( dir, ee.getBioAssays(), bioAssayMapper, cellRangerPrefix, geoSeries ), bioAssayMapper, config );
    }

    private SingleCellDataLoader getLoomLoader() {
        throw new UnsupportedOperationException( "Loom is not supported yet." );
    }

    private SingleCellDataLoader getNullLoader( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        return configureLoader( new NullSingleCellDataLoaderConfigurer(), getBioAssayMapper( ee, config ), config );
    }

    private SingleCellDataLoader configureLoader( SingleCellDataLoaderConfigurer<?> loaderConfigurer, BioAssayMapper bioAssayMapper, SingleCellDataLoaderConfig config ) {
        if ( config.getSequencingMetadataFile() != null || config.getDefaultSequencingMetadata() != null ) {
            loaderConfigurer = new SequencingMetadataFileSingleCellDataLoaderConfigurer( loaderConfigurer, bioAssayMapper );
        }
        if ( config.getCellTypeAssignmentFile() != null || config.getOtherCellLevelCharacteristicsFile() != null ) {
            loaderConfigurer = new GenericMetadataSingleCellDataLoaderConfigurer( loaderConfigurer, bioAssayMapper );
        }
        return loaderConfigurer.configureLoader( config );
    }

    /**
     * Select an appropriate {@link BioAssayMapper} implementation for a given dataset.
     * <p>
     * In most cases, this should be a {@link GeoBioAssayMapper}.
     */
    private BioAssayMapper getBioAssayMapper( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        // select the best strategy for mapping sample names to assays
        BioAssayMapper mapper;
        if ( ee.getAccession() != null && ee.getAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            log.info( String.format( "%s has a GEO accession, using %s for matching samples names to assays.",
                    ee, GeoBioAssayMapper.class.getSimpleName() ) );
            mapper = new GeoBioAssayMapper();
        } else {
            log.info( String.format( "%s does not have a GEO accession, using %s for matching sample names to assays.",
                    ee, SimpleBioAssayMapper.class.getSimpleName() ) );
            mapper = new SimpleBioAssayMapper();
        }
        if ( config.getRenamingFile() != null ) {
            log.info( "Applying a sample renaming file " + config.getRenamingFile() + " to the sample name mapping strategy." );
            try {
                return new RenamingBioAssayMapperParser( mapper )
                        .parse( config.getRenamingFile() );
            } catch ( IOException e ) {
                throw new RuntimeException( "Failed to parse renaming file " + config.getRenamingFile() + ".", e );
            }
        } else {
            return mapper;
        }
    }

    private Path getAnnDataFile( ExpressionExperiment ee ) {
        if ( ee.getAccession() == null ) {
            return singleCellDataBasePath
                    .resolve( "local" )
                    .resolve( ee.getShortName() + ".h5ad" );
        }
        return singleCellDataBasePath
                .resolve( ee.getAccession().getExternalDatabase().getName() )
                .resolve( ee.getAccession().getAccession() + ".h5ad" );
    }

    public Path getSeuratDiskFile( ExpressionExperiment ee ) {
        if ( ee.getAccession() == null ) {
            return singleCellDataBasePath
                    .resolve( "local" )
                    .resolve( ee.getShortName() + ".h5Seurat" );
        }
        return singleCellDataBasePath
                .resolve( ee.getAccession().getExternalDatabase().getName() )
                .resolve( ee.getShortName() + ".h5Seurat" );
    }

    private Path getMexDir( ExpressionExperiment ee ) {
        if ( ee.getAccession() == null ) {
            return singleCellDataBasePath
                    .resolve( "local" )
                    .resolve( ee.getShortName() );
        }
        return singleCellDataBasePath
                .resolve( ee.getAccession().getExternalDatabase().getName() )
                .resolve( ee.getAccession().getAccession() );
    }

    private Path getLoomFile( ExpressionExperiment ee ) {
        if ( ee.getAccession() == null ) {
            return singleCellDataBasePath
                    .resolve( "local" )
                    .resolve( ee.getShortName() + ".loom" );
        }
        return singleCellDataBasePath
                .resolve( ee.getAccession().getExternalDatabase().getName() )
                .resolve( ee.getAccession().getAccession() + ".loom" );
    }
}
