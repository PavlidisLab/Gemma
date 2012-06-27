package ubic.gemma.association.phenotype.fileUpload;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;

public class EvidenceImporterCLI extends EvidenceImporterAbstractCLI {

    // configuration here can replace if needed to use the command line
    private static String[] initArguments() {

        // specify what is the name of the imported file
        String fileName = "CTD.tsv";

        String[] args = new String[10];
        // user
        args[0] = "-u";
        args[1] = "administrator";
        // password
        args[2] = "-p";
        args[3] = "administrator";
        // file
        args[4] = "-f";
        args[5] = "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/FilesToImport/" + fileName;
        // create the evidence in the database
        args[6] = "-c";
        args[7] = "true";
        // environment we dont have all genes on a test database, if we are using the production let it know should find
        // true == production database
        // false = testDatabase, put gene not found to NCBI 1
        args[8] = "-e";
        args[9] = "true";
        return args;
    }

    private String errorMessage = "";
    private String warningMessage = "";

    public static void main( String[] args ) {

        EvidenceImporterCLI evidenceImporterCLI = new EvidenceImporterCLI();

        try {
            // to pass args by the command line dont use the initArguments method
            // Exception ex = p.doWork( args );

            // *** Hardcoded Arguments
            Exception ex = evidenceImporterCLI.doWork( initArguments() );

            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "EvidenceImporterCLI", args );
        if ( err != null ) return err;

        try {

            FileWriter fstream = new FileWriter(
                    "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/FilesToImport/log" );
            BufferedWriter out = new BufferedWriter( fstream );

            System.out.println( "File: " + this.inputFile );
            System.out.println( "Create in Database: " + this.createInDatabase );
            if ( this.prodDatabase ) {
                System.out.println( "Connection read or write to Production Database" );
            } else {
                System.out.println( "Using a test database" );
            }

            this.br = new BufferedReader( new FileReader( this.inputFile ) );

            String typeOfEvidence = findTypeOfEvidence();

            // take the file received and create the real objects from it
            Collection<EvidenceValueObject> evidenceValueObjects = file2Objects( typeOfEvidence );

            if ( !this.errorMessage.isEmpty() ) {
                System.out.println( this.warningMessage );

                out.write( this.warningMessage );
                out.write( this.errorMessage );
                out.close();

                throw new Exception( this.errorMessage );
            }

            if ( !this.warningMessage.isEmpty() ) {
                System.out.println( this.warningMessage );
                out.write( this.warningMessage );
            }

            out.close();

            if ( this.createInDatabase ) {
                int i = 1;

                for ( EvidenceValueObject e : evidenceValueObjects ) {
                    this.phenotypeAssociationService.create( e );
                    System.out.println( "created evidence " + i );
                    i++;
                }
            }

            System.out.println( "Import of evidence is finish" );

        } catch ( Exception e ) {
            return e;
        }

        return null;
    }

    /**
     * Change the file received into an entity that can save in the database
     * 
     * @throws Exception
     */
    private Collection<EvidenceValueObject> file2Objects( String evidenceType ) throws Exception {

        Collection<EvidenceValueObject> evidenceValueObjects = new ArrayList<EvidenceValueObject>();
        String line = "";
        int i = 1;

        // for each line of the file
        while ( ( line = this.br.readLine() ) != null ) {

            String[] tokens = line.split( "\t" );

            System.out.println( "Reading evidence: " + i );
            i++;

            if ( evidenceType.equals( this.LITERATURE_EVIDENCE ) ) {
                evidenceValueObjects.add( convertFileLine2LiteratureValueObjects( tokens ) );
            } else if ( evidenceType.equals( this.EXPERIMENTAL_EVIDENCE ) ) {
                evidenceValueObjects.add( convertFileLine2ExperimentalValueObjects( tokens ) );
            }
        }

        this.br.close();

        return evidenceValueObjects;
    }

    private LiteratureEvidenceValueObject convertFileLine2LiteratureValueObjects( String[] tokens ) {

        boolean isNegativeEvidence = false;

        String geneName = tokens[0].trim();
        String geneID = tokens[1].trim();
        String primaryReferencePubmed = tokens[2].trim();
        String evidenceCode = tokens[3].trim();
        String description = tokens[4].trim();

        if ( tokens[5].trim().equals( "1" ) ) {
            isNegativeEvidence = true;
        }

        String externalDatabaseName = tokens[6].trim();
        String databaseID = tokens[7].trim();

        Set<String> phenotypeFromArray = trimArray( tokens[8].split( ";" ) );

        int geneNcbiId = verifyGeneIdExist( geneID, geneName );

        LiteratureEvidenceValueObject evidence = new LiteratureEvidenceValueObject( geneNcbiId,
                toValuesUri( phenotypeFromArray ), description, evidenceCode, isNegativeEvidence, makeEvidenceSource(
                        databaseID, externalDatabaseName ), primaryReferencePubmed );

        return evidence;

    }

