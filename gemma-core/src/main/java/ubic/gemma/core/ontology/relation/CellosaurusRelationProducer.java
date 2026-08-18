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
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.OntologyServiceResolver;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.common.description.AnnotationRelationStatus;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns Cellosaurus into {@link AnnotationRelationBasis#EXTERNAL} relations: which disease a cell
 * line's donor had, and which part of the body it came from.
 *
 * <p>This is the source that unblocks cell-line provenance. It was recorded as blocked on CLO
 * "asserting almost nothing", which is true — CLO states 345 donor diseases and 3 anatomic parts —
 * and the conclusion drawn from it was wrong: Cellosaurus states a disease for 81,041 lines and a
 * derived-from site for 142,374, and has been on disk the whole time.</p>
 *
 * <p><b>Two subjects per fact, deliberately.</b> A relation is stored once under the Cellosaurus
 * accession and once under each CLO or EFO term the record cross-references, so it is reachable from
 * whichever vocabulary a curator happened to annotate in. Neither key can be dropped: CLO is what
 * most existing annotations use, and Cellosaurus's whole value is the lines that are <b>not</b>
 * anywhere else — 111,863 of the 141,670 have no CLO or EFO term at all.</p>
 *
 * <p><b>Same predicates as CLO uses</b> ({@code CLO_0000015}, {@code CLO_0037208}), so where both
 * sources speak about one line the read groups them into a single corroborated relation instead of
 * two that happen to look alike.</p>
 *
 * <p>Species, cell-line type, donor sex and the misidentification flag are NOT read here. They are
 * already parsed by {@code CellosaurusOntologyService} and served on the term itself, and a relation's
 * subject is that term.</p>
 */
public class CellosaurusRelationProducer {

    private static final Logger log = LoggerFactory.getLogger( CellosaurusRelationProducer.class );

    /** As {@link AnnotationRelation#getSource()} spells it. */
    public static final String SOURCE = "CELLOSAURUS";

    private static final String CACHE_NAME = "cellosaurus";

    /** Cellosaurus's own resolvable URI form; the OBO PURL 404s. Matches CellosaurusOntologyService. */
    private static final String CVCL_URI_PREFIX = "https://www.cellosaurus.org/";
    private static final String OBO = "http://purl.obolibrary.org/obo/";
    private static final String EFO = "http://www.ebi.ac.uk/efo/";

    private static final String DERIVES_FROM_PATIENT_URI = OBO + "CLO_0000015";
    private static final String DERIVES_FROM_PATIENT_LABEL = "derives from patient having disease";
    private static final String DERIVES_FROM_PART_URI = OBO + "CLO_0037208";
    private static final String DERIVES_FROM_PART_LABEL = "derives from anatomic part";

    private static final String XREF_SOURCE_TOKEN = "MONDO";

    /** Where the anatomic-part identifiers point; consulted for their labels only. */
    private static final String SITE_SOURCE_TOKEN = "UBERON";

    /** Memo sentinel: this URI was looked up and the ontology had no label for it. */
    private static final String NO_LABEL = "";

    /**
     * The only species whose cell lines Gemma has any use for.
     *
     * <p>🛑 Worth knowing what this does and does not buy: it removes 4.6% of the rows, not the bulk.
     * Cellosaurus is overwhelmingly human — 121,440 of the relations — so this is a correctness filter
     * ("we do not curate zebrafish cell lines") and never a volume control.</p>
     *
     * <p>{@code 10116} is <i>Rattus norvegicus</i>, the laboratory rat. {@code 10117} is
     * <i>Rattus rattus</i>, the black rat, and is not included; the two differ by one character and a
     * wrong rat is far harder to notice than a wrong species.</p>
     */
    private static final Set<Integer> WANTED_TAXA =
            Collections.unmodifiableSet( new HashSet<>( Arrays.asList( 9606, 10090, 10116 ) ) );

    private static final int VALUE_MAX = 255;

    private final List<ubic.gemma.core.ontology.providers.OntologyService> ontologies;
    private final AnnotationRelationDao annotationRelationDao;
    private final TransactionTemplate transactionTemplate;
    @Nullable
    private final TaxonService taxonService;

    public CellosaurusRelationProducer( @Nullable List<ubic.gemma.core.ontology.providers.OntologyService> ontologies,
            AnnotationRelationDao annotationRelationDao, TransactionTemplate transactionTemplate,
            @Nullable TaxonService taxonService ) {
        this.ontologies = ontologies != null ? ontologies
                : Collections.<ubic.gemma.core.ontology.providers.OntologyService>emptyList();
        this.annotationRelationDao = annotationRelationDao;
        this.transactionTemplate = transactionTemplate;
        this.taxonService = taxonService;
    }

    public int produce() throws IOException {
        try ( InputStream is = CachedSource.open( CACHE_NAME ) ) {
            return produce( is );
        }
    }

    /**
     * Rebuild every Cellosaurus relation.
     *
     * <p>Rebuild rather than upsert and scoped to this source, for the reason every producer here is:
     * a statement Cellosaurus has since withdrawn must not outlive the release it came from, and no
     * other EXTERNAL source may be touched.</p>
     */
    public int produce( InputStream is ) throws IOException {
        StopWatch timer = StopWatch.createStarted();
        OntologyXrefIndex xrefs = OntologyXrefIndex.fromSource(
                OntologyServiceResolver.resolve( ontologies, XREF_SOURCE_TOKEN )
                        .filter( ubic.gemma.core.ontology.providers.OntologyService::isOntologyLoaded )
                        .orElse( null ) );
        if ( xrefs.isEmpty() ) {
            // Every disease here is an NCIt identifier. Without the translation there is no disease
            // relation to write, and an NCIt in a stored row is the thing the store forbids.
            log.warn( "{} is not loaded; no NCIt disease can be translated. Only the anatomic-part"
                    + " relations will be written.", XREF_SOURCE_TOKEN );
        }
        ubic.gemma.core.ontology.providers.OntologyService sites =
                OntologyServiceResolver.resolve( ontologies, SITE_SOURCE_TOKEN )
                        .filter( ubic.gemma.core.ontology.providers.OntologyService::isOntologyLoaded )
                        .orElse( null );
        if ( sites == null ) {
            // Not fatal the way an untranslatable NCIt is: the identifier is already UBERON's, so the
            // row is correct and only its object reads as Cellosaurus's sentence instead of the term.
            log.warn( "{} is not loaded; anatomic-part relations will carry the source's raw site"
                    + " description as the object's name.", SITE_SOURCE_TOKEN );
        }
        Map<String, String> siteLabels = new HashMap<>();
        Date generatedAt = new Date();
        Map<Integer, Taxon> taxaByNcbiId = new HashMap<>();
        Reading reading = new Reading();
        List<AnnotationRelation> rows = new ArrayList<>();

        CellosaurusRelationReport.parse( is, e -> {
            reading.records++;
            if ( e.getNcbiTaxonId() == null || !WANTED_TAXA.contains( e.getNcbiTaxonId() ) ) {
                reading.wrongSpecies++;
                return;
            }
            Taxon taxon = resolveTaxon( e.getNcbiTaxonId(), taxaByNcbiId );
            for ( String subjectUri : subjectsOf( e ) ) {
                if ( e.getDiseaseCurie() != null ) {
                    addDisease( rows, e, subjectUri, taxon, xrefs, generatedAt, reading );
                }
                if ( e.getSiteUri() != null ) {
                    rows.add( build( e, subjectUri, DERIVES_FROM_PART_URI, DERIVES_FROM_PART_LABEL,
                            e.getSiteUri(), siteLabel( sites, e, siteLabels, reading ),
                            Categories.ORGANISM_PART, taxon,
                            e.getSiteDescription(), generatedAt ) );
                    reading.site++;
                }
            }
        } );

        log.info( "Read {} Cellosaurus relations ({} disease, {} anatomic part) from {} records in {} ms.{}",
                rows.size(), reading.disease, reading.site, reading.records, timer.getTime(), reading.report() );

        Integer written = transactionTemplate.execute( status -> {
            int removed = annotationRelationDao.removeByBasis( AnnotationRelationBasis.EXTERNAL, null, SOURCE );
            if ( !rows.isEmpty() ) {
                annotationRelationDao.create( rows );
            }
            log.info( "Removed {} and wrote {} EXTERNAL relation rows for {}.", removed, rows.size(), SOURCE );
            return rows.size();
        } );
        return written != null ? written : 0;
    }

    /**
     * The accession, plus every CLO or EFO term the record cross-references.
     *
     * <p>🛑 Both, always. Dropping the aliases would make a fact unreachable from the vocabulary most
     * existing annotations use; dropping the accession would discard the lines that exist nowhere else,
     * which is the reason Cellosaurus is loaded at all.</p>
     */
    private static List<String> subjectsOf( CellosaurusRelationReport.Entry e ) {
        List<String> out = new ArrayList<>( 1 + e.getAliasLocalNames().size() );
        out.add( CVCL_URI_PREFIX + e.getId() );
        for ( String localName : e.getAliasLocalNames() ) {
            out.add( localName.startsWith( "EFO_" ) ? EFO + localName : OBO + localName );
        }
        return out;
    }

    private void addDisease( List<AnnotationRelation> rows, CellosaurusRelationReport.Entry e,
            String subjectUri, @Nullable Taxon taxon, OntologyXrefIndex xrefs, Date generatedAt,
            Reading reading ) {
        Set<String> translated = xrefs.resolve( e.getDiseaseCurie() );
        if ( translated.isEmpty() ) {
            reading.untranslatable++;
            reading.unresolved.merge( e.getDiseaseCurie(), 1, Integer::sum );
            return;
        }
        for ( String mondoUri : translated ) {
            String label = xrefs.labelOf( mondoUri );
            if ( label == null ) {
                reading.unlabelled++;
                continue;
            }
            rows.add( build( e, subjectUri, DERIVES_FROM_PATIENT_URI, DERIVES_FROM_PATIENT_LABEL,
                    mondoUri, label, Categories.DISEASE, taxon,
                    // what Cellosaurus called it, so a curator can see what was translated
                    e.getDiseaseLabel() != null ? e.getDiseaseCurie() + " " + e.getDiseaseLabel()
                            : e.getDiseaseCurie(),
                    generatedAt ) );
            reading.disease++;
        }
    }

    private AnnotationRelation build( CellosaurusRelationReport.Entry e, String subjectUri, String predicateUri,
            String predicateLabel, String objectUri, String objectLabel, Category objectCategory,
            @Nullable Taxon taxon, @Nullable String evidence, Date generatedAt ) {
        AnnotationRelation r = new AnnotationRelation();
        // Cellosaurus's own name on every row, including the CLO-keyed ones: Cellosaurus is the source
        // making the statement, and the URI beside it is what identifies the term.
        r.setSubjectValue( truncate( e.getName() ) );
        r.setSubjectValueUri( subjectUri );
        Category subject = OntologyRelationSource.CELL_LINE;
        r.setSubjectCategory( subject.getCategory() );
        r.setSubjectCategoryUri( subject.getCategoryUri() );
        r.setPredicate( predicateLabel );
        r.setPredicateUri( predicateUri );
        r.setObjectValue( truncate( objectLabel ) );
        r.setObjectValueUri( objectUri );
        r.setObjectCategory( objectCategory.getCategory() );
        r.setObjectCategoryUri( objectCategory.getCategoryUri() );
        r.setTaxon( taxon );
        r.setBasis( AnnotationRelationBasis.EXTERNAL );
        r.setStatus( AnnotationRelationStatus.ASSERTED );
        r.setSource( SOURCE );
        // IIA rather than TAS: Cellosaurus states these without citing, per relation, what they rest on
        r.setEvidenceCode( GOEvidenceCode.IIA );
        r.setEvidence( evidence != null ? truncate( evidence ) : null );
        r.setGeneratedAt( generatedAt );
        return r;
    }

    /**
     * The object's name is the term's label; Cellosaurus's own sentence stays in {@code evidence}.
     *
     * <p>The file gives an identifier and a free-text site description, and the description used to
     * double as the label. That put a string no ontology contains where a curator reads the term's
     * name — {@code In situ; Lung}, {@code Metastatic; Pleural effusion}, {@code In situ; Uterus,
     * cervix} — while {@code evidence} carried a verbatim copy of the same string, so the row stated
     * the raw field twice and the term's name never. The disease branch had always resolved its
     * label; this is that branch's behaviour, applied here.</p>
     *
     * <p>🛑 The raw string is not redundant and is not dropped. {@code In situ; Fetal kidney} grounds
     * to plain {@code kidney}, and {@code Metastatic; Pleural effusion} to the same URI as
     * {@code In situ; Pleural effusion} — developmental stage, and whether the sample was primary or
     * metastatic, survive only in that sentence. It is evidence, and it stays in {@code evidence}.</p>
     *
     * <p>A side effect worth expecting: object breadth is counted over the stored rows, so the three
     * spellings of UBERON_0002048 that used to be counted apart ({@code In situ; Lung} 2843,
     * {@code Metastatic; Lung} 164, the bronchoalveolar-lavage note 4) now collapse into one number.
     * Any threshold calibrated against the split counts is reading a different scale afterwards.</p>
     *
     * <p>Falls back to the old behaviour when UBERON is not loaded or has no label for the
     * identifier: a sentence in the object field is worse than a term name and better than nothing,
     * and the count of fallbacks is reported at the end of the run.</p>
     */
    private static String siteLabel( @Nullable ubic.gemma.core.ontology.providers.OntologyService sites,
            CellosaurusRelationReport.Entry e, Map<String, String> memo, Reading reading ) {
        String label = memo.computeIfAbsent( e.getSiteUri(), uri -> {
            String resolved = lookUpLabel( sites, uri );
            // Cache the miss too — an unresolvable URI recurs across thousands of records, and
            // computeIfAbsent would re-ask the ontology for every one of them.
            return resolved != null && !resolved.isEmpty() ? resolved : NO_LABEL;
        } );
        if ( !NO_LABEL.equals( label ) ) {
            return label;
        }
        reading.unlabelledSite++;
        return lastSegmentLabel( e );
    }

    @Nullable
    private static String lookUpLabel( @Nullable ubic.gemma.core.ontology.providers.OntologyService sites,
            String uri ) {
        if ( sites == null ) {
            return null;
        }
        try {
            OntologyTerm term = sites.getTerm( uri );
            return term != null ? term.getLabel() : null;
        } catch ( Exception ex ) {
            // One unreadable term must not abandon 142k rows.
            log.warn( "Failed to resolve a label for {} from {}: {}", uri, SITE_SOURCE_TOKEN, ex.getMessage() );
            return null;
        }
    }

    /**
     * Last resort when the site identifier resolves to no label: the source's sentence if it has one,
     * else the identifier's last segment.
     */
    private static String lastSegmentLabel( CellosaurusRelationReport.Entry e ) {
        if ( e.getSiteDescription() != null ) {
            return e.getSiteDescription();
        }
        String uri = e.getSiteUri();
        int cut = uri.lastIndexOf( '/' );
        return cut >= 0 ? uri.substring( cut + 1 ) : uri;
    }

    @Nullable
    private Taxon resolveTaxon( int ncbiId, Map<Integer, Taxon> cache ) {
        if ( taxonService == null ) {
            return null;
        }
        return cache.computeIfAbsent( ncbiId, taxonService::findByNcbiId );
    }

    private static String truncate( String s ) {
        return s.length() <= VALUE_MAX ? s : s.substring( 0, VALUE_MAX );
    }

    private static class Reading {
        private int records = 0;
        private int disease = 0;
        private int site = 0;
        private int wrongSpecies = 0;
        private int untranslatable = 0;
        private int unlabelled = 0;
        private int unlabelledSite = 0;
        private final Map<String, Integer> unresolved = new HashMap<>();

        private String report() {
            StringBuilder sb = new StringBuilder();
            sb.append( "\nskipped\tnot human/mouse/rat\t" ).append( wrongSpecies )
                    .append( "\tuntranslatable NCIt\t" ).append( untranslatable )
                    .append( "\ttranslated but unnamed\t" ).append( unlabelled )
                    .append( "\tsite kept its raw description\t" ).append( unlabelledSite );
            if ( !unresolved.isEmpty() ) {
                sb.append( "\nNCIt terms MONDO does not cross-reference (" ).append( unresolved.size() )
                        .append( "), most affected:\n" );
                unresolved.entrySet().stream()
                        .sorted( ( a, b ) -> Integer.compare( b.getValue(), a.getValue() ) )
                        .limit( 15 )
                        .forEach( u -> sb.append( '\t' ).append( u.getKey() ).append( '\t' )
                                .append( u.getValue() ).append( '\n' ) );
            }
            return sb.toString();
        }
    }
}
