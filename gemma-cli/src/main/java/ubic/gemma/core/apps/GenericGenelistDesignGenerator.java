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
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
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
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Create (or update) an array design based on the current set of transcripts for a taxon.
 * This is used to create a 'platform' for linking non-array based data to the system, or data for which we have only
 * gene or transcript-level information.
 * See also: To generate annotation files for all genes in a taxon, this can also accomplished by
 * ArrayDesignAnnotationFileCli. The difference here is that an array design is actually created.
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
    private ExternalDatabaseService externalDatabaseService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private PersisterHelper persisterHelper;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private TaxonService taxonService;

    private Taxon taxon = null;
    private boolean useEnsemblIds = false;
    private boolean useNCBIIds = false;

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
        options.addOption( Option.builder( "t" ).longOpt( "taxon" ).desc( "Taxon of the genes" ).argName( "taxon" ).hasArg().build() );
        options.addOption( "ncbiids", "use NCBI numeric IDs as the identifiers instead of gene symbols" );
        options.addOption( "ensembl", "use Ensembl identifiers instead of gene symbols" );
    }

    @Override
    protected void doWork() throws Exception {
        ExternalDatabase genbank = externalDatabaseService.findByName( "Genbank" );
        ExternalDatabase ensembl = externalDatabaseService.findByName( "Ensembl" );
        if ( genbank == null || ensembl == null ) {
            throw new IllegalStateException( "A record for Genbank and/or Ensembl couldn't be found" );
        }

        /*
         * Create the stub array design for the organism. The name and etc. are generated automatically. If the design
         * exists, we update it.
         */

        String shortName = this.generateShortName();

        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setShortName( shortName );

        // common name
        arrayDesign.setPrimaryTaxon( taxon );
        String nameExt = useNCBIIds ? ", indexed by NCBI IDs" : useEnsemblIds ? ", indexed by Ensembl IDs" : "";
        arrayDesign.setName( "Generic platform for " + taxon.getScientificName() + nameExt );
        arrayDesign.setDescription( "Created by Gemma" );
        arrayDesign.setTechnologyType( TechnologyType.GENELIST ); // this is key

        if ( arrayDesignService.find( arrayDesign ) != null ) {
            AbstractCLI.log.info( "Platform for " + taxon + " already exists, will update" );
            arrayDesign = arrayDesignService.findOrFail( arrayDesign );
            arrayDesignService.deleteGeneProductAnnotationAssociations( arrayDesign );
            arrayDesign = arrayDesignService.loadOrFail( arrayDesign.getId() );

        } else {
            AbstractCLI.log.info( "Creating new 'generic' platform" );
            arrayDesign = arrayDesignService.create( arrayDesign );
        }
        arrayDesign = arrayDesignService.thaw( arrayDesign );

        // temporary: making sure we set it, as it is new.
        arrayDesign.setTechnologyType( TechnologyType.GENELIST );

        /*
         * Load up the genes for the organism.
         */
        Collection<Gene> knownGenes = geneService.loadAll( taxon );
        AbstractCLI.log.info( "Taxon has " + knownGenes.size() + " genes" );

        // this would be good for cases where the identifier we are using has changed.
        Map<Gene, CompositeSequence> existingGeneMap = new HashMap<>();

        if ( !useNCBIIds && !useEnsemblIds ) {
            // only using this for symbol changes.
            existingGeneMap = this.getExistingGeneMap( arrayDesign );
        }

        Map<String, CompositeSequence> existingSymbolMap = this.getExistingProbeNameMap( arrayDesign );

        int count = 0;
        int numWithNoTranscript = 0;
        // int hasGeneAlready = 0;
        // int numNewGenes = 0;
        int numNewElements = 0;
        int numUpdatedElements = 0;
        for ( Gene gene : knownGenes ) {
            gene = geneService.thaw( gene );

            Collection<GeneProduct> products = gene.getProducts();

            log.debug( "> Processing: " + gene.getOfficialSymbol() );

            if ( products.isEmpty() ) {
                numWithNoTranscript++;
                AbstractCLI.log.info( "No transcript for " + gene );
                continue;
            }

            count++;

            CompositeSequence csForGene = null;

            if ( useNCBIIds ) {
                if ( gene.getNcbiGeneId() == null ) {
                    AbstractCLI.log.debug( "No NCBI ID for " + gene + ", skipping" );
                    continue;
                }
                if ( existingSymbolMap.containsKey( gene.getNcbiGeneId().toString() ) ) {
                    csForGene = existingSymbolMap.get( gene.getNcbiGeneId().toString() );
                    log.debug( " ... gene exists on platform, will update if necessary [" + csForGene + "]" );
                }
            } else if ( useEnsemblIds ) {
                if ( gene.getEnsemblId() == null ) {
                    AbstractCLI.log.debug( "No Ensembl ID for " + gene + ", skipping" );
                    continue;
                }
                if ( existingSymbolMap.containsKey( gene.getEnsemblId() ) ) {
                    csForGene = existingSymbolMap.get( gene.getEnsemblId() );
                    log.debug( " ... gene exists on platform , will update if necessary [" + csForGene + "]" );
                }
            } else {
                // the "by symbols" platform.

                /*
                 * detect when the symbol has changed
                 */
                if ( existingSymbolMap.containsKey( gene.getOfficialSymbol() ) ) {
                    csForGene = existingSymbolMap.get( gene.getOfficialSymbol() );
                } else if ( existingGeneMap.containsKey( gene ) ) {
                    csForGene = existingGeneMap.get( gene );
                    AbstractCLI.log
                            .debug( "Gene symbol has changed for: " + gene + "? Current element has name=" + csForGene
                                    .getName() );
                    csForGene.setName( gene.getOfficialSymbol() );
                }
            }

            assert csForGene == null || csForGene.getId() != null : "Null id for " + csForGene;

            /*
             * We arbitrarily link the "probe" to one of the gene's RNA transcripts. We could consider other strategies
             * to pick the representative, but it generally doesn't matter.
             */
            for ( GeneProduct geneProduct : products ) {

                /*
                 * Name is usually the genbank or ensembl accession
                 */
                String name = geneProduct.getName();
                BioSequence bioSequence = BioSequence.Factory.newInstance();
                Collection<DatabaseEntry> accessions = geneProduct.getAccessions();
                bioSequence.setName( name );
                bioSequence.setTaxon( taxon );
                bioSequence.setPolymerType( PolymerType.RNA );
                bioSequence.setType( SequenceType.mRNA );
                BioSequence existing = null;

                if ( accessions.isEmpty() ) {
                    // this should not be hit.
                    AbstractCLI.log.warn( "No accession for " + name );
                    DatabaseEntry de = DatabaseEntry.Factory.newInstance();
                    de.setAccession( name );
                    if ( name.startsWith( "ENS" ) && name.length() > 10 ) {
                        de.setExternalDatabase( ensembl );
                    } else {
                        if ( name.matches( "^[A-Z]{1,2}(_?)[0-9]+(\\.[0-9]+)?$" ) ) {
                            de.setExternalDatabase( genbank );
                        } else {
                            AbstractCLI.log.info( "Name doesn't look like genbank or ensembl, skipping: " + name );
                            continue;
                        }
                    }
                    bioSequence.setSequenceDatabaseEntry( de );
                } else {
                    // FIXME It is possible that this sequence will have been aligned to the genome, which is a bit
                    // confusing. So it will map to a gene. Worse case: it maps to more than one gene ...
                    existing = bioSequenceService.findByAccession( accessions.iterator().next() );
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
                }

                if ( existing == null ) {
                    bioSequence = ( BioSequence ) persisterHelper.persist( bioSequence );
                } else {
                    bioSequence = existing;
                }

                assert bioSequence != null && bioSequence.getId() != null;

                if ( bioSequence.getSequenceDatabaseEntry() == null ) {
                    AbstractCLI.log.info( "No DB entry for " + bioSequence + "(" + gene
                            + "), will look for a better sequence to use ..." );
                    continue;
                }

                if ( csForGene == null ) { // i.e. it is new
                    log.info( "New element " + " with sequence used:" + bioSequence.getName() + " for " + gene.getOfficialSymbol() );
                    csForGene = CompositeSequence.Factory.newInstance();
                    if ( useNCBIIds ) {
                        csForGene.setName( gene.getNcbiGeneId().toString() );
                    } else if ( useEnsemblIds ) {
                        csForGene.setName( gene.getEnsemblId() );
                    } else {
                        csForGene.setName( gene.getOfficialSymbol() );
                    }

                    csForGene.setArrayDesign( arrayDesign );
                    csForGene.setBiologicalCharacteristic( bioSequence );
                    csForGene.setDescription( "Generic expression element for " + gene );
                    csForGene = compositeSequenceService.create( csForGene );
                    assert csForGene.getId() != null : "No id for " + csForGene + " for " + gene;
                    arrayDesign.getCompositeSequences().add( csForGene );
                    numNewElements++;
                } else { // i.e. it is already in the system; just updating
                    boolean changed = false;
                    assert csForGene.getId() != null : "No id for " + csForGene + " for " + gene;

                    if ( !csForGene.getArrayDesign().equals( arrayDesign ) ) {
                        // does this happen?
                        log.debug( "Platform changed? " + csForGene + " on " + csForGene.getArrayDesign() );
                        csForGene.setArrayDesign( arrayDesign );
                        csForGene.setDescription( "Generic expression element for " + gene );
                        changed = true;
                    }

                    if ( csForGene.getBiologicalCharacteristic() == null ) {
                        log.warn( csForGene + " had no sequence, setting to " + bioSequence );
                        csForGene.setBiologicalCharacteristic( bioSequence );
                        changed = true;
                    }

                    if ( !csForGene.getBiologicalCharacteristic().equals( bioSequence ) ) {
                        // does this happen?
                        csForGene.setBiologicalCharacteristic( bioSequence );
                        changed = true;
                    }

                    if ( changed ) {
                        compositeSequenceService.update( csForGene );
                    }

                    // making sure ...
                    csForGene = compositeSequenceService.loadOrFail( csForGene.getId() );
                    assert csForGene.getId() != null;
                    arrayDesign.getCompositeSequences().add( csForGene );

                    if ( changed ) {
                        if ( AbstractCLI.log.isDebugEnabled() )
                            AbstractCLI.log
                                    .debug( "Updating existing element: " + csForGene + " with " + bioSequence + " for "
                                            + gene );
                        numUpdatedElements++;
                    }
                }

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

        AbstractCLI.log.info( "Platform has " + arrayDesignService.numCompositeSequenceWithGenes( arrayDesign )
                + " 'elements' associated with genes." );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );

        log.info( count + " genes processed; " + numNewElements + " new elements; " + numUpdatedElements
                + " updated elements; " + numWithNoTranscript + " genes had no transcript and were skipped." );

        auditTrailService.addUpdateEvent( arrayDesign, AnnotationBasedGeneMappingEvent.class,
                count + " genes processed; " + numNewElements + " new elements; " + numUpdatedElements
                        + " updated elements; " + numWithNoTranscript + " genes had no transcript and were skipped." );
        arrayDesignAnnotationService.deleteExistingFiles( arrayDesign );

        AbstractCLI.log.info( "Don't forget to update the annotation files" );
    }

    @Override
    public String getShortDesc() {
        return "Update or create a 'platform' based on the genes for the organism";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 't' ) ) {
            this.taxon = this.getTaxonByName( commandLine );
        }
        if ( commandLine.hasOption( "ncbiids" ) ) {
            this.useNCBIIds = true;
        } else if ( commandLine.hasOption( "ensembl" ) ) {
            this.useEnsemblIds = true;
        }

        if ( useNCBIIds && useEnsemblIds ) {
            throw new IllegalArgumentException( "Choose one of ensembl or ncbi ids or gene symbols" );
        }
    }

    private String generateShortName() {
        String ncbiIdSuffix = useNCBIIds ? "_ncbiIds" : "";
        String ensemblIdSuffix = useEnsemblIds ? "_ensemblIds" : "";
        String shortName;
        if ( StringUtils.isBlank( taxon.getCommonName() ) ) {
            shortName = "Generic_" + StringUtils.strip( taxon.getScientificName() ).replaceAll( " ", "_" ) + ncbiIdSuffix;
        } else {
            shortName = "Generic_" + StringUtils.strip( taxon.getCommonName() ).replaceAll( " ", "_" ) + ncbiIdSuffix
                    + ensemblIdSuffix;
        }
        return shortName;
    }

    /**
     * For gene symbols.
     */
    private Map<Gene, CompositeSequence> getExistingGeneMap( ArrayDesign arrayDesign ) {

        Map<Gene, CompositeSequence> existingElements = new HashMap<>();

        if ( arrayDesign.getCompositeSequences().isEmpty() )
            return existingElements;

        AbstractCLI.log.info( "Loading genes for existing platform ..." );
        Map<CompositeSequence, Collection<Gene>> geneMap = compositeSequenceService
                .getGenes( arrayDesign.getCompositeSequences() );

        AbstractCLI.log
                .info( "Platform has genes already for " + geneMap.size() + "/" + arrayDesign.getCompositeSequences()
                        .size() + " elements." );

        for ( CompositeSequence cs : geneMap.keySet() ) {
            Collection<Gene> genes = geneMap.get( cs );

            /*
             * Two genes with the same symbol, but might be a mistake from an earlier run.
             */
            Gene g = null;
            if ( genes.size() > 1 ) {
                AbstractCLI.log.warn( "More than one gene for: " + cs + ": " + StringUtils.join( genes, ";" ) );
                for ( Gene cg : genes ) {
                    if ( cg.getOfficialSymbol().equals( cs.getName() ) ) {
                        g = cg;
                    }
                }
            } else {
                g = genes.iterator().next();
            }
            existingElements.put( g, cs );
        }

        return existingElements;
    }

    private Map<String, CompositeSequence> getExistingProbeNameMap( ArrayDesign arrayDesign ) {

        Map<String, CompositeSequence> existingElements = new HashMap<>();

        if ( arrayDesign.getCompositeSequences().isEmpty() )
            return existingElements;

        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            assert cs.getId() != null : "Null id for " + cs;
            existingElements.put( cs.getName(), cs );
        }
        return existingElements;
    }

    private Taxon getTaxonByName( CommandLine commandLine ) {
        String taxonName = commandLine.getOptionValue( 't' );
        ubic.gemma.model.genome.Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            AbstractCLI.log.error( "ERROR: Cannot find taxon " + taxonName );
        }
        return taxon;
    }
}
