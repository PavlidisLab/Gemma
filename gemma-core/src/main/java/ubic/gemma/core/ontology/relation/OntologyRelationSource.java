/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.ontology.relation;

import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which object properties {@link OntologyRelationProducer} reads out of which ontology, and what the
 * two ends of the resulting relation are.
 *
 * <p><b>An allow-list, deliberately, and the opposite of the {@code CURATED} harvest's choice.</b> That
 * one is predicate-agnostic because a curator writing a statement has already decided the triple is
 * worth recording. An ontology has not: CLO's classes carry {@code OBI} assay axioms, cardinality
 * restrictions and part-of chains that are true and useless here, and CHEBI's chemistry hierarchy would
 * arrive as relations if nothing filtered it. So the properties are named.</p>
 *
 * <p>🛑 <b>The object category is what each property says it is and nothing looser.</b> A CHEBI role is
 * a role, not a disease: imatinib bears {@code antiviral agent} and {@code antihypertensive agent},
 * acetylsalicylic acid bears {@code antidepressant} — reported activities from the literature, not
 * indications. Filing those under {@code disease} would have Gemma asserting that aspirin treats
 * depression. The drug-to-indication question is a different source and not this one.</p>
 */
class OntologyRelationSource {

    /** CLO's own cell-line category term, which is also Gemma's. */
    static final Category CELL_LINE = new Category( "cell line", "http://purl.obolibrary.org/obo/CLO_0000031" );

    /**
     * CHEBI's root role term. Not one of Gemma's annotation categories, because a role is not something
     * anybody annotates a sample with — it names what the object of a {@code has role} relation is, and
     * the nearest Gemma category ({@code disease}) would be a false claim.
     */
    static final Category ROLE = new Category( "role", "http://purl.obolibrary.org/obo/CHEBI_50906" );

    /**
     * EFO's strain term, which is what the corpus already annotates these under.
     *
     * <p>🛑 One category per source is a poorer fit for TGEMO than for CLO or CHEBI, whose classes are
     * all one thing. Measured on prod 2026-08-30, the thirteen TGEMO classes carrying an MGI xref are
     * annotated under {@code strain} 279 times, {@code genotype} 48 and {@code disease model} 11 --
     * strain is the majority, not the whole of it. It is recorded as metadata on the row and does NOT
     * gate the search widening, which matches on subject/object value URIs alone
     * ({@code OntologySearchSource.expandByInferredRelations}), so a dataset that annotated APP/PS1 as a
     * genotype is still reached. A caller that filters {@code subjectCategoryUris} would see only the
     * strain spelling.</p>
     */
    static final Category STRAIN = new Category( "strain", "http://www.ebi.ac.uk/efo/EFO_0005135" );

    /**
     * One readable property.
     */
    static class Relation {

        private final String propertyUri;
        private final String fallbackLabel;
        @Nullable
        private final Category objectCategory;
        private final boolean foreignTargets;
        private final boolean categoryFromTargetVocabulary;
        @Nullable
        private final Category subjectCategory;

        Relation( String propertyUri, String fallbackLabel, @Nullable Category objectCategory,
                boolean foreignTargets ) {
            this( propertyUri, fallbackLabel, objectCategory, foreignTargets, false );
        }

        Relation( String propertyUri, String fallbackLabel, @Nullable Category objectCategory,
                boolean foreignTargets, boolean categoryFromTargetVocabulary ) {
            this( propertyUri, fallbackLabel, objectCategory, foreignTargets, categoryFromTargetVocabulary, null );
        }

        Relation( String propertyUri, String fallbackLabel, @Nullable Category objectCategory,
                boolean foreignTargets, boolean categoryFromTargetVocabulary,
                @Nullable Category subjectCategory ) {
            this.propertyUri = propertyUri;
            this.fallbackLabel = fallbackLabel;
            this.objectCategory = objectCategory;
            this.foreignTargets = foreignTargets;
            this.categoryFromTargetVocabulary = categoryFromTargetVocabulary;
            this.subjectCategory = subjectCategory;
        }

        /**
         * What the SUBJECT is, when this one property says something different from the rest of its
         * source.
         *
         * <p>Null means the source's own answer, which is right wherever an ontology's classes are all
         * one thing. TGEMO's are not: {@code RO_0003301}'s subjects there are mouse strains and
         * {@code RO_0016002}'s are fusion genes, and filing either under the other's category would be
         * a claim nobody made.</p>
         */
        @Nullable
        Category getSubjectCategory() {
            return subjectCategory;
        }

