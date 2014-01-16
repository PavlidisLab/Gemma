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
package ubic.gemma.loader.association.phenotype;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.providers.NIFSTDOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSourceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractCLIContextCLI;
import ubic.gemma.util.Settings;

public abstract class EvidenceImporterAbstractCLI extends AbstractCLIContextCLI {

    protected static final String WRITE_FOLDER = Settings.getString( "gemma.appdata.home" ) + File.separator
            + "EvidenceImporterNeurocarta";

    protected PhenotypeAssociationManagerService phenotypeAssociationService = null;
    protected GeneService geneService = null;

    protected DiseaseOntologyService diseaseOntologyService = null;
    protected MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    protected HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    protected OntologyService ontologyService = null;
    protected NIFSTDOntologyService nifstdOntologyService = null;
    protected ObiService obiService = null;
    protected FMAOntologyService fmaOntologyService = null;
    protected TaxonService taxonService = null;

    protected Set<String> errorMessage = new TreeSet<String>();
    protected String warningMessage = "";
    protected BufferedWriter logFileWriter = null;
    protected HashMap<String, Integer> mapColumns = new HashMap<String, Integer>();

    // input file path
    protected String inputFile = "";
    protected BufferedReader br = null;
    protected boolean createInDatabase = false;

    protected final String EXPERIMENTAL_EVIDENCE = "EXPERIMENTAL";
    protected final String LITERATURE_EVIDENCE = "LITERATURE";
    protected final String GENERIC_EVIDENCE = "GENERIC";
    protected final String DEVELOPMENTAL_STAGE = "DevelopmentalStage";
    protected final String BIOSOURCE = "BioSource";
    protected final String ORGANISM_PART = "OrganismPart";
    protected final String EXPERIMENT_DESIGN = "ExperimentDesign";
    protected final String TREATMENT = "Treatment";
    protected final String EXPERIMENT = "Experiment";

    protected final String DEVELOPMENTAL_STAGE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#DevelopmentalStage";
    protected final String BIOSOURCE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#BioSource";
    protected final String ORGANISM_PART_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#OrganismPart";
    protected final String EXPERIMENT_DESIGN_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#ExperimentDesign";
    protected final String TREATMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Treatment";
    protected final String EXPERIMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Experiment";

    @Override
    protected void buildOptions() {
        @SuppressWarnings("static-access")
        Option fileOption = OptionBuilder.withDescription( "The file" ).hasArg().withArgName( "file path" )
                .isRequired().create( "f" );
        addOption( fileOption );
        @SuppressWarnings("static-access")
        Option createOption = OptionBuilder.withDescription( "Create in Database (false or true)" ).hasArg()
                .withArgName( "create in Database" ).isRequired().create( "c" );
        addOption( createOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        this.inputFile = getOptionValue( 'f' );
        this.createInDatabase = new Boolean( getOptionValue( 'c' ) );
    }

    protected EvidenceSourceValueObject makeEvidenceSource( String databaseID, String externalDatabaseName ) {

        EvidenceSourceValueObject evidenceSourceValueObject = null;
        if ( databaseID != null && externalDatabaseName != null && !externalDatabaseName.isEmpty() ) {
            ExternalDatabaseValueObject externalDatabase = new ExternalDatabaseValueObject();
            externalDatabase.setName( externalDatabaseName );
            evidenceSourceValueObject = new EvidenceSourceValueObject( databaseID, externalDatabase );
        }

        return evidenceSourceValueObject;
    }

    /**
     * Loads all services that will be needed
     * 
     * @param experimentalEvidenceServicesNeeded if the type is an experimental evidence (we need then to load more)
     */
    protected synchronized void loadServices( boolean experimentalEvidenceServicesNeeded ) throws Exception {

        this.phenotypeAssociationService = this.getBean( PhenotypeAssociationManagerService.class );

        this.geneService = this.getBean( GeneService.class );
        this.taxonService = this.getBean( TaxonService.class );

        this.ontologyService = this.getBean( OntologyService.class );

        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = this.ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();

        while ( this.diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            log.info( "waiting for the Disease Ontology to load" );
        }

        while ( this.humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 3000 );
            log.info( "waiting for the HP Ontology to load" );
        }

        // only need those services for experimental evidences
        if ( experimentalEvidenceServicesNeeded ) {

            this.nifstdOntologyService = this.ontologyService.getNifstfOntologyService();
            this.obiService = this.ontologyService.getObiService();
            this.fmaOntologyService = this.ontologyService.getFmaOntologyService();

            while ( this.mammalianPhenotypeOntologyService.isOntologyLoaded() == false ) {
                wait( 3000 );
                log.info( "waiting for the MP Ontology to load" );
            }

            while ( this.obiService.isOntologyLoaded() == false ) {
                wait( 3000 );
                log.info( "waiting for the OBI Ontology to load" );
            }

            while ( this.nifstdOntologyService.isOntologyLoaded() == false ) {
                wait( 3000 );
                log.info( "waiting for the NIF Ontology to load" );
            }

            while ( this.fmaOntologyService.isOntologyLoaded() == false ) {
                wait( 3000 );
                log.info( "waiting for the FMA Ontology to load" );
            }
        }
    }

    /**
     * Trim an array of String
     */
    protected Set<String> trimArray( String[] array ) {

        Set<String> mySet = new HashSet<String>();

        String[] trimmedArray = new String[array.length];

        for ( int i = 0; i < trimmedArray.length; i++ ) {
            String value = array[i].trim();

            if ( !value.equals( "" ) ) {
                mySet.add( value );
            }
        }

        return mySet;
    }