    private ExperimentalEvidenceValueObject convertFileLine2ExperimentalValueObjects( String[] tokens ) {

        boolean isNegativeEvidence = false;

        String primaryReferencePubmed = null;

        String geneName = tokens[0].trim();
        String geneID = tokens[1].trim();
        if ( !tokens[2].trim().equals( "" ) ) {
            primaryReferencePubmed = tokens[2].trim();
        }

        String reviewReferencePubmed = tokens[3].trim();
        String evidenceCode = tokens[4].trim();
        String description = tokens[5].trim();

        if ( tokens[6].trim().equals( "1" ) ) {
            isNegativeEvidence = true;
        }

        Set<String> developmentStage = trimArray( tokens[7].split( ";" ) );
        Set<String> bioSource = trimArray( tokens[8].split( ";" ) );
        Set<String> organismPart = trimArray( tokens[9].split( ";" ) );
        Set<String> experimentDesign = trimArray( tokens[10].split( ";" ) );
        Set<String> treatment = trimArray( tokens[11].split( ";" ) );
        Set<String> experimentOBI = trimArray( tokens[12].split( ";" ) );

        // added to represent externalDatabase
        String externalDatabaseName = tokens[13].trim();
        String databaseID = tokens[14].trim();

        Set<String> phenotypeFromArray = trimArray( tokens[15].split( ";" ) );

        int geneNcbiId = verifyGeneIdExist( geneID, geneName );
        Set<String> relevantPublicationsPubmed = new HashSet<String>();
        if ( !reviewReferencePubmed.equals( "" ) ) {

            relevantPublicationsPubmed.add( reviewReferencePubmed );
        }

        Set<CharacteristicValueObject> experimentTags = new HashSet<CharacteristicValueObject>();

        experimentTags.addAll( experiementTags2Ontology( developmentStage, this.DEVELOPMENTAL_STAGE,
                this.DEVELOPMENTAL_STAGE_ONTOLOGY, this.nifstdOntologyService ) );
        experimentTags.addAll( experiementTags2Ontology( bioSource, this.BIOSOURCE_ONTOLOGY, this.BIOSOURCE_ONTOLOGY,
                null ) );
        experimentTags.addAll( experiementTags2Ontology( organismPart, this.ORGANISM_PART, this.ORGANISM_PART_ONTOLOGY,
                this.fmaOntologyService ) );
        experimentTags.addAll( experiementTags2Ontology( experimentDesign, this.EXPERIMENT_DESIGN,
                this.EXPERIMENT_DESIGN_ONTOLOGY, this.obiService ) );
        experimentTags.addAll( experiementTags2Ontology( treatment, this.TREATMENT, this.TREATMENT_ONTOLOGY, null ) );
        experimentTags.addAll( experiementTags2Ontology( experimentOBI, this.EXPERIMENT, this.EXPERIMENT_ONTOLOGY,
                this.obiService ) );

        ExperimentalEvidenceValueObject evidence = new ExperimentalEvidenceValueObject( new Integer( geneNcbiId ),
                toValuesUri( phenotypeFromArray ), description, evidenceCode, isNegativeEvidence, makeEvidenceSource(
                        databaseID, externalDatabaseName ), primaryReferencePubmed, relevantPublicationsPubmed,
                experimentTags );

        return evidence;
    }

    private SortedSet<CharacteristicValueObject> toValuesUri( Set<String> phenotypes ) {

        SortedSet<CharacteristicValueObject> characteristicPhenotypes = new TreeSet<CharacteristicValueObject>();

        for ( String phenotype : phenotypes ) {

            CharacteristicValueObject c = new CharacteristicValueObject( phenotype2Ontology( phenotype ) );
            characteristicPhenotypes.add( c );
        }

        return characteristicPhenotypes;
    }

