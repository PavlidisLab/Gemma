package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.ontology.providers.UberonOntologyService;
import ubic.gemma.cli.completion.CompletionType;
import ubic.gemma.cli.completion.CompletionUtils;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.cli.util.EnumeratedByCommandStringConverter;
import ubic.gemma.cli.util.EnumeratedStringConverter;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneFetcher;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.OntologyTerm;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import org.springframework.lang.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils.getGeoAccessions;

public class CellXGeneGrabberCli extends AbstractCLI {

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExperimentalFactorOntologyService experimentalFactorOntologyService;

    @Autowired
    private UberonOntologyService uberonOntologyService;

    @Value("${cellxgene.local.singleCellData.basepath}")
    private Path cellXGeneDownloadPath;

    /**
     * Either term IDs, URIs or labels of taxa.
     * <p>
     * No inference is done.
     */
    private Set<String> allowedTaxa;

    /**
     * Either term IDs, URIs or labels of tissues.
     * <p>
     * Inference can only be performed on URIs and term IDs.
     */
    @Nullable
    private Set<String> tissues;

    /**
     * Either term IDs, URIs or labels of assays.
     * <p>
     * Inference is done only for user-supplied assays.
     */
    @Nullable
    private Set<String> assays;

    private boolean useOntologyInference;

