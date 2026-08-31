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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns MGI's genotype-to-disease reports into {@link AnnotationRelationBasis#EXTERNAL} relations:
 * which mutant alleles MGI's curators say model which diseases, and which they say do not.
 *
 * <p>This is the source the store was designed for and never had. Gemma's own corpus can only attest
 * a disease-model relation where somebody happened to curate both ends onto one experiment; MGI has
 * been recording the relation itself, with citations, for decades. Measured 2026-08-18: 4,124 asserted
 * (allele, disease) pairs over 1,749 diseases, and 161 refuted ones.</p>
 *
 * <p><b>The refutations are the unusual part.</b> {@code MGI_Geno_NotDiseaseDO.rpt} says a genotype
 * does <i>not</i> model a disease — curated, cited disconfirmation, which nothing else in Gemma can
 * produce and which no amount of co-occurrence could ever contradict. They are stored
 * {@link AnnotationRelationStatus#REFUTED}, which keeps them out of every read that asks what is
 * supported and out of every inference.</p>
 *
 * <h3>Three reports, one precedence order</h3>
 *
 * <p>{@code MGI_DiseaseMouseModel.rpt} is the disease-first view of the same curation and is the
 * larger of the two sources: 6,299 asserted (allele, DOID) pairs against the genotype report's 4,516,
 * 3,331 of them in neither genotype report, over 2,770 alleles the genotype reports never name
 * (measured 2026-08-30). It carries its refutations in a {@code NOT} column rather than a separate
 * file, and it has no PubMed column at all, so every statement it contributes is
 * {@link GOEvidenceCode#IIA}.</p>
 *
 * <p>The three reports overlap and contradict each other, so a pair is written once under one rule,
 * applied by reading in this order into a shared first-wins set:</p>
 *
 * <ol>
 * <li>{@code MGI_Geno_NotDiseaseDO.rpt} — refuted, cited</li>
 * <li>{@code MGI_DiseaseMouseModel.rpt} {@code NOT} rows — refuted, uncited</li>
 * <li>{@code MGI_Geno_DiseaseDO.rpt} — asserted, cited</li>
 * <li>{@code MGI_DiseaseMouseModel.rpt} model rows — asserted, uncited</li>
 * </ol>
 *
 * <p><b>Refutation outranks assertion; within one status the cited report outranks the uncited one.</b>
 * Both halves earn their place. A refutation is a curator saying no in as many words and is the one
 * thing this store cannot reconstruct from anything else, so it is not overwritten by an assertion
 * MGI also publishes — 15 pairs are {@code NOT} here and asserted in the genotype report, 20 the other
 * way round, and 60 are stated both ways inside the disease report alone, at different allele pairs or
 * strain backgrounds. Within a status the tie-break keeps the row that names its papers: the disease
 * report cannot cite, so reading it last is what stops 2,950 already-cited {@code TAS} statements
 * being replaced by an {@code IIA} copy of themselves.</p>
 *
 * @see MgiDiseaseModelReport for the genotype reports' shape and why rows are not relations
 * @see MgiDiseaseMouseModelReport for the disease report's shape and its missing citation column
 */
public class MgiRelationProducer {

    private static final Logger log = LoggerFactory.getLogger( MgiRelationProducer.class );

    /** As {@link AnnotationRelation#getSource()} spells it. */
    public static final String SOURCE = "MGI";

    /**
     * RO_0003301. The constant name keeps the colloquial "is model of" because that is what
     * everyone here calls the relation; the VALUE is RO's own {@code rdfs:label}, which is what
     * {@code Relation.terms.txt} sanctions and therefore what the commit validator compares
     * against. Do not "fix" the value back to the readable spelling — that is a 400.
     */
    private static final String IS_MODEL_OF_URI = "http://purl.obolibrary.org/obo/RO_0003301";
    private static final String IS_MODEL_OF_LABEL = "has role in modeling";

    /** MGI ships mouse genetics; the reports carry no taxon column. */
    private static final int MOUSE_NCBI_TAXON_ID = 10090;

    /**
     * The allele URI space. MGI's OBO PURL form 404s, so the canonical resolvable one is minted here,
     * matching what {@code MgiStrainOntologyService} does for strains.
     */
    private static final String ALLELE_URI_PREFIX = "https://www.informatics.jax.org/allele/";

    /** The ontology whose cross-references translate {@code DOID:} into something Gemma annotates in. */
    private static final String XREF_SOURCE_TOKEN = "MONDO";

    /**
     * The ontology whose classes cross-reference these alleles, so a fact MGI states about an allele is
     * also stored against the term the corpus actually annotates.
     */
    private static final String BRIDGE_SOURCE_TOKEN = "TGEMO";

    private static final int VALUE_MAX = 255;

    private final List<ubic.gemma.core.ontology.providers.OntologyService> ontologies;
    private final AnnotationRelationDao annotationRelationDao;
    private final TransactionTemplate transactionTemplate;
    @Nullable
    private final TaxonService taxonService;

    public MgiRelationProducer( @Nullable List<ubic.gemma.core.ontology.providers.OntologyService> ontologies,
            AnnotationRelationDao annotationRelationDao, TransactionTemplate transactionTemplate,
            @Nullable TaxonService taxonService ) {
        this.ontologies = ontologies != null ? ontologies
                : Collections.<ubic.gemma.core.ontology.providers.OntologyService>emptyList();
        this.annotationRelationDao = annotationRelationDao;
        this.transactionTemplate = transactionTemplate;
        this.taxonService = taxonService;
    }

    /** Config keys and disk-cache names for the three reports. */
    private static final String ASSERTED_CACHE = "mgiGenoDisease";
    private static final String REFUTED_CACHE = "mgiGenoNotDisease";
    private static final String MOUSE_MODEL_CACHE = "mgiDiseaseMouseModel";

    /** What each report is called where a tally or a log line has to name it. */
    private static final String ASSERTED_REPORT = "MGI_Geno_DiseaseDO.rpt";
    private static final String REFUTED_REPORT = "MGI_Geno_NotDiseaseDO.rpt";
    private static final String MOUSE_MODEL_REPORT = "MGI_DiseaseMouseModel.rpt";

    /**
     * Rebuild from the reports as configured, fetching and caching them the way the lexical ontology
     * services do.
     *
     * <p>Only the genotype positives are required. The other two are logged and skipped if they cannot
     * be fetched, because losing one report's contribution is worse than losing all three — and the
     * log line is the only place their absence is visible, since a missing statement and a statement
     * MGI never made look identical at the table.</p>
     *
     * @return how many relation rows were written
     */
    public int produce() throws IOException {
        try ( InputStream asserted = CachedSource.open( ASSERTED_CACHE ) ) {
            InputStream refuted = open( REFUTED_CACHE, REFUTED_REPORT, "~174 refutations" );
            try {
                InputStream mouseModel = open( MOUSE_MODEL_CACHE, MOUSE_MODEL_REPORT,
                        "~6,300 statements over ~5,500 alleles, most of them named nowhere else" );
                try {
                    return produce( asserted, refuted, mouseModel );
                } finally {
                    if ( mouseModel != null ) {
                        mouseModel.close();
                    }
                }
            } finally {
                if ( refuted != null ) {
                    refuted.close();
                }
            }
        }
    }

    @Nullable
    private InputStream open( String cacheName, String report, String whatIsLost ) {
        try {
            return CachedSource.open( cacheName );
        } catch ( IOException e ) {
            log.warn( "Could not read {}; the {} it carries will be absent and nothing at the table will"
                    + " mark their absence.", report, whatIsLost, e );
            return null;
        }
    }

    /**
     * Rebuild every MGI relation from the two genotype reports, without the disease report.
     *
     * @see #produce(InputStream, InputStream, InputStream)
     */
    public int produce( InputStream asserted, @Nullable InputStream refuted ) throws IOException {
        return produce( asserted, refuted, null );
    }

    /**
     * Rebuild every MGI relation from the three reports.
     *
     * <p>Rebuild rather than upsert, and scoped to this source: an upsert can only correct rows the new
     * read still produces, so a statement MGI has since withdrawn would outlive the report it came
     * from. Narrowing the delete to {@code SOURCE = 'MGI'} leaves every other EXTERNAL source alone.</p>
     *
     * @param asserted   the {@code MGI_Geno_DiseaseDO.rpt} stream
     * @param refuted    the {@code MGI_Geno_NotDiseaseDO.rpt} stream, or null to skip the negatives
     * @param mouseModel the {@code MGI_DiseaseMouseModel.rpt} stream, or null to skip it
     * @return how many relation rows were written
     */
    public int produce( InputStream asserted, @Nullable InputStream refuted,
            @Nullable InputStream mouseModel ) throws IOException {
        StopWatch timer = StopWatch.createStarted();
        OntologyXrefIndex xrefs = OntologyXrefIndex.fromSource(
                OntologyServiceResolver.resolve( ontologies, XREF_SOURCE_TOKEN )
                        .filter( ubic.gemma.core.ontology.providers.OntologyService::isOntologyLoaded )
                        .orElse( null ) );
        if ( xrefs.isEmpty() ) {
            // Every object in these reports is a DOID, so with nothing to translate through there is
            // no relation to write -- and writing DOIDs raw is the one thing the store forbids.
            log.warn( "{} is not loaded, so no DOID can be translated and no MGI relation can be stored.",
                    XREF_SOURCE_TOKEN );
            return 0;
        }
        // 🛑 The second subject key, and the reason these rows are reachable at all. Every relation
        // here is keyed on an MGI allele URI, and NO corpus annotation uses one -- measured on prod
        // 2026-08-30, both the allele and strain MGI namespaces appear zero times in EE2C. TGEMO's
        // classes are what datasets are annotated with (6,490 of them), and TGEMO cross-references the
        // alleles, so the same fact is stored a second time under the TGEMO term. Same shape as
        // CellosaurusRelationProducer's two subjects per fact. An absent or unloaded TGEMO costs the
        // bridge, not the run.
        OntologyXrefIndex bridge = OntologyXrefIndex.fromSource(
                OntologyServiceResolver.resolve( ontologies, BRIDGE_SOURCE_TOKEN )
                        .filter( ubic.gemma.core.ontology.providers.OntologyService::isOntologyLoaded )
                        .orElse( null ) );
        if ( bridge.isEmpty() ) {
            log.warn( "{} is not loaded, so MGI relations are stored only under their allele URIs,"
                    + " which no corpus annotation uses.", BRIDGE_SOURCE_TOKEN );
        }

        Date generatedAt = new Date();
        Taxon mouse = resolveMouse();

        // 🛑 ONE set across all three reports, so first-wins really is first-wins. It used to be
        // per-file, which meant the 5 pairs the two genotype reports state both ways were written
        // TWICE -- once ASSERTED and once REFUTED, from the same source, with nothing to choose
        // between them. Sharing it is what turns the read order below into a precedence rule.
        Set<String> seen = new LinkedHashSet<>();
        List<MgiDiseaseMouseModelReport.Entry> models = mouseModel != null
                ? new ArrayList<>( MgiDiseaseMouseModelReport.parse( mouseModel ) )
                : Collections.emptyList();

        Reading reading = new Reading();
        List<AnnotationRelation> rows = new ArrayList<>();
        // The precedence order; see the class note. Refutation before assertion, cited before uncited.
        if ( refuted != null ) {
            rows.addAll( read( MgiDiseaseModelReport.parse( refuted ), AnnotationRelationStatus.REFUTED,
                    xrefs, bridge, mouse, generatedAt, seen, reading.of( REFUTED_REPORT ) ) );
        }
        rows.addAll( read( filter( models, true ), AnnotationRelationStatus.REFUTED,
                xrefs, bridge, mouse, generatedAt, seen, reading.of( MOUSE_MODEL_REPORT ) ) );
        rows.addAll( read( MgiDiseaseModelReport.parse( asserted ), AnnotationRelationStatus.ASSERTED,
                xrefs, bridge, mouse, generatedAt, seen, reading.of( ASSERTED_REPORT ) ) );
        rows.addAll( read( filter( models, false ), AnnotationRelationStatus.ASSERTED,
                xrefs, bridge, mouse, generatedAt, seen, reading.of( MOUSE_MODEL_REPORT ) ) );

        log.info( "Read {} MGI relations ({} asserted, {} refuted) from {} statements in {} ms.{}",
                rows.size(), reading.asserted(), reading.refuted(), reading.statements(), timer.getTime(),
                reading.report() );

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

    /** The {@code NOT}-model half of the disease report, or the model half. */
    private static List<MgiDiseaseModelReport.Entry> filter( List<MgiDiseaseMouseModelReport.Entry> entries,
            boolean notModel ) {
        List<MgiDiseaseModelReport.Entry> out = new ArrayList<>();
        for ( MgiDiseaseMouseModelReport.Entry e : entries ) {
            if ( e.isNotModel() == notModel ) {
                out.add( e );
            }
        }
        return out;
    }

    private List<AnnotationRelation> read( Collection<? extends MgiDiseaseModelReport.Entry> entries,
            AnnotationRelationStatus status, OntologyXrefIndex xrefs, OntologyXrefIndex bridge,
            @Nullable Taxon mouse, Date generatedAt, Set<String> seen, Tally tally ) {
        List<AnnotationRelation> out = new ArrayList<>();
        for ( MgiDiseaseModelReport.Entry e : entries ) {
            tally.statements++;
            Set<String> translated = xrefs.resolve( e.getDoid() );
            if ( translated.isEmpty() ) {
                tally.untranslatable++;
                tally.unresolved.add( e.getDoid() );
                continue;
            }
            for ( String mondoUri : translated ) {
                String label = xrefs.labelOf( mondoUri );
                if ( label == null ) {
                    tally.unlabelled++;
                    continue;
                }
                // one allele may translate to several MONDO terms; the ambiguity is kept rather than
                // resolved, exactly as the ontology producer keeps it
                if ( !seen.add( e.getAlleleSymbol() + '\t' + mondoUri ) ) {
                    // a report read earlier already claimed this pair, and its claim stands -- either
                    // the same statement twice, or the contradiction the read order exists to settle
                    tally.superseded++;
                    continue;
                }
                out.add( build( e, mondoUri, label, status, mouse, generatedAt ) );
                tally.count( status );
                // the same fact again, under whichever bridging terms cross-reference this allele
                for ( String bridgedUri : bridge.resolve( e.getAlleleId() ) ) {
                    String bridgedLabel = bridge.labelOf( bridgedUri );
                    if ( bridgedLabel == null || !seen.add( bridgedUri + '\t' + mondoUri ) ) {
                        continue;
                    }
                    AnnotationRelation b = build( e, mondoUri, label, status, mouse, generatedAt );
                    b.setSubjectValue( truncate( bridgedLabel ) );
                    b.setSubjectValueUri( bridgedUri );
                    b.setSubjectCategory( OntologyRelationSource.STRAIN.getCategory() );
                    b.setSubjectCategoryUri( OntologyRelationSource.STRAIN.getCategoryUri() );
                    out.add( b );
                    tally.bridged++;
                    tally.count( status );
                }
            }
        }
        return out;
    }

    private AnnotationRelation build( MgiDiseaseModelReport.Entry e, String objectUri, String objectLabel,
            AnnotationRelationStatus status, @Nullable Taxon mouse, Date generatedAt ) {
        AnnotationRelation r = new AnnotationRelation();
        r.setSubjectValue( truncate( e.getAlleleSymbol() ) );
        if ( e.getAlleleId() != null ) {
            r.setSubjectValueUri( ALLELE_URI_PREFIX + e.getAlleleId() );
        }
        Category subject = Categories.GENOTYPE;
        r.setSubjectCategory( subject.getCategory() );
        r.setSubjectCategoryUri( subject.getCategoryUri() );
        r.setPredicate( IS_MODEL_OF_LABEL );
        r.setPredicateUri( IS_MODEL_OF_URI );
        r.setObjectValue( truncate( objectLabel ) );
        r.setObjectValueUri( objectUri );
        Category object = Categories.DISEASE;
        r.setObjectCategory( object.getCategory() );
        r.setObjectCategoryUri( object.getCategoryUri() );
        r.setTaxon( mouse );
        r.setBasis( AnnotationRelationBasis.EXTERNAL );
        r.setStatus( status );
        r.setSource( SOURCE );
        // The code says what KIND of evidence this is and EVIDENCE says what it actually was, because
        // a curator cannot click a code. A cited statement is traceable to an author; an uncited one
        // is an import whose own basis we cannot see.
        r.setEvidenceCode( e.getCitations().isEmpty() ? GOEvidenceCode.IIA : GOEvidenceCode.TAS );
        r.setEvidence( e.getEvidence() );
        r.setGeneratedAt( generatedAt );
        return r;
    }

    @Nullable
    private Taxon resolveMouse() {
        if ( taxonService == null ) {
            return null;
        }
        Taxon t = taxonService.findByNcbiId( MOUSE_NCBI_TAXON_ID );
        if ( t == null ) {
            log.warn( "No taxon with NCBI id {}; MGI relations will be stored without one, which reads as"
                    + " 'unknown taxon' and weakens every claim built on them.", MOUSE_NCBI_TAXON_ID );
        }
        return t;
    }

    private static String truncate( String s ) {
        return s.length() <= VALUE_MAX ? s : s.substring( 0, VALUE_MAX );
    }

    /** What one report contributed. */
    private static class Tally {
        private int statements = 0;
        private int asserted = 0;
        private int refuted = 0;
        private int untranslatable = 0;
        private int bridged = 0;
        private int unlabelled = 0;
        private int superseded = 0;
        private final Set<String> unresolved = new LinkedHashSet<>();

        private void count( AnnotationRelationStatus status ) {
            if ( status == AnnotationRelationStatus.REFUTED ) {
                refuted++;
            } else {
                asserted++;
            }
        }
    }

    /**
     * The tallies that make a run's coverage observable, for the reason the ontology producer has
     * them: "the relation is missing" and "the identifier could not be translated" look identical from
     * the table.
     *
     * <p>🛑 <b>Kept per report.</b> Three sources now feed one rebuild and they overlap heavily —
     * measured 2026-08-30, 2,950 of the disease report's pairs are already in the genotype report. A
     * single merged count answers neither of the questions actually asked of this run: what did the
     * new file add, and how much of it did an earlier report already have. Re-deriving that means
     * re-downloading three reports and writing a script, which is how the last person found out.</p>
     */
    private static class Reading {

        /** Report name → its tally, in the order the reports were read. */
        private final java.util.Map<String, Tally> byReport = new java.util.LinkedHashMap<>();

        /**
         * The tally for one report. The disease report asks for it twice — once for its {@code NOT}
         * rows and once for its model rows — and gets the same one back, so its two passes read as
         * one file.
         */
        private Tally of( String report ) {
            return byReport.computeIfAbsent( report, k -> new Tally() );
        }

        private int statements() {
            return byReport.values().stream().mapToInt( t -> t.statements ).sum();
        }

        private int asserted() {
            return byReport.values().stream().mapToInt( t -> t.asserted ).sum();
        }

        private int refuted() {
            return byReport.values().stream().mapToInt( t -> t.refuted ).sum();
        }

        private String report() {
            StringBuilder sb = new StringBuilder();
            sb.append( "\nreport\tstatements\tasserted\trefuted\tbridged\tuntranslatable DOID"
                    + "\ttranslated but unnamed\talready claimed by an earlier report" );
            for ( java.util.Map.Entry<String, Tally> e : byReport.entrySet() ) {
                Tally t = e.getValue();
                sb.append( '\n' ).append( e.getKey() )
                        .append( '\t' ).append( t.statements )
                        .append( '\t' ).append( t.asserted )
                        .append( '\t' ).append( t.refuted )
                        .append( '\t' ).append( t.bridged )
                        .append( '\t' ).append( t.untranslatable )
                        .append( '\t' ).append( t.unlabelled )
                        .append( '\t' ).append( t.superseded );
            }
            Set<String> unresolved = new LinkedHashSet<>();
            byReport.values().forEach( t -> unresolved.addAll( t.unresolved ) );
            if ( !unresolved.isEmpty() ) {
                sb.append( "\nDOIDs MONDO does not cross-reference (" ).append( unresolved.size() )
                        .append( "): " );
                unresolved.stream().limit( 15 ).forEach( u -> sb.append( u ).append( ' ' ) );
            }
            return sb.toString();
        }
    }
}
