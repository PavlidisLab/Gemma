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
     * One readable property.
     */
    static class Relation {

        private final String propertyUri;
        private final String fallbackLabel;
        @Nullable
        private final Category objectCategory;
        private final boolean foreignTargets;
        private final boolean categoryFromTargetVocabulary;

        Relation( String propertyUri, String fallbackLabel, @Nullable Category objectCategory,
                boolean foreignTargets ) {
            this( propertyUri, fallbackLabel, objectCategory, foreignTargets, false );
        }

        Relation( String propertyUri, String fallbackLabel, @Nullable Category objectCategory,
                boolean foreignTargets, boolean categoryFromTargetVocabulary ) {
            this.propertyUri = propertyUri;
            this.fallbackLabel = fallbackLabel;
            this.objectCategory = objectCategory;
            this.foreignTargets = foreignTargets;
            this.categoryFromTargetVocabulary = categoryFromTargetVocabulary;
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
    private final Category subjectCategory;
    private final Map<String, Relation> relations;

    private OntologyRelationSource( String name, String resolverToken, Category subjectCategory,
            List<Relation> relations ) {
        this.name = name;
        this.resolverToken = resolverToken;
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
    static final OntologyRelationSource CLO = new OntologyRelationSource( "CLO", "CLO", CELL_LINE, Arrays.asList(
            new Relation( OBO + "CLO_0000015", "derives from patient having disease", Categories.DISEASE, true ),
            new Relation( OBO + "CLO_0000179", "is disease model for", Categories.DISEASE, true ),
            new Relation( OBO + "CLO_0037208", "derives from anatomic part", Categories.ORGANISM_PART, false ),
            new Relation( OBO + "CLO_0037227", "cell line cell derived from anatomical part", Categories.ORGANISM_PART, false ),
            // the object is an NCBITaxon class, which is not one of Gemma's annotation categories; the
            // taxon itself is carried on TAXON_FK where it can be joined
            new Relation( OBO + "CLO_0037207", "derives from organism", null, false ),
            new Relation( OBO + "CLO_0037229", "cell line cell derived from organism", null, false ),
            new Relation( OBO + "CLO_0037209", "derives from cell", Categories.CELL_TYPE, false ),
            new Relation( OBO + "CLO_0037210", "derived from cell line", CELL_LINE, false ),
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
    static final OntologyRelationSource CHEBI = new OntologyRelationSource( "CHEBI", "CHEBI",
            // the Gemma category these values are annotated under, not a claim by CHEBI about what a
            // molecular entity is
            Categories.TREATMENT, Collections.singletonList(
            new Relation( OBO + "RO_0000087", "has role", ROLE, false ) ) );

    static final List<OntologyRelationSource> ALL = Collections.unmodifiableList( Arrays.asList( CLO, CHEBI ) );
}
