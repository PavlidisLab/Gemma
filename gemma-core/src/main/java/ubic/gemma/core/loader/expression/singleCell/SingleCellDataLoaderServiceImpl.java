package ubic.gemma.core.loader.expression.singleCell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
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
public class SingleCellDataLoaderServiceImpl implements SingleCellDataLoaderService {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Value("${gemma.download.path}/singleCellData")
    private Path singleCellDataBasePath;

    @Override
    @Transactional
    public void load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataLoaderConfig config ) {
        Assert.notNull( ee.getAccession() );
        SingleCellDataLoader loader;
        if ( config instanceof AnnDataSingleCellDataLoaderConfig && Files.exists( getAnnDataFile( ee ) ) ) {
            loader = getAnnDataLoader( ee, ( AnnDataSingleCellDataLoaderConfig ) config );
        } else if ( Files.exists( getSeuratDiskFile( ee ) ) ) {
            loader = getSeuratDiskLoader( ee );
        } else if ( Files.exists( getMexDir( ee ) ) ) {
            loader = getMexLoader( ee );
        } else if ( Files.exists( getLoomDir( ee ) ) ) {
            loader = getLoomLoader( ee );
        } else {
            throw new IllegalArgumentException( "No single-cell data found in " + singleCellDataBasePath + "." );
        }
        load( ee, platform, loader, config );
    }

    @Override
    @Transactional
    public void load( ExpressionExperiment ee, ArrayDesign platform, SingleCellDataType dataType, SingleCellDataLoaderConfig config ) {
        Assert.notNull( ee.getAccession() );
        SingleCellDataLoader loader;
        switch ( dataType ) {
            case ANNDATA:
                loader = getAnnDataLoader( ee, ( AnnDataSingleCellDataLoaderConfig ) config );
                break;
            case SEURAT_DISK:
                loader = getSeuratDiskLoader( ee );
                break;
            case MEX:
                loader = getMexLoader( ee );
                break;
            case LOOM:
                loader = getLoomLoader( ee );
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
                availableQts = "Choose one among:\n\t" + qts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) );
            } else {
                availableQts = null;
            }
            if ( qts.isEmpty() ) {
                throw new IllegalArgumentException( String.format( "No quantitation available%s.%s",
                        qtName != null ? " with name " + qtName : "", availableQts != null ? " " + availableQts : "" ) );
            } else if ( qts.size() > 1 ) {
                throw new IllegalArgumentException( String.format( "More than one available quantitation type%s.%s",
                        qtName != null ? " with name " + qtName : "", availableQts != null ? " " + availableQts : "" ) );
            } else {
                qt = qts.iterator().next();
            }
            if ( cellTypeAssignmentPath != null ) {
                new GenericMetadataSingleCellDataLoader( loader, cellTypeAssignmentPath );
            }
            loader.getCellTypeAssignment( dim ).ifPresent( cta -> {
                dim.getCellTypeAssignments().add( cta );
            } );
            vectors = loader.loadVectors( elementsMapping, dim, qt ).collect( Collectors.toSet() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
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
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( getAnnDataFile( ee ) );
        loader.setSampleFactorName( config.getSampleFactorName() );
        loader.setCellTypeFactorName( config.getCellTypeFactorName() );
        loader.setUnknownCellTypeIndicator( config.getUnknownCellTypeIndicator() );
        return loader;
    }

    private SingleCellDataLoader getSeuratDiskLoader( ExpressionExperiment ee ) {
        throw new UnsupportedOperationException( "Seurat Disk is not directly supported, convert it to AnnData first." );
    }

    private SingleCellDataLoader getMexLoader( ExpressionExperiment ee ) {
        Path mexPath = getMexDir( ee );
        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodeFiles = new ArrayList<>();
        List<Path> genesFiles = new ArrayList<>();
        List<Path> matrixFiles = new ArrayList<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            Assert.notNull( ba.getSampleUsed().getExternalAccession() );
            Path dir = mexPath.resolve( ba.getSampleUsed().getExternalAccession().getAccession() );
            sampleNames.add( ba.getName() );
            barcodeFiles.add( dir.resolve( "barcodes.tsv.gz" ) );
            genesFiles.add( dir.resolve( "features.tsv.gz" ) );
            matrixFiles.add( dir.resolve( "matrix.tsv.gz" ) );
        }
        return new MexSingleCellDataLoader( sampleNames, barcodeFiles, genesFiles, matrixFiles );
    }

    private SingleCellDataLoader getLoomLoader( ExpressionExperiment ee ) {
        throw new UnsupportedOperationException( "Loom is not supported yet and no other supported format was found." );
    }

    private Path getAnnDataFile( ExpressionExperiment ee ) {
        Assert.notNull( ee.getAccession() );
        return singleCellDataBasePath
                .resolve( ee.getAccession().getExternalDatabase().getName() )
                .resolve( ee.getAccession().getAccession() + ".h5ad" );
    }

    public Path getSeuratDiskFile( ExpressionExperiment ee ) {
        Assert.notNull( ee.getAccession() );
        return singleCellDataBasePath
                .resolve( ee.getAccession().getExternalDatabase().getName() )
                .resolve( ee.getShortName() + ".h5Seurat" );
    }

    private Path getMexDir( ExpressionExperiment ee ) {
        Assert.notNull( ee.getAccession() );
        return singleCellDataBasePath
                .resolve( ee.getAccession().getExternalDatabase().getName() )
                .resolve( ee.getAccession().getAccession() );
    }

    private Path getLoomDir( ExpressionExperiment ee ) {
        Assert.notNull( ee.getAccession() );
        return singleCellDataBasePath
                .resolve( ee.getAccession().getExternalDatabase().getName() )
                .resolve( ee.getShortName() );
    }
}
