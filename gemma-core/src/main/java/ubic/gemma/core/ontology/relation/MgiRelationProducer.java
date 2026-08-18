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
 * @see MgiDiseaseModelReport for the file shape and why rows are not relations
 */
public class MgiRelationProducer {

    private static final Logger log = LoggerFactory.getLogger( MgiRelationProducer.class );

    /** As {@link AnnotationRelation#getSource()} spells it. */
    public static final String SOURCE = "MGI";

    private static final String IS_MODEL_OF_URI = "http://purl.obolibrary.org/obo/RO_0003301";
    private static final String IS_MODEL_OF_LABEL = "is model of";

    /** MGI ships mouse genetics; the reports carry no taxon column. */
    private static final int MOUSE_NCBI_TAXON_ID = 10090;

    /**
     * The allele URI space. MGI's OBO PURL form 404s, so the canonical resolvable one is minted here,
     * matching what {@code MgiStrainOntologyService} does for strains.
     */
    private static final String ALLELE_URI_PREFIX = "https://www.informatics.jax.org/allele/";

    /** The ontology whose cross-references translate {@code DOID:} into something Gemma annotates in. */
    private static final String XREF_SOURCE_TOKEN = "MONDO";

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

    /**
     * Rebuild every MGI relation from the two reports.
     *
     * <p>Rebuild rather than upsert, and scoped to this source: an upsert can only correct rows the new
     * read still produces, so a statement MGI has since withdrawn would outlive the report it came
     * from. Narrowing the delete to {@code SOURCE = 'MGI'} leaves every other EXTERNAL source alone.</p>
     *
     * @param asserted the {@code MGI_Geno_DiseaseDO.rpt} stream
     * @param refuted  the {@code MGI_Geno_NotDiseaseDO.rpt} stream, or null to skip the negatives
     * @return how many relation rows were written
     */
    public int produce( InputStream asserted, @Nullable InputStream refuted ) throws IOException {
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
        Date generatedAt = new Date();
        Taxon mouse = resolveMouse();

        Reading reading = new Reading();
        List<AnnotationRelation> rows = new ArrayList<>();
        rows.addAll( read( asserted, AnnotationRelationStatus.ASSERTED, xrefs, mouse, generatedAt, reading ) );
        if ( refuted != null ) {
            rows.addAll( read( refuted, AnnotationRelationStatus.REFUTED, xrefs, mouse, generatedAt, reading ) );
        }

        log.info( "Read {} MGI relations ({} asserted, {} refuted) from {} statements in {} ms.{}",
                rows.size(), reading.asserted, reading.refuted, reading.statements, timer.getTime(),
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

    private List<AnnotationRelation> read( InputStream is, AnnotationRelationStatus status,
            OntologyXrefIndex xrefs, @Nullable Taxon mouse, Date generatedAt, Reading reading )
            throws IOException {
        List<AnnotationRelation> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for ( MgiDiseaseModelReport.Entry e : MgiDiseaseModelReport.parse( is ) ) {
            reading.statements++;
            Set<String> translated = xrefs.resolve( e.getDoid() );
            if ( translated.isEmpty() ) {
                reading.untranslatable++;
                reading.unresolved.add( e.getDoid() );
                continue;
            }
            for ( String mondoUri : translated ) {
                String label = xrefs.labelOf( mondoUri );
                if ( label == null ) {
                    reading.unlabelled++;
                    continue;
                }
                // one allele may translate to several MONDO terms; the ambiguity is kept rather than
                // resolved, exactly as the ontology producer keeps it
                if ( !seen.add( e.getAlleleSymbol() + '\t' + mondoUri ) ) {
                    continue;
                }
                out.add( build( e, mondoUri, label, status, mouse, generatedAt ) );
                if ( status == AnnotationRelationStatus.REFUTED ) {
                    reading.refuted++;
                } else {
                    reading.asserted++;
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

    /**
     * The tallies that make a run's coverage observable, for the reason the ontology producer has
     * them: "the relation is missing" and "the identifier could not be translated" look identical from
     * the table.
     */
    private static class Reading {
        private int statements = 0;
        private int asserted = 0;
        private int refuted = 0;
        private int untranslatable = 0;
        private int unlabelled = 0;
        private final Set<String> unresolved = new LinkedHashSet<>();

        private String report() {
            StringBuilder sb = new StringBuilder();
            sb.append( "\nstatements read\t" ).append( statements )
                    .append( "\tuntranslatable DOID\t" ).append( untranslatable )
                    .append( "\ttranslated but unnamed\t" ).append( unlabelled );
            if ( !unresolved.isEmpty() ) {
                sb.append( "\nDOIDs MONDO does not cross-reference (" ).append( unresolved.size() )
                        .append( "): " );
                unresolved.stream().limit( 15 ).forEach( u -> sb.append( u ).append( ' ' ) );
            }
            return sb.toString();
        }
    }
}
