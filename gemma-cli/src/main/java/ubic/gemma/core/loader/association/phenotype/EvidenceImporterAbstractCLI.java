/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.loader.association.phenotype;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.basecode.ontology.providers.UberonOntologyService;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Deprecated
public abstract class EvidenceImporterAbstractCLI extends AbstractAuthenticatedCLI {

    static final String WRITE_FOLDER = Settings.getString( "gemma.appdata.home" ) + File.separator + "EvidenceImporterNeurocarta";

    /* replacement for MGED Biosource, which is defined as "the original source material before any treatment events" */
    final String BIOSOURCE = "material entity";
    final String BIOSOURCE_ONTOLOGY = "http://purl.obolibrary.org/obo/BFO_0000040";

    final String DEVELOPMENTAL_STAGE = "developmental stage";
    final String DEVELOPMENTAL_STAGE_ONTOLOGY = "http://www.ebi.ac.uk/efo/EFO_0000399";

    final String EXPERIMENT = "experimental process";
    final String EXPERIMENT_ONTOLOGY = "http://www.ebi.ac.uk/efo/EFO_0002694";

    final String EXPERIMENT_DESIGN = "study design";
    final String EXPERIMENT_DESIGN_ONTOLOGY = "http://www.ebi.ac.uk/efo/EFO_0001426";

    final String EXPERIMENTAL_EVIDENCE = "EXPERIMENTAL";
    final String LITERATURE_EVIDENCE = "LITERATURE";

    final String ORGANISM_PART = "organism part";
    final String ORGANISM_PART_ONTOLOGY = "http://www.ebi.ac.uk/efo/EFO_0000635";

    final String TREATMENT = "treatment";
    final String TREATMENT_ONTOLOGY = "http://www.ebi.ac.uk/efo/EFO_0000727";

    /*
     * Note: we don't have development ontologies configured here.
     */

    final Set<String> errorMessage = new TreeSet<>();
    final Map<String, Integer> mapColumns = new HashMap<>();
    BufferedReader br = null;
    boolean createInDatabase = false;
    MondoOntologyService diseaseOntologyService = null;
    GeneService geneService = null;
    HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    UberonOntologyService uberonOntologyService = null;
    // input file path
    String inputFile = "";
    BufferedWriter logFileWriter = null;
    MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    ObiService obiService = null;
    PhenotypeAssociationManagerService phenotypeAssociationManagerService = null;
    TaxonService taxonService = null;
    String warningMessage = "";

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @Override
    protected void buildOptions( Options options ) {
        @SuppressWarnings("static-access")
        Option fileOption = Option.builder( "f" ).desc( "The file" ).hasArg()
                .argName( "file path" ).required().build();
        options.addOption( fileOption );
        Option createOption = Option.builder( "c" ).desc( "Create in database; default is false (prints to stdout)" ).build();
        options.addOption( createOption );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        this.inputFile = commandLine.getOptionValue( 'f' );
        this.createInDatabase = commandLine.hasOption( 'c' );
    }

    /**
     * Make sure the evidence code found exist in Gemma
     */
    void checkEvidenceCodeExits( String evidenceCode ) {

        if ( !( evidenceCode.equalsIgnoreCase( "IC" ) || evidenceCode.equalsIgnoreCase( "IDA" ) || evidenceCode
                .equalsIgnoreCase( "IEA" ) || evidenceCode.equalsIgnoreCase( "IEP" )
                || evidenceCode
                .equalsIgnoreCase( "IGI" )
                || evidenceCode.equalsIgnoreCase( "IMP" ) || evidenceCode
                .equalsIgnoreCase( "IPI" )
                || evidenceCode.equalsIgnoreCase( "ISS" ) || evidenceCode
                .equalsIgnoreCase( "NAS" )
                || evidenceCode.equalsIgnoreCase( "ND" ) || evidenceCode
                .equalsIgnoreCase( "RCA" )
                || evidenceCode.equalsIgnoreCase( "TAS" ) || evidenceCode
                .equalsIgnoreCase( "NR" )
                || evidenceCode.equalsIgnoreCase( "EXP" ) || evidenceCode
                .equalsIgnoreCase( "ISA" )
                || evidenceCode.equalsIgnoreCase( "ISM" ) || evidenceCode
                .equalsIgnoreCase( "IGC" )
                || evidenceCode.equalsIgnoreCase( "ISO" ) || evidenceCode
                .equalsIgnoreCase( "IIA" )
                || evidenceCode.equalsIgnoreCase( "IBA" ) || evidenceCode
                .equalsIgnoreCase( "IBD" )
                || evidenceCode.equalsIgnoreCase( "IKR" ) || evidenceCode
                .equalsIgnoreCase( "IRD" )
                || evidenceCode.equalsIgnoreCase( "IMR" ) || evidenceCode
                .equalsIgnoreCase( "IED" )
                || evidenceCode.equalsIgnoreCase( "IAGP" ) || evidenceCode
                .equalsIgnoreCase( "IPM" )
                || evidenceCode.equalsIgnoreCase( "QTM" ) ) ) {
            this.writeError( "evidenceCode not known: " + evidenceCode );
        }
    }

