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
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.model.common.auditAndSecurity.eventType.AnnotationBasedGeneMappingEvent;
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
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.File;
import java.util.*;

/**
 * Create (or update) an array design based on a list of NCBI gene IDs desired to be on the platform.
 *
 * @author paul
 */
public class GenericGenelistDesignGenerator extends AbstractAuthenticatedCLI {

    @Autowired
    private AnnotationAssociationService annotationAssociationService;
    @Autowired
    private ArrayDesignAnnotationService arrayDesignAnnotationService;
    @Autowired
    private ArrayDesignReportService arrayDesignReportService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private BioSequenceService bioSequenceService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private GeneService geneService;

    @Autowired
    private GeneProductService geneProductService;

    @Autowired
    private TaxonService taxonService;

    private String platformShortName = null;
    private String geneListFileName = null;
    private Taxon taxon = null;

    private boolean noDB = false;


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

        options.addOption( Option.builder( "nodb" ).desc( "Dry run: Do not update the database nor delete any flat files" ).build() );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {

        ArrayDesign platform = arrayDesignService.findByShortName( this.platformShortName );
        platform = arrayDesignService.thaw( platform );

        // test whether the geneListFileName file exists and is readable
        File geneListFile = new File( this.geneListFileName );
        if ( !geneListFile.exists() || !geneListFile.canRead() ) {
            throw new IllegalArgumentException( "File " + this.geneListFileName + " does not exist or cannot be read" );
        }

        Set<String> ncbiIds = new HashSet<String>( FileUtils.readListFileToStrings( this.geneListFileName ) );
        log.info( "File had " + ncbiIds.size() + " gene ids" );

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

            log.debug( "> Processing element for NCBI ID = " + ncbiId );

            CompositeSequence csForGene = null;
            if ( existingSymbolMap.containsKey( ncbiId ) ) {
                /*
                Work out if the existing association is to a dummy sequence or not; if not, we have to make a new one.
                 */
                csForGene = existingSymbolMap.get( ncbiId );

                if ( csForGene.getBiologicalCharacteristic().getType() == null ) {
//                    log.info( "Gene NCBI ID=" + ncbiId + " already has an element [" + csForGene + "], but sequence type of " + csForGene.getBiologicalCharacteristic()
//                            + " is null, will replace with dummy" ); // rare case
                    needsDummyElement++;
                } else if ( csForGene.getBiologicalCharacteristic().getType().equals( SequenceType.DUMMY ) ) {
                    log.debug( "Gene NCBI ID=" + ncbiId + " already has a usable element, nothing to be done" ); // FOR NOW. This could be a dangling sequence if the gene didn't exist.
                    continue;
                } else {
                    needsDummyElement++;
                    //  log.info( "Gene NCBI ID=" + ncbiId + " already has an element [" + csForGene + "], but it is not a dummy, will update" );
                }
            } else {
                log.info( "Gene NCBI ID=" + ncbiId + " not on platform, may add" );
            }

            Gene gene = null;
            try {
                gene = geneService.findByNCBIId( Integer.parseInt( ncbiId ) );
            } catch ( NumberFormatException e ) {
                // shouldn't happen but just in case
                log.error( "Could not parse NCBI ID = " + ncbiId + " as an integer" );
            }

            boolean geneExists = gene != null;
            if ( !geneExists ) {
                log.warn( "No gene for NCBI ID = " + ncbiId + " but adding dummy sequence anyway (no gene product association wil be made)" );
                geneNotFound++;
                // continue;
                // but still make sure there is an element on the platform for it.
            } else {
                gene = geneService.thawLite( gene );
            }

            if ( gene != null && gene.getProducts().isEmpty() ) {
                numWithNoTranscript++;
                log.info( "No transcripts for " + gene + ", adding element anyway" );
            }

            AnnotationAssociation aa = null;
            Collection<AnnotationAssociation> associationsToRemove = new HashSet<>();
            /*
            This block is to try to re-use existing usable dummy elements for the gene, but for the first time run it mostly just finds one that we want to remove.
            Such re-use makes sense if we have multiple "generations" of the same platform but if we have just one, this really isn't necessary (and it's going to be slow because of the thaws)
             */
            if ( gene != null && !gene.getProducts().isEmpty() ) {
                Collection<AnnotationAssociation> aas = annotationAssociationService.find( gene ); // making fetching eager would help avoid thaws below, but not a big deal.
                for ( AnnotationAssociation aae : aas ) {
                    GeneProduct gp = geneProductService.thaw( aae.getGeneProduct() );
                    BioSequence bp = bioSequenceService.thaw( aae.getBioSequence() );
                    if ( gp == null || bp == null ) {
                        log.warn( "Invalid association of gp=" + gp + " and bp=" + bp + " for " + gene + ", marking for removal" );
                        associationsToRemove.add( aae );
                    } else if ( bp.getType() != null && bp.getType().equals( SequenceType.DUMMY ) && gp.isDummy() ) {
                        if ( aa != null ) { // this is a sanity check, if we are sure this isn't an issue we can just break here.
                            throw new IllegalStateException( "More than one dummy annotation association for " + gene );
                        }
                        log.info( "Re-using dummy association for " + gene );
                        aa = aae;
                    } else {
                        // otherwise, we're going to want to delete these old AnnotationAssociations assuming they aren't used for anything.
                        associationsToRemove.add( aae );
                    }
                }
            }

            BioSequence bioSequence = null;
            if ( aa == null ) {
                /* create a dummy gene Product and sequence for the gene. */

                bioSequence = csForGene == null ? null : csForGene.getBiologicalCharacteristic();
                if ( bioSequence != null && bioSequence.getType() != null && bioSequence.getType().equals( SequenceType.DUMMY ) ) {
                    log.debug( "Existing dummy sequence for element, will reuse" );
                } else {
                    bioSequence = BioSequence.Factory.newInstance();

                    if ( gene == null ) {
                        bioSequence.setName( "NCBI ID=" + ncbiId + " generic sequence placeholder" );
                    } else {
                        bioSequence.setName( gene.getOfficialSymbol() + " [NCBI ID=" + gene.getNcbiGeneId() + "] generic sequence placeholder" );
                    }
                    bioSequence.setTaxon( this.taxon );
                    bioSequence.setPolymerType( PolymerType.RNA );
                    bioSequence.setType( SequenceType.DUMMY );
                    if ( !noDB ) bioSequence = bioSequenceService.create( bioSequence );
                }

                if ( geneExists ) { // we only create the Biosequence side if the gene doesn't exist. But we make this dummy gene product even if the gene has no transcripts in Gemma.
                    GeneProduct geneProduct = GeneProduct.Factory.newInstance();
                    geneProduct.setGene( gene );
                    geneProduct.setDummy( true );
                    geneProduct.setName( gene.getOfficialSymbol() + " [NCBI ID=" + gene.getNcbiGeneId() + "] generic element placeholder" );
                    if ( !noDB ) geneProduct = geneProductService.create( geneProduct );

                    aa = AnnotationAssociation.Factory.newInstance();
                    aa.setGeneProduct( geneProduct );
                    aa.setBioSequence( bioSequence );
                    if ( !noDB ) aa = annotationAssociationService.create( aa );

                    assert noDB || bioSequence.getId() != null;
                } else {
                    log.info( "Gene does not exist, will not create dummy gene product or annotation association" );
                }
            }

            if ( csForGene == null ) {
                if ( gene == null ) {
                    log.info( "New platform element for NCBI=" + ncbiId + " (" + this.taxon.getCommonName() + ") - but no corresponding gene exists in Gemma" );
                } else {
                    log.info( "New platform element for " + gene.getOfficialSymbol() + " NCBI=" + gene.getNcbiGeneId() + " (" + gene.getTaxon().getCommonName() + ")" );
                }
                csForGene = CompositeSequence.Factory.newInstance();
                csForGene.setName( ncbiId ); // IMPORTANT that this be just the NCBI ID.
                csForGene.setArrayDesign( platform );
                csForGene.setBiologicalCharacteristic( bioSequence );
                if ( gene == null ) {
                    csForGene.setDescription( "Generic expression element for NCBI ID = " + ncbiId );
                } else {
                    csForGene.setDescription( "Generic expression element for " + gene );
                }
                if ( !noDB ) csForGene = compositeSequenceService.create( csForGene );

                platform.getCompositeSequences().add( csForGene );
                numNewElements++;
            } else {
                if ( gene == null ) { // this shouldn't happen.
                    log.info( "Updating element to use dummy for NCBI=" + ncbiId + " (" + taxon.getCommonName() + ")" );
                } else {
                    log.info( "Updating element to use dummy for " + gene.getOfficialSymbol() + ": NCBI=" + gene.getNcbiGeneId() + " (" + gene.getTaxon().getCommonName() + ")" );
                }
                csForGene.setBiologicalCharacteristic( aa.getBioSequence() );
                if ( !noDB ) compositeSequenceService.update( csForGene );
                numUpdatedElements++;
            }

            if ( !associationsToRemove.isEmpty() ) {
                log.info( associationsToRemove.size() + " old 'non-dummy' annotation associations to remove" );
                if ( !noDB ) {
                    try {
                        annotationAssociationService.remove( associationsToRemove ); // may fail if there are other associations.
                    } catch ( Exception e ) {
                        log.warn( "Could not delete old annotation associations for " + gene + ": " + e.getMessage() );
                    }
                }
            }

            assert noDB || ( csForGene.getBiologicalCharacteristic() != null
                    && csForGene.getBiologicalCharacteristic().getId() != null );

            count++;
            if ( count % 200 == 0 )
                log.info( " >>>>>>>>> " + count + " genes processed; " + numNewElements + " new elements; " + numUpdatedElements
                        + " updated elements; " + numWithNoTranscript
                        + " genes had no transcript." );
        }

