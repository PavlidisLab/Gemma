/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.ontology.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndex;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.RDFS;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.FSDirectory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Jena-text 4.10 / Lucene 9 implementation of {@link OntologySearchService}.
 *
 * <p><b>Origin.</b> This class is the Gemma-side replacement for baseCode's
 * pre-renovations {@code OntologyIndexer}/{@code LuceneSearchIndex} (baseCode
 * commit {@code a7e7112^}). The original built a Lucene-3-era on-disk index
 * per ontology from an in-memory Jena {@code OntModel}; that path is
 * unrecoverable on Lucene 9 (API breaks every major) and baseCode 1.1.34's
 * {@code OntologyIndexer} is now a stub. The reimplementation pulled into
 * Gemma (see {@code SEARCH_RECCE.md} Section 6) uses Jena's
 * {@code jena-text} module to keep a Lucene index in sync with the
 * unified-ontology TDB managed by
 * {@link ubic.gemma.core.ontology.OntologyConfig#unifiedOntologyService}.
 *
 * <p><b>Lifecycle.</b> Constructed by {@link ubic.gemma.core.ontology.OntologyConfig}
 * given the unified-TDB directory. The Lucene index is stored under
 * {@code <tdbDir>/.text/} alongside the TDB data. The dataset is opened
 * read-write so that future TDB load operations are auto-indexed by
 * jena-text's {@code TextDocProducer} listener; reads use a short
 * {@link ReadWrite#READ} transaction. The dataset is closed when the Spring
 * context shuts down.
 *
 * <p><b>Indexed fields.</b> The {@link EntityDefinition} mirrors the property
 * set baseCode's {@code OntologyIndexer.DEFAULT_INDEXABLE_PROPERTIES} used
 * pre-strip: {@code rdfs:label}, OBO synonym variants, and a couple of OBO
 * id-bearing predicates. Queries are forwarded to {@code text:query} as
 * Lucene query strings; callers must escape user input via
 * {@link #escape(String)}.
 *
 * <p><b>Scope.</b> Only the unified TDB is wired. Per-ontology
 * {@code UrlOntologyService}-backed providers (GO, MONDO via baseCode's
 * URL loader, etc.) load their OWL into in-memory OntModels that baseCode
 * does not expose, so those ontologies are NOT searchable through this
 * service yet. See {@code SEARCH_RECCE.md} Section 6 for the architectural
 * gap and the path to closing it (either baseCode gains a public OntModel
 * accessor, or Gemma reimplements its own URL-based loader).
 */
public class JenaTextOntologySearchService implements OntologySearchService {

    private static final Log log = LogFactory.getLog( JenaTextOntologySearchService.class );

    /**
     * Entity field name used in the jena-text index. Surfaced in SPARQL
     * results as the {@code ?s} binding of the {@code text:query} match.
     */
    private static final String ENTITY_FIELD = "uri";

    /** Indexed field name for rdfs:label values. Matches the SPARQL projector below. */
    private static final String LABEL_FIELD = "label";

    /**
     * Per-property field names indexed alongside the label. Each name is
     * arbitrary inside jena-text; we keep them human-readable for debugging.
     */
    private static final String[][] EXTRA_FIELDS_AND_PREDICATES = {
            // OBO synonyms — see baseCode IndexableProperty set
            { "syn_exact", "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym" },
            { "syn_broad", "http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym" },
            { "syn_narrow", "http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym" },
            { "syn_related", "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym" },
            { "syn_generic", "http://www.geneontology.org/formats/oboInOwl#hasSynonym" },
            { "obo_id", "http://www.geneontology.org/formats/oboInOwl#id" },
            { "obo_xref", "http://www.geneontology.org/formats/oboInOwl#hasDbXref" },
            { "alt_label", "http://purl.obolibrary.org/obo/IAO_0000118" },
    };

    @Nullable
    private final Dataset textDataset;

    @Nullable
    private final Path tdbDir;

    private final boolean enabled;

    /**
     * Construct a search service backed by the TDB at {@code tdbDir}. If
     * {@code enabled} is false (typically {@code gemma.ontology.unified.enabled=false})
     * the service is constructed in disabled mode and all searches return empty.
     *
     * <p>The Lucene index is stored under {@code <tdbDir>/.text/}.
     */
    public JenaTextOntologySearchService( @Nullable Path tdbDir, boolean enabled ) {
        this.tdbDir = tdbDir;
        this.enabled = enabled;
        if ( !enabled || tdbDir == null ) {
            this.textDataset = null;
            if ( tdbDir == null ) {
                log.info( "JenaTextOntologySearchService: no TDB directory configured; service will return empty results." );
            } else {
                log.info( "JenaTextOntologySearchService: disabled by configuration; service will return empty results." );
            }
            return;
        }
        try {
            Files.createDirectories( tdbDir );
            Path indexDir = tdbDir.resolve( ".text" );
            Files.createDirectories( indexDir );
            Dataset baseDataset = TDB2Factory.connectDataset( tdbDir.toString() );
            EntityDefinition entityDef = buildEntityDefinition();
            // FSDirectory is owned by the TextIndex for the life of the
            // service; do not close it eagerly. close() on the dataset cascades.
            FSDirectory luceneDir = FSDirectory.open( indexDir );
            TextIndex textIndex = TextDatasetFactory.createLuceneIndex(
                    luceneDir,
                    entityDef,
                    new StandardAnalyzer()
            );
            this.textDataset = TextDatasetFactory.create( baseDataset, textIndex, true );
            log.info( "JenaTextOntologySearchService initialised against TDB=" + tdbDir + ", index=" + indexDir );
        } catch ( IOException e ) {
            throw new UncheckedIOException( "Failed to initialise JenaTextOntologySearchService at " + tdbDir, e );
        }
    }

    private static EntityDefinition buildEntityDefinition() {
        EntityDefinition def = new EntityDefinition( ENTITY_FIELD, LABEL_FIELD, RDFS.label.asNode() );
        for ( String[] pair : EXTRA_FIELDS_AND_PREDICATES ) {
            def.set( pair[0], org.apache.jena.graph.NodeFactory.createURI( pair[1] ) );
        }
        return def;
    }

    @Override
    public List<Result> search( String query, int maxResults ) {
        if ( textDataset == null ) {
            return Collections.emptyList();
        }
        if ( query == null || query.trim().isEmpty() ) {
            throw new IllegalArgumentException( "Query cannot be blank" );
        }
        if ( maxResults <= 0 ) {
            throw new IllegalArgumentException( "maxResults must be > 0" );
        }

        // SPARQL projector. text:query returns (subject, score) bindings; we
        // then OPTIONAL-fetch rdfs:label for display. The dataset is mixed
        // (the text-index sits over the TDB graph) so subject ?s is a real
        // Resource on the default graph.
        String sparql = ""
                + "PREFIX text: <http://jena.apache.org/text#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT ?s ?score ?label WHERE {\n"
                + "  (?s ?score) text:query ( '" + escapeForSparql( query ) + "' " + maxResults + " ) .\n"
                + "  OPTIONAL { ?s rdfs:label ?label }\n"
                + "}\n"
                + "LIMIT " + maxResults;

        Query parsed = QueryFactory.create( sparql );
        textDataset.begin( ReadWrite.READ );
        try ( QueryExecution qexec = QueryExecutionFactory.create( parsed, textDataset ) ) {
            ResultSet rs = qexec.execSelect();
            List<Result> out = new ArrayList<>();
            while ( rs.hasNext() ) {
                QuerySolution row = rs.nextSolution();
                RDFNode s = row.get( "s" );
                if ( s == null || !s.isResource() ) {
                    continue;
                }
                Resource res = s.asResource();
                String label = null;
                RDFNode lblNode = row.get( "label" );
                if ( lblNode != null && lblNode.isLiteral() ) {
                    label = ( ( Literal ) lblNode ).getString();
                }
                double score = 0.0;
                RDFNode scoreNode = row.get( "score" );
                if ( scoreNode != null && scoreNode.isLiteral() ) {
                    try {
                        score = ( ( Literal ) scoreNode ).getDouble();
                    } catch ( Exception ignored ) {
                        // jena-text emits xsd:float; defensive
                    }
                }
                out.add( new Result( res, label, score ) );
            }
            return out;
        } finally {
            textDataset.end();
        }
    }

    @Override
    public String escape( String query ) {
        if ( query == null ) {
            return "";
        }
        // Equivalent to QueryParserBase.escape — escape all Lucene 9
        // query-parser metacharacters with a leading backslash.
        StringBuilder sb = new StringBuilder( query.length() + 8 );
        for ( int i = 0; i < query.length(); i++ ) {
            char c = query.charAt( i );
            if ( c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')'
                    || c == ':' || c == '^' || c == '[' || c == ']' || c == '"'
                    || c == '{' || c == '}' || c == '~' || c == '*' || c == '?'
                    || c == '|' || c == '&' || c == '/' ) {
                sb.append( '\\' );
            }
            sb.append( c );
        }
        return sb.toString();
    }

    @Override
    public boolean isReady() {
        return textDataset != null && enabled;
    }

    /**
     * Escape characters that would break the embedded SPARQL literal
     * containing the Lucene query: backslash and single-quote. The Lucene
     * metacharacter escaping is the caller's responsibility (use
     * {@link #escape(String)}).
     */
    private static String escapeForSparql( String s ) {
        StringBuilder sb = new StringBuilder( s.length() + 4 );
        for ( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );
            if ( c == '\\' || c == '\'' ) {
                sb.append( '\\' );
            }
            sb.append( c );
        }
        return sb.toString();
    }

    /**
     * Best-effort cleanup of the underlying dataset. Invoked by Spring on
     * context shutdown via the {@code destroyMethod} declared in
     * {@link ubic.gemma.core.ontology.OntologyConfig}.
     */
    public void close() {
        if ( textDataset != null ) {
            try {
                textDataset.close();
            } catch ( Exception e ) {
                log.warn( "Failed to close text dataset: " + e.getMessage() );
            }
        }
    }
}