    protected void createWriteFolder() throws Exception {

        File folder = new File( EvidenceImporterAbstractCLI.WRITE_FOLDER );
        if ( !folder.mkdir() && !folder.exists() ) {
            throw new Exception( "Could not create directory " + folder );
        }
    }

    /**
     * Look at all Headers and identify them to determine the type of evidence
     * @return type
     * @throws Exception IO problems
     */
    protected String findTypeOfEvidence() throws Exception {

        // lets check what type of evidence when have on the sheet, each header can have any position but must use
        // strict syntax
        String[] headers = this.br.readLine().split( "\t" );

        int index = 0;

        // all possible headers
        for ( String header : headers ) {

            header = header.trim();

            if ( header.equalsIgnoreCase( "GeneSymbol" ) ) {
                this.mapColumns.put( "GeneSymbol", index );
            } else if ( header.equalsIgnoreCase( "GeneId" ) ) {
                this.mapColumns.put( "GeneId", index );
            } else if ( header.equalsIgnoreCase( "EvidenceCode" ) ) {
                this.mapColumns.put( "EvidenceCode", index );
            } else if ( header.equalsIgnoreCase( "Comments" ) ) {
                this.mapColumns.put( "Comments", index );
            } else if ( header.equalsIgnoreCase( "IsNegative" ) ) {
                this.mapColumns.put( "IsNegative", index );
            } else if ( header.equalsIgnoreCase( "ExternalDatabase" ) ) {
                this.mapColumns.put( "ExternalDatabase", index );
            } else if ( header.equalsIgnoreCase( "DatabaseLink" ) ) {
                this.mapColumns.put( "DatabaseLink", index );
            } else if ( header.equalsIgnoreCase( "Phenotypes" ) ) {
                this.mapColumns.put( "Phenotypes", index );
            } else if ( header.equalsIgnoreCase( "PrimaryPubMeds" ) ) {
                this.mapColumns.put( "PrimaryPubMeds", index );
            } else if ( header.equalsIgnoreCase( "OtherPubMed" ) ) {
                this.mapColumns.put( "OtherPubMed", index );
            } else if ( header.equalsIgnoreCase( "Score" ) ) {
                this.mapColumns.put( "Score", index );
            } else if ( header.equalsIgnoreCase( "ScoreType" ) ) {
                this.mapColumns.put( "ScoreType", index );
            } else if ( header.equalsIgnoreCase( "Strength" ) ) {
                this.mapColumns.put( "Strength", index );
            } else if ( header.equalsIgnoreCase( "DevelopmentalStage" ) ) {
                this.mapColumns.put( DEVELOPMENTAL_STAGE, index );
            } else if ( header.equalsIgnoreCase( "BioSource" ) ) {
                this.mapColumns.put( BIOSOURCE, index );
            } else if ( header.equalsIgnoreCase( "OrganismPart" ) ) {
                this.mapColumns.put( ORGANISM_PART, index );
            } else if ( header.equalsIgnoreCase( "ExperimentDesign" ) ) {
                this.mapColumns.put( "ExperimentDesign", index );
            } else if ( header.equalsIgnoreCase( "Treatment" ) ) {
                this.mapColumns.put( TREATMENT, index );
            } else if ( header.equalsIgnoreCase( "Experiment" ) ) {
                this.mapColumns.put( "Experiment", index );
            } else if ( header.equalsIgnoreCase( "Strength" ) ) {
                this.mapColumns.put( "Strength", index );
            } else if ( header.equalsIgnoreCase( "Score" ) ) {
                this.mapColumns.put( "Score", index );
            } else if ( header.equalsIgnoreCase( "ScoreType" ) ) {
                this.mapColumns.put( "ScoreType", index );
            } else if ( header.equalsIgnoreCase( "PhenotypeMapping" ) ) {
                this.mapColumns.put( "PhenotypeMapping", index );
            } else if ( header.equalsIgnoreCase( "OrginalPhenotype" ) ) {
                this.mapColumns.put( "OriginalPhenotype", index );
            }
            // not used by evidence importer, used to add extra information only human readable, let this header pass
            else if ( !header.equalsIgnoreCase( "ExtraInfo" ) ) {
                throw new Exception( "header not found: " + header );
            }
            index++;
        }

        // Minimum fields any evidence should have, need a (taxon+geneSymbol) or (geneId+geneSymbol)
        if ( !( ( this.mapColumns.containsKey( "GeneId" ) ) && this.mapColumns.containsKey( "GeneSymbol" )
                && this.mapColumns.containsKey( "EvidenceCode" ) && this.mapColumns.containsKey( "Comments" )
                && this.mapColumns.containsKey( "Phenotypes" ) && this.mapColumns.containsKey( "OriginalPhenotype" )
                && this.mapColumns.containsKey( "PhenotypeMapping" ) ) ) {
            throw new Exception( "Headers not set correctly" );
        }

        // score set ???
        if ( this.mapColumns.containsKey( "Score" ) && this.mapColumns.containsKey( "ScoreType" ) && this.mapColumns
                .containsKey( "Strength" ) ) {
            AbstractCLI.log.info( "Found a score on the evidence" );
        } else {
            AbstractCLI.log.info( "No score on the evidence" );
        }

        // using an external database ???
        if ( this.mapColumns.containsKey( "ExternalDatabase" ) && this.mapColumns.containsKey( "DatabaseLink" ) ) {
            AbstractCLI.log.info( "External database link to evidence" );
        } else {
            AbstractCLI.log.info( "No external database" );
        }

        // rules to be an experimentalEvidence
        if ( this.mapColumns.containsKey( EXPERIMENT ) && this.mapColumns.containsKey( TREATMENT )
                && this.mapColumns.containsKey( EXPERIMENT_DESIGN ) && this.mapColumns.containsKey( ORGANISM_PART )
                && this.mapColumns.containsKey( BIOSOURCE ) && this.mapColumns.containsKey( DEVELOPMENTAL_STAGE )
                && this.mapColumns.containsKey( "OtherPubMed" ) && this.mapColumns.containsKey( "PrimaryPubMeds" ) ) {

            AbstractCLI.log.info( "The type of Evidence found is: " + this.EXPERIMENTAL_EVIDENCE );
            this.loadServices( true );
            return this.EXPERIMENTAL_EVIDENCE;
        }

        this.loadServices( false );

        return this.LITERATURE_EVIDENCE;

    }

