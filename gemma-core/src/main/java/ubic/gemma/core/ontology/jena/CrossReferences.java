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
package ubic.gemma.core.ontology.jena;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import ubic.gemma.core.ontology.model.OntologyXref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads an ontology's cross-references off the Jena model, <b>keeping the mapping qualifier</b>.
 *
 * <p>Two passes, because OBO ontologies state cross-references two ways and MONDO uses both:</p>
 *
 * <ul>
 * <li>{@code oboInOwl:hasDbXref "DOID:3458"} — a plain string. Its qualifier, when there is one, lives
 * on a separate {@code owl:Axiom} node reifying the assertion and carrying {@code oboInOwl:source
 * "MONDO:equivalentTo"}. Reading the assertion without the axiom is what loses the qualifier, which is
 * exactly what the flat list served by the API does today.</li>
 * <li>{@code skos:exactMatch <…/DOID_3458>} — resource-valued, with the qualifier in the predicate.</li>
 * </ul>
 *
 * <p>Scanning the model once for all cross-references, rather than asking each term for its own, is
 * deliberate: the reverse index this feeds is built whole, and a per-term walk over 25,000 MONDO
 * classes would pay the lookup cost 25,000 times to assemble the same map.</p>
 */
class CrossReferences {

    private static final Property ANNOTATED_SOURCE = ResourceFactory.createProperty( OWL.getURI() + "annotatedSource" );
    private static final Property ANNOTATED_PROPERTY = ResourceFactory.createProperty( OWL.getURI() + "annotatedProperty" );
    private static final Property ANNOTATED_TARGET = ResourceFactory.createProperty( OWL.getURI() + "annotatedTarget" );

    private static final Map<Property, OntologyXref.Strength> SKOS_STRENGTHS;

    static {
        Map<Property, OntologyXref.Strength> m = new LinkedHashMap<>();
        m.put( SKOS.exactMatch, OntologyXref.Strength.EXACT );
        m.put( SKOS.closeMatch, OntologyXref.Strength.RELATED );
        // SKOS reads "A narrowMatch B" as B being narrower than A, and OntologyXref.Strength is defined
        // from the declaring term's point of view, so these carry across unflipped.
        m.put( SKOS.narrowMatch, OntologyXref.Strength.NARROW );
        m.put( SKOS.broadMatch, OntologyXref.Strength.BROAD );
        m.put( SKOS.relatedMatch, OntologyXref.Strength.RELATED );
        SKOS_STRENGTHS = m;
    }

    /**
     * Every class-level cross-reference in the model.
     *
     * <p>Anonymous subjects are skipped: a cross-reference hanging off a blank node names no term to
     * map to.</p>
     */
    public static Collection<OntologyXref> list( Model model ) {
        Map<String, OntologyXref.Strength> qualifiers = readAxiomQualifiers( model );
        Map<String, String> labels = readLabels( model );
        List<OntologyXref> result = new ArrayList<>();

        StmtIterator it = model.listStatements( null, OBO.hasDbXref, ( RDFNode ) null );
        try {
            while ( it.hasNext() ) {
                Statement st = it.next();
                String termUri = st.getSubject().getURI();
                if ( termUri == null ) {
                    continue;
                }
                String curie = OntologyXref.normalizeCurie( literalOrUri( st.getObject() ) );
                if ( curie == null ) {
                    continue;
                }
                OntologyXref.Strength strength = qualifiers.getOrDefault( key( termUri, curie ),
                        OntologyXref.Strength.UNSPECIFIED );
                result.add( new OntologyXref( termUri, curie, strength, labels.get( termUri ) ) );
            }
        } finally {
            it.close();
        }

        for ( Map.Entry<Property, OntologyXref.Strength> e : SKOS_STRENGTHS.entrySet() ) {
            StmtIterator sit = model.listStatements( null, e.getKey(), ( RDFNode ) null );
            try {
                while ( sit.hasNext() ) {
                    Statement st = sit.next();
                    String termUri = st.getSubject().getURI();
                    if ( termUri == null ) {
                        continue;
                    }
                    String curie = OntologyXref.normalizeCurie( literalOrUri( st.getObject() ) );
                    if ( curie != null ) {
                        result.add( new OntologyXref( termUri, curie, e.getValue(), labels.get( termUri ) ) );
                    }
                }
            } finally {
                sit.close();
            }
        }

        return result;
    }

