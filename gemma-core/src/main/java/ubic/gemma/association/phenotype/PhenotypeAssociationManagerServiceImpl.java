package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidencesValueObject;

/** High Level Service used to add Candidate Gene Management System capabilities */
@Component
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService {

    @Autowired
    private PhenotypeAssociationService associationService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private PhenotypeAssoManagerServiceHelper phenotypeAssoManagerServiceHelper;

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return The Gene updated with the new evidence and phenotypes
     */
    public GeneEvidencesValueObject linkGeneToEvidence( String geneNCBI, EvidenceValueObject evidence ) {

        // find the gene we wish to add the evidence and phenotype
        Gene gene = this.geneService.findByNCBIId( geneNCBI );

        // convert all evidence for this gene to valueObject
        Collection<EvidenceValueObject> evidenceValueObjects = EvidenceValueObject.convert2ValueObjects( gene
                .getPhenotypeAssociations() );

        // verify that the evidence is not a duplicate
        for ( EvidenceValueObject evidenceFound : evidenceValueObjects ) {
            if ( evidenceFound.equals( evidence ) ) {
                // the evidence already exists, no need to create it again
                return new GeneEvidencesValueObject( gene );
            }
        }

        // convert the valueObject received to the corresponding entity
        PhenotypeAssociation pheAsso = this.phenotypeAssoManagerServiceHelper.valueObject2Entity( evidence );

        // add the entity to the gene
        gene.getPhenotypeAssociations().add( pheAsso );

        // save result
        this.geneService.update( gene );

        // return the saved gene result
        return new GeneEvidencesValueObject( gene );
    }

    /**
     * Return all evidences for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    public Collection<EvidenceValueObject> findEvidences( String geneNCBI ) {

        Gene gene = geneService.findByNCBIId( geneNCBI );

        if ( gene == null ) {
            return null;
        }
        return EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    /**
     * Return all evidences for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    public Collection<EvidenceValueObject> findEvidences( Long geneId ) {

        Gene gene = geneService.load( geneId );

        if ( gene == null ) {
            return null;
        }
        return EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    /**
     * Given an array of phenotypes returns the genes that have all those phenotypes
     * 
     * @param 1 to many phenotypes
     * @return A collection of the genes found
     */
    public Collection<GeneEvidencesValueObject> findCandidateGenes( String... phenotypesValues ) {

        if ( phenotypesValues.length == 0 ) {
            return null;
        }

        Collection<GeneEvidencesValueObject> genesVO = new HashSet<GeneEvidencesValueObject>();

        // find all the Genes with the first phenotype
        Collection<Gene> genes = this.associationService.findCandidateGenes( phenotypesValues[0] );
        Collection<GeneEvidencesValueObject> genesWithFirstPhenotype = GeneEvidencesValueObject
                .convert2GeneEvidencesValueObjects( genes );

        if ( phenotypesValues.length == 1 ) {
            genesVO = genesWithFirstPhenotype;
        }
        // there is more than 1 phenotype, lets filter the content
        else {
            for ( GeneEvidencesValueObject gene : genesWithFirstPhenotype ) {

                // contains all phenotypes for one gene
                HashSet<String> allPhenotypes = new HashSet<String>();

                for ( EvidenceValueObject evidence : gene.getEvidences() ) {
                    for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {
                        allPhenotypes.add( phenotype.getValue() );
                    }
                }

                boolean containAllPhenotypes = true;

                // verify if all phenotypes we are looking for are present in the gene
                for ( int i = 1; i < phenotypesValues.length; i++ ) {

                    if ( !allPhenotypes.contains( phenotypesValues[i].toLowerCase() ) ) {
                        containAllPhenotypes = false;
                    }
                }

                // if the gene had all phenotypes
                if ( containAllPhenotypes ) {
                    genesVO.add( gene );
                }
            }
        }

        // for each evidence on the gene, lets put a flag if that evidence got the chosen phenotype
        for ( GeneEvidencesValueObject gene : genesVO ) {

            for ( EvidenceValueObject evidence : gene.getEvidences() ) {

                boolean evidenceHasPhenotype = false;

                for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {

                    for ( int i = 0; i < phenotypesValues.length; i++ ) {

                        if ( phenotype.getValue().equalsIgnoreCase( phenotypesValues[i] ) ) {
                            evidenceHasPhenotype = true;
                        }
                    }
                }

                if ( evidenceHasPhenotype ) {
                    // score between 0 and 1
                    evidence.setRelevance( 1.0 );
                }
            }
        }

        return genesVO;
    }

    /**
     * Get all phenotypes linked to genes and count how many genes are link to each phenotype
     * 
     * @return A collection of the phenotypes with the gene occurence
     */
    public Collection<CharacteristicValueObject> findAllPhenotypes() {
        // find of all the phenotypes present in Gemma
        Collection<CharacteristicValueObject> phenotypes = this.associationService.findAllPhenotypes();

        // for each of them, find the occurence
        for ( CharacteristicValueObject phenotype : phenotypes ) {
            phenotype.setOccurence( this.associationService.findCandidateGenes( phenotype.getValue() ).size() );
            // TODO for now lets use lowerCase until we have a tree
            phenotype.setValue( phenotype.getValue().toLowerCase() );
        }

        return phenotypes;
    }

    /**
     * Removes an evidence
     * 
     * @param id The Evidence database id
     */
    public void removeEvidence( Long id ) {
        this.associationService.removePhenotypeAssociation( id );
    }

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     */
    public void modifyEvidence( EvidenceValueObject evidenceValueObject ) {

        Long id = evidenceValueObject.getDatabaseId();

        if ( evidenceValueObject.getDatabaseId() != null ) {

            // load the phenotypeAssociation
            PhenotypeAssociation phenotypeAssociation = this.associationService.loadEvidence( id );

            if ( phenotypeAssociation != null ) {

                // change field in the phenotypeAssociation using the valueObject
                this.phenotypeAssoManagerServiceHelper.populatePhenotypeAssociation( phenotypeAssociation,
                        evidenceValueObject );

                // update changes to database
                this.associationService.updateEvidence( phenotypeAssociation );
            }
        }
    }

}
