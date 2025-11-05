package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneFetcher;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.OntologyTerm;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils.getGeoAccessions;

public class CellXGeneGrabberCli extends AbstractCLI {

    @Autowired
    private TaxonService taxonService;

    @Value("${cellxgene.local.singleCellData.basepath}")
    private Path cellXGeneDownloadPath;

    private Set<String> allowedTaxa;

    @Override
    public String getCommandName() {
        return "listCELLxGENEData";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "allowedTaxa" ).longOpt( "allowed-taxa" ).hasArgs()
                .valueSeparator( ',' )
                .desc( "Limit to selected taxa. Defaults to all taxa declared in Gemma." ).get() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "allowedTaxa" ) ) {
            allowedTaxa = new HashSet<>( Arrays.asList( commandLine.getOptionValues( "allowedTaxa" ) ) );
        }
    }

    @Override
    protected void doWork() throws Exception {
        if ( allowedTaxa == null ) {
            allowedTaxa = getTaxaInGemma();
        }
        CellXGeneFetcher fetcher = new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1, 1.5 ), cellXGeneDownloadPath );
        getCliContext().getOutputStream().println( "collection_id\tdataset_id\tgeo_accessions\ttaxa\ttissues\tcell_types\tdevelopment_stages\tdiseases\tassays\tnumber_of_samples\tnumber_of_cells" );
        for ( CollectionMetadata cm : fetcher.fetchAllCollectionMetadata() ) {
            cm = fetcher.fetchCollectionMetadata( cm.getId() );
            assert cm.getDatasets() != null;
            List<String> geoAccessions = getGeoAccessions( cm ).stream().sorted().collect( Collectors.toList() );
            for ( DatasetMetadata dm : cm.getDatasets() ) {
                if ( dm.getOrganism().stream().map( OntologyTerm::getLabel ).noneMatch( allowedTaxa::contains ) ) {
                    log.warn( dm.getId() + ": Dataset does not have a supported taxa: " + dm.getOrganism() + ", skipping." );
                    continue;
                }
                if ( dm.getAssay().stream().noneMatch( CellXGeneUtils::isGeneExpressionAssay ) ) {
                    log.warn( dm.getId() + ": Dataset does not use a gene expression assay: " + dm.getAssay() + ", skipping." );
                    continue;
                }
                getCliContext().getOutputStream().printf( "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                        TsvUtils.format( cm.getId() ), TsvUtils.format( dm.getId() ), TsvUtils.format( geoAccessions ),
                        format( dm.getOrganism() ), format( dm.getTissue() ), format( dm.getCellType() ),
                        format( dm.getDevelopmentStage() ), format( dm.getDisease() ), format( dm.getAssay() ),
                        TsvUtils.format( dm.getDonorId().size() ), TsvUtils.format( dm.getCellCount() ) );
            }
        }
    }

    private String format( Collection<OntologyTerm> terms ) {
        return TsvUtils.format( terms.stream().map( OntologyTerm::getLabel ).sorted().distinct().collect( Collectors.toList() ) );
    }

    /**
     * Obtain a set of taxa that are considered usable in Gemma.
     */
    private Set<String> getTaxaInGemma() {
        Set<String> allowedTaxa = new HashSet<>();
        for ( Taxon t : taxonService.loadAll() ) {
            allowedTaxa.add( t.getScientificName() );
        }
        log.info( allowedTaxa.size() + " Taxa considered usable" );
        return allowedTaxa;
    }
}