        String getPropertyUri() {
            return propertyUri;
        }

        /**
         * Used only when the loaded ontology declares no {@code rdfs:label} for the property. CLO
         * labels all of these, so this is a guard rather than the normal path.
         */
        String getFallbackLabel() {
            return fallbackLabel;
        }

        @Nullable
        Category getObjectCategory() {
            return objectCategory;
        }

        /**
         * What the object is, for a property that does not settle it.
         *
         * <p>Most properties name their object's kind by naming themselves — a
         * {@code derives from anatomic part} target is an organism part and cannot be anything else.
         * {@code RO_0001000 derives from} does not: its 340 flat targets in CLO 2026-06-19 are 166 CL
         * cell types, 157 UBERON parts, 14 CLO cell lines, 2 DDANAT parts and one NCBITaxon organism.
         * The target's own vocabulary is what says which, the same way a subject's vocabulary says what
         * it is when the curated category will not.</p>
         *
         * <p>Null for a vocabulary not listed, which stores the relation without asserting what kind of
         * thing the object is. That is an ordinary state — every {@code CURATED} row has a null object
         * category by construction, and so does {@code CLO_0037207 derives from organism} — and it is
         * the honest answer where guessing would file a term under a category nobody asserted.</p>
         */
        @Nullable
        Category getObjectCategory( @Nullable String targetUri ) {
            if ( !categoryFromTargetVocabulary ) {
                return objectCategory;
            }
            return categoryForVocabularyOf( targetUri );
        }

        /**
         * Whether this property's targets live in an identifier space Gemma does not annotate in and
         * must be translated out of before the row is stored.
         */
        boolean hasForeignTargets() {
            return foreignTargets;
        }
    }

    private final String name;
    private final String resolverToken;
    private final String namespace;
    private final Category subjectCategory;
    private final Map<String, Relation> relations;

    private OntologyRelationSource( String name, String resolverToken, String namespace,
            Category subjectCategory, List<Relation> relations ) {
        this.name = name;
        this.resolverToken = resolverToken;
        this.namespace = namespace;
        this.subjectCategory = subjectCategory;
        Map<String, Relation> byUri = new LinkedHashMap<>();
        for ( Relation r : relations ) {
            byUri.put( r.getPropertyUri(), r );
        }
        this.relations = Collections.unmodifiableMap( byUri );
    }

    /**
     * As {@link ubic.gemma.model.common.description.AnnotationRelation#getSource()} records it.
     */
    String getName() {
        return name;
    }

    /**
     * The token {@link ubic.gemma.core.ontology.providers.OntologyServiceResolver} matches the ontology
     * on, so the producer does not hold a hard bean dependency on a particular ontology service.
     */
    String getResolverToken() {
        return resolverToken;
    }

    /**
     * The URI prefix a class of this ontology's own has, used to tell those apart from the classes it
     * merges in.
     *
     * <p>🛑 Carried per source rather than built from {@link #getName()} and the OBO PURL. CLO and CHEBI
     * are both under {@code purl.obolibrary.org/obo/}; TGEMO is under {@code gemma.msl.ubc.ca/ont/}, so
     * deriving the prefix from the name matches nothing for it -- the read visits zero classes and
     * reports success.</p>
     */
    String getNamespace() {
        return namespace;
    }

    /**
     * What every subject in this source is. CLO classes are cell lines and CHEBI classes are chemicals;
     * neither ontology needs to be asked term by term.
     */
    Category getSubjectCategory() {
        return subjectCategory;
    }

    @Nullable
    Relation getRelation( String propertyUri ) {
        return relations.get( propertyUri );
    }

    Map<String, Relation> getRelations() {
        return relations;
    }

    private static final String OBO = "http://purl.obolibrary.org/obo/";

    /**
     * Term vocabulary → the Gemma category a term from it belongs to.
     *
     * <p>Ordered longest-prefix-first is unnecessary here because OBO local names are
     * {@code PREFIX_digits} and the prefixes below are disjoint. {@code NCBITaxon} is deliberately
     * absent: an organism is not one of Gemma's annotation categories, and the taxon it names is
     * carried on {@code TAXON_FK} where it can be joined — exactly as
     * {@code CLO_0037207 derives from organism} already does.</p>
     */
    private static final Map<String, Category> CATEGORY_BY_VOCABULARY;

