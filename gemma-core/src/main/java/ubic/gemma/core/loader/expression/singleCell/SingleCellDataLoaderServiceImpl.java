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
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeServiceImpl;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public void load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoaderConfig config ) {
        Assert.isNull( config.getExplicitPath(), "An explicit path cannot be provided when detecting the data type automatically." );
        SingleCellDataLoader loader;
        if ( config instanceof AnnDataSingleCellDataLoaderConfig && Files.exists( getAnnDataFile( ee ) ) ) {
            loader = getAnnDataLoader( ee, ( AnnDataSingleCellDataLoaderConfig ) config );
        } else if ( Files.exists( getSeuratDiskFile( ee ) ) ) {
            loader = getSeuratDiskLoader();
        } else if ( Files.exists( getMexDir( ee ) ) ) {
            loader = getMexLoader( ee, config );
        } else if ( Files.exists( getLoomFile( ee ) ) ) {
            loader = getLoomLoader();
        } else {
            throw new IllegalArgumentException( "No single-cell data found in " + singleCellDataBasePath + "." );
        }
        load( ee, platform, loader, config );
    }

    @Override
    @Transactional
    public void load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        if ( config.getExplicitPath() != null ) {
            log.info( "Loading single-cell data for " + ee + " from " + config.getExplicitPath() + "..." );
        }
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
        load( ee, platform, loader, config );
    }

    private void load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoader loader, SingleCellDataLoaderConfig config ) {
        Assert.isTrue( platform.getPrimaryTaxon().equals( expressionExperimentService.getTaxon( ee ) ),
                "Platform primary taxon does not match dataset." );

        String qtName = config.getQuantitationTypeName();
        Path cellTypeAssignmentPath = config.getCellTypeAssignmentPath();

        // apply GEO strategy for matching
        if ( ee.getAccession() != null && ee.getAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            loader.setBioAssayToSampleNameMatcher( new GeoBioAssayToSampleNameMatcher() );
        } else {
            log.info( String.format( "%s does not have a GEO accession, using %s for matching sample names to BioAssays.",
                    ee, SimpleBioAssayToSampleNameMatcher.class.getSimpleName() ) );
            loader.setBioAssayToSampleNameMatcher( new SimpleBioAssayToSampleNameMatcher() );
        }

        // create mapping by precedence of ID type
        Map<CompositeSequence, List<Gene>> cs2g = arrayDesignService.getGenes( platform );
        Map<String, CompositeSequence> elementsMapping = new HashMap<>();
        addMappings( elementsMapping, cs2g, Gene::getOfficialSymbol );
        addMappings( elementsMapping, cs2g, Gene::getName );
        addMappings( elementsMapping, cs2g, Gene::getEnsemblId );
        addMappings( elementsMapping, cs2g, gene -> gene.getNcbiGeneId() != null ? String.valueOf( gene.getNcbiGeneId() ) : null );

        SingleCellDimension dim;
        QuantitationType qt;
        Set<SingleCellExpressionDataVector> vectors;
        try {
            dim = loader.getSingleCellDimension( ee.getBioAssays() );
            Set<QuantitationType> qts = loader.getQuantitationTypes();
            if ( qtName != null ) {
                qts = qts.stream().filter( q -> q.getName().equals( qtName ) ).collect( Collectors.toSet() );
            }
            String availableQts;
            if ( !qts.isEmpty() ) {
                availableQts = " Choose one among:\n\t" + qts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) );
            } else {
                availableQts = "";
            }
            if ( qts.isEmpty() ) {
                throw new IllegalArgumentException( String.format( "No quantitation available%s.%s",
                        qtName != null ? " with name " + qtName : "", availableQts ) );
            } else if ( qts.size() > 1 ) {
                throw new IllegalArgumentException( String.format( "More than one available quantitation type%s.%s",
                        qtName != null ? " with name " + qtName : "", availableQts ) );
            } else {
                qt = qts.iterator().next();
            }
            if ( config.isPrimaryQt() ) {
                log.info( "Marking " + qt + " as preferred." );
                qt.setIsPreferred( true );
            }
            if ( cellTypeAssignmentPath != null ) {
                log.info( "Loading cell type assignments from " + cellTypeAssignmentPath );
                new GenericMetadataSingleCellDataLoader( loader, cellTypeAssignmentPath );
            }
            loader.getCellTypeAssignment( dim ).ifPresent( cta -> {
                if ( config.isPrimaryCta() ) {
                    log.info( "Marking " + cta + " as preferred." );
                    cta.setPreferred( config.isPrimaryCta() );
                }
                dim.getCellTypeAssignments().add( cta );
            } );
            vectors = loader.loadVectors( elementsMapping, dim, qt ).collect( Collectors.toSet() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        if ( config.isReplaceExistingQt() ) {
            // find the persistent QT matching the data
            QuantitationType existingQt = quantitationTypeService.find( ee, qt, SingleCellExpressionDataVector.class );
            if ( existingQt == null ) {
                String availableQts = singleCellExpressionExperimentService.getSingleCellQuantitationTypes( ee ).stream()
                        .map( QuantitationType::toString )
                        .collect( Collectors.joining( "\n\t" ) );
                throw new IllegalArgumentException( qt + " does not match any quantitation type from " + ee + ". Possible single-cell QTs are:\n\t" + availableQts );
            }
            int replacedVectors = singleCellExpressionExperimentService.replaceSingleCellDataVectors( ee, existingQt, vectors );
            log.info( String.format( "Replaced %d single-cell vectors in %s.", replacedVectors, existingQt ) );
        } else {
            int addedVectors = singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
            log.info( String.format( "Added %d single-cell vectors to %s in %s.", addedVectors, ee, qt ) );
        }
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

    private SingleCellDataLoader getAnnDataLoader( ExpressionExperiment ee, AnnDataSingleCellDataLoaderConfig config ) {
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( config.getExplicitPath() != null ? config.getExplicitPath() : getAnnDataFile( ee ) );
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
            if ( config.getExplicitPath() != null ) {
                dir = config.getExplicitPath();
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
            matrixFiles.add( sampleDir.resolve( "matrix.tsv.gz" ) );
        }
        return new MexSingleCellDataLoader( sampleNames, barcodeFiles, genesFiles, matrixFiles );
    }

    private SingleCellDataLoader getLoomLoader() {
        throw new UnsupportedOperationException( "Loom is not supported yet and no other supported format was found." );
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
