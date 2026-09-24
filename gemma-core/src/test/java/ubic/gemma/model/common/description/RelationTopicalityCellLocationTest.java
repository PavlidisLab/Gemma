package ubic.gemma.model.common.description;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🛑 The third gate a new predicate has to pass, and the one that is silent.
 *
 * <p>{@link RelationTopicality#of} is an allow-list whose default is
 * {@link RelationTopicality#EXPERIMENT_LEVEL}, and {@code GET /annotations/relations} drops those
 * unless {@code includeExperimentLevel=true}. So CL's 1,206 rows landed on prod on 2026-09-02,
 * read back correctly through the DAO, and answered an ordinary API call with an EMPTY LIST --
 * indistinguishable from the rebuild never having run.</p>
 *
 * <p>Adding a predicate takes three registrations, each failing closed on its own:
 * {@code Relation.terms.txt} (the vocabulary), {@link RelationInferenceDirection} (what it
 * licenses), and this (whether a reader is shown it).</p>
 *
 * @author gembro
 */
class RelationTopicalityCellLocationTest {

    private static final String PART_OF = "http://purl.obolibrary.org/obo/BFO_0000050";
    private static final String HAS_SOMA_LOCATION = "http://purl.obolibrary.org/obo/RO_0002100";
    private static final String CELL_TYPE = "http://www.ebi.ac.uk/efo/EFO_0000324";

    /**
     * A Mueller cell is in the retina in every experiment there is; that is a property of the cell
     * type and not a parameter of one study.
     */
    @Test
    void whereACellTypeSitsAnatomicallyIsATermLevelFact() {
        assertThat( RelationTopicality.of( PART_OF, CELL_TYPE ) )
                .isEqualTo( RelationTopicality.TERM_LEVEL );
        assertThat( RelationTopicality.of( HAS_SOMA_LOCATION, CELL_TYPE ) )
                .isEqualTo( RelationTopicality.TERM_LEVEL );
    }

    /**
     * The default has to stay closed: an unregistered predicate is experiment-level, which is what
     * keeps dose and duration out of a term's card.
     */
    @Test
    void anUnregisteredPredicateIsStillExperimentLevel() {
        assertThat( RelationTopicality.of( "http://purl.obolibrary.org/obo/RO_9999999", CELL_TYPE ) ).isEqualTo( RelationTopicality.EXPERIMENT_LEVEL );
    }
}
