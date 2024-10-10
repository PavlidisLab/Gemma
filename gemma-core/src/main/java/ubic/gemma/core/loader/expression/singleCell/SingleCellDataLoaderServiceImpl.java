package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoBioAssayToSampleNameMatcher;
import ubic.gemma.model.common.description.ExternalDatabases;
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
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

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
    private QuantitationTypeService quantitationTypeService;

    @Value("${gemma.download.path}/singleCellData")
    private Path singleCellDataBasePath;

    @Override
    @Transactional
    public QuantitationType load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoaderConfig config ) {
        Assert.isNull( config.getDataPath(), "An explicit path cannot be provided when detecting the data type automatically." );
        return load( ee, platform, getLoader( ee, config ), config );
    }

    @Override
    @Transactional
    public QuantitationType load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        if ( config.getDataPath() != null ) {
            log.info( "Loading single-cell data for " + ee + " from " + config.getDataPath() + "..." );
        }
        return load( ee, platform, getLoader( ee, dataType, config ), config );
    }

    @Override
    @Transactional
    public Collection<CellTypeAssignment> loadCellTypeAssignments( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        try {
            SingleCellDimension dimension = getSingleCellDimension( ee, config );
            Collection<CellTypeAssignment> created = new HashSet<>();
            for ( CellTypeAssignment cta : getLoader( ee, config ).getCellTypeAssignments( dimension ) ) {
                created.add( singleCellExpressionExperimentService.addCellTypeAssignment( ee, dimension, cta ) );
            }
            return created;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional
    public Collection<CellTypeAssignment> loadCellTypeAssignments( ExpressionExperiment ee, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        try {
            SingleCellDimension dimension = getSingleCellDimension( ee, config );
            Collection<CellTypeAssignment> created = new HashSet<>();
            for ( CellTypeAssignment cta : getLoader( ee, dataType, config ).getCellTypeAssignments( dimension ) ) {
                created.add( singleCellExpressionExperimentService.addCellTypeAssignment( ee, dimension, cta ) );
            }
            return created;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional
    public Collection<CellLevelCharacteristics> loadOtherCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        try {
            SingleCellDimension dimension = getSingleCellDimension( ee, config );
            Collection<CellLevelCharacteristics> created = new HashSet<>();
            for ( CellLevelCharacteristics clc : getLoader( ee, config ).getOtherCellLevelCharacteristics( dimension ) ) {
                created.add( singleCellExpressionExperimentService.addCellLevelCharacteristics( ee, dimension, clc ) );
            }
            return created;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional
    public Collection<CellLevelCharacteristics> loadOtherCellLevelCharacteristics( ExpressionExperiment ee, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        try {
            SingleCellDimension dimension = getSingleCellDimension( ee, config );
            Collection<CellLevelCharacteristics> created = new HashSet<>();
            for ( CellLevelCharacteristics clc : getLoader( ee, dataType, config ).getOtherCellLevelCharacteristics( dimension ) ) {
                created.add( singleCellExpressionExperimentService.addCellLevelCharacteristics( ee, dimension, clc ) );
            }
            return created;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private SingleCellDimension getSingleCellDimension( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        if ( config.getQuantitationTypeName() != null ) {
            try {
                QuantitationType qt = quantitationTypeService.findByNameAndVectorType( ee, config.getQuantitationTypeName(), SingleCellExpressionDataVector.class );
                if ( qt == null ) {
                    throw new IllegalArgumentException( "No quantitation type with name " + config.getQuantitationTypeName() + " for " + ee + "." );
                }
                return singleCellExpressionExperimentService.getSingleCellDimensionWithCellLevelCharacteristics( ee, qt );
            } catch ( NonUniqueQuantitationTypeByNameException e ) {
                throw new RuntimeException( e );
            }
        } else {
            return singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithCellLevelCharacteristics( ee )
                    .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred single-cell dimension." ) );
        }
    }

    private QuantitationType load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoader loader, SingleCellDataLoaderConfig config ) {
        ee = requireNonNull( singleCellExpressionExperimentService.loadWithSingleCellVectors( ee.getId() ) );
        Assert.isTrue( platform.getPrimaryTaxon().equals( expressionExperimentService.getTaxon( ee ) ),
                "Platform primary taxon does not match dataset." );

        // TODO: reuse elements mappings when loading multiple datasets
        Map<String, CompositeSequence> elementsMapping = createElementsMapping( platform );

        SingleCellDimension dim = loadSingleCellDimension( loader, ee.getBioAssays() );
        QuantitationType qt = loadQuantitationType( loader, ee, config );
        loadCellTypeAssignments( loader, dim, config );
        loadCellLevelCharacteristics( loader, dim );
        loadVectors( loader, ee, dim, qt, elementsMapping, config );
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
            throw new IllegalArgumentException( String.format( "No quantitation available%s. Choose one among:\n%s",
                    qtName != null ? " with name " + qtName : "",
                    availableQts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
        } else if ( qts.size() > 1 ) {
            throw new IllegalArgumentException( String.format( "More than one available quantitation type%s. Choose one among:\n%s",
                    qtName != null ? " with name " + qtName : "",
                    availableQts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
        } else {
            qt = qts.iterator().next();
        }
        if ( config.isReplaceExistingQuantitationType() ) {
            // find the persistent QT matching the data
            QuantitationType existingQt = quantitationTypeService.find( ee, qt, SingleCellExpressionDataVector.class );
            if ( existingQt != null ) {
                qt = existingQt;
                log.info( "Data will be replaced for " + existingQt + "..." );
            } else {
                availableQts = singleCellExpressionExperimentService.getSingleCellQuantitationTypes( ee );
                throw new IllegalArgumentException( String.format( "%s does not match any existing single-cell quantitation type. Choose one among:\n%s",
                        qt,
                        availableQts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
            }
        } else {
            if ( ee.getQuantitationTypes().contains( qt ) ) {
                // this check is also done in SingleCellExpressionExperimentService.addSingleCellDataVectors(), but
                // after loading the vectors from disk which is time-consuming
                throw new IllegalArgumentException( ee + " already has a quantitation type matching " + qt + ". Set replaceExistingQuantitationType to replace existing vectors instead." );
            }
            log.info( "Data will be added for " + qt + "..." );

        }
        if ( config.isMarkQuantitationTypeAsPreferred() ) {
            log.info( "Marking " + qt + " as preferred." );
            qt.setIsPreferred( true );
        }
        return qt;
    }

    private void loadCellTypeAssignments( SingleCellDataLoader loader, SingleCellDimension dim, SingleCellDataLoaderConfig config ) {
        try {
            dim.getCellTypeAssignments().addAll( loader.getCellTypeAssignments( dim ) );
        } catch ( UnsupportedOperationException e ) {
            log.info( e.getMessage() ); // no need for the stacktrace
            return;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        CellTypeAssignment preferredCta;
        if ( config.getPreferredCellTypeAssignmentName() != null ) {
            String name = config.getPreferredCellTypeAssignmentName();
            Set<CellTypeAssignment> preferredCtas = dim.getCellTypeAssignments().stream()
                    .filter( cta -> name.equals( cta.getName() ) )
                    .collect( Collectors.toSet() );
            if ( preferredCtas.isEmpty() ) {
                String possibleNames = dim.getCellTypeAssignments().stream()
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
            if ( dim.getCellTypeAssignments().isEmpty() ) {
                throw new IllegalStateException( "No cell type assignment." );
            } else if ( dim.getCellTypeAssignments().size() > 1 ) {
                throw new IllegalStateException( "More than one cell type assignment." );
            }
            preferredCta = dim.getCellTypeAssignments().iterator().next();
            log.info( "Marking the only cell-type assignment " + preferredCta + " as preferred." );
            preferredCta.setPreferred( true );
        }
    }

    private void loadCellLevelCharacteristics( SingleCellDataLoader loader, SingleCellDimension dim ) {
        try {
            dim.getCellLevelCharacteristics().addAll( loader.getOtherCellLevelCharacteristics( dim ) );
        } catch ( UnsupportedOperationException e ) {
            log.info( e.getMessage() ); // no need for the stacktrace
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private void loadVectors( SingleCellDataLoader loader, ExpressionExperiment ee, SingleCellDimension dim, QuantitationType qt, Map<String, CompositeSequence> elementsMapping, SingleCellDataLoaderConfig config ) {
        Set<SingleCellExpressionDataVector> vectors;
        try {
            vectors = loader.loadVectors( elementsMapping, dim, qt ).collect( Collectors.toSet() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        if ( config.isReplaceExistingQuantitationType() ) {
            int replacedVectors = singleCellExpressionExperimentService.replaceSingleCellDataVectors( ee, qt, vectors );
            log.info( String.format( "Replaced %d single-cell vectors in %s.", replacedVectors, qt ) );
        } else {
            int addedVectors = singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
            log.info( String.format( "Added %d single-cell vectors to %s in %s.", addedVectors, ee, qt ) );
        }
    }

    private Map<String, CompositeSequence> createElementsMapping( ArrayDesign platform ) {
        // create mapping by precedence of ID type
        Map<CompositeSequence, List<Gene>> cs2g = arrayDesignService.getGenesByCompositeSequence( platform );
        // highest precedence is the probe name
        Map<String, CompositeSequence> elementsMapping = new HashMap<>();
        for ( CompositeSequence cs : platform.getCompositeSequences() ) {
            elementsMapping.putIfAbsent( cs.getName(), cs );
        }
        // then look for gene, E
        addMappings( elementsMapping, cs2g, gene -> gene.getNcbiGeneId() != null ? String.valueOf( gene.getNcbiGeneId() ) : null );
        addMappings( elementsMapping, cs2g, Gene::getEnsemblId );
        addMappings( elementsMapping, cs2g, Gene::getOfficialSymbol );
        addMappings( elementsMapping, cs2g, Gene::getName );
        return elementsMapping;
    }

    private void addMappings( Map<String, CompositeSequence> elementsMapping, Map<CompositeSequence, List<Gene>> cs2g, Function<Gene, String> g2s ) {
        for ( Map.Entry<CompositeSequence, List<Gene>> e : cs2g.entrySet() ) {
            for ( Gene g : e.getValue() ) {
                String k = g2s.apply( g );
                if ( k != null ) {
                    elementsMapping.putIfAbsent( k, e.getKey() );
                }
            }
        }
    }

    /**
     * Obtain a loader with an automatically detected data type.
     */
    private SingleCellDataLoader getLoader( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        SingleCellDataType dataType;
        if ( config instanceof AnnDataSingleCellDataLoaderConfig && Files.exists( getAnnDataFile( ee ) ) ) {
            dataType = SingleCellDataType.ANNDATA;
        } else if ( Files.exists( getSeuratDiskFile( ee ) ) ) {
            dataType = SingleCellDataType.SEURAT_DISK;
        } else if ( Files.exists( getMexDir( ee ) ) ) {
            dataType = SingleCellDataType.MEX;
        } else if ( Files.exists( getLoomFile( ee ) ) ) {
            dataType = SingleCellDataType.LOOM;
        } else {
            throw new IllegalArgumentException( "No single-cell data found for " + ee + " in " + singleCellDataBasePath + "." );
        }
        return getLoader( ee, dataType, config );
    }

    /**
     * Obtain a loader for a specific data type.
     */
    private SingleCellDataLoader getLoader( ExpressionExperiment ee, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        SingleCellDataLoader loader;
        switch ( dataType ) {
            case ANNDATA:
                loader = getAnnDataLoader( ee, ( AnnDataSingleCellDataLoaderConfig ) config );
                break;
            case SEURAT_DISK:
                loader = getSeuratDiskLoader();
                break;
            case MEX:
                loader = getMexLoader( ee, config );
                break;
            case LOOM:
                loader = getLoomLoader();
                break;
            default:
                throw new IllegalArgumentException( "Unknown single-cell data type " + dataType + "." );
        }
        return configureLoader( loader, ee, config );
    }

    private SingleCellDataLoader configureLoader( SingleCellDataLoader loader, ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        // apply GEO strategy for matching
        if ( ee.getAccession() != null && ee.getAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            loader.setBioAssayToSampleNameMatcher( new GeoBioAssayToSampleNameMatcher() );
        } else {
            log.info( String.format( "%s does not have a GEO accession, using %s for matching sample names to BioAssays.",
                    ee, SimpleBioAssayToSampleNameMatcher.class.getSimpleName() ) );
            loader.setBioAssayToSampleNameMatcher( new SimpleBioAssayToSampleNameMatcher() );
        }
        Path cellTypeAssignmentPath = config.getCellTypeAssignmentPath();
        Path otherCellCharacteristicsPath = config.getOtherCellLevelCharacteristicsFile();
        if ( cellTypeAssignmentPath != null || otherCellCharacteristicsPath != null ) {
            if ( cellTypeAssignmentPath != null ) {
                log.info( "Loading cell type assignments from " + cellTypeAssignmentPath );
            }
            if ( otherCellCharacteristicsPath != null ) {
                log.info( "Loading additional cell-level characteristics from " + otherCellCharacteristicsPath );
            }
            return new GenericMetadataSingleCellDataLoader( loader, cellTypeAssignmentPath, otherCellCharacteristicsPath );
        }
        return loader;
    }

    private SingleCellDataLoader getAnnDataLoader( ExpressionExperiment ee, AnnDataSingleCellDataLoaderConfig config ) {
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( config.getDataPath() != null ? config.getDataPath() : getAnnDataFile( ee ) );
        loader.setSampleFactorName( config.getSampleFactorName() );
        loader.setCellTypeFactorName( config.getCellTypeFactorName() );
        loader.setUnknownCellTypeIndicator( config.getUnknownCellTypeIndicator() );
        return loader;
    }

    private SingleCellDataLoader getSeuratDiskLoader() {
        throw new UnsupportedOperationException( "Seurat Disk is not directly supported, convert it to AnnData first." );
    }

    private SingleCellDataLoader getMexLoader( ExpressionExperiment ee, SingleCellDataLoaderConfig config ) {
        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodeFiles = new ArrayList<>();
        List<Path> genesFiles = new ArrayList<>();
        List<Path> matrixFiles = new ArrayList<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            Path dir;
            if ( config.getDataPath() != null ) {
                dir = config.getDataPath();
            } else {
                dir = getMexDir( ee );
            }
            Path sampleDir;
            if ( ba.getAccession() != null ) {
                sampleDir = dir.resolve( ba.getAccession().getAccession() );
            } else {
                sampleDir = dir.resolve( ba.getName() );
            }
            sampleNames.add( ba.getName() );
            barcodeFiles.add( sampleDir.resolve( "barcodes.tsv.gz" ) );
            genesFiles.add( sampleDir.resolve( "features.tsv.gz" ) );
            matrixFiles.add( sampleDir.resolve( "matrix.mtx.gz" ) );
        }
        return new MexSingleCellDataLoader( sampleNames, barcodeFiles, genesFiles, matrixFiles );
    }

    private SingleCellDataLoader getLoomLoader() {
        throw new UnsupportedOperationException( "Loom is not supported yet." );
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
