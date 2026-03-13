package ubic.gemma.core.loader.expression.cellxgene;

import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.Link;
import ubic.gemma.core.loader.expression.cellxgene.model.OntologyTerm;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Convert CELLxGENE dataset metadata into an ExpressionExperiment.
 *
 * @author poirigui
 */
public class CellXGeneConverter {

    private final ExternalDatabaseService externalDatabaseService;
    private final TaxonService taxonService;
    private final PubMedSearch pubMedSearch;

    private static final Map<String, Category> CATEGORY_MAP = new HashMap<>();
    static {
        CATEGORY_MAP.put( "developmental_stage", Categories.DEVELOPMENT_STAGE );
        CATEGORY_MAP.put( "sex", Categories.BIOLOGICAL_SEX );
        CATEGORY_MAP.put( "cell_type", Categories.CELL_TYPE );
        CATEGORY_MAP.put( "tissue", Categories.ORGANISM_PART );
        CATEGORY_MAP.put( "disease", Categories.DISEASE );
    }

    public CellXGeneConverter( ExternalDatabaseService externalDatabaseService, TaxonService taxonService, PubMedSearch pubMedSearch ) {
        this.externalDatabaseService = externalDatabaseService;
        this.taxonService = taxonService;
        this.pubMedSearch = pubMedSearch;
    }

    /**
     *
     * @param datasetMetadata    CELLxGENE dataset metadata to convert
     * @param platform           platform to use for mapping deign elements from the data
     * @param dataLoader         single-cell data loader for loading sample names and data vectors (if requested)
     * @param loadSingleCellData whether to load the single-cell data vectors, this can be done later if needed
     * @return a transient {@link ExpressionExperiment} pre-populated with CELLxGENE metadata
     */
    public ExpressionExperiment convert( CollectionMetadata collectionMetadata, DatasetMetadata datasetMetadata, ArrayDesign platform, String datasetShortName, SingleCellDataLoader dataLoader, boolean loadSingleCellData ) throws IOException {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( datasetShortName );
        ee.setName( datasetMetadata.getName() );
        ee.setDescription( collectionMetadata.getDescription() );
        ee.setAccession( convertAccession( datasetMetadata ) );
        ee.setSource( "Imported from CELLxGENE." );
        List<BibliographicReference> bibrefs = convertPublications( collectionMetadata );
        if ( !bibrefs.isEmpty() ) {
            ee.setPrimaryPublication( bibrefs.get( 0 ) );
            if ( bibrefs.size() > 1 ) {
                ee.getOtherRelevantPublications().addAll( bibrefs.subList( 1, bibrefs.size() ) );
            }
        }
        Taxon taxon = convertTaxon( datasetMetadata );
        if ( !platform.getPrimaryTaxon().equals( taxon ) ) {
            throw new IllegalArgumentException( "The taxon from the dataset metadata (" + taxon + ") does not match the platform's (" + platform.getPrimaryTaxon() + ")." );
        }
        ee.setTaxon( taxon );
        ee.setCharacteristics( convertExperimentTags( datasetMetadata ) );

        for ( String sampleName : datasetMetadata.getDonorId() ) {
            BioMaterial sample = BioMaterial.Factory.newInstance( sampleName, taxon );
            BioAssay assay = BioAssay.Factory.newInstance( sampleName, platform, sample );
            sample.getBioAssaysUsedIn().add( assay );
            ee.getBioAssays().add( assay );
        }
        ee.setNumberOfSamples( ee.getBioAssays().size() );

        // TODO: filter which sample characteristics we want in Gemma
        dataLoader.getSamplesCharacteristics( ee.getBioAssays() )
                .forEach( ( bm, cs ) -> bm.getCharacteristics().addAll( cs ) );

        ee.getBioAssays().forEach( ( ba ) -> {
            ba.getSampleUsed().getCharacteristics().forEach( ( c ) -> {
                processCharacteristics( c );
            } );
        } );

        ee.getBioAssays().forEach( ( ba ) -> ba.getSampleUsed().getCharacteristics() );

        // TODO: prefill the experimental design
        ee.setExperimentalDesign( ExperimentalDesign.Factory.newInstance() );

        if ( loadSingleCellData ) {
            SingleCellDimension dimension = dataLoader.getSingleCellDimension( ee.getBioAssays() );
            // load the data?
            for ( QuantitationType qt : dataLoader.getQuantitationTypes() ) {
                List<SingleCellExpressionDataVector> vectors = dataLoader.loadVectors( platform.getCompositeSequences(), dimension, qt )
                        .collect( Collectors.toList() );
                ee.getQuantitationTypes().add( qt );
                ee.getSingleCellExpressionDataVectors().addAll( vectors );
            }
        }

        return ee;
    }