    @Override
    public String getCommandName() {
        return "listCELLxGENEData";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "allowedTaxa" ).longOpt( "allowed-taxa" ).hasArgs()
                .valueSeparator( ',' )
                .argName( "URI, term ID or label" )
                .desc( "Limit to selected taxa. Defaults to all taxa declared in Gemma." ).get() );
        options.addOption( Option.builder( "assays" ).longOpt( "assays" ).hasArgs()
                .valueSeparator( ',' )
                .converter( EnumeratedStringConverter.of( Arrays.stream( CellXGeneUtils.GENE_EXPRESSION_ASSAYS ).collect( Collectors.toMap( OntologyTerm::getOntologyTermId, ot -> new DefaultMessageSourceResolvable( null, ot.getLabel() ) ) ) ) )
                .argName( "URI, term ID or label" )
                .desc( "Limit results to selected assays. URIs and term IDs from EFO can be used. Defaults to a predefined set of gene expression assays." )
                .get() );
        options.addOption( Option.builder( "tissues" ).longOpt( "tissues" ).hasArgs()
                .valueSeparator( ',' )
                .argName( "URI, term ID or label" )
                .desc( "Limit results to selected tissues. URIs and term IDs from Uberon can be used. Defaults to any tissue." )
                .converter( EnumeratedByCommandStringConverter.of( CompletionUtils.generateCompleteCommand( CompletionType.ONTOLOGY_TERM ) ) )
                .get() );
        options.addOption( "noInference", "no-inference", false, "Do not perform ontology inference on the provided terms." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "allowedTaxa" ) ) {
            allowedTaxa = new HashSet<>( Arrays.asList( commandLine.getOptionValues( "allowedTaxa" ) ) );
        }
        if ( commandLine.hasOption( "assays" ) ) {
            assays = new HashSet<>( Arrays.asList( commandLine.getOptionValues( "assays" ) ) );
        }
        if ( commandLine.hasOption( "tissues" ) ) {
            tissues = new HashSet<>( Arrays.asList( commandLine.getOptionValues( "tissues" ) ) );
        }
        useOntologyInference = !commandLine.hasOption( "noInference" );
    }

    @Override
    protected void doWork() throws Exception {
        Set<String> allowedTaxa;
        if ( this.allowedTaxa != null ) {
            allowedTaxa = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
            allowedTaxa.addAll( this.allowedTaxa );
        } else {
            allowedTaxa = getTaxaInGemma();
        }
        Set<String> allowedAssays;
        if ( assays != null ) {
            allowedAssays = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
            if ( useOntologyInference ) {
                OntologyUtils.ensureInitializedLite( experimentalFactorOntologyService );
                allowedAssays.addAll( expandKeywords( experimentalFactorOntologyService, this.assays ) );
                log.info( "Found " + allowedAssays.size() + " assay terms via ontology inference in EFO." );
            } else {
                allowedAssays.addAll( this.assays );
            }
            if ( allowedAssays.isEmpty() ) {
                throw new IllegalArgumentException( "No terms found for the requested assays." );
            }
        } else {
            // no ontology inference needed, we have a predefined set of assays that should be exhaustive
            allowedAssays = Arrays.stream( CellXGeneUtils.GENE_EXPRESSION_ASSAYS )
                    .flatMap( ot -> Stream.of( ot.getOntologyTermId(), ot.getLabel(), OntologyUtils.termIdToUri( ot.getOntologyTermId() ) ) )
                    .collect( Collectors.toCollection( () -> new TreeSet<>( String.CASE_INSENSITIVE_ORDER ) ) );
        }
        Set<String> allowedTissues;
        if ( tissues != null ) {
            allowedTissues = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
            if ( useOntologyInference ) {
                log.info( "Ensuring that Uberon is loaded..." );
                OntologyUtils.ensureInitializedLite( uberonOntologyService );
                allowedTissues.addAll( expandKeywords( uberonOntologyService, tissues ) );
                log.info( "Found " + allowedTissues.size() + " tissue terms via ontology inference in Uberon." );
            } else {
                allowedTissues.addAll( tissues );
            }
            if ( allowedTissues.isEmpty() ) {
                throw new IllegalArgumentException( "No terms found for the requested tissues." );
            }
        } else {
            allowedTissues = null;
        }
        CellXGeneFetcher fetcher = new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), cellXGeneDownloadPath );
        getCliContext().getOutputStream().println( "collection_id\tdataset_id\tdataset_name\tgeo_accessions\ttaxa\ttissues\tcell_types\tdevelopment_stages\tdiseases\tassays\tnumber_of_samples\tnumber_of_cells" );
        for ( CollectionMetadata cm : fetcher.fetchAllCollectionMetadata() ) {
            cm = fetcher.fetchCollectionMetadata( cm.getId() );
            assert cm.getDatasets() != null;
            List<String> geoAccessions = getGeoAccessions( cm ).stream().sorted().collect( Collectors.toList() );
            for ( DatasetMetadata dm : cm.getDatasets() ) {
                if ( dm.getOrganism().stream().map( OntologyTerm::getLabel ).noneMatch( allowedTaxa::contains ) ) {
                    log.warn( dm.getId() + ": Dataset does not have a supported taxa: " + dm.getOrganism() + ", skipping." );
                    continue;
                }
                if ( dm.getAssay().stream().noneMatch( t -> hasAnyKeyword( t, allowedAssays ) ) ) {
                    if ( this.assays != null ) {
                        log.debug( dm.getId() + ": Dataset does not use a specified assay: " + dm.getAssay() + ", skipping." );
                    } else {
                        // our list of gene expression assays should be exhaustive, so treat skipped datasets as
                        // warnings as we might have missed something
                        log.warn( dm.getId() + ": Dataset does not use a gene expression assay: " + dm.getAssay() + ", skipping." );
                    }
                    continue;
                }
                if ( allowedTissues != null && dm.getTissue().stream().noneMatch( ot -> hasAnyKeyword( ot, allowedTissues ) ) ) {
                    log.debug( dm.getId() + ": Dataset does not use a requested tissue: " + dm.getTissue() + ", skipping." );
                    continue;
                }
                getCliContext().getOutputStream().printf( "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                        TsvUtils.format( cm.getId() ), TsvUtils.format( dm.getId() ), TsvUtils.format( dm.getName() ),
                        TsvUtils.format( geoAccessions ),
                        format( dm.getOrganism() ), format( dm.getTissue() ), format( dm.getCellType() ),
                        format( dm.getDevelopmentStage() ), format( dm.getDisease() ), format( dm.getAssay() ),
                        TsvUtils.format( dm.getDonorId().size() ), TsvUtils.format( dm.getCellCount() ) );
            }
        }
    }

    /**
     * Expand keywords which might designate either term IDs, URIs or labels.
     */
    private Collection<String> expandKeywords( OntologyService ontologyService, Collection<String> keywords ) {
        Set<String> result = new HashSet<>();
        Set<ubic.basecode.ontology.model.OntologyTerm> terms = new HashSet<>();
        for ( String t : keywords ) {
            result.add( t );
            if ( OntologyUtils.isTermUri( t ) ) {
                // use as-is
            } else if ( OntologyUtils.isTermId( t, false ) ) {
                t = OntologyUtils.termIdToUri( t );
            } else {
                continue;
            }
            ubic.basecode.ontology.model.OntologyTerm ot = ontologyService.getTerm( t );
            if ( ot != null ) {
                terms.add( ot );
            } else {
                log.warn( "Could not find a term for '" + t + "', skipping." );
            }
        }
        terms.addAll( ontologyService.getChildren( terms, false, true ) );
        for ( ubic.basecode.ontology.model.OntologyTerm t : terms ) {
            String termId = OntologyUtils.getTermId( t );
            if ( termId != null ) {
                result.add( termId );
            }
            if ( t.getLabel() != null ) {
                result.add( t.getLabel() );
            }
        }
        return result;
    }

    /**
     * Check if the term matches any of the provided keywords, either by term ID or label.
     */
    private boolean hasAnyKeyword( OntologyTerm term, Set<String> keywords ) {
        return keywords.contains( term.getOntologyTermId() ) || keywords.contains( term.getLabel() );
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
            if ( t.getNcbiId() != null ) {
                allowedTaxa.add( "NCBITaxon:" + t.getNcbiId() );
            }
            if ( t.getScientificName() != null ) {
                allowedTaxa.add( t.getScientificName() );
            }
        }
        log.info( allowedTaxa.size() + " Taxa considered usable" );
        return allowedTaxa;
    }
}