    /**
     * Look at all Headers and identify them to determine the type of evidence
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
                && this.mapColumns.containsKey( "Phenotypes" ) && this.mapColumns.containsKey( "OriginalPhenotype" ) && this.mapColumns
                    .containsKey( "PhenotypeMapping" ) ) ) {
            throw new Exception( "Headers not set correctly" );
        }

        // score set ???
        if ( this.mapColumns.containsKey( "Score" ) && this.mapColumns.containsKey( "ScoreType" )
                && this.mapColumns.containsKey( "Strength" ) ) {
            log.info( "Found a score on the evidence" );
        } else {
            log.info( "No score on the evidence" );
        }

        // using an external database ???
        if ( this.mapColumns.containsKey( "ExternalDatabase" ) && this.mapColumns.containsKey( "DatabaseLink" ) ) {
            log.info( "External database link to evidence" );
        } else {
            log.info( "No external database" );
        }

        // rules to be an experimentalEvidence
        if ( this.mapColumns.containsKey( "Experiment" ) && this.mapColumns.containsKey( "Treatment" )
                && this.mapColumns.containsKey( "ExperimentDesign" ) && this.mapColumns.containsKey( "OrganismPart" )
                && this.mapColumns.containsKey( "BioSource" ) && this.mapColumns.containsKey( "DevelopmentalStage" )
                && this.mapColumns.containsKey( "OtherPubMed" ) && this.mapColumns.containsKey( "PrimaryPubMeds" ) ) {

            log.info( "The type of Evidence found is: " + this.EXPERIMENTAL_EVIDENCE );
            loadServices( true );
            return this.EXPERIMENTAL_EVIDENCE;
        }

        loadServices( false );

        return this.LITERATURE_EVIDENCE;

    }

    /**
     * find the exact term of a search term in a Collection of Ontology terms
     * 
     * @param ontologyTerms Collection of ontologyTerms
     * @param search The value we are interested in finding
     * @return OntologyTerm the exact match value found
     * @throws IOException
     */
    protected OntologyTerm findExactTerm( Collection<OntologyTerm> ontologyTerms, String search ) throws IOException {

        // list of OntologyTerms found
        Collection<OntologyTerm> ontologyKept = new HashSet<OntologyTerm>();
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
                    if ( o.getLabel().equalsIgnoreCase( "apraxia" )
                            && o.getUri().equalsIgnoreCase( "http://purl.obolibrary.org/obo/DOID_4019" ) ) {
                        return o;
                    }
                }
            }

            writeError( "More than 1 term found for : " + search + "   " + ontologyKept.size() );

            for ( OntologyTerm o : ontologyKept ) {
                writeError( o.getLabel() + " " + o.getUri() );
            }
        }

        return termFound;
    }

    protected void writeWarning( String message ) throws IOException {

        log.info( message );
        this.warningMessage += "\n" + message;
        this.logFileWriter.write( "\n" + message + " (warning)" );
        this.logFileWriter.flush();
    }

    protected void writeError( String message ) {

        log.error( message );
        this.errorMessage.add( message );
    }

    protected void writeAllExceptions() throws IOException {

        for ( String message : this.errorMessage ) {
            this.logFileWriter.write( "\n" + message );
            this.logFileWriter.flush();
        }
    }

    /**
     * Make sure the evidence code found exist in Gemma
     */
    protected void checkEvidenceCodeExits( String evidenceCode ) {

        if ( !( evidenceCode.equalsIgnoreCase( "IC" ) || evidenceCode.equalsIgnoreCase( "IDA" )
                || evidenceCode.equalsIgnoreCase( "IEA" ) || evidenceCode.equalsIgnoreCase( "IEP" )
                || evidenceCode.equalsIgnoreCase( "IGI" ) || evidenceCode.equalsIgnoreCase( "IMP" )
                || evidenceCode.equalsIgnoreCase( "IPI" ) || evidenceCode.equalsIgnoreCase( "ISS" )
                || evidenceCode.equalsIgnoreCase( "NAS" ) || evidenceCode.equalsIgnoreCase( "ND" )
                || evidenceCode.equalsIgnoreCase( "RCA" ) || evidenceCode.equalsIgnoreCase( "TAS" )
                || evidenceCode.equalsIgnoreCase( "NR" ) || evidenceCode.equalsIgnoreCase( "EXP" )
                || evidenceCode.equalsIgnoreCase( "ISA" ) || evidenceCode.equalsIgnoreCase( "ISM" )
                || evidenceCode.equalsIgnoreCase( "IGC" ) || evidenceCode.equalsIgnoreCase( "ISO" )
                || evidenceCode.equalsIgnoreCase( "IIA" ) || evidenceCode.equalsIgnoreCase( "IBA" )
                || evidenceCode.equalsIgnoreCase( "IBD" ) || evidenceCode.equalsIgnoreCase( "IKR" )
                || evidenceCode.equalsIgnoreCase( "IRD" ) || evidenceCode.equalsIgnoreCase( "IMR" )
                || evidenceCode.equalsIgnoreCase( "IED" ) || evidenceCode.equalsIgnoreCase( "IAGP" )
                || evidenceCode.equalsIgnoreCase( "IPM" ) || evidenceCode.equalsIgnoreCase( "QTM" ) ) ) {
            writeError( "evidenceCode not in gemma: " + evidenceCode );
        }
    }

    protected void createWriteFolder() throws Exception {

        File folder = new File( WRITE_FOLDER );
        folder.mkdir();

        if ( !folder.exists() ) {
            throw new Exception( "having trouble to create a folder" );
        }
    }
}