    static {
        Map<String, Category> m = new LinkedHashMap<>();
        m.put( "CL_", Categories.CELL_TYPE );
        m.put( "UBERON_", Categories.ORGANISM_PART );
        m.put( "DDANAT_", Categories.ORGANISM_PART );   // Dictyostelium anatomy; 2 uses, same kind of thing
        m.put( "CLO_", CELL_LINE );
        CATEGORY_BY_VOCABULARY = Collections.unmodifiableMap( m );
    }

    /**
     * @see Relation#getObjectCategory(String)
     */
    @Nullable
    private static Category categoryForVocabularyOf( @Nullable String termUri ) {
        if ( termUri == null ) {
            return null;
        }
        int cut = Math.max( termUri.lastIndexOf( '/' ), termUri.lastIndexOf( '#' ) );
        String localName = cut >= 0 ? termUri.substring( cut + 1 ) : termUri;
        for ( Map.Entry<String, Category> e : CATEGORY_BY_VOCABULARY.entrySet() ) {
            if ( localName.startsWith( e.getKey() ) ) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * CLO — the flat {@code someValuesFrom} restrictions on its cell-line classes.
     *
     * <p>{@code RO_0001000 derives from} is deliberately absent. On the same classes it is a nested
     * intersection — an epithelial cell {@code part_of} breast {@code part_of} a human who has
     * {@code DOID_3008} — carrying cell type, organism part, species and the donor's disease in one
     * axiom that {@code getRestrictions()} surfaces only the outermost layer of. Measured against
     * CLO 2026-06-19: 340 flat and 7,776 nested. Unwinding it is its own piece of work.</p>
     */
    static final OntologyRelationSource CLO = new OntologyRelationSource( "CLO", "CLO", OBO + "CLO_", CELL_LINE, Arrays.asList(
            new Relation( OBO + "CLO_0000015", "derives from patient having disease", Categories.DISEASE, true ),
            new Relation( OBO + "CLO_0000179", "is disease model for", Categories.DISEASE, true ),
            new Relation( OBO + "CLO_0037208", "derives from anatomic part", Categories.ORGANISM_PART, false ),
            new Relation( OBO + "CLO_0037227", "cell line cell derived from anatomical part", Categories.ORGANISM_PART, false ),
            // the object is an NCBITaxon class, which is not one of Gemma's annotation categories; the
            // taxon itself is carried on TAXON_FK where it can be joined
            new Relation( OBO + "CLO_0037207", "derives from organism", null, false ),
            new Relation( OBO + "CLO_0037229", "cell line cell derived from organism", null, false ),
            new Relation( OBO + "CLO_0037209", "derives from cell", Categories.CELL_TYPE, false ),
            new Relation( OBO + "CLO_0037210", "derives from cell line cell", CELL_LINE, false ),
            // 🛑 RO_0001000 derives from -- the FLAT ones only, and they are the bulk of this source.
            // CLO curators mostly wrote the generic property and let the target's vocabulary carry the
            // meaning, so the specific properties above are the rare spelling of the same relation.
            // Measured with ROBOT against CLO 2026-06-19:
            //
            //     relation          specific property   classes    RO_0001000 flat
            //     -> cell type      CLO_0037209              56               166
            //     -> organism part  CLO_0037208               3               157
            //     -> cell line      CLO_0037210               1                14
            //     -> organism       CLO_0037207              11                 1
            //
            // The nested ones are NOT read and remain out of scope: 7,084 restrictions whose target is
            // an anonymous intersection plus 692 buried inside intersection lists (7,776 together).
            // They arrive here as anonymous targets and are tallied as such, which is now the honest
            // measure of that backlog rather than an invisible gap.
            new Relation( OBO + "RO_0001000", "derives from", null, false, true ) ) );

    /**
     * CHEBI — {@code RO_0000087 has role}, which is what makes {@code imatinib} findable as an
     * antineoplastic agent and a tyrosine kinase inhibitor.
     *
     * <p>CHEBI already folds this property into {@code additionalPropertyUris}, so the roles come back
     * as a term's <i>parents</i> today. Reading them here as relations is the point: a parent list
     * cannot say which of a chemical's ancestors is a role and which is chemistry, and the flattening is
     * not something to extend.</p>
     */
    static final OntologyRelationSource CHEBI = new OntologyRelationSource( "CHEBI", "CHEBI", OBO + "CHEBI_",
            // the Gemma category these values are annotated under, not a claim by CHEBI about what a
            // molecular entity is
            Categories.TREATMENT, Collections.singletonList(
            new Relation( OBO + "RO_0000087", "has role", ROLE, false ) ) );

    /**
     * TGEMO -- {@code RO_0003301 has role in modeling}, which is what makes an experiment annotated
     * {@code APP/PS1} findable as an Alzheimer disease model.
     *
     * <p>TGEMO is Gemma's own ontology and holds the strains and engineered genotypes that no external
     * vocabulary has a term for. Until these axioms were added the disease each one models was stated
     * only in the class's {@code IAO_0000115} definition text -- prose, reachable by nothing.</p>
     *
     * <p>🛑 Its classes are NOT under the OBO PURL, which is why {@link #getNamespace()} exists. A
     * source whose prefix was derived from its name would visit zero TGEMO classes and report
     * success.</p>
     *
     * <p>{@code RO_0003301} and not {@code RO_0002200 has phenotype}: the latter is sanctioned by
     * {@code Relation.terms.txt} but is classified into neither direction set, and
     * {@code RelationInferenceDirection.byPredicate} fails closed, so those rows would store, read back
     * and license no inference at all.</p>
     */
    static final OntologyRelationSource TGEMO = new OntologyRelationSource( "TGEMO", "TGEMO",
            "http://gemma.msl.ubc.ca/ont/TGEMO_", STRAIN, Arrays.asList(
            // targets are MONDO URIs written directly into the axiom, so nothing needs translating
            new Relation( OBO + "RO_0003301", "has role in modeling", Categories.DISEASE, false ),
            // 🛑 A fusion gene is NOT a model of the disease it is found in, so this is a different
            // property and not a second spelling of the one above. RO_0016002 is what Gemma already
            // uses for gene-to-disease (see RelationInferenceDirection's SUBJECT_SIDE, "SNCA implies
            // Parkinson"), and its subjects here are genotypes rather than strains.
            new Relation( OBO + "RO_0016002", "has disease", Categories.DISEASE, false, false,
                    Categories.GENOTYPE ) ) );

    /**
     * CL -- where a cell type sits anatomically, which CL asserts on the class and nothing read.
     *
     * <p>Measured on {@code cl-base} 2026-06-08, the artifact Gemma loads: 1,208 location axioms over
     * 1,180 CL classes, 1,203 of them targeting UBERON. Every one is asserted on the class itself --
     * {@code cl-base} and the full merged {@code cl.json} carry the identical count, so CL does not
     * propagate these down {@code is_a} and neither does anything here. A cell type whose only
     * location comes from an ancestor is NOT produced: the entailment is valid and frequently useless
     * ({@code Mueller cell} inherits {@code part of photoreceptor array} from {@code retinal cell}),
     * and sorting the useful ones out is a separate piece of work.</p>
     *
     * <p>🛑 Two properties, not one. {@code BFO_0000050 part of} carries 953 of the edges and
     * {@code RO_0002100 has soma location} another 250 -- the latter is how CL locates most neurons,
     * because an axon leaves the structure its soma sits in. Reading only {@code part of} silently
     * drops a fifth of the source. {@code RO_0002220 adjacent to} is deliberately absent: adjacency is
     * not membership.</p>
     *
     * <p>⚠️ {@code cl-base} does not merge UBERON, so these targets are anonymous in CL's own model and
     * are named from the owning ontology by the producer's fallback -- the same path TGEMO needed.
     * {@code OBJECT_VALUE} is NOT NULL, so without it every row would be dropped.</p>
     */
    static final OntologyRelationSource CL = new OntologyRelationSource( "CL", "CL", OBO + "CL_",
            Categories.CELL_TYPE, Arrays.asList(
            new Relation( OBO + "BFO_0000050", "part of", Categories.ORGANISM_PART, false ),
            new Relation( OBO + "RO_0002100", "has soma location", Categories.ORGANISM_PART, false ) ) );

    static final List<OntologyRelationSource> ALL = Collections.unmodifiableList( Arrays.asList( CLO, CHEBI, TGEMO, CL ) );
}
