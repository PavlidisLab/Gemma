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
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.model.common.auditAndSecurity.eventType.AnnotationBasedGeneMappingEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.*;

/**
 * Create (or update) an array design based on a list of NCBI gene IDs desired to be on the platform.
 *
 * @author paul
 */
public class GenericGenelistDesignGenerator extends AbstractAuthenticatedCLI {

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


    @Autowired
    private AuditTrailService auditTrailService;

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

        Set<String> ncbiIds = new HashSet<String>( FileUtils.readListFileToStrings( this.geneListFileName ) );
        AbstractCLI.log.info( "File had " + ncbiIds.size() + " gene ids" );

        // this would be good for cases where the identifier we are using has changed.
        Map<Gene, CompositeSequence> existingGeneMap = new HashMap<>();


        Map<String, CompositeSequence> existingSymbolMap = this.nameMap( platform );

        int count = 0;
        int numWithNoTranscript = 0;
        int hasGeneAlready = 0;
        // int numNewGenes = 0;
        int geneNotFound = 0;
        int numNewElements = 0;
        int numUpdatedElements = 0;
        int needsDummyElement = 0;
        for ( String ncbiId : ncbiIds ) {
            CompositeSequence csForGene = null;
            if ( existingSymbolMap.containsKey( ncbiId ) ) {
                /*
                Work out if the existing association is to a dummy sequence or not; if not, we have to make a new one.
                 */
                csForGene = existingSymbolMap.get( ncbiId );
                if ( csForGene.getBiologicalCharacteristic().getType().equals( SequenceType.DUMMY ) ) {
                    hasGeneAlready++;
                    continue;
                } else {
                    needsDummyElement++;
                    AbstractCLI.log.info( "Gene " + ncbiId + " already has an element [" + csForGene + "], but it is not a dummy, will update" );
                }
            }

            Gene gene = null;
            try {
                gene = geneService.findByNCBIId( Integer.parseInt( ncbiId ) );
            } catch ( NumberFormatException e ) {
                AbstractCLI.log.error( "Could not parse " + ncbiId + " as an integer" );
            }
            if ( gene == null ) {
                AbstractCLI.log.warn( "No gene for " + ncbiId );
                geneNotFound++;
                continue;
            }

            gene = geneService.thaw( gene );

            Collection<GeneProduct> products = gene.getProducts();

            log.debug( "> Processing: " + gene.getOfficialSymbol() );

            if ( products.isEmpty() ) {
                numWithNoTranscript++;
                AbstractCLI.log.info( "No transcript for " + gene + ", skipping" );
                /*
                 * If a gene has no transcript, there is no reason to add a 'dummy' element for it.
                 */
                continue;
            }

            AnnotationAssociation aa = null;

            Collection<AnnotationAssociation> aas = annotationAssociationService.find( gene );
            for ( AnnotationAssociation aae : aas ) {
                if ( aae.getBioSequence().getType().equals( SequenceType.DUMMY ) && aae.getGeneProduct().isDummy() ) {
                    if ( aa != null ) { // this is a sanity check, if we are sure this isn't an issue we can just break here.
                        throw new IllegalStateException( "More than one dummy annotation association for " + gene );
                    }
                    aa = aae;
                }
            }

            if ( aa == null ) {
                /* create a dummy gene Product and sequence for the gene. */
                /* NOTE I am not checking here to see if there is already a dummy gene product for this gene. */
                GeneProduct geneProduct = GeneProduct.Factory.newInstance();
                geneProduct.setGene( gene );
                geneProduct.setDummy( true );
                geneProduct.setName( gene.getOfficialSymbol() + " generic element placeholder" );
                BioSequence bioSequence = BioSequence.Factory.newInstance();
                bioSequence.setName( gene.getOfficialSymbol() + " generic sequence placeholder" );
                bioSequence.setTaxon( this.taxon );
                bioSequence.setPolymerType( PolymerType.RNA );
                bioSequence.setType( SequenceType.DUMMY );

                bioSequence = bioSequenceService.create( bioSequence );
                aa = AnnotationAssociation.Factory.newInstance();
                aa.setGeneProduct( geneProduct );
                aa.setBioSequence( bioSequence );
                aa = annotationAssociationService.create( aa );

                assert bioSequence.getId() != null;
            }

            if ( csForGene == null ) {
                log.info( "New element for " + gene.getOfficialSymbol() + " NCBI=" + gene.getNcbiGeneId() + " (" + gene.getTaxon().getCommonName() + ")" );
                csForGene = CompositeSequence.Factory.newInstance();
                csForGene.setName( gene.getNcbiGeneId().toString() ); // IMPORTANT that this be just the NCBI ID.
                csForGene.setArrayDesign( platform );
                csForGene.setBiologicalCharacteristic( aa.getBioSequence() );
                csForGene.setDescription( "Generic expression element for " + gene );
                csForGene = compositeSequenceService.create( csForGene );

                platform.getCompositeSequences().add( csForGene );
                numNewElements++;
            } else {
                log.info( "Updating element to use dummy sfor " + gene.getOfficialSymbol() + " NCBI=" + gene.getNcbiGeneId() + " (" + gene.getTaxon().getCommonName() + ")" );
                csForGene.setBiologicalCharacteristic( aa.getBioSequence() );
                compositeSequenceService.update( csForGene );
                numUpdatedElements++;
            }

            assert csForGene.getBiologicalCharacteristic() != null
                    && csForGene.getBiologicalCharacteristic().getId() != null;

            count++;
            if ( count % 200 == 0 )
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

        /*
        TODO possibly: delete elements for the platform that are not on the input list.
         */
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
        this.taxon = taxonService.findByCommonName( commandLine.getOptionValue( "t" ) );
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
