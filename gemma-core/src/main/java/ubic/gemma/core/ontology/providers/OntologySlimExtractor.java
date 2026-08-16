package ubic.gemma.core.ontology.providers;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Module-extract a slim subset of an OWL ontology around a given seed of term URIs.
 *
 * <p>Backs the {@code *Ontology-slim.owl} caches that {@link SlimmableOntologyService}
 * implementations consult on startup so the runtime Jena load drops from minutes (full
 * source) to seconds (slim subset). Used today by {@code ChebiOntologyService} and
 * {@code MondoOntologyService}; any future slimmable provider can reuse this class
 * unchanged.
 *
 * <p>Algorithm: OWL-API's {@link SyntacticLocalityModuleExtractor} with
 * {@link ModuleType#STAR}, which preserves entailments over the seed signature. STAR
 * is the same algorithm the ROBOT CLI exposes via its extract command; we use
 * OWL-API directly because ROBOT 1.9.x pins on Jena 3.17 and would collide with
 * Gemma's Jena 4.10.
 *
 * <p>STAR is correctness-preserving over OWL logic but it drops {@code subClassOf}
 * ancestors that don't logically constrain the seed (a chemical's is-a chemical-entity
 * adds no entailment if {@code chemical entity} has no further axioms). For a curator-
 * facing UI we want labels for every term on the breadcrumb to root, so the seed is
 * <em>expanded</em> before extraction: each seed term contributes its transitive
 * {@code subClassOf} ancestors and the chemical-side of every {@code RO:0000087 has_role}
 * axiom it carries (plus those targets' ancestors). STAR then runs over the expanded set.
 * The {@code has_role} unwrap is CHEBI-shaped — it's a no-op on ontologies that don't
 * use that relation (MONDO, GO, etc.).
 *
 * <p>This class is stateless and thread-safe — the OWL-API manager is created per call
 * so concurrent extractions don't share mutable cache state.
 */
@Component
public class OntologySlimExtractor {

    private static final Logger log = LoggerFactory.getLogger( OntologySlimExtractor.class );

    private static final String HAS_ROLE_IRI = "http://purl.obolibrary.org/obo/RO_0000087";

    /**
     * Extract a STAR module of {@code source} around {@code seedUris}, writing the
     * resulting OWL to {@code slimOut} as RDF/XML.
     *
     * @param source   the full source OWL file on disk (already downloaded + cached by
     *                 the upstream {@code OntologyLoader})
     * @param seedUris term URIs to anchor the slim around. The extractor adds
     *                 ancestors + axiom-closure terms automatically.
     * @param slimOut  destination file for the extracted slim. Parent directory must
     *                 exist. Overwrites if present.
     * @return summary of what was retained (term count, seed coverage)
     */
    public ExtractResult extract( File source, Collection<String> seedUris, File slimOut )
            throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
        return extract( source, seedUris, Collections.emptySet(), slimOut );
    }

    /**
     * As {@link #extract(File, Collection, File)}, but additionally seeds every class that bears
     * one of {@code roleUris} (or a descendant of one) via {@code RO:0000087 has_role}.
     *
     * <p>The corpus seed answers "terms we HAVE annotated". This answers "terms we plausibly WILL
     * annotate": seeding CHEBI's {@code drug} role pulls in the whole pharmacopoeia, including
     * compounds nobody has curated yet. Without it a curator searching for a drug new to the corpus
     * finds nothing, because a slim built only from usage can only ever return what was already
     * used — the search is unable to help you annotate anything for the first time.
     *
     * @param roleUris role classes whose bearers should be pulled in; empty for corpus-only
     */
    public ExtractResult extract( File source, Collection<String> seedUris,
            Collection<String> roleUris, File slimOut )
            throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
        Objects.requireNonNull( source, "source" );
        Objects.requireNonNull( seedUris, "seedUris" );
        Objects.requireNonNull( roleUris, "roleUris" );
        Objects.requireNonNull( slimOut, "slimOut" );
        if ( !source.isFile() ) {
            throw new IOException( "Source OWL does not exist: " + source );
        }
        if ( seedUris.isEmpty() ) {
            throw new IllegalArgumentException( "seedUris must not be empty" );
        }

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();

        log.info( "Loading source OWL from {} for slim extraction...", source );
        long loadStart = System.currentTimeMillis();
        OWLOntology fullOntology = manager.loadOntologyFromOntologyDocument( source );
        log.info( "Loaded {} axioms in {} ms.", fullOntology.getAxiomCount(),
                System.currentTimeMillis() - loadStart );

        Set<OWLEntity> signature = new HashSet<>();
        Set<String> coveredSeeds = new HashSet<>();
        for ( String uri : seedUris ) {
            OWLClass cls = df.getOWLClass( IRI.create( uri ) );
            // declared classes only — silently skip URIs the source ontology doesn't
            // know about (curator typos, deprecated terms not in the current release)
            if ( fullOntology.containsClassInSignature( cls.getIRI(), Imports.INCLUDED ) ) {
                signature.add( cls );
                coveredSeeds.add( uri );
            }
        }
        int missingSeedCount = seedUris.size() - coveredSeeds.size();
        if ( missingSeedCount > 0 ) {
            log.warn( "{} of {} seed URIs not found in the source ontology and will be skipped.",
                    missingSeedCount, seedUris.size() );
        }

        int directSeedCount = signature.size();
        int roleBearerCount = 0;
        if ( !roleUris.isEmpty() ) {
            roleBearerCount = addRoleBearers( signature, fullOntology, df, roleUris );
            log.info( "Added {} classes bearing one of {} roles (or their descendants).",
                    roleBearerCount, roleUris.size() );
        }
        expandSeedSignature( signature, fullOntology );
        log.info( "Expanded {} seed classes to {} via subClassOf + has_role closure.",
                directSeedCount, signature.size() );

        log.info( "Running STAR module extraction over {} seed classes...", signature.size() );
        long extractStart = System.currentTimeMillis();
        SyntacticLocalityModuleExtractor extractor =
                new SyntacticLocalityModuleExtractor( manager, fullOntology, ModuleType.STAR );
        OWLOntology slim = extractor.extractAsOntology( signature, IRI.create( slimOut.toURI() ) );
        long classCount = slim.getClassesInSignature().size();
        int axiomCount = slim.getAxiomCount();
        log.info( "STAR extraction returned {} classes / {} axioms in {} ms.",
                classCount, axiomCount, System.currentTimeMillis() - extractStart );

        // Must happen before the source is released below — the annotations are read off it.
        String sourceVersion = copyOntologyMetadata( fullOntology, slim, df );
        log.info( "Carried source ontology metadata onto the slim (version: {}).",
                sourceVersion != null ? sourceVersion : "none declared upstream" );

        // Release the source OWL-API representation BEFORE serialising the slim. This is
        // the critical memory step on real-size sources: the in-memory full ontology is
        // multi-GB and is unneeded once STAR has run. Without this the saveOntology
        // call below runs with both the source and the extracted module live in heap.
        manager.removeOntology( fullOntology );
        fullOntology = null;
        signature.clear();
        System.gc();

        log.info( "Writing slim to {} as RDF/XML...", slimOut );
        long writeStart = System.currentTimeMillis();
        slim.getOWLOntologyManager().saveOntology( slim, new RDFXMLDocumentFormat(),
                IRI.create( slimOut.toURI() ) );
        log.info( "Wrote {} axioms in {} ms.", slim.getAxiomCount(),
                System.currentTimeMillis() - writeStart );

        return new ExtractResult( coveredSeeds, missingSeedCount, axiomCount, classCount, sourceVersion );
    }

    /**
     * Copy the source's ontology-level metadata onto the extracted slim.
     *
     * <p>{@link SyntacticLocalityModuleExtractor#extractAsOntology} mints a fresh ontology whose
     * only identity is the output file path, so {@code owl:versionInfo}, {@code owl:versionIRI},
     * {@code dc:title} and {@code dc:description} are all left behind with the source. Those are
     * exactly the properties {@code AbstractOntologyService} scans for in
     * {@code getVersion()} / {@code getName()} / {@code getDescription()}, so a slimmed ontology
     * reported a null version — indistinguishable from an upstream that declares none. That made
     * "which CHEBI release is loaded?" unanswerable from a running instance; the only way to tell
     * was to read the cached source OWL's header on the deploy host.
     *
     * @return the version the source declared: {@code owl:versionInfo} when present (OBO
     *         ontologies put the release number or date there), else the version IRI, else
     *         {@code null}
     */
    private String copyOntologyMetadata( OWLOntology source, OWLOntology slim, OWLDataFactory df ) {
        OWLOntologyManager slimManager = slim.getOWLOntologyManager();
        String versionInfo = null;
        for ( OWLAnnotation ann : source.getAnnotations() ) {
            slimManager.applyChange( new AddOntologyAnnotation( slim, ann ) );
            if ( ann.getProperty().equals( df.getOWLVersionInfo() ) && ann.getValue() instanceof OWLLiteral ) {
                versionInfo = ( ( OWLLiteral ) ann.getValue() ).getLiteral();
            }
        }
        // versionIRI belongs to the ontology ID rather than the annotation set, so it needs a
        // SetOntologyID. Only the version is borrowed — the slim keeps its own ontology IRI,
        // which is the output file, so it never claims to BE the upstream release.
        var versionIri = source.getOntologyID().getVersionIRI();
        if ( versionIri.isPresent() ) {
            slimManager.applyChange( new SetOntologyID( slim,
                    new OWLOntologyID( slim.getOntologyID().getOntologyIRI(), versionIri ) ) );
        }
        if ( versionInfo != null ) {
            return versionInfo;
        }
        return versionIri.isPresent() ? versionIri.get().toString() : null;
    }

    /**
     * Add every class that bears one of {@code roleUris}, or any descendant of one, through
     * {@code has_role}.
     *
     * <p>Two passes over the source: close the role hierarchy downwards (so seeding {@code drug}
     * also catches {@code antineoplastic agent}, {@code analgesic}, …), then scan class axioms for
     * {@code has_role someValuesFrom R} with {@code R} in that closure. The scan is linear in the
     * ontology's axiom count and runs once per slim build, which is offline.
     *
     * @return the number of classes added to {@code signature}
     */
    private int addRoleBearers( Set<OWLEntity> signature, OWLOntology source, OWLDataFactory df,
            Collection<String> roleUris ) {
        // 1. descendants of the requested roles
        Set<OWLClass> roleClosure = new HashSet<>();
        Deque<OWLClass> work = new ArrayDeque<>();
        for ( String uri : roleUris ) {
            OWLClass r = df.getOWLClass( IRI.create( uri ) );
            if ( source.containsClassInSignature( r.getIRI(), Imports.INCLUDED ) ) {
                work.add( r );
            } else {
                log.warn( "Role {} is not declared in the source ontology; no bearers will be seeded for it.", uri );
            }
        }
        while ( !work.isEmpty() ) {
            OWLClass r = work.pop();
            if ( !roleClosure.add( r ) ) {
                continue;
            }
            for ( OWLSubClassOfAxiom ax : source.getSubClassAxiomsForSuperClass( r ) ) {
                OWLClassExpression sub = ax.getSubClass();
                if ( !sub.isAnonymous() ) {
                    work.add( sub.asOWLClass() );
                }
            }
        }
        // 2. classes asserting has_role onto anything in that closure
        int added = 0;
        for ( OWLClass cls : source.getClassesInSignature( Imports.INCLUDED ) ) {
            if ( signature.contains( cls ) ) {
                continue;
            }
            for ( OWLSubClassOfAxiom ax : source.getSubClassAxiomsForSubClass( cls ) ) {
                OWLClassExpression sup = ax.getSuperClass();
                if ( sup.isAnonymous()
                        && sup.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM ) {
                    OWLObjectSomeValuesFrom svf = ( OWLObjectSomeValuesFrom ) sup;
                    if ( !svf.getProperty().isAnonymous()
                            && svf.getProperty().asOWLObjectProperty().getIRI().toString().equals( HAS_ROLE_IRI )
                            && !svf.getFiller().isAnonymous()
                            && roleClosure.contains( svf.getFiller().asOWLClass() ) ) {
                        signature.add( cls );
                        added++;
                        break;
                    }
                }
            }
        }
        return added;
    }

    /**
     * Walk {@code subClassOf} and {@code has_role} from each input class, adding the
     * transitive ancestors and role targets in place. Bounded by a worklist over the
     * source ontology so loops in {@code subClassOf} (rare but possible) terminate.
     */
    private void expandSeedSignature( Set<OWLEntity> signature, OWLOntology source ) {
        Deque<OWLClass> work = new ArrayDeque<>();
        Set<OWLClass> visited = new HashSet<>();
        for ( OWLEntity e : signature ) {
            if ( e instanceof OWLClass ) {
                work.add( ( OWLClass ) e );
            }
        }

        while ( !work.isEmpty() ) {
            OWLClass cls = work.pop();
            if ( !visited.add( cls ) ) {
                continue;
            }
            signature.add( cls );
            for ( OWLSubClassOfAxiom ax : source.getSubClassAxiomsForSubClass( cls ) ) {
                OWLClassExpression sup = ax.getSuperClass();
                if ( !sup.isAnonymous() ) {
                    work.add( sup.asOWLClass() );
                } else if ( sup.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM ) {
                    OWLObjectSomeValuesFrom svf = ( OWLObjectSomeValuesFrom ) sup;
                    if ( !svf.getProperty().isAnonymous()
                            && svf.getProperty().asOWLObjectProperty().getIRI().toString().equals( HAS_ROLE_IRI )
                            && !svf.getFiller().isAnonymous() ) {
                        work.add( svf.getFiller().asOWLClass() );
                    }
                }
            }
        }
    }

    /**
     * Summary returned from {@link #extract(File, Collection, File)} for caller bookkeeping
     * (sidecar meta.json, log lines, regression tests).
     */
    public static final class ExtractResult {
        private final Set<String> coveredSeedUris;
        private final int missingSeedCount;
        private final int axiomCount;
        private final long classCount;
        @Nullable
        private final String sourceVersion;

        ExtractResult( Set<String> coveredSeedUris, int missingSeedCount, int axiomCount, long classCount,
                @Nullable String sourceVersion ) {
            this.coveredSeedUris = Set.copyOf( coveredSeedUris );
            this.missingSeedCount = missingSeedCount;
            this.axiomCount = axiomCount;
            this.classCount = classCount;
            this.sourceVersion = sourceVersion;
        }

        /**
         * The release the source ontology declared, or {@code null} when it declares none.
         *
         * @see #copyOntologyMetadata(OWLOntology, OWLOntology, OWLDataFactory)
         */
        @Nullable
        public String getSourceVersion() {
            return sourceVersion;
        }

        public Set<String> getCoveredSeedUris() {
            return coveredSeedUris;
        }

        public int getMissingSeedCount() {
            return missingSeedCount;
        }

        public int getAxiomCount() {
            return axiomCount;
        }

        public long getClassCount() {
            return classCount;
        }

        @Override
        public String toString() {
            return "ExtractResult{coveredSeeds=" + coveredSeedUris.size()
                    + ", missingSeeds=" + missingSeedCount
                    + ", classes=" + classCount
                    + ", axioms=" + axiomCount + "}";
        }
    }
}
