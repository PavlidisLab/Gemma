package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneFetcher;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.Link;
import ubic.gemma.core.loader.expression.cellxgene.model.OntologyTerm;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.TsvUtils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CellXGeneGrabberCli extends AbstractCLI {

    @Value("${cellxgene.local.singleCellData.basepath}")
    private Path cellXGeneDownloadPath;

    @Override
    public String getCommandName() {
        return "listCELLxGENEData";
    }

    @Override
    protected void doWork() throws Exception {
        CellXGeneFetcher fetcher = new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1, 1.5 ), cellXGeneDownloadPath );
        getCliContext().getOutputStream().println( "collection_id\tdataset_id\tgeo_accession\ttaxa\ttissues\tcell_types\tdevelopment_stages\tdiseases\tassays\tnumber_of_samples\tnumber_of_cells" );
        for ( CollectionMetadata cm : fetcher.fetchAllCollectionMetadata() ) {
            cm = fetcher.fetchCollectionMetadata( cm.getId() );
            assert cm.getDatasets() != null;
            Set<String> geoAccession = getGeoAccessions( cm );
            for ( DatasetMetadata dm : cm.getDatasets() ) {
                getCliContext().getOutputStream().printf( "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                        TsvUtils.format( cm.getId() ), TsvUtils.format( dm.getId() ), TsvUtils.format( geoAccession ),
                        format( dm.getOrganism() ), format( dm.getTissue() ), format( dm.getCellType() ),
                        format( dm.getDevelopmentStage() ), format( dm.getDisease() ), format( dm.getAssay() ),
                        TsvUtils.format( dm.getDonorId().size() ), TsvUtils.format( dm.getCellCount() ) );
            }
        }
    }

    private SortedSet<String> getGeoAccessions( CollectionMetadata cm ) {
        SortedSet<String> result = new TreeSet<>();
        assert cm.getLinks() != null;
        for ( Link link : cm.getLinks() ) {
            if ( link.getLinkUrl().startsWith( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" ) ) {
                result.add( link.getLinkUrl().substring( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=".length() ) );
            }
        }
        return result;
    }

    private String format( Collection<OntologyTerm> terms ) {
        return TsvUtils.format( terms.stream().map( OntologyTerm::getLabel ).sorted().distinct().collect( Collectors.toList() ) );
    }
}