    EvidenceSourceValueObject makeEvidenceSource( String databaseID, String externalDatabaseName ) {

        EvidenceSourceValueObject evidenceSourceValueObject = null;
        if ( databaseID != null && externalDatabaseName != null && !externalDatabaseName.isEmpty() ) {
            ExternalDatabaseValueObject externalDatabase = new ExternalDatabaseValueObject();
            externalDatabase.setName( externalDatabaseName );
            evidenceSourceValueObject = new EvidenceSourceValueObject( databaseID, externalDatabase );
        }

        return evidenceSourceValueObject;
    }

    /**
     * Trim an array of String
     */
    Set<String> trimArray( String[] array ) {

        Set<String> mySet = new HashSet<>();

        for ( String anArray : array ) {
            String value = anArray.trim();

            if ( !value.equals( "" ) ) {
                mySet.add( value );
            }
        }

        return mySet;
    }

    void writeAllExceptions() throws IOException {

        for ( String message : this.errorMessage ) {
            this.logFileWriter.write( "\n" + message );
            this.logFileWriter.flush();
        }
    }

    void writeError( String message ) {

        AbstractCLI.log.error( message );
        this.errorMessage.add( message );
    }

    void writeWarning( String message ) throws IOException {

        AbstractCLI.log.info( message );
        this.warningMessage += "\n" + message;
        this.logFileWriter.write( "\n" + message + " (warning)" );
        this.logFileWriter.flush();
    }

