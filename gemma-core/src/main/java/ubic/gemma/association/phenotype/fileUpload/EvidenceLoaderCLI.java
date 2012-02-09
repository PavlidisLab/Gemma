package ubic.gemma.association.phenotype.fileUpload;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public abstract class EvidenceLoaderCLI extends AbstractSpringAwareCLI {

    // flag to decided if the data is ready to be imported
    protected boolean createInDatabase = false;
    protected boolean testEnvironment = false;
    protected String testMessage = "";

    // input file path
    protected String inputFile = "";

    protected GeneService geneService = null;

    protected DiseaseOntologyService diseaseOntologyService = null;
    protected MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    protected HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    protected OntologyService ontologyService = null;
    protected PhenotypeAssociationManagerService phenotypeAssociationService = null;

    @Override
    protected void buildOptions() {
        @SuppressWarnings("static-access")
        Option fileOption = OptionBuilder.withDescription( "The file" ).hasArg().withArgName( "file path" )
                .isRequired().create( "f" );
        addOption( fileOption );

        @SuppressWarnings("static-access")
        Option optionCreateInDatabase = OptionBuilder.withDescription( "Create in database" ).create( "create" );
        addOption( optionCreateInDatabase );

        @SuppressWarnings("static-access")
        Option optionTestDatabase = OptionBuilder.withDescription( "Test database" ).create( "test" );
        addOption( optionTestDatabase );
    }

    protected static String[] initArguments() {

        // example of parameters
        String[] args = new String[8];
        args[0] = "-u";
        args[1] = "administrator";
        args[2] = "-p";
        args[3] = "administrator";
        args[4] = "-f";

        args[5] = "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/experimentalEvidence/CathyExperimental.tsv";

        args[6] = "-create";
        args[7] = "-test";

        return args;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        this.inputFile = getOptionValue( 'f' );

        if ( hasOption( "create" ) ) {
            this.createInDatabase = true;
        }
        if ( hasOption( "test" ) ) {
            this.testEnvironment = true;
        }
    }

    /**
     * check that all gene exists in Gemma
     * 
     * @throws Exception
     */
    protected void verifyGeneIdExist( Collection<? extends EvidenceLineInfo> evidenceLineInfos ) throws Exception {

        for ( EvidenceLineInfo lineInfo : evidenceLineInfos ) {

            Gene gene = this.geneService.findByNCBIId( new Integer( lineInfo.getGeneID() ) );

            if ( gene == null ) {

                System.err.println( "Gene not found in Gemma: " + lineInfo.getGeneID() + " Description: "
                        + lineInfo.getComment() );

                if ( this.testEnvironment ) {
                    lineInfo.setGeneID( "1" );
                } else {
                    throw new Exception( "Gene NCBI not found in Gemma" );
                }

            } else if ( !gene.getName().equalsIgnoreCase( lineInfo.getGeneName() ) ) {
                System.err.println( "************Different Gene name found************" );
                System.err.println( "Gene name in File: " + lineInfo.getGeneName() );
                System.err.println( "Gene name in Gemma: " + gene.getName() );
                System.err.println( "*************************************************" );
            }
        }
    }

    /** load services and verify that Ontology are loaded */
    protected synchronized void loadServices() throws Exception {

        this.phenotypeAssociationService = ( PhenotypeAssociationManagerService ) this
                .getBean( "phenotypeAssociationManagerService" );

        this.geneService = ( GeneService ) this.getBean( "geneService" );

        this.ontologyService = ( OntologyService ) this.getBean( "ontologyService" );

        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = this.ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();

        while ( this.diseaseOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for Disease Ontology to load" );
        }

        while ( this.mammalianPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for MP Ontology to load" );
        }

        while ( this.humanPhenotypeOntologyService.isOntologyLoaded() == false ) {
            wait( 1000 );
            System.out.println( "waiting for HP Ontology to load" );
        }
    }

    /** search phenotype term the diseaseOntology then hp, then mp */
    protected String phenotype2Ontology( EvidenceLineInfo lineInfo, int index ) throws Exception {

        String search = lineInfo.getPhenotype()[index];

        OntologyTerm ot = null;

        // we got an uri
        if ( search.indexOf( "http://purl.org" ) != -1 ) {
            System.out.println( "Found an URI" );
            ot = this.diseaseOntologyService.getTerm( search );

            if ( ot == null ) {
                ot = this.humanPhenotypeOntologyService.getTerm( search );
            }
            if ( ot == null ) {
                ot = this.mammalianPhenotypeOntologyService.getTerm( search );
            }
        }
        // value found
        else {

            // search disease
            Collection<OntologyTerm> ontologyTerms = this.diseaseOntologyService.findTerm( search );

            ot = findExactTerm( ontologyTerms, search );

            if ( ot == null ) {

                // search hp
                ontologyTerms = this.humanPhenotypeOntologyService.findTerm( search );
                ot = findExactTerm( ontologyTerms, search );

            }
            if ( ot == null ) {

                // search mamalian
                ontologyTerms = this.mammalianPhenotypeOntologyService.findTerm( search );
                ot = findExactTerm( ontologyTerms, search );

            }
            if ( ot == null ) {

                if ( this.testEnvironment ) {
                    this.testMessage = this.testMessage + "phenotype not found in disease, hp and mp Ontology : "
                            + search + "\n";
                    System.err.println( "phenotype not found in disease, hp and mp Ontology : " + search );
                } else {
                    // all phenotypes must be find
                    throw new Exception( "phenotype not found in disease, hp and mp Ontology : " + search );
                }
            }
        }

        if ( ot != null ) {

            if ( ot.isTermObsolete() ) {
                if ( this.testEnvironment ) {
                    this.testMessage = this.testMessage + "TERM IS OBSOLETE: " + ot.getLabel() + "\n";
                    System.err.println( "TERM IS OBSOLETE: " + ot.getLabel() );
                } else {
                    throw new Exception( "TERM IS OBSOLETE: " + ot.getLabel() );
                }
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
    protected OntologyTerm findExactTerm( Collection<OntologyTerm> ontologyTerms, String search ) throws Exception {

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
            throw new Exception( "More than 1 term found for : " + search + "   " + ontologyKept.size() );
        }

        return termFound;
    }

    /** change each line of the file by Ontology terms */
    protected void convertOntologiesTerms( Collection<? extends EvidenceLineInfo> evidenceLineInfos ) throws Exception {

        int line = 1;

        for ( EvidenceLineInfo lineInfo : evidenceLineInfos ) {

            line++;

            System.out.println( "Treating Ontology Phenotype terms for line: " + line );

            // The phenotype column
            for ( int i = 0; i < lineInfo.getPhenotype().length; i++ ) {
                if ( !lineInfo.getPhenotype()[i].equalsIgnoreCase( "" ) ) {

                    String valueUri = phenotype2Ontology( lineInfo, i );

                    lineInfo.addPhenotype( new CharacteristicValueObject( valueUri ) );
                }
            }
        }
    }

}
