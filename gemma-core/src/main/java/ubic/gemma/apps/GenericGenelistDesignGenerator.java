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
package ubic.gemma.apps;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Creates an array design based on the current set of transcripts for a taxon.
 * <p>
 * This is used to create a 'platform' for linking non-array based data to the system, or data for which we have only
 * gene or transcript-level information.
 * <p>
 * See also: To generate annotation files for all genes in a taxon, this can also accomplished by
 * ArrayDesignAnnotationFileCli. The difference here is that an array design is actually created.
 * 
 * @author paul
 * @version $Id$
 */
public class GenericGenelistDesignGenerator extends AbstractSpringAwareCLI {
    public static void main( String[] args ) {
        GenericGenelistDesignGenerator b = new GenericGenelistDesignGenerator();
        b.doWork( args );
    }

    private AnnotationAssociationService annotationAssociationService;
    private ArrayDesignAnnotationService arrayDesignAnnotationService;
    private ArrayDesignService arrayDesignService;
    private BioSequenceService bioSequenceService;
    private CompositeSequenceService compositeSequenceService;
    private ExternalDatabaseService externalDatabaseService;
    private ArrayDesignReportService arrayDesignReportService;
    private GeneService geneService;

    private Taxon taxon = null;

    private TaxonService taxonService;

    private boolean useNCBIIds = false;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option taxonOption = OptionBuilder.hasArg().withDescription( "taxon name" )
                .withDescription( "Taxon of the genes" ).withLongOpt( "taxon" ).isRequired().create( 't' );
        addOption( taxonOption );

