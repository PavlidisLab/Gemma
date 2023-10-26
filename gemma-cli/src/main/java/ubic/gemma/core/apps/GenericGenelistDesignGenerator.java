/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.util.Hash;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.AnnotationBasedGeneMappingEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.*;

/**
 * Create (or update) an array design based on a list of NCBI gene IDs corresponding to
 *
 * @author paul
 */
public class GenericGenelistDesignGenerator extends AbstractCLIContextCLI {

    private AnnotationAssociationService annotationAssociationService;
    private ArrayDesignAnnotationService arrayDesignAnnotationService;
    private ArrayDesignReportService arrayDesignReportService;
    private ArrayDesignService arrayDesignService;
    private BioSequenceService bioSequenceService;
    private CompositeSequenceService compositeSequenceService;
    private GeneService geneService;

    private String platformShortName = null;
    private String geneListFileName = null;
    private Taxon taxon = null;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @Override
    public String getCommandName() {
        return "genericPlatform";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "t" ).longOpt( "taxon" ).desc( "Taxon of the genes" ).argName( "taxon" ).required().hasArg().build() );
        Option arrayDesignOption = Option.builder( "a" ).hasArg().argName( "shortName" )
                .desc( "Platform short name (existing or new to add)" ).required().longOpt( "platform" ).build();
        options.addOption( arrayDesignOption );

        Option geneListOption = Option.builder( "f" ).hasArg().argName( "file" ).desc(
                        "File with list of NCBI IDs of genes to add to platform (one per line)" )
                .longOpt( "geneListFile" ).required().build();
        options.addOption( geneListOption );
    }

    @Override
    protected void doWork() throws Exception {

        ArrayDesign platform = arrayDesignService.findByShortName( this.platformShortName );
        platform = arrayDesignService.thaw( platform );

        Set<String> ncbiIds = new HashSet<String>( AbstractCLIContextCLI.readListFileToStrings( this.geneListFileName ) );
        AbstractCLI.log.info( "File had " + ncbiIds.size() + " gene ids" );

        // this would be good for cases where the identifier we are using has changed.
        Map<Gene, CompositeSequence> existingGeneMap = new HashMap<>();


        Map<String, CompositeSequence> existingSymbolMap = this.nameMap( platform );

        int count = 0;
        int numWithNoTranscript = 0;
        // int hasGeneAlready = 0;
        // int numNewGenes = 0;
        int numNewElements = 0;
        int numUpdatedElements = 0;
        for ( String ncbiId : ncbiIds ) {


            if ( existingSymbolMap.containsKey( ncbiId ) ) {
                continue;
            }

            Gene gene = null;
            try {
                gene = geneService.findByNCBIId( Integer.parseInt( ncbiId ) );
            } catch ( NumberFormatException e ) {
                AbstractCLI.log.error( "Could not parse " + ncbiId + " as an integer" );
            }
            if ( gene == null ) {
                AbstractCLI.log.warn( "No gene for " + ncbiId );
                continue;
            }


            gene = geneService.thaw( gene );

            Collection<GeneProduct> products = gene.getProducts();

            log.debug( "> Processing: " + gene.getOfficialSymbol() );

            if ( products.isEmpty() ) {
                numWithNoTranscript++;
                AbstractCLI.log.info( "No transcript for " + gene + ", skipping" );
                continue;
            }

            count++;

            CompositeSequence csForGene = null;

            /*
             * We arbitrarily link the "probe" to one of the gene's RNA transcripts. We could consider other strategies
             * to pick the representative, but it generally doesn't matter.
             */
            for ( GeneProduct geneProduct : products ) {

                /*
                 * Name is usually the genbank or ensembl accession
                 */
                String name = geneProduct.getName();

                Collection<DatabaseEntry> accessions = geneProduct.getAccessions();
                if ( accessions.isEmpty() ) {
                    throw new IllegalStateException( "Gene product has no biosequence accessions: " + geneProduct );
                }

                BioSequence bioSequence = BioSequence.Factory.newInstance();
                bioSequence.setName( name );
                bioSequence.setTaxon( this.taxon );
                bioSequence.setPolymerType( PolymerType.RNA );
                bioSequence.setType( SequenceType.mRNA );
                BioSequence existing = null;

                for(DatabaseEntry accession : accessions) {

                    existing = bioSequenceService.findByAccession( accessions.iterator().next() );

                    if (existing == null ||  existing.getSequenceDatabaseEntry() == null ) {
                        continue; // looping over geneproducts
                    }


                    // FIXME It is possible that this sequence will have been aligned to the genome, which is a bit
                    // confusing. So it will map to a gene. Worse case: it maps to more than one gene ...
                    if ( existing == null ) {
                        // create a copy, each biosequence must own their database entry
                        DatabaseEntry databaseEntry = accessions.iterator().next();
                        DatabaseEntry clone = DatabaseEntry.Factory.newInstance();
                        clone.setAccession( databaseEntry.getAccession() );
                        clone.setAccessionVersion( databaseEntry.getAccessionVersion() );
                        clone.setUri( databaseEntry.getUri() );
                        clone.setExternalDatabase( databaseEntry.getExternalDatabase() );
                        bioSequence.setSequenceDatabaseEntry( clone );
                    }

                    if ( existing == null ) {
                        bioSequence = ( BioSequence ) this.getPersisterHelper().persist( bioSequence );
                    } else {
                        bioSequence = existing;
                    }
                }



                log.info( "New element " + " with sequence used:" + bioSequence.getName() + " for " + gene.getOfficialSymbol() );
                csForGene = CompositeSequence.Factory.newInstance();

                csForGene.setName( gene.getNcbiGeneId().toString() );

                csForGene.setArrayDesign( platform );
                csForGene.setBiologicalCharacteristic( bioSequence );
                csForGene.setDescription( "Generic expression element for " + gene );
                csForGene = compositeSequenceService.create( csForGene );
                assert csForGene.getId() != null : "No id for " + csForGene + " for " + gene;
                platform.getCompositeSequences().add( csForGene );
                numNewElements++;

                assert bioSequence.getId() != null;
                assert geneProduct.getId() != null;
                assert csForGene.getBiologicalCharacteristic() != null
                        && csForGene.getBiologicalCharacteristic().getId() != null;

                AnnotationAssociation aa = AnnotationAssociation.Factory.newInstance();
                aa.setGeneProduct( geneProduct );
                aa.setBioSequence( bioSequence );
                annotationAssociationService.create( aa );
                break;
            }

            if ( count % 100 == 0 )
                AbstractCLI.log
                        .info( count + " genes processed; " + numNewElements + " new elements; " + numUpdatedElements
                                + " updated elements; " + numWithNoTranscript
                                + " genes had no transcript and were skipped." );
        }

        AbstractCLI.log.info( "Platform has " + arrayDesignService.numCompositeSequenceWithGenes( platform )
                + " 'elements' associated with genes." );

        arrayDesignReportService.generateArrayDesignReport( platform.getId() );

        log.info( count + " genes processed; " + numNewElements + " new elements; " + numUpdatedElements
                + " updated elements; " + numWithNoTranscript + " genes had no transcript and were skipped." );

        auditTrailService.addUpdateEvent( platform, AnnotationBasedGeneMappingEvent.class,
                count + " genes processed; " + numNewElements + " new elements; " + numUpdatedElements
                        + " updated elements; " + numWithNoTranscript + " genes had no transcript and were skipped." );
        arrayDesignAnnotationService.deleteExistingFiles( platform );

        AbstractCLI.log.info( "Don't forget to update the annotation files" );
    }

    @Override
    public String getShortDesc() {
        return "Update a 'platform' based on a list of NCBI IDs";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        geneService = this.getBean( GeneService.class );
        arrayDesignAnnotationService = this.getBean( ArrayDesignAnnotationService.class );
        TaxonService taxonService = this.getBean( TaxonService.class );
        bioSequenceService = this.getBean( BioSequenceService.class );
        arrayDesignService = this.getBean( ArrayDesignService.class );
        compositeSequenceService = this.getBean( CompositeSequenceService.class );
        annotationAssociationService = this.getBean( AnnotationAssociationService.class );
        arrayDesignReportService = this.getBean( ArrayDesignReportService.class );
        this.platformShortName = commandLine.getOptionValue( "a" );
        this.taxon = this.setTaxonByName( commandLine, taxonService );
    }


    private Map<String, CompositeSequence> nameMap( ArrayDesign arrayDesign ) {

        Map<String, CompositeSequence> existingElements = new HashMap<>();

        if ( arrayDesign.getCompositeSequences().isEmpty() )
            return existingElements;

        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            assert cs.getId() != null : "Null id for " + cs;
            existingElements.put( cs.getName(), cs );
        }
        return existingElements;
    }
}