    private void processCharacteristics( Characteristic c ) {
        Category matchingCategory = CATEGORY_MAP.get( c.getCategory() );

        if ( matchingCategory != null ) {
            c.setCategory( matchingCategory.getCategory() );
            c.setCategoryUri( matchingCategory.getCategoryUri() );
        }
    }

    private DatabaseEntry convertAccession( DatasetMetadata datasetMetadata ) {
        ExternalDatabase cellxgeneDatabase = externalDatabaseService.findByName( ExternalDatabases.CELLXGENE );
        if ( cellxgeneDatabase == null ) {
            throw new IllegalStateException( "CELLxGENE external database not found in the system. Make sure it is created before importing CELLxGENE datasets.." );
        }
        DatabaseEntry de = DatabaseEntry.Factory.newInstance( datasetMetadata.getId(), cellxgeneDatabase );
        de.setUri( CellXGeneUtils.getCollectionUri( datasetMetadata.getCollectionId() ) );
        return de;
    }

    private Taxon convertTaxon( DatasetMetadata datasetMetadata ) {
        if ( datasetMetadata.getOrganism().isEmpty() ) {
            throw new IllegalArgumentException( "Dataset does not specify an organism." );
        } else if ( datasetMetadata.getOrganism().size() > 1 ) {
            throw new IllegalArgumentException( "Dataset has more than one organism: " + datasetMetadata.getOrganism() + ", but splitting is not supported yet." );
        }
        OntologyTerm ot = datasetMetadata.getOrganism().iterator().next();
        return requireNonNull( taxonService.findByScientificName( ot.getLabel() ), "No taxon found for organism: " + ot.getLabel() );
    }

    private Set<Characteristic> convertExperimentTags( DatasetMetadata datasetMetadata ) {
        Set<Characteristic> tags = new HashSet<>();
        for ( OntologyTerm ot : datasetMetadata.getAssay() ) {
            tags.add( convertOntologyTerm( ot, Categories.ASSAY ) );
        }
        for ( OntologyTerm ot : datasetMetadata.getDevelopmentStage() ) {
            tags.add( convertOntologyTerm( ot, Categories.DEVELOPMENT_STAGE ) );
        }
        for ( OntologyTerm ot : datasetMetadata.getCellType() ) {
            tags.add( convertOntologyTerm( ot, Categories.CELL_TYPE ) );
        }
        for ( OntologyTerm ot : datasetMetadata.getDisease() ) {
            tags.add( convertOntologyTerm( ot, Categories.DISEASE ) );
        }
        for ( OntologyTerm ot : datasetMetadata.getTissue() ) {
            tags.add( convertOntologyTerm( ot, Categories.ORGANISM_PART ) );
        }
        return tags;
    }

    private Characteristic convertOntologyTerm( OntologyTerm term, Category category ) {
        return Characteristic.Factory.newInstance( category, term.getLabel(), CellXGeneUtils.getTermUri( term ) );
    }

    private List<BibliographicReference> convertPublications( CollectionMetadata cm ) throws IOException {
        assert cm.getLinks() != null;
        List<BibliographicReference> results = new ArrayList<>();
        for ( Link link : cm.getLinks() ) {
            if ( "DOI".equals( link.getLinkType() ) ) {
                results.addAll( pubMedSearch.searchAndRetrieveByDoi( link.getLinkUrl() ) );
            }
        }
        return results;
    }
}
