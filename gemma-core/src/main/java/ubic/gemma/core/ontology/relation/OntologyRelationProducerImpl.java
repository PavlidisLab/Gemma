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

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.ontology.model.OntologyClassRestriction;
import ubic.gemma.core.ontology.model.OntologyProperty;
import ubic.gemma.core.ontology.model.OntologyRestriction;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.model.OntologyXref;
import ubic.gemma.core.ontology.providers.OntologyServiceResolver;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads {@link OntologyTerm#getRestrictions()} over the sources named in {@link OntologyRelationSource}
 * and writes what they assert into {@code ANNOTATION_RELATION}.
 *
 * <p>The reading runs outside the transaction and the write inside it. An ontology pass is minutes of
 * in-memory graph walking over tens of thousands of classes; holding a database connection open for the
 * duration to do nothing with it is how a maintenance job starts contending with the application.</p>
 *
 * @see OntologyRelationProducer
 */
public class OntologyRelationProducerImpl implements OntologyRelationProducer {

    private static final Logger log = LoggerFactory.getLogger( OntologyRelationProducerImpl.class );

    /**
     * Identifier spaces Gemma neither loads nor annotates in, so a target in one of them has to be
     * translated before it can be stored.
     *
     * <p>CLO points at DOID and Cellosaurus at NCIt; Gemma annotates in MONDO, which is what DOID is
     * being consolidated into. Translating out of them on read is the whole design — adopting DOID would
     * mean carrying a superseded vocabulary, its download and its refresh schedule to answer a question
     * MONDO's own cross-references already answer from a model in memory.</p>
     */
    private static final Set<String> FOREIGN_PREFIXES = Collections.unmodifiableSet( new HashSet<>(
            Arrays.asList( "DOID", "NCIT", "ORPHANET", "ORDO", "UMLS", "MESH", "OMIM", "ICD10", "MEDDRA" ) ) );

    private static final Pattern NCBI_TAXON_URI =
            Pattern.compile( "^http://purl\\.obolibrary\\.org/obo/NCBITaxon_(\\d+)$" );

    /**
     * The ontology whose cross-references are inverted to translate foreign targets.
     */
    private static final String XREF_SOURCE_TOKEN = "MONDO";

    private static final int PROGRESS_EVERY = 25000;

    /** {@code SOURCE_VERSION} is {@code VARCHAR(64)}. */
    private static final int SOURCE_VERSION_MAX = 64;

    /** every other text column here is {@code VARCHAR(255)}. */
    private static final int VALUE_MAX = 255;

    private final List<ubic.gemma.core.ontology.providers.OntologyService> ontologies;
    private final AnnotationRelationDao annotationRelationDao;
    private final TransactionTemplate transactionTemplate;
    @Nullable
    private final TaxonService taxonService;
    @Nullable
    private final ubic.gemma.core.ontology.OntologyService ontologyService;

    public OntologyRelationProducerImpl( @Nullable List<ubic.gemma.core.ontology.providers.OntologyService> ontologies,
            AnnotationRelationDao annotationRelationDao, TransactionTemplate transactionTemplate,
            @Nullable TaxonService taxonService,
            @Nullable ubic.gemma.core.ontology.OntologyService ontologyService ) {
        this.ontologies = ontologies != null ? ontologies : Collections.<ubic.gemma.core.ontology.providers.OntologyService>emptyList();
        this.annotationRelationDao = annotationRelationDao;
        this.transactionTemplate = transactionTemplate;
        this.taxonService = taxonService;
        this.ontologyService = ontologyService;
    }

    @Override
    public Collection<String> getSupportedSources() {
        List<String> names = new ArrayList<>( OntologyRelationSource.ALL.size() );
        for ( OntologyRelationSource source : OntologyRelationSource.ALL ) {
            names.add( source.getName() );
        }
        return names;
    }

    @Override
    public int produce() {
        return produce( null );
    }

    @Override
    public int produce( @Nullable Collection<String> sources ) {
        List<OntologyRelationSource> selected = select( sources );
        if ( selected.isEmpty() ) {
            log.warn( "No known ONTOLOGY relation source matched {}; nothing to do. Known sources: {}.",
                    sources, getSupportedSources() );
            return 0;
        }
        Optional<ubic.gemma.core.ontology.providers.OntologyService> xrefOntology = loadedOntology( XREF_SOURCE_TOKEN );
        OntologyXrefIndex xrefs = buildXrefIndex( xrefOntology.orElse( null ) );
        int written = 0;
        for ( OntologyRelationSource source : selected ) {
            written += produce( source, xrefs, xrefOntology.orElse( null ) );
        }
        return written;
    }

    private int produce( OntologyRelationSource source, OntologyXrefIndex xrefs,
            @Nullable ubic.gemma.core.ontology.providers.OntologyService xrefOntology ) {
        Optional<ubic.gemma.core.ontology.providers.OntologyService> ontology = loadedOntology( source.getResolverToken() );
        if ( !ontology.isPresent() ) {
            // Left alone rather than deleted: an ontology that is not loaded asserts nothing, and
            // rebuilding from it would look exactly like the ontology having retracted every axiom.
            log.warn( "{} is not available or not loaded; its ONTOLOGY relation rows are left as they are.",
                    source.getName() );
            return 0;
        }

        StopWatch timer = StopWatch.createStarted();
        Reading reading = read( source, ontology.get(), xrefs, xrefOntology );
        log.info( "Read {} {} relations from {} classes in {} ms.\n{}", reading.rows.size(), source.getName(),
                reading.classesVisited, timer.getTime(), reading.report( source ) );

        int written = write( source, reading.rows );
        log.info( "Rebuilt {} ONTOLOGY relation rows for {} ({}) in {} ms.", written, source.getName(),
                reading.sourceVersion != null ? reading.sourceVersion : "no version declared", timer.getTime() );
        return written;
    }

    // -----------------------------------------------------------------------------------------------
    // reading
    // -----------------------------------------------------------------------------------------------

    /**
     * One pass over an ontology's classes, plus the tallies that make its coverage observable.
     *
     * <p>The tallies are not decoration. "The relation is missing" and "the axiom was dropped by the
     * artifact we happen to load" look identical from the table, and the recce this implements asked for
     * exactly these numbers.</p>
     */
    private static class Reading {

        private final List<AnnotationRelation> rows = new ArrayList<>();
        /** deduplicates a triple reached twice, which inheritance guarantees will happen */
        private final Set<String> seen = new HashSet<>();
        private final Map<String, Set<String>> classesPerProperty = new TreeMap<>();
        private final Map<String, int[]> countsPerProperty = new TreeMap<>();
        private final Map<String, Integer> unresolvedTargets = new TreeMap<>();
        private int classesVisited = 0;
        private int anonymousTargets = 0;
        private int untranslatable = 0;
        private int unlabelledTargets = 0;
        /**
         * Translated targets the loaded model could not name, that the source artifact could.
         *
         * <p>Reported rather than left silent because it measures exactly how far the loaded model has
         * drifted from the source — a slim rebuild with different seeds moves this number, and nothing
         * else in the run would show it.</p>
         */
        private int labelledFromSource = 0;
        private int readFailures = 0;
        /**
         * Restrictions on an allow-listed property that were rejected because they are not an
         * {@link OntologyClassRestriction}, tallied by the concrete type that came back.
         *
         * <p>🛑 This branch used to drop silently, and the drop is large: CLO's {@code clo.owl}
         * 2026-06-19 uses {@code CLO_0000179} as a restriction's {@code onProperty} 8,580 times and the
         * pass reported 441. The "restrictions" column counts what survived this test, not what was
         * found, so the difference was invisible in the report by construction — including to the two
         * cell lines the whole feature exists for.</p>
         */
        private final Map<String, Integer> nonClassRestrictions = new TreeMap<>();
        /**
         * Classes the model actually offered, as distinct from the ones we kept. Recorded because
         * {@code SOURCE_VERSION} cannot tell a full artifact from a slim one — CHEBI reported version
         * {@code 254} both when it yielded 25,231 relations from 237,842 classes and when it yielded
         * 11,378 from 20,964. The version was identical; the artifact was not.
         */
        private int classesOffered = 0;
        @Nullable
        private String sourceVersion;

        /** [restrictions read, rows written] */
        private int[] counts( String propertyUri ) {
            return countsPerProperty.computeIfAbsent( propertyUri, k -> new int[2] );
        }

        private void sawClass( String propertyUri, String classUri ) {
            classesPerProperty.computeIfAbsent( propertyUri, k -> new HashSet<>() ).add( classUri );
        }

        private String report( OntologyRelationSource source ) {
            StringBuilder sb = new StringBuilder();
            sb.append( "coverage\t" ).append( source.getName() ).append( '\t' )
                    .append( sourceVersion != null ? sourceVersion : "(no version declared)" ).append( '\n' );
            sb.append( "property\tclasses\trestrictions\trows\n" );
            for ( Map.Entry<String, int[]> e : countsPerProperty.entrySet() ) {
                sb.append( e.getKey() ).append( '\t' )
                        .append( classesPerProperty.getOrDefault( e.getKey(), Collections.emptySet() ).size() )
                        .append( '\t' ).append( e.getValue()[0] )
                        .append( '\t' ).append( e.getValue()[1] ).append( '\n' );
            }
            sb.append( "classes visited\t" ).append( classesVisited )
                    .append( "\tclasses offered by the model\t" ).append( classesOffered ).append( '\n' );
            if ( !nonClassRestrictions.isEmpty() ) {
                sb.append( "restrictions rejected as non-class (property -> type, count):\n" );
                nonClassRestrictions.entrySet().stream()
                        .sorted( ( a, b ) -> Integer.compare( b.getValue(), a.getValue() ) )
                        .limit( 25 )
                        .forEach( e -> sb.append( '\t' ).append( e.getKey() ).append( '\t' )
                                .append( e.getValue() ).append( '\n' ) );
            }
            sb.append( "dropped\tanonymous target (nested axiom)\t" ).append( anonymousTargets )
                    .append( "\tuntranslatable foreign target\t" ).append( untranslatable )
                    .append( "\ttarget absent from the loaded model\t" ).append( unlabelledTargets )
                    .append( "\tunreadable class\t" ).append( readFailures ).append( '\n' );
            if ( labelledFromSource > 0 ) {
                sb.append( "recovered\ttarget absent from the loaded model, named from the source\t" )
                        .append( labelledFromSource ).append( '\n' );
            }
            if ( !unresolvedTargets.isEmpty() ) {
                sb.append( "unresolved targets (identifier, restrictions affected):\n" );
                unresolvedTargets.entrySet().stream()
                        .sorted( ( a, b ) -> Integer.compare( b.getValue(), a.getValue() ) )
                        .limit( 25 )
                        .forEach( e -> sb.append( '\t' ).append( e.getKey() ).append( '\t' )
                                .append( e.getValue() ).append( '\n' ) );
            }
            return sb.toString();
        }
    }

    private Reading read( OntologyRelationSource source,
            ubic.gemma.core.ontology.providers.OntologyService ontology, OntologyXrefIndex xrefs,
            @Nullable ubic.gemma.core.ontology.providers.OntologyService xrefOntology ) {
        Reading reading = new Reading();
        reading.sourceVersion = truncate( ontology.getVersion(), SOURCE_VERSION_MAX );
        Date generatedAt = new Date();
        Set<String> sanctionedPredicates = sanctionedPredicateUris();
        Set<String> unsanctioned = new LinkedHashSet<>();
        Map<Integer, Taxon> taxaByNcbiId = new HashMap<>();

        Collection<String> allUris = ontology.getAllURIs();
        reading.classesOffered = allUris.size();
        for ( String uri : allUris ) {
            if ( uri == null || !isOwnTerm( source, uri ) ) {
                continue;
            }
            reading.classesVisited++;
            if ( reading.classesVisited % PROGRESS_EVERY == 0 ) {
                log.info( "Read {} {} classes so far, {} relations.", reading.classesVisited, source.getName(),
                        reading.rows.size() );
            }
            OntologyTerm term = ontology.getTerm( uri );
            if ( term == null || term.isObsolete() || term.getLabel() == null ) {
                continue;
            }
            Collection<OntologyRestriction> restrictions;
            try {
                restrictions = term.getDirectRestrictions();
            } catch ( Exception e ) {
                // getRestrictions() throws outright on a restriction shape it cannot convert, and one bad
                // class must not end the pass over 40,000 good ones
                reading.readFailures++;
                if ( log.isDebugEnabled() ) {
                    log.debug( "Could not read restrictions on " + uri, e );
                }
                continue;
            }
            if ( restrictions.isEmpty() ) {
                continue;
            }

            // the taxon is one of this class's own restrictions, so it comes out of the same pass rather
            // than a second one, and applies to every row the class emits
            Taxon taxon = resolveTaxon( source, restrictions, taxaByNcbiId );

            for ( OntologyRestriction restriction : restrictions ) {
                OntologyProperty property = restriction.getRestrictionOn();
                if ( property == null || property.getUri() == null ) {
                    continue;
                }
                OntologyRelationSource.Relation spec = source.getRelation( property.getUri() );
                if ( spec == null ) {
                    continue;
                }
                if ( !( restriction instanceof OntologyClassRestriction ) ) {
                    // counted, not swallowed: this is where the bulk of CLO_0000179 goes, and until it
                    // was tallied the coverage block reported the survivors as though they were the
                    // whole population
                    reading.nonClassRestrictions.merge(
                            property.getUri() + " -> " + restriction.getClass().getSimpleName(), 1, Integer::sum );
                    continue;
                }
                if ( !sanctionedPredicates.isEmpty() && !sanctionedPredicates.contains( property.getUri() ) ) {
                    unsanctioned.add( property.getUri() + " (" + spec.getFallbackLabel() + ")" );
                }
                reading.counts( property.getUri() )[0]++;
                reading.sawClass( property.getUri(), uri );

                OntologyTerm target = ( ( OntologyClassRestriction ) restriction ).getRestrictedTo();
                if ( target == null || target.getUri() == null ) {
                    // an anonymous intersection filler -- CLO's nested `derives from` chain, whose
                    // outermost layer is all getRestrictions() surfaces. Out of scope by design.
                    reading.anonymousTargets++;
                    continue;
                }
                for ( AnnotationRelation row : toRelations( source, spec, term, property, target, taxon, xrefs,
                        xrefOntology, reading, generatedAt ) ) {
                    if ( reading.seen.add( key( row ) ) ) {
                        reading.rows.add( row );
                        reading.counts( property.getUri() )[1]++;
                    }
                }
            }
        }

        if ( !unsanctioned.isEmpty() ) {
            log.warn( "{} relations were read on predicates that are NOT in Relation.terms.txt, Gemma's "
                            + "sanctioned relation vocabulary: {}. The rows are written — ANNOTATION_RELATION is a "
                            + "derived index rather than a curation surface, and dropping them would discard most of "
                            + "what the ontology asserts — but the vocabulary file should be extended deliberately "
                            + "and not by this job.",
                    source.getName(), unsanctioned );
        }
        return reading;
    }

    /**
     * Build the rows one restriction yields, translating the target out of a foreign identifier space
     * where the property needs it.
     *
     * <p>Usually one row. More than one when a foreign identifier is claimed by several terms, and that
     * ambiguity is <b>kept rather than resolved</b>: picking among them would be picking a disease, and
     * the read side is built to report the alternatives side by side.</p>
     */
    private List<AnnotationRelation> toRelations( OntologyRelationSource source,
            OntologyRelationSource.Relation spec, OntologyTerm subject, OntologyProperty property,
            OntologyTerm target, @Nullable Taxon taxon, OntologyXrefIndex xrefs,
            @Nullable ubic.gemma.core.ontology.providers.OntologyService xrefOntology, Reading reading,
            Date generatedAt ) {
        String objectUri = target.getUri();

        if ( spec.hasForeignTargets() && isForeign( objectUri ) ) {
            Set<String> translated = xrefs.resolve( objectUri );
            if ( translated.isEmpty() ) {
                reading.untranslatable++;
                reading.unresolvedTargets.merge( objectUri, 1, Integer::sum );
                return Collections.emptyList();
            }
            List<AnnotationRelation> rows = new ArrayList<>( translated.size() );
            for ( String candidate : translated ) {
                String label = labelOf( candidate, xrefOntology );
                if ( label == null ) {
                    // 🛑 The loaded model is not the authority on what a translated term is called, and
                    // treating it as one is what left the xref fix half-done. It may be a corpus-seeded
                    // slim, holding the diseases Gemma already annotates -- the complement of what a
                    // foreign identifier is translated to reach. Measured 2026-08-18: every DOID
                    // resolved and 977 restrictions were dropped here anyway, DOID_0050427 alone
                    // accounting for 172. The artifact that resolved the identifier also names it.
                    label = xrefs.labelOf( candidate );
                    if ( label != null ) {
                        reading.labelledFromSource++;
                    }
                }
                if ( label == null ) {
                    reading.unlabelledTargets++;
                    continue;
                }
                AnnotationRelation row = build( source, spec, subject, property, candidate, label, taxon,
                        reading.sourceVersion, generatedAt );
                if ( row != null ) {
                    rows.add( row );
                }
            }
            if ( rows.isEmpty() ) {
                reading.unresolvedTargets.merge( objectUri, 1, Integer::sum );
            }
            return rows;
        }

        String nativeLabel = target.getLabel();
        if ( nativeLabel == null ) {
            // 🛑 Same trap as the translated branch above, reached a different way. A source that does
            // not merge in the vocabulary it points at cannot name the target: TGEMO writes MONDO URIs
            // straight into its axioms and is loaded with processImports off, so every one of its
            // targets is anonymous in its own model. Ask the vocabulary that owns the term, then the
            // cross-reference source, before giving up -- otherwise the whole source reads as
            // unlabelled targets and writes nothing.
            nativeLabel = labelOf( objectUri, xrefOntology );
            if ( nativeLabel == null ) {
                nativeLabel = xrefs.labelOf( objectUri );
            }
            if ( nativeLabel != null ) {
                reading.labelledFromSource++;
            }
        }
        if ( nativeLabel == null ) {
            // no label means no OBJECT_VALUE, which is NOT NULL, and a URI's local name is not a label
            reading.unlabelledTargets++;
            return Collections.emptyList();
        }
        AnnotationRelation row = build( source, spec, subject, property, objectUri, nativeLabel, taxon,
                reading.sourceVersion, generatedAt );
        return row != null ? Collections.singletonList( row ) : Collections.<AnnotationRelation>emptyList();
    }

    @Nullable
    private AnnotationRelation build( OntologyRelationSource source, OntologyRelationSource.Relation spec,
            OntologyTerm subject, OntologyProperty property, String objectUri, String objectLabel,
            @Nullable Taxon taxon, @Nullable String sourceVersion, Date generatedAt ) {
        if ( isForeign( objectUri ) ) {
            // Unreachable if the translation above is right, and a defect if it is not: a DOID in a stored
            // relation is a disease identifier Gemma cannot resolve, reported with full confidence.
            log.error( "Refusing to store {} as the object of {}: it is in an identifier space Gemma does not "
                    + "annotate in. This is a bug in the translation, not bad data.", objectUri, subject.getUri() );
            return null;
        }
        String subjectLabel = subject.getLabel();
        if ( subjectLabel == null || subject.getUri() == null ) {
            return null;
        }

        AnnotationRelation relation = new AnnotationRelation();
        relation.setSubjectValue( truncate( subjectLabel, VALUE_MAX ) );
        relation.setSubjectValueUri( subject.getUri() );
        Category subjectCategory = spec.getSubjectCategory() != null
                ? spec.getSubjectCategory() : source.getSubjectCategory();
        relation.setSubjectCategory( subjectCategory.getCategory() );
        relation.setSubjectCategoryUri( subjectCategory.getCategoryUri() );
        relation.setPredicate( truncate(
                property.getLabel() != null ? property.getLabel() : spec.getFallbackLabel(), VALUE_MAX ) );
        relation.setPredicateUri( property.getUri() );
        relation.setObjectValue( truncate( objectLabel, VALUE_MAX ) );
        relation.setObjectValueUri( objectUri );
        Category objectCategory = spec.getObjectCategory( objectUri );
        if ( objectCategory != null ) {
            relation.setObjectCategory( objectCategory.getCategory() );
            relation.setObjectCategoryUri( objectCategory.getCategoryUri() );
        }
        relation.setTaxon( taxon );
        relation.setBasis( AnnotationRelationBasis.ONTOLOGY );
        relation.setSource( source.getName() );
        relation.setSourceVersion( sourceVersion );
        // software derived it from an axiom, with nobody having checked this particular row
        relation.setEvidenceCode( GOEvidenceCode.IEA );
        // No experiment and no mask: this is a claim about a term, not about anything Gemma holds. The
        // read path lets it through the ACL clause untouched precisely because the column is null.
        relation.setExpressionExperiment( null );
        relation.setAclIsAuthenticatedAnonymouslyMask( 0 );
        relation.setGeneratedAt( generatedAt );
        return relation;
    }

    /**
     * Whether a URI belongs to the ontology being read rather than to something it merges in. CLO ships
     * the DOID, CL, UBERON and NCBITaxon classes it references; only its own classes are cell lines.
     *
     * <p>The prefix comes off the source rather than being built from its name and the OBO PURL: TGEMO
     * publishes under {@code gemma.msl.ubc.ca/ont/}, and the derived form silently matched none of it.</p>
     */
    private static boolean isOwnTerm( OntologyRelationSource source, String uri ) {
        return uri.startsWith( source.getNamespace() );
    }

    private static boolean isForeign( @Nullable String uri ) {
        String curie = OntologyXref.normalizeCurie( uri );
        if ( curie == null ) {
            return false;
        }
        int colon = curie.indexOf( ':' );
        return colon > 0 && FOREIGN_PREFIXES.contains( curie.substring( 0, colon ) );
    }

    /**
     * The label the translated term carries in the model Gemma actually loaded.
     *
     * <p>Null — and therefore a dropped row — when the term is absent or retired. Falling back to the
     * foreign resource's own label instead is tempting and wrong: the two vocabularies do not always
     * name a disease the same way, and a DOID label paired with a MONDO URI is a mismatch
     * {@code fixOntologyTermLabels} would later "correct" into something nobody asserted. A slim or
     * {@code -base} artifact makes this common, which is why every row carries its
     * {@code SOURCE_VERSION}.</p>
     */
    @Nullable
    private String labelOf( String uri, @Nullable ubic.gemma.core.ontology.providers.OntologyService ontology ) {
        if ( ontology == null ) {
            return null;
        }
        OntologyTerm term = ontology.getTerm( uri );
        return term != null && !term.isObsolete() ? term.getLabel() : null;
    }

    @Nullable
    private Taxon resolveTaxon( OntologyRelationSource source, Collection<OntologyRestriction> restrictions,
            Map<Integer, Taxon> cache ) {
        if ( taxonService == null ) {
            return null;
        }
        for ( OntologyRestriction restriction : restrictions ) {
            OntologyProperty property = restriction.getRestrictionOn();
            if ( property == null || property.getUri() == null
                    || source.getRelation( property.getUri() ) == null
                    || !( restriction instanceof OntologyClassRestriction ) ) {
                continue;
            }
            OntologyTerm target = ( ( OntologyClassRestriction ) restriction ).getRestrictedTo();
            if ( target == null || target.getUri() == null ) {
                continue;
            }
            Matcher m = NCBI_TAXON_URI.matcher( target.getUri() );
            if ( !m.matches() ) {
                continue;
            }
            int ncbiId;
            try {
                ncbiId = Integer.parseInt( m.group( 1 ) );
            } catch ( NumberFormatException e ) {
                continue;
            }
            if ( cache.containsKey( ncbiId ) ) {
                return cache.get( ncbiId );
            }
            Taxon taxon = null;
            try {
                taxon = taxonService.findByNcbiId( ncbiId );
            } catch ( RuntimeException e ) {
                log.warn( "Could not resolve NCBI taxon " + ncbiId + "; the relation is stored without one.", e );
            }
            cache.put( ncbiId, taxon );
            return taxon;
        }
        return null;
    }

    /**
     * The triple, for deduplication. Inheritance guarantees repeats: a restriction declared on a parent
     * class is returned again for every descendant, which is correct OWL and would otherwise be a
     * duplicate row.
     */
    private static String key( AnnotationRelation r ) {
        return r.getSubjectValueUri() + ' ' + r.getPredicateUri() + ' '
                + ( r.getObjectValueUri() != null ? r.getObjectValueUri() : r.getObjectValue() )
                + ' ' + r.getSource();
    }

    @Nullable
    private static String truncate( @Nullable String s, int max ) {
        if ( s == null ) {
            return null;
        }
        return s.length() <= max ? s : s.substring( 0, max );
    }

    /**
     * {@code Relation.terms.txt}, which is what Gemma sanctions as a predicate. Empty when the service
     * that owns it is not wired, in which case nothing is checked rather than everything being flagged.
     */
    private Set<String> sanctionedPredicateUris() {
        if ( ontologyService == null ) {
            return Collections.emptySet();
        }
        Set<String> uris = new HashSet<>();
        try {
            for ( OntologyProperty p : ontologyService.getRelationTerms() ) {
                if ( p.getUri() != null ) {
                    uris.add( p.getUri() );
                }
            }
        } catch ( RuntimeException e ) {
            log.warn( "Could not read the sanctioned relation vocabulary; predicates will not be checked.", e );
            return Collections.emptySet();
        }
        return uris;
    }

    // -----------------------------------------------------------------------------------------------
    // writing
    // -----------------------------------------------------------------------------------------------

    private int write( OntologyRelationSource source, List<AnnotationRelation> rows ) {
        Integer written = transactionTemplate.execute( status -> {
            int removed = annotationRelationDao.removeByBasis( AnnotationRelationBasis.ONTOLOGY, null,
                    source.getName() );
            annotationRelationDao.create( rows );
            log.info( "Removed {} and wrote {} ONTOLOGY relation rows for {}.", removed, rows.size(),
                    source.getName() );
            return rows.size();
        } );
        return written != null ? written : 0;
    }

    private OntologyXrefIndex buildXrefIndex( @Nullable ubic.gemma.core.ontology.providers.OntologyService mondo ) {
        if ( mondo == null ) {
            log.warn( "{} is not available, so no foreign identifier can be translated and every disease "
                            + "relation an ontology states in DOID will be dropped rather than stored as a DOID.",
                    XREF_SOURCE_TOKEN );
            return OntologyXrefIndex.empty();
        }
        StopWatch timer = StopWatch.createStarted();
        // 🛑 From the SOURCE, not the loaded model. A corpus-seeded slim holds the diseases Gemma
        // already annotates, which is precisely the wrong set for translating a foreign identifier --
        // the identifiers that fail are the ones for diseases we do NOT yet annotate, and those are
        // what the slim leaves out. Measured 2026-08-18: the slim yielded 32,594 cross-references
        // (DOID 3,111) against 145,917 (DOID 12,091) from the full artifact, and 980 CLO restrictions
        // became untranslatable as a result -- about 970 relations lost, one disease term accounting
        // for 172 of them.
        //
        // Reading the source is a plain-triples parse, lighter than the inference-mode model this
        // service already builds at boot, so it does not give back what slimming bought.
        OntologyXrefIndex index = OntologyXrefIndex.fromSource( mondo );
        log.info( "Inverted {} cross-references from the {} source in {} ms: {}.", index.size(),
                XREF_SOURCE_TOKEN, timer.getTime(), index.countsByPrefix() );
        return index;
    }

    private Optional<ubic.gemma.core.ontology.providers.OntologyService> loadedOntology( String token ) {
        return OntologyServiceResolver.resolve( ontologies, token )
                .filter( ubic.gemma.core.ontology.providers.OntologyService::isOntologyLoaded );
    }

    private List<OntologyRelationSource> select( @Nullable Collection<String> sources ) {
        if ( sources == null || sources.isEmpty() ) {
            return OntologyRelationSource.ALL;
        }
        List<OntologyRelationSource> selected = new ArrayList<>();
        for ( OntologyRelationSource source : OntologyRelationSource.ALL ) {
            for ( String s : sources ) {
                if ( s != null && source.getName().equalsIgnoreCase( s.trim() ) ) {
                    selected.add( source );
                    break;
                }
            }
        }
        return selected;
    }
}