        log.info( "Platform has " + arrayDesignService.numCompositeSequenceWithGenes( platform )
                + " 'elements' associated with genes." );

        if ( !noDB ) arrayDesignReportService.generateArrayDesignReport( platform.getId() );

        String auditMessage = count + " genes processed; " + numNewElements + " new elements; " + numUpdatedElements
                + " updated elements; " + numWithNoTranscript + " genes had no transcript; " + geneNotFound + " genes from the file could not be found";
        log.info( auditMessage );

        if ( !noDB ) auditTrailService.addUpdateEvent( platform, AnnotationBasedGeneMappingEvent.class, auditMessage );

        log.info( "Don't forget to update the annotation files, any old ones will be deleted (unless dry run)" );
        if ( !noDB ) arrayDesignAnnotationService.deleteExistingFiles( platform );

        /*
        Delete elements for the platform that are not on the input list. This should probably not be kept here; we'll do it offline.
         */
//        for ( String geneID : existingSymbolMap.keySet() ) {
//            if ( !ncbiIds.contains( geneID ) ) {
//                log.info( "Gene " + geneID + " is not in the input list, will remove element from platform if possible (" + existingSymbolMap.get( geneID ) + ")" );
//                if ( !noDB ) {
//                    try {
//                        // compositeSequenceService.remove( existingSymbolMap.get( geneID ) );
//                    } catch ( Exception e ) {
//                        // if there is data associated with it, this will fail. We would need to delete DEA results at least.
//                        log.warn( "Could not remove unneeded platform element for geneID=" + geneID + ": " + existingSymbolMap.get( geneID ) + ": " + e.getMessage() );
//                    }
//                }
//            }
//        }
    }

    @Override
    public String getShortDesc() {
        return "Update a 'platform' based on a list of NCBI IDs";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

        this.platformShortName = commandLine.getOptionValue( "a" );
        this.taxon = this.taxonService.findByCommonName( commandLine.getOptionValue( "t" ) );
        this.geneListFileName = commandLine.getOptionValue( "f" );

        this.noDB = commandLine.hasOption( "nodb" );

        if ( noDB ) {
            log.warn( "***** DRY RUN - no changes will be saved (you may still see relevant logging messages) *****" );
        }

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