    /**
     * {@code rdfs:label} per named class, minus the obsolete ones.
     *
     * <p>Read whole-model in the same style as {@link #readAxiomQualifiers}, and for the same reason:
     * what consumes this is a whole reverse index, so paying a per-term lookup 145,917 times to
     * assemble the same map is precisely what to avoid.</p>
     *
     * <p>🛑 <b>Obsolete terms are dropped rather than labelled.</b> They keep their {@code rdfs:label}
     * and their cross-references — MONDO and EFO both carry retired classes complete with both — so a
     * consumer that stored whatever label came back would file a retired term as the object of a
     * relation with nothing to mark it. The two conditions mirror {@code OntologyTermImpl.isObsolete()}
     * read against a plain model: {@code owl:deprecated true}, or a subclass of
     * {@code oboInOwl:ObsoleteClass}.</p>
     */
    private static Map<String, String> readLabels( Model model ) {
        Set<String> obsolete = new HashSet<>();
        StmtIterator dep = model.listStatements( null, OWL2.deprecated, ( RDFNode ) null );
        try {
            while ( dep.hasNext() ) {
                Statement st = dep.next();
                RDFNode object = st.getObject();
                if ( st.getSubject().getURI() != null && object.isLiteral()
                        && "true".equalsIgnoreCase( object.asLiteral().getLexicalForm() ) ) {
                    obsolete.add( st.getSubject().getURI() );
                }
            }
        } finally {
            dep.close();
        }
        StmtIterator obs = model.listStatements( null, RDFS.subClassOf, OBO.ObsoleteClass );
        try {
            while ( obs.hasNext() ) {
                Resource subject = obs.next().getSubject();
                if ( subject.getURI() != null ) {
                    obsolete.add( subject.getURI() );
                }
            }
        } finally {
            obs.close();
        }

        Map<String, String> labels = new HashMap<>();
        // an English label, once found, is not displaced by a translation appearing later in the file
        Set<String> settled = new HashSet<>();
        StmtIterator it = model.listStatements( null, RDFS.label, ( RDFNode ) null );
        try {
            while ( it.hasNext() ) {
                Statement st = it.next();
                String uri = st.getSubject().getURI();
                if ( uri == null || obsolete.contains( uri ) || settled.contains( uri )
                        || !st.getObject().isLiteral() ) {
                    continue;
                }
                String value = StringUtils.normalizeSpace( st.getObject().asLiteral().getString() );
                if ( StringUtils.isBlank( value ) ) {
                    continue;
                }
                String lang = st.getObject().asLiteral().getLanguage();
                boolean english = StringUtils.isBlank( lang )
                        || lang.toLowerCase( Locale.ROOT ).startsWith( "en" );
                if ( english ) {
                    settled.add( uri );
                } else if ( labels.containsKey( uri ) ) {
                    continue;
                }
                labels.put( uri, value );
            }
        } finally {
            it.close();
        }
        return labels;
    }

    /**
     * The {@code owl:Axiom} nodes qualifying a {@code hasDbXref}, keyed by the assertion they reify.
     */
    private static Map<String, OntologyXref.Strength> readAxiomQualifiers( Model model ) {
        Map<String, OntologyXref.Strength> qualifiers = new HashMap<>();
        StmtIterator it = model.listStatements( null, ANNOTATED_PROPERTY, OBO.hasDbXref );
        try {
            while ( it.hasNext() ) {
                Resource axiom = it.next().getSubject();
                Resource subject = axiom.getPropertyResourceValue( ANNOTATED_SOURCE );
                Statement target = axiom.getProperty( ANNOTATED_TARGET );
                if ( subject == null || subject.getURI() == null || target == null ) {
                    continue;
                }
                String curie = OntologyXref.normalizeCurie( literalOrUri( target.getObject() ) );
                if ( curie == null ) {
                    continue;
                }
                OntologyXref.Strength strength = strengthOf( axiom );
                // several axioms can reify the same assertion (MONDO records more than one provenance);
                // the strongest claim among them is the one the mapping actually makes
                qualifiers.merge( key( subject.getURI(), curie ), strength, CrossReferences::strongest );
            }
        } finally {
            it.close();
        }
        return qualifiers;
    }

    /**
     * Read {@code oboInOwl:source} off an axiom node.
     *
     * <p>One axiom can carry several sources — {@code {source="MONDO:obsoleteEquivalent",
     * source="EFO:0002616"}} in the OBO rendering — where only one of them is a mapping predicate and
     * the rest are provenance. So every source is inspected rather than just the first, and a source
     * that names no mapping predicate leaves the cross-reference {@link OntologyXref.Strength#UNSPECIFIED}
     * instead of being mistaken for a weaker mapping.</p>
     */
    private static OntologyXref.Strength strengthOf( Resource axiom ) {
        OntologyXref.Strength strength = OntologyXref.Strength.UNSPECIFIED;
        StmtIterator it = axiom.listProperties( OBO.source );
        try {
            while ( it.hasNext() ) {
                RDFNode node = it.next().getObject();
                String value = literalOrUri( node );
                if ( value == null ) {
                    continue;
                }
                String v = value.toLowerCase( Locale.ROOT );
                OntologyXref.Strength candidate;
                if ( v.contains( "equivalent" ) ) {
                    // MONDO:equivalentTo, and the obsoleteEquivalent / equivalentObsolete variants: the
                    // mapping is still an equivalence, and the obsolescence belongs to the term, which a
                    // consumer checks on the term itself.
                    candidate = OntologyXref.Strength.EXACT;
                } else if ( v.contains( "narrow" ) ) {
                    candidate = OntologyXref.Strength.NARROW;
                } else if ( v.contains( "broad" ) ) {
                    candidate = OntologyXref.Strength.BROAD;
                } else if ( v.contains( "relatedto" ) || v.contains( "otherhierarchy" )
                        || v.contains( "directsibling" ) ) {
                    candidate = OntologyXref.Strength.RELATED;
                } else {
                    continue; // a provenance pointer, not a mapping predicate
                }
                strength = strongest( strength, candidate );
            }
        } finally {
            it.close();
        }
        return strength;
    }

    /**
     * EXACT wins over everything; otherwise a stated qualifier wins over none.
     */
    private static OntologyXref.Strength strongest( OntologyXref.Strength a, OntologyXref.Strength b ) {
        if ( a == OntologyXref.Strength.EXACT || b == OntologyXref.Strength.EXACT ) {
            return OntologyXref.Strength.EXACT;
        }
        return a == OntologyXref.Strength.UNSPECIFIED ? b : a;
    }

    private static String literalOrUri( RDFNode node ) {
        if ( node.isLiteral() ) {
            return node.asLiteral().getString();
        }
        return node.isURIResource() ? node.asResource().getURI() : null;
    }

    private static String key( String termUri, String curie ) {
        return termUri + ' ' + curie;
    }
}