    private String phenotype2Ontology( String phenotypeToSearch ) {

        OntologyTerm ot = null;

        // we got an uri, search by uri
        if ( phenotypeToSearch.indexOf( "http://purl." ) != -1 ) {
            System.out.println( "Found an URI: " + phenotypeToSearch );
            ot = this.diseaseOntologyService.getTerm( phenotypeToSearch );

            if ( ot == null ) {
                ot = this.humanPhenotypeOntologyService.getTerm( phenotypeToSearch );
            }
            if ( ot == null ) {
                ot = this.mammalianPhenotypeOntologyService.getTerm( phenotypeToSearch );
            }
        }
        // value found
        else {

            // search disease
            Collection<OntologyTerm> ontologyTerms = this.diseaseOntologyService.findTerm( phenotypeToSearch );

            ot = findExactTerm( ontologyTerms, phenotypeToSearch );

            if ( ot == null ) {

                // search hp
                ontologyTerms = this.humanPhenotypeOntologyService.findTerm( phenotypeToSearch );
                ot = findExactTerm( ontologyTerms, phenotypeToSearch );

            }
            if ( ot == null ) {

                // search mamalian
                ontologyTerms = this.mammalianPhenotypeOntologyService.findTerm( phenotypeToSearch );
                ot = findExactTerm( ontologyTerms, phenotypeToSearch );

            }
            if ( ot == null ) {
                // dont send exception even if this is wrong, treat all the data and show all exceptions at the same
                // time
                System.err.println( "phenotype not found in disease, hp and mp Ontology : " + phenotypeToSearch );
                this.errorMessage += "\nphenotype not found in disease, hp and mp Ontology : " + phenotypeToSearch;
            }
        }

        if ( ot != null ) {

            if ( ot.isTermObsolete() ) {
                System.err.println( "TERM IS OBSOLETE: " + ot.getLabel() );
                this.errorMessage += "\nTERM IS OBSOLETE: " + ot.getLabel();
            }
            return ot.getUri();
        }

        return null;
    }

    /**
     * find the exact term of a search term in a Collection of Ontology terms
     * 
     * @param ontologyTerms Collection of ontologyTerms
     * @param search The value we are interested in finding
     * @return OntologyTerm the exact match value found
     * @throws Exception
     */
    private OntologyTerm findExactTerm( Collection<OntologyTerm> ontologyTerms, String search ) {

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
            } else if ( search.equalsIgnoreCase( "spondyloepiphyseal dysplasia congenita" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase( "http://purl.org/obo/owl/DOID#DOID_14789" ) ) {
                        return ontologyTerm;
                    }
                }
            } else if ( search.equalsIgnoreCase( "polycystic kidney disease" ) ) {

                for ( OntologyTerm ontologyTerm : ontologyKept ) {

                    if ( ontologyTerm.getUri().equalsIgnoreCase( "http://purl.org/obo/owl/DOID#DOID_898" ) ) {
                        return ontologyTerm;
                    }
                }
            }
        }

        if ( ontologyKept.size() > 1 ) {
            this.errorMessage += "\nMore than 1 term found for : " + search + "   " + ontologyKept.size();

            for ( OntologyTerm o : ontologyKept ) {
                this.errorMessage += "\n" + o.getLabel() + " " + o.getUri();
            }

        }

        return termFound;
    }

    /**
     * check that all gene exists in Gemma
     * 
     * @throws Exception
     */
    private int verifyGeneIdExist( String geneId, String geneName ) {

        Gene g = this.geneService.findByNCBIId( new Integer( geneId ) );

        // no gene found but we are on a test database so its fine
        if ( g == null && this.prodDatabase == false ) {
            return 1;
        }
        // we found a gene
        if ( g != null ) {
            if ( !g.getOfficialSymbol().equalsIgnoreCase( geneName ) ) {
                this.warningMessage += "\nDifferent Gene name found: file=" + geneName + "      Gene name in Gemma="
                        + g.getOfficialSymbol();
            }

            if ( !g.getTaxon().getCommonName().equals( "human" ) && !g.getTaxon().getCommonName().equals( "mouse" )
                    && !g.getTaxon().getCommonName().equals( "rat" ) ) {
                this.warningMessage += "\nStrange species Found !!! : " + geneId + "    " + geneName + "    "
                        + g.getTaxon().getCommonName();
            }

            return g.getNcbiGeneId();
        }

        // this should never happen if not there is a problem
        this.errorMessage += "\nGene not found in Gemma: " + geneId + "   " + geneName;
        return -1;
    }

    private Set<CharacteristicValueObject> experiementTags2Ontology( Set<String> values, String category,
            String categoryUri, AbstractOntologyService ontologyUsed ) {

        Set<CharacteristicValueObject> experimentTags = new HashSet<CharacteristicValueObject>();

        for ( String term : values ) {

            String valueUri = "";

            if ( ontologyUsed != null ) {
                Collection<OntologyTerm> ontologyTerms = ontologyUsed.findTerm( term );
                OntologyTerm ot = findExactTerm( ontologyTerms, term );

                if ( ot != null ) {
                    valueUri = ot.getUri();
                }
            }

            CharacteristicValueObject c = new CharacteristicValueObject( term, category, valueUri, categoryUri );
            experimentTags.add( c );
        }
        return experimentTags;
    }

}
