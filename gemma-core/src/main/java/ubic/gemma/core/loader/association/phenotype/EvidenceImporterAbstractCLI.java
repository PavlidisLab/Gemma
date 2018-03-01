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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.*;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class EvidenceImporterAbstractCLI extends AbstractCLIContextCLI {

    static final String WRITE_FOLDER =
            Settings.getString( "gemma.appdata.home" ) + File.separator + "EvidenceImporterNeurocarta";

    final String BIOSOURCE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#BioSource";
    final String DEVELOPMENTAL_STAGE = "DevelopmentalStage";
    final String DEVELOPMENTAL_STAGE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#DevelopmentalStage";
    final String EXPERIMENT = "Experiment";
    final String EXPERIMENT_DESIGN = "ExperimentDesign";
    final String EXPERIMENT_DESIGN_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#ExperimentDesign";
    final String EXPERIMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Experiment";
    final String EXPERIMENTAL_EVIDENCE = "EXPERIMENTAL";
    final String LITERATURE_EVIDENCE = "LITERATURE";
    final String ORGANISM_PART = "OrganismPart";
    final String ORGANISM_PART_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#OrganismPart";
    final String TREATMENT = "Treatment";
    final String TREATMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Treatment";
    final Set<String> errorMessage = new TreeSet<>();
    final HashMap<String, Integer> mapColumns = new HashMap<>();
    BufferedReader br = null;
    boolean createInDatabase = false;
    DiseaseOntologyService diseaseOntologyService = null;
    FMAOntologyService fmaOntologyService = null;
    GeneService geneService = null;
    HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    // input file path
    String inputFile = "";
    BufferedWriter logFileWriter = null;
    MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    NIFSTDOntologyService nifstdOntologyService = null;
    ObiService obiService = null;
    PhenotypeAssociationManagerService phenotypeAssociationService = null;
    TaxonService taxonService = null;
    String warningMessage = "";

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @Override
    protected void buildOptions() {
        @SuppressWarnings("static-access") Option fileOption = OptionBuilder.withDescription( "The file" ).hasArg()
                .withArgName( "file path" ).isRequired().create( "f" );
        this.addOption( fileOption );
        @SuppressWarnings("static-access") Option createOption = OptionBuilder
                .withDescription( "Create in Database (false or true)" ).hasArg().withArgName( "create in Database" )
                .isRequired().create( "c" );
        this.addOption( createOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        this.inputFile = this.getOptionValue( 'f' );
        this.createInDatabase = Boolean.valueOf( this.getOptionValue( 'c' ) );
    }

    /**
     * Make sure the evidence code found exist in Gemma
     */
    void checkEvidenceCodeExits( String evidenceCode ) {

        if ( !( evidenceCode.equalsIgnoreCase( "IC" ) || evidenceCode.equalsIgnoreCase( "IDA" ) || evidenceCode
                .equalsIgnoreCase( "IEA" ) || evidenceCode.equalsIgnoreCase( "IEP" ) || evidenceCode
                .equalsIgnoreCase( "IGI" ) || evidenceCode.equalsIgnoreCase( "IMP" ) || evidenceCode
                .equalsIgnoreCase( "IPI" ) || evidenceCode.equalsIgnoreCase( "ISS" ) || evidenceCode
                .equalsIgnoreCase( "NAS" ) || evidenceCode.equalsIgnoreCase( "ND" ) || evidenceCode
                .equalsIgnoreCase( "RCA" ) || evidenceCode.equalsIgnoreCase( "TAS" ) || evidenceCode
                .equalsIgnoreCase( "NR" ) || evidenceCode.equalsIgnoreCase( "EXP" ) || evidenceCode
                .equalsIgnoreCase( "ISA" ) || evidenceCode.equalsIgnoreCase( "ISM" ) || evidenceCode
                .equalsIgnoreCase( "IGC" ) || evidenceCode.equalsIgnoreCase( "ISO" ) || evidenceCode
                .equalsIgnoreCase( "IIA" ) || evidenceCode.equalsIgnoreCase( "IBA" ) || evidenceCode
                .equalsIgnoreCase( "IBD" ) || evidenceCode.equalsIgnoreCase( "IKR" ) || evidenceCode
                .equalsIgnoreCase( "IRD" ) || evidenceCode.equalsIgnoreCase( "IMR" ) || evidenceCode
                .equalsIgnoreCase( "IED" ) || evidenceCode.equalsIgnoreCase( "IAGP" ) || evidenceCode
                .equalsIgnoreCase( "IPM" ) || evidenceCode.equalsIgnoreCase( "QTM" ) ) ) {
            this.writeError( "evidenceCode not in gemma: " + evidenceCode );
        }
    }

    void createWriteFolder() throws Exception {

        File folder = new File( EvidenceImporterAbstractCLI.WRITE_FOLDER );
        if ( !folder.mkdir() && !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }
    }

    /**
     * Look at all Headers and identify them to determine the type of evidence
     */
    String findTypeOfEvidence() throws Exception {

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
                this.mapColumns.put( "DevelopmentalStage", index );
            } else if ( header.equalsIgnoreCase( "BioSource" ) ) {
                this.mapColumns.put( "BioSource", index );
            } else if ( header.equalsIgnoreCase( "OrganismPart" ) ) {
                this.mapColumns.put( "OrganismPart", index );
            } else if ( header.equalsIgnoreCase( "ExperimentDesign" ) ) {
                this.mapColumns.put( "ExperimentDesign", index );
            } else if ( header.equalsIgnoreCase( "Treatment" ) ) {
                this.mapColumns.put( "Treatment", index );
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
        if ( this.mapColumns.containsKey( "Experiment" ) && this.mapColumns.containsKey( "Treatment" )
                && this.mapColumns.containsKey( "ExperimentDesign" ) && this.mapColumns.containsKey( "OrganismPart" )
                && this.mapColumns.containsKey( "BioSource" ) && this.mapColumns.containsKey( "DevelopmentalStage" )
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
     * @param ontologyTerms Collection of ontologyTerms
     * @param search        The value we are interested in finding
     * @return OntologyTerm the exact match value found
     */
    OntologyTerm findExactTerm( Collection<OntologyTerm> ontologyTerms, String search ) {

        // list of OntologyTerms found
        Collection<OntologyTerm> ontologyKept = new HashSet<>();
        OntologyTerm termFound = null;

        for ( OntologyTerm ot : ontologyTerms ) {
            if ( ot.getLabel() != null ) {
                if ( ot.getLabel().equalsIgnoreCase( search ) ) {
                    ontologyKept.add( ot );
                    termFound = ot;
                }
            }
        }

        // if we have more than 1 result, hardcode the one to choose
        if ( ontologyKept.size() > 1 ) {

            if ( search.equalsIgnoreCase( "juvenile" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {
                    if ( ontologyTerm.getUri().equalsIgnoreCase( "http://purl.org/obo/owl/PATO#PATO_0001190" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "adult" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_681" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "newborn" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_699" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "prenatal" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_7014" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "infant" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_695" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "elderly" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase(
                            "http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Organism.owl#birnlex_691" ) ) {
                        return ontologyTerm;
                    }
                }
            }
        }

        if ( ontologyKept.size() > 1 ) {

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

        this.phenotypeAssociationService = this.getBean( PhenotypeAssociationManagerService.class );

        this.geneService = this.getBean( GeneService.class );
        this.taxonService = this.getBean( TaxonService.class );

        OntologyService ontologyService = this.getBean( OntologyService.class );

        this.diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = ontologyService.getHumanPhenotypeOntologyService();

        while ( !this.diseaseOntologyService.isOntologyLoaded() ) {
            this.wait( 3000 );
            AbstractCLI.log.info( "waiting for the Disease Ontology to load" );
        }

        while ( !this.humanPhenotypeOntologyService.isOntologyLoaded() ) {
            this.wait( 3000 );
            AbstractCLI.log.info( "waiting for the HP Ontology to load" );
        }

        // only need those services for experimental evidences
        if ( experimentalEvidenceServicesNeeded ) {

            this.nifstdOntologyService = ontologyService.getNifstfOntologyService();
            this.obiService = ontologyService.getObiService();
            this.fmaOntologyService = ontologyService.getFmaOntologyService();

            while ( !this.mammalianPhenotypeOntologyService.isOntologyLoaded() ) {
                this.wait( 3000 );
                AbstractCLI.log.info( "waiting for the MP Ontology to load" );
            }

            while ( !this.obiService.isOntologyLoaded() ) {
                this.wait( 3000 );
                AbstractCLI.log.info( "waiting for the OBI Ontology to load" );
            }

            while ( !this.nifstdOntologyService.isOntologyLoaded() ) {
                this.wait( 3000 );
                AbstractCLI.log.info( "waiting for the NIF Ontology to load" );
            }

            while ( !this.fmaOntologyService.isOntologyLoaded() ) {
                this.wait( 3000 );
                AbstractCLI.log.info( "waiting for the FMA Ontology to load" );
            }
        }
    }
}