    /**
     * find the exact term of a search term in a Collection of Ontology terms
     *
     * @param  ontologyTerms Collection of ontologyTerms
     * @param  search        The value we are interested in finding
     * @return OntologyTerm the exact match value found
     */
    OntologyTerm findExactTerm( Collection<OntologySearchResult<OntologyTerm>> ontologyTerms, String search ) {

        // list of OntologyTerms found
        Collection<OntologyTerm> ontologyKept = new HashSet<>();
        OntologyTerm termFound = null;

        for ( OntologySearchResult<OntologyTerm> ot : ontologyTerms ) {
            if ( ot.getResult().getLabel() != null ) {
                if ( ot.getResult().getLabel().equalsIgnoreCase( search ) ) {
                    ontologyKept.add( ot.getResult() );
                    termFound = ot.getResult();
                }
            }
        }

        // if we have more than 1 result, hardcode the one to choose
        /*
         * See valueStringToOntologyTermMappings.txt (and GeoConverter) for ways to automate this.
         */
        if ( ontologyKept.size() > 1 ) {

            if ( search.equalsIgnoreCase( "juvenile" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {
                    if ( ontologyTerm.getUri().equalsIgnoreCase( "http://purl.obolibrary.org/obo/UBERON_0034919" ) ) { /*
                     * juvenile
                     * stage
                     */
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "adult" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://www.ebi.ac.uk/efo/EFO_0001272" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "newborn" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://www.ebi.ac.uk/efo/EFO_0001372" ) ) { /* neonate */
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "prenatal" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://www.ebi.ac.uk/efo/EFO_0007725" ) ) { /* embryo stage */
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "infant" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://www.ebi.ac.uk/efo/EFO_0001355" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "elderly" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://purl.obolibrary.org/obo/UBERON_0007222" ) ) { /* late adult stage */
                        return ontologyTerm;
                    }
                }
            }
        }

        if ( ontologyKept.size() > 1 ) {

            /* why is this a special case? */
            if ( search.equalsIgnoreCase( "apraxia" ) ) {

                for ( OntologyTerm o : ontologyKept ) {
                    if ( o.getLabel().equalsIgnoreCase( "apraxia" ) && o.getUri()
                            .equalsIgnoreCase( "http://purl.obolibrary.org/obo/DOID_4019" ) ) {
                        return o;
                    }
                }
            }

            this.writeError( "More than 1 term found for : " + search + "   " + ontologyKept.size() );

            for ( OntologyTerm o : ontologyKept ) {
                this.writeError( o.getLabel() + " " + o.getUri() );
            }
        }

        return termFound;
    }

    /**
     * Loads all services that will be needed
     *
     * @param experimentalEvidenceServicesNeeded if the type is an experimental evidence (we need then to load more)
     */
    private synchronized void loadServices( boolean experimentalEvidenceServicesNeeded ) throws Exception {

        this.phenotypeAssociationManagerService = this.getBean( PhenotypeAssociationManagerService.class );

        this.geneService = this.getBean( GeneService.class );
        this.taxonService = this.getBean( TaxonService.class );

        this.diseaseOntologyService = this.getBean( MondoOntologyService.class );
        this.humanPhenotypeOntologyService = this.getBean( HumanPhenotypeOntologyService.class );

        // ensure load, but only reindex if needed
        this.diseaseOntologyService.startInitializationThread( true, false );
        this.humanPhenotypeOntologyService.startInitializationThread( true, false );

        while ( !this.diseaseOntologyService.isOntologyLoaded() ) {
            this.wait( 5000 );
            AbstractCLI.log.info( "waiting for the Disease Ontology to load ..." );
        }

        while ( !this.humanPhenotypeOntologyService.isOntologyLoaded() ) {
            this.wait( 5000 );
            AbstractCLI.log.info( "waiting for the HP Ontology to load ..." );
        }

        // only need those services for experimental evidences
        if ( experimentalEvidenceServicesNeeded ) {

            this.obiService = this.getBean( ObiService.class );
            this.uberonOntologyService = this.getBean( UberonOntologyService.class );
            this.mammalianPhenotypeOntologyService = this.getBean( MammalianPhenotypeOntologyService.class );

            this.uberonOntologyService.startInitializationThread( true, false );
            this.mammalianPhenotypeOntologyService.startInitializationThread( true, false );
            this.obiService.startInitializationThread( true, false );

            while ( !this.mammalianPhenotypeOntologyService.isOntologyLoaded() ) {
                this.wait( 3000 );
                AbstractCLI.log.info( "waiting for the MP Ontology to load" );
            }

            while ( !this.obiService.isOntologyLoaded() ) {
                this.wait( 3000 );
                AbstractCLI.log.info( "waiting for the OBI Ontology to load" );
            }

            while ( !this.uberonOntologyService.isOntologyLoaded() ) {
                this.wait( 3000 );
                AbstractCLI.log.info( "waiting for the Uberon Ontology to load" );
            }
        }
    }
}
