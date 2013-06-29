package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.Set;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

public interface PhenotypeAssoOntologyHelper {

    /**
     * Gemma might be ready but the ontology thread not finish loading
     */
    public abstract boolean areOntologiesAllLoaded();

    /**
     * CharacteristicValueObject to Characteristic with no valueUri given
     */
    public abstract VocabCharacteristic characteristicValueObject2Characteristic(
            CharacteristicValueObject characteristicValueObject );

    /**
     * Giving some Ontology terms return all valueUri of Ontology Terms + children
     */
    public abstract Set<String> findAllChildrenAndParent( Collection<OntologyTerm> ontologyTerms );

    /**
     * For a valueUri return the OntologyTerm found
     */
    public abstract OntologyTerm findOntologyTermByUri( String valueUri );

    /**
     * search the disease,hp and mp ontology for a searchQuery and return an ordered set of CharacteristicVO
     */
    public abstract Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery );

    /**
     * search the disease, hp and mp ontology for OntologyTerm
     * 
     * @param searchQuery free text query?
     */
    public abstract Collection<OntologyTerm> findValueUriInOntology( String searchQuery );

    /**
     * Helper method. For a valueUri return the Characteristic (represents a phenotype)
     */
    public abstract Characteristic valueUri2Characteristic( String valueUri );

}