        addOption( OptionBuilder.withDescription( "use NCBI numeric IDs as the identifiers instead of gene symbols" )
                .create( "ncbiids" ) );

    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "Update or create a 'platform' based on the genes for the organism", args );

        ExternalDatabase genbank = externalDatabaseService.find( "Genbank" );
        ExternalDatabase ensembl = externalDatabaseService.find( "Ensembl" );
        assert genbank != null;
        assert ensembl != null;

        /*
         * Create the stub array design for the organism. The name and etc. are generated automatically. If the design
         * exists, we update it.
         */

        String shortName = generateShortName();

        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setShortName( shortName );

        // common name
        arrayDesign.setPrimaryTaxon( taxon );
        String ncbiIDNameExtra = useNCBIIds ? ", indexed by NCBI IDs" : "";
        arrayDesign.setName( "Generic platform for " + taxon.getScientificName() + ncbiIDNameExtra );
        arrayDesign.setDescription( "Created by Gemma" );
        arrayDesign.setTechnologyType( TechnologyType.NONE ); // this is key

        if ( arrayDesignService.find( arrayDesign ) != null ) {
            log.info( "Platform for " + taxon + " already exists, will update" );
            arrayDesign = arrayDesignService.find( arrayDesign );
            arrayDesignService.deleteGeneProductAssociations( arrayDesign );

        } else {
            log.info( "Creating new 'generic' platform" );
            arrayDesign = arrayDesignService.create( arrayDesign );
        }
        arrayDesign = arrayDesignService.thaw( arrayDesign );

        // temporary: making sure we set it, as it is new.
        arrayDesign.setTechnologyType( TechnologyType.NONE );

        /*
         * Load up the genes for the organism.
         */
        Collection<Gene> knownGenes = geneService.loadAll( taxon );
        log.info( knownGenes.size() + " genes" );

        Map<Gene, CompositeSequence> existingGeneMap = getExistingGeneMap( arrayDesign );

        /*
         * Create a biosequence for each transcript, if there isn't one.
         */
        int count = 0;
        int numWithNoTranscript = 0;
        int numNewGenes = 0;

        for ( Gene gene : knownGenes ) {
            gene = geneService.thaw( gene );

            count++;

            if ( existingGeneMap.containsKey( gene ) ) {

                if ( gene.getProducts().isEmpty() ) {
                    /*
                     * We should delete this from the platform. Not sure this will happen so just putting this here in
                     * case.
                     */
                    log.warn( "Should delete from platform: " + existingGeneMap.get( gene ) );
                } else {
                    if ( log.isDebugEnabled() ) log.debug( "Already have gene: " + gene );
                }

                continue;
            }

            gene = geneService.thaw( gene );
            Collection<GeneProduct> products = gene.getProducts();

            if ( products.isEmpty() ) {
                numWithNoTranscript++;
                log.debug( "No transcript for " + gene );
                continue;
            }

            numNewGenes++;

            /*
             * We arbitrarily link the "probe" to one of the gene's RNA transcripts.
             */
            for ( GeneProduct geneProduct : products ) {
                if ( !GeneProductType.RNA.equals( geneProduct.getType() ) ) {
                    continue;
                }

                /*
                 * Name is usually the genbank or ensembl accession
                 */
                String name = geneProduct.getName();
                BioSequence bioSequence = BioSequence.Factory.newInstance();
                Collection<DatabaseEntry> accessions = geneProduct.getAccessions();
                bioSequence.setName( name );
                bioSequence.setTaxon( taxon );

                bioSequence.setPolymerType( PolymerType.RNA );
                // FIXME miRNAs (though, we don't really use this field.)

                bioSequence.setType( SequenceType.mRNA );
                BioSequence existing = null;

                if ( accessions.isEmpty() ) {
                    // this should not be hit.
                    log.warn( "No accession for " + name );
                    DatabaseEntry de = DatabaseEntry.Factory.newInstance();
                    de.setAccession( name );
                    if ( name.startsWith( "ENS" ) && name.length() > 10 ) {
                        de.setExternalDatabase( ensembl );
                    } else {
                        if ( name.matches( "^[A-Z]{1,2}(_?)[0-9]+(\\.[0-9]+)?$" ) ) {
                            de.setExternalDatabase( genbank );
                        } else {
                            log.info( "Name doesn't look like genbank or ensembl, skipping: " + name );
                            continue;
                        }
                    }
                    bioSequence.setSequenceDatabaseEntry( de );
                } else {
                    // if ( accessions.size() > 1 ) {
                    // log.warn( "Ambiguous accessions for GeneProduct " + name );
                    // }
                    bioSequence.setSequenceDatabaseEntry( accessions.iterator().next() );
                    existing = bioSequenceService.findByAccession( accessions.iterator().next() );

                    // FIXME It is possible that this sequence will have been aligned to the genome, which is a bit
                    // confusing. So it will map to a gene. Worse case: it maps to more than one gene ...

                }
                if ( existing == null ) {
                    bioSequence = ( BioSequence ) getPersisterHelper().persist( bioSequence );
                } else {
                    bioSequence = existing;
                }
                assert bioSequence != null;
                if ( bioSequence.getSequenceDatabaseEntry() == null ) {
                    log.info( "No DB entry for " + bioSequence + "(" + gene + "), skipping" );
                    continue;
                }

                CompositeSequence cs = CompositeSequence.Factory.newInstance();

                if ( useNCBIIds ) {
                    if ( gene.getNcbiGeneId() == null ) {
                        continue;
                    }
                    cs.setName( gene.getNcbiGeneId().toString() );
                } else {
                    cs.setName( gene.getOfficialSymbol() );
                }

                cs.setArrayDesign( arrayDesign );
                cs.setBiologicalCharacteristic( bioSequence );
                cs.setDescription( "Generic expression element for " + gene );
                cs = compositeSequenceService.findOrCreate( cs );
                arrayDesign.getCompositeSequences().add( cs );

                assert bioSequence.getId() != null;
                assert geneProduct.getId() != null;

                AnnotationAssociation aa = AnnotationAssociation.Factory.newInstance();
                aa.setGeneProduct( geneProduct );
                aa.setBioSequence( bioSequence );
                annotationAssociationService.create( aa );

                break;
            }

            if ( count % 100 == 0 )
                log.info( count + " genes processed; " + numWithNoTranscript + " had no transcript and were skipped; "
                        + numNewGenes + " genes are new to the platform." );
        }

        arrayDesignService.update( arrayDesign );

        log.info( count + " genes processed; " + numWithNoTranscript + " had no transcript and were skipped; "
                + numNewGenes + " genes are new to the platform." );

        log.info( "Array design has " + arrayDesignService.numCompositeSequenceWithGenes( arrayDesign )
                + " 'probes' associated with genes." );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );

        try {
            arrayDesignAnnotationService.deleteExistingFiles( arrayDesign );
        } catch ( IOException e ) {
            log.error( "Problem deleting old annotation files: " + e.getMessage() );
        }

        log.info( "Don't forget to update the annotation files" );

        return null;

    }

    @Override
    protected void processOptions() {
        super.processOptions();

        geneService = this.getBean( GeneService.class );
        arrayDesignAnnotationService = this.getBean( ArrayDesignAnnotationService.class );
        taxonService = getBean( TaxonService.class );
        bioSequenceService = getBean( BioSequenceService.class );
        arrayDesignService = getBean( ArrayDesignService.class );
        compositeSequenceService = getBean( CompositeSequenceService.class );
        annotationAssociationService = getBean( AnnotationAssociationService.class );
        externalDatabaseService = getBean( ExternalDatabaseService.class );
        arrayDesignReportService = getBean( ArrayDesignReportService.class );

        if ( hasOption( 't' ) ) {
            String taxonName = getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                log.error( "ERROR: Cannot find taxon " + taxonName );
            }
        }
        if ( hasOption( "ncbiids" ) ) {
            this.useNCBIIds = true;
        }
    }

    /**
     * @return
     */
    private String generateShortName() {
        String ncbiIdSuffix = useNCBIIds ? "_ncbiIds" : "";

        String shortName = "";
        if ( StringUtils.isBlank( taxon.getCommonName() ) ) {
            shortName = "Generic_" + StringUtils.strip( taxon.getScientificName() ).replaceAll( " ", "_" )
                    + ncbiIdSuffix;
        } else {
            shortName = "Generic_" + StringUtils.strip( taxon.getCommonName() ).replaceAll( " ", "_" ) + ncbiIdSuffix;
        }
        return shortName;
    }

    /**
     * @param arrayDesign
     * @return
     */
    private Map<Gene, CompositeSequence> getExistingGeneMap( ArrayDesign arrayDesign ) {

        Map<Gene, CompositeSequence> existingElements = new HashMap<Gene, CompositeSequence>();

        if ( arrayDesign.getCompositeSequences().isEmpty() ) return existingElements;

        Map<CompositeSequence, Collection<Gene>> genemap = compositeSequenceService.getGenes( arrayDesign
                .getCompositeSequences() );
        for ( CompositeSequence cs : genemap.keySet() ) {
            Collection<Gene> genes = genemap.get( cs );
            assert genes.size() == 1;
            Gene g = genes.iterator().next();

            existingElements.put( g, cs );
        }
        return existingElements;
    }
}
