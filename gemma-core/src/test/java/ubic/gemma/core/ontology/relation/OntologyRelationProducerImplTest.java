package ubic.gemma.core.ontology.relation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.ontology.model.OntologyClassRestriction;
import ubic.gemma.core.ontology.model.OntologyProperty;
import ubic.gemma.core.ontology.model.OntologyRestriction;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.model.OntologyXref;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Turning an ontology's OWL restrictions into stored relations.
 *
 * <p>The reading machinery has existed all along and was called from nowhere, so what is worth testing
 * is not "can a restriction be read" but the four ways the translation can be quietly wrong: a DOID
 * reaching a stored row, a nested axiom being mistaken for a flat one, an ambiguous mapping being
 * resolved to one arbitrary disease, and a CHEBI role being filed as a disease.</p>
 */
class OntologyRelationProducerImplTest {

    private static final String OBO = "http://purl.obolibrary.org/obo/";

    private static final String MCF7 = OBO + "CLO_0007606";
    private static final String DERIVES_FROM_PATIENT_HAVING_DISEASE = OBO + "CLO_0000015";
    private static final String IS_DISEASE_MODEL_FOR = OBO + "CLO_0000179";
    private static final String DERIVES_FROM_ORGANISM = OBO + "CLO_0037207";
    private static final String DERIVES_FROM_ANATOMIC_PART = OBO + "CLO_0037208";
    /** the nested-intersection property, deliberately not readable here */
    private static final String DERIVES_FROM = OBO + "RO_0001000";

    private static final String DOID_BREAST_ADENOCARCINOMA = OBO + "DOID_3458";
    private static final String DOID_ADENOCARCINOMA = OBO + "DOID_299";
    private static final String DOID_UNMAPPED = OBO + "DOID_9999999";
    private static final String MONDO_BREAST_ADENOCARCINOMA = OBO + "MONDO_0004988";
    private static final String MONDO_ADENOCARCINOMA = OBO + "MONDO_0004970";
    /** the identifier that cost the most rows in the 2026-08-18 run: 172 restrictions on its own */
    private static final String DOID_MACULAR_DEGENERATION = OBO + "DOID_0050427";
    private static final String MONDO_MACULAR_DEGENERATION = OBO + "MONDO_0005298";

    private static final String HUMAN = OBO + "NCBITaxon_9606";
    private static final String BREAST = OBO + "UBERON_0000310";

    private static final String TGEMO = "http://gemma.msl.ubc.ca/ont/TGEMO_";
    private static final String APP_PS1 = TGEMO + "00174";
    private static final String HAS_ROLE_IN_MODELLING = OBO + "RO_0003301";
    private static final String MONDO_ALZHEIMER = OBO + "MONDO_0004975";
    private static final String BCR_ABL = TGEMO + "00151";
    private static final String HAS_DISEASE = OBO + "RO_0016002";
    private static final String MONDO_AML = OBO + "MONDO_0018874";

    private static final String IMATINIB = OBO + "CHEBI_45783";
    private static final String HAS_ROLE = OBO + "RO_0000087";
    private static final String ANTINEOPLASTIC_AGENT = OBO + "CHEBI_35610";

    private AnnotationRelationDao dao;
    private TransactionTemplate transactionTemplate;
    private TaxonService taxonService;

    private final Map<String, OntologyTerm> cloTerms = new LinkedHashMap<>();
    private final Map<String, OntologyTerm> chebiTerms = new LinkedHashMap<>();
    private final Map<String, OntologyTerm> tgemoTerms = new LinkedHashMap<>();
    private final Map<String, OntologyTerm> mondoTerms = new LinkedHashMap<>();
    private final List<OntologyXref> xrefs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        dao = mock( AnnotationRelationDao.class );
        taxonService = mock( TaxonService.class );
        transactionTemplate = mock( TransactionTemplate.class );
        when( transactionTemplate.execute( any() ) ).thenAnswer( inv ->
                ( ( TransactionCallback<?> ) inv.getArgument( 0 ) ).doInTransaction( null ) );

