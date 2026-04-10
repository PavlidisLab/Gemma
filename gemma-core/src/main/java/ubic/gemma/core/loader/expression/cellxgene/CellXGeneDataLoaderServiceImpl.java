package ubic.gemma.core.loader.expression.cellxgene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAsset;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTransformationFactory;
import ubic.gemma.core.loader.util.mapper.EnsemblIdDesignElementMapper;
import ubic.gemma.core.util.ProgressReporterFactory;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * @author poirigui
 */
@Service
@Transactional(propagation = Propagation.NEVER)
public class CellXGeneDataLoaderServiceImpl implements CellXGeneDataLoaderService {

    private final CellXGeneFetcher cellXGeneFetcher;
    private final CellXGeneConverter cellXGeneConverter;
    private final Persister persister;
    private final ExpressionExperimentService expressionExperimentService;
    private final ArrayDesignService arrayDesignService;
    private final SingleCellDataTransformationFactory singleCellDataTransformationFactory;
    private final Path cellXGeneTransposedPath;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public CellXGeneDataLoaderServiceImpl(
            Persister persister, ArrayDesignService arrayDesignService,
            ExpressionExperimentService expressionExperimentService,
            ExternalDatabaseService externalDatabaseService, TaxonService taxonService,
            SingleCellDataTransformationFactory singleCellDataTransformationFactory,
            PlatformTransactionManager transactionManager,
            @Value("${cellxgene.local.singleCellData.basepath}") Path cellXGeneDownloadPath,
            @Value("${gemma.download.path}/singleCellData/CELLxGENE_Transposed") Path cellXGeneTransposedPath,
            @Value("${entrez.efetch.apikey}") String ncbiApiKey
    ) {
        this.cellXGeneFetcher = new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1000, 3 ), cellXGeneDownloadPath );
        this.singleCellDataTransformationFactory = singleCellDataTransformationFactory;
        this.cellXGeneConverter = new CellXGeneConverter( externalDatabaseService, taxonService, new PubMedSearch( ncbiApiKey ) );
        this.persister = persister;
        this.arrayDesignService = arrayDesignService;
        this.expressionExperimentService = expressionExperimentService;
        this.cellXGeneTransposedPath = cellXGeneTransposedPath;
        this.transactionTemplate = new TransactionTemplate( transactionManager );
    }

    @Override
    public ExpressionExperiment fetchAndLoad( String collectionId, @Nullable String datasetId, @Nullable String assetId,
            ArrayDesign platform, String datasetShortName, boolean loadSingleCellData, boolean keepPooledSample, boolean keepUnknownSample, boolean dryRun) throws IOException {
        if ( expressionExperimentService.existsByShortName( datasetShortName ) ) {
            //throw new IllegalArgumentException( "An ExpressionExperiment with short name " + datasetShortName + " already exists in the database." );
        }

        CollectionMetadata cm = cellXGeneFetcher.fetchCollectionMetadata( collectionId );

        DatasetMetadata metadata;
        if ( datasetId == null ) {
            assert cm.getDatasets() != null;
            if ( cm.getDatasets().isEmpty() ) {
                throw new IllegalStateException( "CELLxGENE collection " + collectionId + " does not contain any datasets." );
            } else if ( cm.getDatasets().size() > 1 ) {
                throw new IllegalStateException( "CELLxGENE collection " + collectionId + " has more than one dataset." );
            }
            metadata = cm.getDatasets().iterator().next();
        } else {
            assert cm.getDatasets() != null;
            metadata = cm.getDatasets().stream()
                    .filter( dm -> dm.getId().equals( datasetId ) )
                    .findFirst()
                    .orElseThrow( () -> new IllegalStateException( "Dataset " + datasetId + " does not exist." ) );
        }
        DatasetAsset asset;
        if ( assetId != null ) {
            asset = metadata.getDatasetAssets().stream()
                    .filter( asset2 -> asset2.getId().equals( assetId ) )
                    .findFirst()
                    .orElseThrow( () -> new IllegalStateException( "CELLxGENE dataset " + datasetId + " does not have an asset with ID " + assetId + "." ) );
        } else {
            asset = metadata.getDatasetAssets().stream()
                    .filter( CellXGeneUtils::isAnnData )
                    .findFirst()
                    .orElseThrow( () -> new IllegalStateException( "CELLxGENE dataset " + datasetId + " does not have any H6AD asset." ) );
        }

        Assert.isTrue( CellXGeneUtils.isAnnData( asset ), "Only H5AD assets can be loaded." );

        Map<CompositeSequence, Set<Gene>> designElementMapping = arrayDesignService.getGenesByCompositeSequence( platform, true );

        // downloading the data is needed even if we do not load it because we pull the sample metadata from the AnnData
        // object
        assert asset.getFiletype() != null;
        Path dataPath = cellXGeneFetcher.downloadDatasetAsset( metadata.getId(), asset.getId(), asset.getFiletype() );

        ExpressionExperiment ee;
        try ( SingleCellDataLoader dataLoader = new CellXGeneAnnDataSingleCellDataConfigurer( dataPath, singleCellDataTransformationFactory,cellXGeneTransposedPath )
                .configureLoader( CellXGeneAnnDataSingleCellDataLoaderConfig.builder()
                        .ignoreDataVectors( !loadSingleCellData )
                        .keepPooledSample( keepPooledSample )
                        .keepUnknownSample( keepUnknownSample )
                        .build() ) ) {
            dataLoader.setDesignElementToGeneMapper( new EnsemblIdDesignElementMapper( designElementMapping ) );
            ee = cellXGeneConverter.convert( cm, metadata, platform, designElementMapping.keySet(), datasetShortName, dataLoader, loadSingleCellData );
        }

        if (dryRun){
            transactionTemplate.execute( status -> {
                persister.persist( ee );
                status.setRollbackOnly();
                return null;
            } );
            return ee;
        } else{
            return persister.persist( ee );
        }
    }

    @Override
    public void setProgressReporterFactory( ProgressReporterFactory progressReporterFactory ) {
        cellXGeneFetcher.setProgressReporterFactory( progressReporterFactory );
    }

}