        mondoTerms.put( MONDO_ALZHEIMER, term( MONDO_ALZHEIMER, "Alzheimer disease" ) );
        mondoTerms.put( MONDO_AML, term( MONDO_AML, "acute myeloid leukemia" ) );
        mondoTerms.put( MONDO_BREAST_ADENOCARCINOMA, term( MONDO_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) );
        mondoTerms.put( MONDO_ADENOCARCINOMA, term( MONDO_ADENOCARCINOMA, "adenocarcinoma" ) );
        xrefs.add( new OntologyXref( MONDO_BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ) );
        xrefs.add( new OntologyXref( MONDO_ADENOCARCINOMA, "DOID:299", OntologyXref.Strength.EXACT ) );
    }

    // ---------------------------------------------------------------------------------------------
    // fixtures
    // ---------------------------------------------------------------------------------------------

    private static OntologyTerm term( String uri, String label, OntologyRestriction... restrictions ) {
        OntologyTerm t = mock( OntologyTerm.class );
        when( t.getUri() ).thenReturn( uri );
        when( t.getLabel() ).thenReturn( label );
        when( t.getRestrictions() ).thenReturn( Arrays.asList( restrictions ) );
        // The producer reads DIRECT restrictions -- the closure walk that getRestrictions() performs is
        // unstable under an inference-mode model, reporting CLO_0000179 at 441, then 1000, then 1899
        // across three runs of one artifact. A mock returns empty for an unstubbed default method, so
        // stubbing only the old one made every row silently disappear.
        when( t.getDirectRestrictions() ).thenReturn( Arrays.asList( restrictions ) );
        return t;
    }

    private static OntologyClassRestriction restriction( String propertyUri, String propertyLabel,
            OntologyTerm target ) {
        OntologyProperty property = mock( OntologyProperty.class );
        when( property.getUri() ).thenReturn( propertyUri );
        when( property.getLabel() ).thenReturn( propertyLabel );
        OntologyClassRestriction r = mock( OntologyClassRestriction.class );
        when( r.getRestrictionOn() ).thenReturn( property );
        when( r.getRestrictedTo() ).thenReturn( target );
        return r;
    }

    /** the shape of CLO's nested {@code derives from} axiom: a filler with no identity of its own */
    private static OntologyClassRestriction anonymousRestriction( String propertyUri, String propertyLabel ) {
        OntologyTerm anonymous = mock( OntologyTerm.class );
        when( anonymous.getUri() ).thenReturn( null );
        return restriction( propertyUri, propertyLabel, anonymous );
    }

    private ubic.gemma.core.ontology.providers.OntologyService ontology( String identifier, String version,
            Map<String, OntologyTerm> terms, Collection<OntologyXref> crossReferences ) {
        ubic.gemma.core.ontology.providers.OntologyService o =
                mock( ubic.gemma.core.ontology.providers.OntologyService.class );
        when( o.getIdentifier() ).thenReturn( identifier );
        when( o.isOntologyLoaded() ).thenReturn( true );
        when( o.getVersion() ).thenReturn( version );
        when( o.getAllURIs() ).thenReturn( new LinkedHashSet<>( terms.keySet() ) );
        when( o.getTerm( anyString() ) ).thenAnswer( inv -> terms.get( inv.<String>getArgument( 0 ) ) );
        when( o.getCrossReferences() ).thenReturn( crossReferences );
        // The producer inverts the SOURCE's cross-references, not the loaded model's: a corpus-seeded
        // slim covers the diseases we already annotate and so cannot translate identifiers for the
        // ones we do not. A mock returns empty for an unstubbed default method, so stubbing only the
        // model variant would leave every foreign target untranslatable.
        when( o.getCrossReferencesFromSource() ).thenReturn( crossReferences );
        return o;
    }

    private OntologyRelationProducerImpl producer() {
        List<ubic.gemma.core.ontology.providers.OntologyService> ontologies = Arrays.asList(
                ontology( "cellLineOntology", "2026-06-19", cloTerms, Collections.emptyList() ),
                ontology( "chebiOntology", "254", chebiTerms, Collections.emptyList() ),
                ontology( "gemmaOntology", "2026-08-30", tgemoTerms, Collections.emptyList() ),
                ontology( "mondoOntology", "2026-08-04", mondoTerms, xrefs ) );
        return new OntologyRelationProducerImpl( ontologies, dao, transactionTemplate, taxonService, null );
    }

    private List<AnnotationRelation> produce( String... sources ) {
        int written = producer().produce( Arrays.asList( sources ) );
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<AnnotationRelation>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( dao ).create( captor.capture() );
        List<AnnotationRelation> rows = new ArrayList<>( captor.getValue() );
        assertThat( written ).isEqualTo( rows.size() );
        return rows;
    }

    // ---------------------------------------------------------------------------------------------
    // tests
    // ---------------------------------------------------------------------------------------------

    /**
     * The end-to-end shape of one CLO row, including the leg that makes it usable at all: the target
     * arrives as a DOID and is stored as the MONDO term Gemma annotates in.
     */
    @Test
    void aCloDiseaseRestrictionBecomesAMondoRelation() {
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ) ) );

        List<AnnotationRelation> rows = produce( "CLO" );

        assertThat( rows ).hasSize( 1 );
        AnnotationRelation r = rows.get( 0 );
        assertThat( r.getSubjectValue() ).isEqualTo( "MCF7 cell" );
        assertThat( r.getSubjectValueUri() ).isEqualTo( MCF7 );
        assertThat( r.getSubjectCategory() ).isEqualTo( "cell line" );
        assertThat( r.getSubjectCategoryUri() ).isEqualTo( OBO + "CLO_0000031" );
        assertThat( r.getPredicate() ).isEqualTo( "derives from patient having disease" );
        assertThat( r.getPredicateUri() ).isEqualTo( DERIVES_FROM_PATIENT_HAVING_DISEASE );
        assertThat( r.getObjectValue() ).isEqualTo( "breast adenocarcinoma" );
        assertThat( r.getObjectValueUri() ).isEqualTo( MONDO_BREAST_ADENOCARCINOMA );
        assertThat( r.getObjectCategoryUri() ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000408" );
        assertThat( r.getBasis() ).isEqualTo( AnnotationRelationBasis.ONTOLOGY );
        assertThat( r.getSource() ).isEqualTo( "CLO" );
        assertThat( r.getSourceVersion() ).isEqualTo( "2026-06-19" );
        assertThat( r.getEvidenceCode() ).isEqualTo( GOEvidenceCode.IEA );
        assertThat( r.getGeneratedAt() ).isNotNull();
    }

    /**
     * 🛑 A claim about a term is not a claim about our holdings, so it carries no experiment and no ACL
     * mask. The read path lets these rows past its ACL clause <i>because</i> the experiment column is
     * null; setting either field would delete every ontology relation from every anonymous response and
     * look exactly like the producer never having run.
     */
    @Test
    void anOntologyRowHasNoExperimentAndNoAclMask() {
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( IS_DISEASE_MODEL_FOR, "is disease model for",
                        term( DOID_ADENOCARCINOMA, "adenocarcinoma" ) ) ) );

        AnnotationRelation r = produce( "CLO" ).get( 0 );

        assertThat( r.getExpressionExperiment() ).isNull();
        assertThat( r.getAclIsAuthenticatedAnonymouslyMask() ).isZero();
    }

    /**
     * 🛑 Gemma does not load DOID and does not annotate in it, so a DOID in a stored relation is a
     * disease identifier nothing downstream can resolve — served with full confidence. If the mapping is
     * missing the row is dropped, never stored raw.
     */
    @Test
    void anUntranslatableDoidIsDroppedRatherThanStored() {
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_UNMAPPED, "some disease MONDO has no xref for" ) ) ) );

        assertThat( produce( "CLO" ) ).isEmpty();
    }

    /**
     * 🛑 <b>Resolving a foreign identifier and naming what it resolved to have to come from the same
     * read.</b> The loaded MONDO may be a corpus-seeded slim — it holds the diseases Gemma already
     * annotates, which is the complement of what a DOID is being translated to reach. On 2026-08-18 the
     * translation succeeded for every CLO restriction and 977 rows were dropped anyway, because the
     * label was still looked up in the slim: the identifier resolved and the term stayed nameless.
     * {@code DOID_0050427} alone accounted for 172 of them, which is why it is the one here.
     *
     * <p>The term is deliberately absent from {@code mondoTerms} and present only in the
     * cross-references, which is exactly the slim's shape.</p>
     */
    @Test
    void aTargetTheLoadedModelOmitsIsNamedFromTheSourceInstead() {
        xrefs.add( new OntologyXref( MONDO_MACULAR_DEGENERATION, "DOID:0050427",
                OntologyXref.Strength.EXACT, "age-related macular degeneration" ) );
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( IS_DISEASE_MODEL_FOR, "is disease model for",
                        term( DOID_MACULAR_DEGENERATION, "age related macular degeneration" ) ) ) );

        assertThat( mondoTerms ).doesNotContainKey( MONDO_MACULAR_DEGENERATION );
        List<AnnotationRelation> rows = produce( "CLO" );

        assertThat( rows ).hasSize( 1 );
        assertThat( rows.get( 0 ).getObjectValueUri() ).isEqualTo( MONDO_MACULAR_DEGENERATION );
        assertThat( rows.get( 0 ).getObjectValue() ).isEqualTo( "age-related macular degeneration" );
    }

    /**
     * The fallback names a term from the source; it does not invent a name. {@code OBJECT_VALUE} is
     * {@code NOT NULL} and a URI's local name is not a label, so a target neither the model nor the
     * source can name is still dropped.
     */
    @Test
    void aTargetNeitherTheModelNorTheSourceCanNameIsStillDropped() {
        xrefs.add( new OntologyXref( MONDO_MACULAR_DEGENERATION, "DOID:0050427",
                OntologyXref.Strength.EXACT ) );
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( IS_DISEASE_MODEL_FOR, "is disease model for",
                        term( DOID_MACULAR_DEGENERATION, "age related macular degeneration" ) ) ) );

        assertThat( produce( "CLO" ) ).isEmpty();
    }

    @Test
    void noStoredRowEverCarriesAForeignIdentifier() {
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ),
                restriction( IS_DISEASE_MODEL_FOR, "is disease model for",
                        term( DOID_ADENOCARCINOMA, "adenocarcinoma" ) ),
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_UNMAPPED, "unmapped" ) ) ) );

        assertThat( produce( "CLO" ) )
                .isNotEmpty()
                .allSatisfy( r -> assertThat( r.getObjectValueUri() ).doesNotContain( "DOID_" ) );
    }

    /**
     * 🛑 Where one identifier is claimed by two terms, both framings are stored. The read side reports
     * them side by side on purpose — MONDO's molecular diagnosis and a curator's clinical syndrome
     * routinely name different terms and are both correct — and a producer that picked one would be
     * picking a disease with nothing recording that it had.
     */
    @Test
    void anAmbiguousMappingKeepsEveryFramingRatherThanChoosing() {
        xrefs.add( new OntologyXref( MONDO_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ) );
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ) ) );

        assertThat( produce( "CLO" ) )
                .extracting( AnnotationRelation::getObjectValueUri )
                .containsExactlyInAnyOrder( MONDO_BREAST_ADENOCARCINOMA, MONDO_ADENOCARCINOMA );
    }

    /**
     * A narrow cross-reference is not an equivalence, so it cannot stand in for the term. Substituting
     * across one returns a different, plausible disease and nothing signals the substitution.
     */
    @Test
    void aNarrowMappingIsNotUsedToTranslate() {
        xrefs.clear();
        xrefs.add( new OntologyXref( MONDO_BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.NARROW ) );
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ) ) );

        assertThat( produce( "CLO" ) ).isEmpty();
    }

    /**
     * The NESTED {@code RO_0001000 derives from} axiom is still not read: on a CLO cell line it is an
     * intersection carrying cell type, organism part, species and the donor's disease in one axiom, of
     * which the restriction walk surfaces only the outermost layer, whose target is a blank node.
     * Measured with ROBOT against CLO 2026-06-19: 7,084 such restrictions, plus 692 more buried inside
     * intersection lists. Out of scope, and it has to stay out.
     *
     * <p>The FLAT ones beside them are read — see
     * {@link #aFlatDerivesFromTakesItsCategoryFromTheTargetsVocabulary()}. This test asserted that both
     * were dropped, which was right when neither was read and is now too strong by half.</p>
     */
    @Test
    void theNestedDerivesFromAxiomIsStillNotRead() {
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                anonymousRestriction( DERIVES_FROM, "derives from" ) ) );

        assertThat( produce( "CLO" ) ).isEmpty();
    }

    /**
     * 🛑 {@code RO_0001000 derives from} does not say what its object is, so the object's own
     * vocabulary does.
     *
     * <p>This is the same lesson as a subject's vocabulary outranking a curated category, one level
     * down. CLO's 340 flat {@code derives from} targets are 166 CL cell types, 157 UBERON parts, 14 CLO
     * cell lines, 2 DDANAT parts and one NCBITaxon organism — one property, five kinds of object — so a
     * fixed {@code objectCategory} would file most of them wrongly.</p>
     *
     * <p>Worth reading beside the specific properties: CLO curators mostly wrote the generic one, so
     * {@code CLO_0037208 derives from anatomic part} covers 3 classes where {@code derives from} covers
     * 157. The rare spelling was the only one being read.</p>
     */
    @Test
    void aFlatDerivesFromTakesItsCategoryFromTheTargetsVocabulary() {
        String lymphocyte = OBO + "CL_0000542";
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM, "derives from", term( BREAST, "breast" ) ),
                restriction( DERIVES_FROM, "derives from", term( lymphocyte, "lymphocyte" ) ) ) );

        List<AnnotationRelation> rows = produce( "CLO" );

        assertThat( rows ).hasSize( 2 );
        assertThat( rows )
                .extracting( AnnotationRelation::getObjectValueUri, AnnotationRelation::getObjectCategory )
                .containsExactlyInAnyOrder(
                        tuple( BREAST, "organism part" ),
                        tuple( lymphocyte, "cell type" ) );
    }

    /**
     * A target from a vocabulary the map does not list is stored WITHOUT a category rather than
     * dropped or guessed at. The relation is still true; what it relates to is simply not one of
     * Gemma's annotation categories — the state {@code CLO_0037207 derives from organism} has carried
     * all along, and the one every CURATED row carries by construction.
     */
    @Test
    void anUnlistedTargetVocabularyStoresTheRelationWithoutACategory() {
        String someGene = "http://purl.org/commons/record/ncbi_gene/672";
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM, "derives from", term( someGene, "BRCA1" ) ) ) );

        assertThat( produce( "CLO" ) )
                .singleElement()
                .satisfies( r -> {
                    assertThat( r.getObjectValueUri() ).isEqualTo( someGene );
                    assertThat( r.getObjectCategory() ).isNull();
                    assertThat( r.getObjectCategoryUri() ).isNull();
                } );
    }

    /**
     * A target already in a vocabulary Gemma annotates in is stored as it stands. Only the foreign
     * identifier spaces are translated, and translating a UBERON term would be inventing a mapping.
     */
    @Test
    void anUberonTargetIsStoredUntranslated() {
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_ANATOMIC_PART, "derives from anatomic part",
                        term( BREAST, "breast" ) ) ) );

        AnnotationRelation r = produce( "CLO" ).get( 0 );

        assertThat( r.getObjectValueUri() ).isEqualTo( BREAST );
        assertThat( r.getObjectValue() ).isEqualTo( "breast" );
        assertThat( r.getObjectCategoryUri() ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000635" );
    }

    /**
     * Taxon is part of the grain because it decides what the relation says, and CLO states it as one of
     * the same class's restrictions — so it is resolved in the same pass and applies to every row the
     * class emits.
     */
    @Test
    void theOrganismRestrictionSetsTheTaxonOnEveryRowTheClassEmits() {
        Taxon human = new Taxon();
        human.setNcbiId( 9606 );
        human.setCommonName( "human" );
        when( taxonService.findByNcbiId( 9606 ) ).thenReturn( human );
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_ORGANISM, "derives from organism",
                        term( HUMAN, "Homo sapiens" ) ),
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ) ) );

        List<AnnotationRelation> rows = produce( "CLO" );

        assertThat( rows ).hasSize( 2 ).allSatisfy( r -> assertThat( r.getTaxon() ).isSameAs( human ) );
    }

    /**
     * 🛑 A CHEBI role is a role. Imatinib bears {@code antiviral agent} and {@code antihypertensive
     * agent}; acetylsalicylic acid bears {@code antidepressant}. Those are reported activities from the
     * literature, not indications, and filing them under {@code disease} would have Gemma asserting that
     * aspirin treats depression.
     */
    @Test
    void aChebiRoleIsStoredAsARoleAndNotAsADisease() {
        chebiTerms.put( IMATINIB, term( IMATINIB, "imatinib",
                restriction( HAS_ROLE, "has role", term( ANTINEOPLASTIC_AGENT, "antineoplastic agent" ) ) ) );

        List<AnnotationRelation> rows = produce( "CHEBI" );

        assertThat( rows ).hasSize( 1 );
        AnnotationRelation r = rows.get( 0 );
        assertThat( r.getPredicateUri() ).isEqualTo( HAS_ROLE );
        assertThat( r.getObjectValue() ).isEqualTo( "antineoplastic agent" );
        assertThat( r.getObjectValueUri() ).isEqualTo( ANTINEOPLASTIC_AGENT );
        assertThat( r.getObjectCategory() ).isEqualTo( "role" );
        assertThat( r.getObjectCategoryUri() ).isEqualTo( OBO + "CHEBI_50906" );
        assertThat( r.getObjectCategoryUri() ).isNotEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000408" );
        assertThat( r.getSource() ).isEqualTo( "CHEBI" );
        assertThat( r.getSourceVersion() ).isEqualTo( "254" );
    }

    /**
     * The table is rebuilt, not upserted — an upsert can only correct rows the new read still produces,
     * so a relation an ontology has since retracted would outlive the axiom. The delete is narrowed to
     * the source being rebuilt, because {@code ONTOLOGY} has more than one producer and a basis-wide
     * delete would silently drop the others.
     */
    @Test
    void theRebuildDeletesOnlyTheSourceBeingProduced() {
        chebiTerms.put( IMATINIB, term( IMATINIB, "imatinib",
                restriction( HAS_ROLE, "has role", term( ANTINEOPLASTIC_AGENT, "antineoplastic agent" ) ) ) );

        produce( "CHEBI" );

        verify( dao ).removeByBasis( eq( AnnotationRelationBasis.ONTOLOGY ), isNull(), eq( "CHEBI" ) );
    }

    /**
     * The same restriction is returned again for every descendant of the class that declares it, which
     * is correct OWL and would otherwise be a duplicate row per descendant.
     */
    @Test
    void aTripleReachedTwiceIsWrittenOnce() {
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ),
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ) ) );

        assertThat( produce( "CLO" ) ).hasSize( 1 );
    }

    /**
     * CLO ships the DOID, CL, UBERON and NCBITaxon classes it references. Only its own classes are cell
     * lines; reading a merged-in DOID class as a subject would store a disease as though it were one.
     */
    @Test
    void classesMergedInFromAnotherOntologyAreNotReadAsSubjects() {
        cloTerms.put( DOID_BREAST_ADENOCARCINOMA, term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma",
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_ADENOCARCINOMA, "adenocarcinoma" ) ) ) );

        assertThat( produce( "CLO" ) ).isEmpty();
    }

    /**
     * A translated target absent from the loaded model has no label to store, and OBJECT_VALUE is not
     * nullable. Falling back to the foreign resource's own label would pair a DOID name with a MONDO URI
     * — a mismatch {@code fixOntologyTermLabels} would later "correct" into something nobody asserted.
     * A slim or {@code -base} artifact makes this ordinary, which is why every row carries its
     * SOURCE_VERSION.
     */
    @Test
    void aTargetMissingFromTheLoadedModelIsDroppedRatherThanRelabelled() {
        mondoTerms.remove( MONDO_BREAST_ADENOCARCINOMA );
        cloTerms.put( MCF7, term( MCF7, "MCF7 cell",
                restriction( DERIVES_FROM_PATIENT_HAVING_DISEASE, "derives from patient having disease",
                        term( DOID_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ) ) );

        assertThat( produce( "CLO" ) ).isEmpty();
    }

    /**
     * An ontology that is not loaded asserts nothing. Rebuilding from it would be indistinguishable from
     * it having retracted every axiom, so its rows are left alone and nothing is deleted.
     */
    @Test
    void anUnloadedOntologyLeavesItsRowsAloneInsteadOfDeletingThem() {
        ubic.gemma.core.ontology.providers.OntologyService clo =
                mock( ubic.gemma.core.ontology.providers.OntologyService.class );
        when( clo.getIdentifier() ).thenReturn( "cellLineOntology" );
        when( clo.isOntologyLoaded() ).thenReturn( false );
        OntologyRelationProducerImpl producer = new OntologyRelationProducerImpl(
                Collections.singletonList( clo ), dao, transactionTemplate, taxonService, null );

        assertThat( producer.produce( Collections.singletonList( "CLO" ) ) ).isZero();
        verify( dao, org.mockito.Mockito.never() ).removeByBasis( any(), any(), any() );
        verify( dao, org.mockito.Mockito.never() ).create( any( Collection.class ) );
    }

    @Test
    void theSupportedSourcesAreTheOnesTheAllowListNames() {
        assertThat( producer().getSupportedSources() ).containsExactly( "CLO", "CHEBI", "TGEMO" );
    }

    /**
     * 🛑 The regression this exists for: TGEMO publishes under {@code gemma.msl.ubc.ca/ont/}, not the
     * OBO PURL. While {@code isOwnTerm} built its prefix out of the source's NAME, a TGEMO source
     * matched none of its own classes -- the read visited every one, kept nothing, and reported
     * success with an empty coverage block. Point the namespace back at the OBO PURL and this test
     * goes to zero rows rather than to a wrong row, which is what made the bug invisible.
     */
    @Test
    void aTgemoStrainModelsADiseaseAndTheGemmaNamespaceIsRecognized() {
        tgemoTerms.put( APP_PS1, term( APP_PS1, "APP/PS1",
                restriction( HAS_ROLE_IN_MODELLING, "has role in modeling",
                        term( MONDO_ALZHEIMER, "Alzheimer disease" ) ) ) );

        List<AnnotationRelation> rows = produce( "TGEMO" );

        assertThat( rows ).hasSize( 1 );
        AnnotationRelation r = rows.get( 0 );
        assertThat( r.getSubjectValue() ).isEqualTo( "APP/PS1" );
        assertThat( r.getSubjectValueUri() ).isEqualTo( APP_PS1 );
        assertThat( r.getSubjectCategory() ).isEqualTo( "strain" );
        assertThat( r.getSubjectCategoryUri() ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0005135" );
        assertThat( r.getPredicateUri() ).isEqualTo( HAS_ROLE_IN_MODELLING );
        assertThat( r.getObjectValue() ).isEqualTo( "Alzheimer disease" );
        assertThat( r.getObjectValueUri() ).isEqualTo( MONDO_ALZHEIMER );
        assertThat( r.getBasis() ).isEqualTo( AnnotationRelationBasis.ONTOLOGY );
        assertThat( r.getSource() ).isEqualTo( "TGEMO" );
    }

    /**
     * A source still reads only its own classes. TGEMO subclasses NCBITaxon and references MONDO; those
     * are other ontologies' terms and must not arrive as TGEMO subjects.
     */
    @Test
    void aTgemoReadStillSkipsTermsBelongingToOtherOntologies() {
        tgemoTerms.put( MONDO_ALZHEIMER, term( MONDO_ALZHEIMER, "Alzheimer disease",
                restriction( HAS_ROLE_IN_MODELLING, "has role in modeling",
                        term( MONDO_BREAST_ADENOCARCINOMA, "breast adenocarcinoma" ) ) ) );
        tgemoTerms.put( APP_PS1, term( APP_PS1, "APP/PS1",
                restriction( HAS_ROLE_IN_MODELLING, "has role in modeling",
                        term( MONDO_ALZHEIMER, "Alzheimer disease" ) ) ) );

        List<AnnotationRelation> rows = produce( "TGEMO" );

        assertThat( rows ).hasSize( 1 );
        assertThat( rows.get( 0 ).getSubjectValueUri() ).isEqualTo( APP_PS1 );
    }

    /**
     * 🛑 One category per source is wrong for TGEMO, and this is where it would show. A fusion gene is
     * a genotype and a mouse line is a strain; the source-wide answer would file one of them under the
     * other's category — a claim nobody made — so the property carries its own.
     */
    @Test
    void eachTgemoPropertyCarriesItsOwnSubjectCategory() {
        tgemoTerms.put( APP_PS1, term( APP_PS1, "APP/PS1",
                restriction( HAS_ROLE_IN_MODELLING, "has role in modeling",
                        term( MONDO_ALZHEIMER, "Alzheimer disease" ) ) ) );
        tgemoTerms.put( BCR_ABL, term( BCR_ABL, "MLL-AF9",
                restriction( HAS_DISEASE, "has disease", term( MONDO_AML, "acute myeloid leukemia" ) ) ) );

        List<AnnotationRelation> rows = produce( "TGEMO" );

        assertThat( rows ).hasSize( 2 );
        assertThat( rows ).extracting( AnnotationRelation::getSubjectValueUri,
                        AnnotationRelation::getSubjectCategory, AnnotationRelation::getPredicateUri )
                .containsExactly(
                        tuple( APP_PS1, "strain", HAS_ROLE_IN_MODELLING ),
                        tuple( BCR_ABL, "genotype", HAS_DISEASE ) );
    }

    /**
     * A fusion gene models nothing, so its relation must not arrive under the modelling property.
     * Both are sanctioned and both are SUBJECT_SIDE, which is exactly why a mix-up here would read as
     * correct.
     */
    @Test
    void anUnlistedPropertyOnATgemoClassIsStillNotRead() {
        tgemoTerms.put( BCR_ABL, term( BCR_ABL, "MLL-AF9",
                restriction( OBO + "RO_0002200", "has phenotype",
                        term( MONDO_AML, "acute myeloid leukemia" ) ) ) );

        assertThat( producer().produce( Arrays.asList( "TGEMO" ) ) ).isZero();
    }
}
