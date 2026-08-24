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
package ubic.gemma.model.common.description;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.lang.Nullable;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Date;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * One term stands in some relation to another term: {@code subject — predicate → object}, plus where
 * that came from.
 *
 * <p><b>Why this exists.</b> Gemma holds this knowledge in four places and can query it from none of
 * them. A curator writes {@code disease model: left ventricular hypertrophy — induced by — aortic
 * banding} and it lands in {@link Characteristic#getPredicate()}/{@link Characteristic#getObject()},
 * indexed per-experiment only; ask "which manipulations are asserted to induce left ventricular
 * hypertrophy?" and there is nothing to ask it of. CLO states which disease a cell line derives from
 * and {@link ubic.gemma.core.ontology.model.OntologyTerm#getRestrictions()} has always been able to
 * read it and is called from nowhere. MGI holds gene-level disease associations we compare against
 * offline and never load. And our own corpus attests pairings by co-occurrence that nobody has
 * written down. This table is one queryable home for all four, with
 * {@link AnnotationRelationBasis} recording which.</p>
 *
 * <p><b>It is not a disease table.</b> Nothing here names disease, genotype or cell line. Those are
 * particular {@code (subjectCategory, predicate, objectCategory)} combinations; cell line → organism
 * part, cell type → organism part, cell line → species and disease → taxon are the same rows with
 * different terms in them. Resist adding a column that only one relation kind would use.</p>
 *
 * <p><b>Grain: one row per basis, and for attested bases, per attesting experiment.</b> The same
 * triple legitimately appears several times — once stated by CLO, once harvested from a curator's
 * statement, once per experiment whose annotations co-attest it. Reads aggregate by triple and report
 * the set of bases, because <b>corroboration is decided at read</b>: a relation only
 * {@link AnnotationRelationBasis#CORPUS} attests is reported as uncorroborated and ranks below one
 * something else also states.</p>
 *
 * <p>🛑 <b>Support is counted at read, never stored.</b> This is why {@link #getExpressionExperiment()}
 * exists instead of a count column. A stored count cannot be ACL-filtered afterwards: the public-only
 * version understates for a curator, and the whole-corpus version leaks the existence of private
 * datasets through the denominator. {@link #getAclIsAuthenticatedAnonymouslyMask()} is carried for the
 * same reason {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} carries it and is read with the same
 * {@code EE2CAclQueryUtils.formNativeAclRestrictionClause}.</p>
 *
 * <p><b>Every row is derived, so the table is rebuilt rather than upserted</b> — deleted by basis (or
 * by basis and experiment) and re-inserted. An upsert can only correct rows the new query still
 * produces, which is how {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} ended up with 1,008 rows a full
 * rebuild could not fix. Nothing here is authored, so nothing is lost by dropping it all and
 * recomputing.</p>
 *
 * @see AnnotationRelationBasis
 */
@Entity
@Table(name = "ANNOTATION_RELATION",
        indexes = {
                @Index(name = "IDX_ANNOTATION_RELATION_SUBJECT", columnList = "SUBJECT_VALUE_URI,PREDICATE_URI"),
                @Index(name = "IDX_ANNOTATION_RELATION_OBJECT", columnList = "OBJECT_VALUE_URI,PREDICATE_URI"),
                @Index(name = "IDX_ANNOTATION_RELATION_SUBJECT_VALUE", columnList = "SUBJECT_VALUE"),
                @Index(name = "IDX_ANNOTATION_RELATION_OBJECT_VALUE", columnList = "OBJECT_VALUE"),
                @Index(name = "IDX_ANNOTATION_RELATION_BASIS", columnList = "BASIS,EXPRESSION_EXPERIMENT_FK"),
                @Index(name = "IDX_ANNOTATION_RELATION_EE", columnList = "EXPRESSION_EXPERIMENT_FK")
        })
public class AnnotationRelation extends AbstractIdentifiable {

    /**
     * The subject term as curated, <b>not</b> a gene.
     *
     * <p>Keying on the value is what lets {@code Myc overexpression} and {@code Myc knockdown} carry
     * different diseases instead of collapsing, and what admits the 115 pairs whose subject names no
     * gene at all — {@code APP/PS1}, {@code 5xFAD}, {@code trisomy 21}, {@code Tp53/Rb1 DKO}. A
     * gene-keyed table cannot represent any of them.</p>
     */
    @Column(name = "SUBJECT_VALUE", nullable = false, columnDefinition = "VARCHAR(255)")
    private String subjectValue;

    /**
     * Null is ordinary and not a defect: plenty of curated statement objects are free text
     * ({@code aortic banding} has no URI), which is why the value legs are indexed separately from the
     * URI legs.
     */
    @Nullable
    @Column(name = "SUBJECT_VALUE_URI", columnDefinition = "VARCHAR(255)")
    private String subjectValueUri;

    @Nullable
    @Column(name = "SUBJECT_CATEGORY", columnDefinition = "VARCHAR(255)")
    private String subjectCategory;

    @Nullable
    @Column(name = "SUBJECT_CATEGORY_URI", columnDefinition = "VARCHAR(255)")
    private String subjectCategoryUri;

    /**
     * Drawn from {@code Relation.terms.txt}, which is the authoritative predicate vocabulary.
     *
     * <p>Null where the producer declines to name a verb, which is the honest state for a
     * {@link AnnotationRelationBasis#CORPUS} row: co-occurrence establishes that two annotations
     * travel together and nothing more, and picking a predicate from it would assert a specific
     * relation the evidence does not support.</p>
     */
    @Nullable
    @Column(name = "PREDICATE", columnDefinition = "VARCHAR(255)")
    private String predicate;

    @Nullable
    @Column(name = "PREDICATE_URI", columnDefinition = "VARCHAR(255)")
    private String predicateUri;

    @Column(name = "OBJECT_VALUE", nullable = false, columnDefinition = "VARCHAR(255)")
    private String objectValue;

    @Nullable
    @Column(name = "OBJECT_VALUE_URI", columnDefinition = "VARCHAR(255)")
    private String objectValueUri;

    @Nullable
    @Column(name = "OBJECT_CATEGORY", columnDefinition = "VARCHAR(255)")
    private String objectCategory;

    @Nullable
    @Column(name = "OBJECT_CATEGORY_URI", columnDefinition = "VARCHAR(255)")
    private String objectCategoryUri;

    /**
     * Part of the grain, because taxon decides what the relation says. A mouse carrying a
     * {@code Mecp2} null is a <i>model of</i> Rett syndrome; a human line carrying
     * {@code LRRK2 G2019S} is not modelling Parkinson disease, it <i>has</i> it. Null means unknown,
     * which is read as the weaker of the two claims rather than guessed at.
     */
    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TAXON_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_ANNOTATION_RELATION_TAXON"))
    private Taxon taxon;

    @Enumerated(EnumType.STRING)
    @Column(name = "BASIS", nullable = false, columnDefinition = "VARCHAR(16)")
    private AnnotationRelationBasis basis;

    /**
     * Whether the source states this relation holds, or states that it does not.
     *
     * <p>🛑 Defaults to {@link AnnotationRelationStatus#ASSERTED} on the field as well as in the
     * schema, because a writer that forgets it must produce an assertion rather than a null that some
     * reader interprets. A {@link AnnotationRelationStatus#REFUTED} row read by code unaware of this
     * column says the opposite of what its source said, which is why the reads filter and why no
     * inference may rest on one.</p>
     *
     * @see AnnotationRelationStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, columnDefinition = "VARCHAR(16)")
    private AnnotationRelationStatus status = AnnotationRelationStatus.ASSERTED;

    /**
     * Which ontology or resource asserted it — {@code CLO}, {@code MONDO}, {@code MGI},
     * {@code CELLOSAURUS} — with {@link #getSourceVersion()} alongside so a row is invalidated with
     * the artifact it came from.
     *
     * <p>{@code 'Gemma'} for {@link AnnotationRelationBasis#CURATED} and
     * {@link AnnotationRelationBasis#CORPUS}, whose source is Gemma itself. That used to be null on
     * the reasoning that it went without saying; it does not go without saying to anyone reading the
     * table, and a bare NULL against 36,073 rows was the first thing anybody asked about.
     *
     * <p>Recording the version is not ceremony: several ontologies load from upstream {@code -base}
     * files and CHEBI/MONDO have slim paths, so a relation can be present in the full artifact and
     * absent from what we actually loaded. Without the version, that difference is invisible.</p>
     */
    @Nullable
    @Column(name = "SOURCE", columnDefinition = "VARCHAR(64)")
    private String source;

    @Nullable
    @Column(name = "SOURCE_VERSION", columnDefinition = "VARCHAR(64)")
    private String sourceVersion;

    /**
     * Reuses the vocabulary annotations already carry. {@link GOEvidenceCode#IC} for a relation a
     * curator asserted, {@link GOEvidenceCode#IEA} for one software derived with no human check,
     * {@link GOEvidenceCode#IIA} for one imported from a resource whose own evidence is unknown.
     */
    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "EVIDENCE_CODE", columnDefinition = "VARCHAR(255)")
    private GOEvidenceCode evidenceCode;

    /**
     * The one-line basis the source gives, meant to be shown — {@code PMID:11242117} for a statement
     * MGI cites.
     *
     * <p>Distinct from {@link #evidenceCode}, which says what KIND of evidence this is. A code cannot
     * be clicked, and for 94% of MGI's genotype-to-disease statements the citation was the only thing
     * distinguishing a curated claim from an unattributable import.</p>
     *
     * <p>Null for most rows and legitimately so: an OWL restriction is asserted by its ontology and
     * cites nothing, and a {@link AnnotationRelationBasis#CORPUS} co-occurrence <i>is</i> evidence
     * rather than having any. Null rather than an empty string, which would be a second spelling of
     * the same state.</p>
     */
    @Nullable
    @Column(name = "EVIDENCE", columnDefinition = "VARCHAR(255)")
    private String evidence;

    /**
     * Anything structured or too long for {@link #evidence}, stored opaquely — the same arrangement as
     * {@link Characteristic#getSupportingEvidence()} and {@code PUBLICATION_ASSOCIATION}, so a reader
     * meets one convention across all three rather than three.
     *
     * <p>Gemma does not interpret it. Whatever wrote it owns its schema, which is what keeps an agent
     * free to record its reasoning without a migration each time that reasoning changes shape.</p>
     *
     * <p>🛑 Not a place to put the displayable basis. {@link #evidence} is what a UI renders verbatim;
     * a payload there would be shown to a curator as-is.</p>
     */
    @Nullable
    @Column(name = "SUPPORTING_EVIDENCE", columnDefinition = "TEXT")
    private String supportingEvidence;

    /**
     * The experiment attesting this row, for bases that are attested rather than asserted
     * ({@link AnnotationRelationBasis#CURATED}, {@link AnnotationRelationBasis#CORPUS}); null for
     * bases that hold independently of anything Gemma stores.
     *
     * <p>One row per attesting experiment, which is what keeps support ACL-exact — see the class
     * javadoc.</p>
     */
    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    // V26 declares FK_ANNOTATION_RELATION_EE ON DELETE CASCADE; keep the mapping in step so a Hibernate-generated schema cascades too
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "EXPRESSION_EXPERIMENT_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_ANNOTATION_RELATION_EE"))
    private ExpressionExperiment expressionExperiment;

    /**
     * Where the attesting annotation sat: a whole-experiment tag, a factor value or a sample
     * characteristic. Reported rather than collapsed, because a property that <i>varies across
     * samples</i> and one asserted of the whole experiment are not the same claim.
     */
    @Nullable
    @Column(name = "LEVEL", columnDefinition = "VARCHAR(255)")
    private String level;

    /**
     * Permission bitmask for the anonymous SID on the attesting experiment, copied from the same
     * source {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} copies it from, so support can be counted
     * behind the caller's ACL without joining the ACL tables per query.
     */
    @Column(name = "ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK", nullable = false, columnDefinition = "INT")
    private int aclIsAuthenticatedAnonymouslyMask;

    /**
     * When the rebuild that produced this row ran. The staleness of a derived table is otherwise
     * unobservable, and "the relation is missing" and "the rebuild has not run since the annotation
     * was added" look identical without it.
     */
    @Column(name = "GENERATED_AT", nullable = false, columnDefinition = "DATETIME(3)")
    private Date generatedAt;

    public AnnotationRelation() {
    }

    public String getSubjectValue() {
        return subjectValue;
    }

    public void setSubjectValue( String subjectValue ) {
        this.subjectValue = subjectValue;
    }

    @Nullable
    public String getSubjectValueUri() {
        return subjectValueUri;
    }

    public void setSubjectValueUri( @Nullable String subjectValueUri ) {
        this.subjectValueUri = subjectValueUri;
    }

    @Nullable
    public String getSubjectCategory() {
        return subjectCategory;
    }

    public void setSubjectCategory( @Nullable String subjectCategory ) {
        this.subjectCategory = subjectCategory;
    }

    @Nullable
    public String getSubjectCategoryUri() {
        return subjectCategoryUri;
    }

    public void setSubjectCategoryUri( @Nullable String subjectCategoryUri ) {
        this.subjectCategoryUri = subjectCategoryUri;
    }

    @Nullable
    public String getPredicate() {
        return predicate;
    }

    public void setPredicate( @Nullable String predicate ) {
        this.predicate = predicate;
    }

    @Nullable
    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri( @Nullable String predicateUri ) {
        this.predicateUri = predicateUri;
    }

    public String getObjectValue() {
        return objectValue;
    }

    public void setObjectValue( String objectValue ) {
        this.objectValue = objectValue;
    }

    @Nullable
    public String getObjectValueUri() {
        return objectValueUri;
    }

    public void setObjectValueUri( @Nullable String objectValueUri ) {
        this.objectValueUri = objectValueUri;
    }

    @Nullable
    public String getObjectCategory() {
        return objectCategory;
    }

    public void setObjectCategory( @Nullable String objectCategory ) {
        this.objectCategory = objectCategory;
    }

    @Nullable
    public String getObjectCategoryUri() {
        return objectCategoryUri;
    }

    public void setObjectCategoryUri( @Nullable String objectCategoryUri ) {
        this.objectCategoryUri = objectCategoryUri;
    }

    @Nullable
    public Taxon getTaxon() {
        return taxon;
    }

    public void setTaxon( @Nullable Taxon taxon ) {
        this.taxon = taxon;
    }

    public AnnotationRelationBasis getBasis() {
        return basis;
    }

    public AnnotationRelationStatus getStatus() {
        return status;
    }

    public void setStatus( AnnotationRelationStatus status ) {
        this.status = status;
    }

    public void setBasis( AnnotationRelationBasis basis ) {
        this.basis = basis;
    }

    @Nullable
    public String getSource() {
        return source;
    }

    public void setSource( @Nullable String source ) {
        this.source = source;
    }

    @Nullable
    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion( @Nullable String sourceVersion ) {
        this.sourceVersion = sourceVersion;
    }

    @Nullable
    public GOEvidenceCode getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode( @Nullable GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    @Nullable
    public String getEvidence() {
        return evidence;
    }

    public void setEvidence( @Nullable String evidence ) {
        this.evidence = evidence;
    }

    @Nullable
    public String getSupportingEvidence() {
        return supportingEvidence;
    }

    public void setSupportingEvidence( @Nullable String supportingEvidence ) {
        this.supportingEvidence = supportingEvidence;
    }

    @Nullable
    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( @Nullable ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    @Nullable
    public String getLevel() {
        return level;
    }

    public void setLevel( @Nullable String level ) {
        this.level = level;
    }

    public int getAclIsAuthenticatedAnonymouslyMask() {
        return aclIsAuthenticatedAnonymouslyMask;
    }

    public void setAclIsAuthenticatedAnonymouslyMask( int aclIsAuthenticatedAnonymouslyMask ) {
        this.aclIsAuthenticatedAnonymouslyMask = aclIsAuthenticatedAnonymouslyMask;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt( Date generatedAt ) {
        this.generatedAt = generatedAt;
    }

    /**
     * Constant, deliberately.
     *
     * <p>The business key here is nine nullable columns wide and is populated through setters, so a
     * key-based hash would be wrong the moment a producer set a field after adding the instance to a
     * set, and an id-based one is wrong by construction (the id appears on persist, moving the object
     * to a different bucket than the one it was filed under). A constant hash degrades a single
     * bucket to a linear scan and is never incorrect. Nothing collects these into large in-memory
     * sets — the reads are native aggregate queries — so the cost is not observable.</p>
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o )
            return true;
        if ( !( o instanceof AnnotationRelation ) )
            return false;
        AnnotationRelation other = ( AnnotationRelation ) o;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( subjectValue, other.subjectValue )
                && Objects.equals( subjectValueUri, other.subjectValueUri )
                && Objects.equals( predicateUri, other.predicateUri )
                && Objects.equals( objectValue, other.objectValue )
                && Objects.equals( objectValueUri, other.objectValueUri )
                && basis == other.basis
                && Objects.equals( source, other.source )
                && Objects.equals( expressionExperiment, other.expressionExperiment );
    }

    @Override
    public String toString() {
        return String.format( "AnnotationRelation[%s -%s-> %s, basis=%s%s]",
                subjectValue, predicate != null ? predicate : "?", objectValue, basis,
                expressionExperiment != null ? ", ee=" + expressionExperiment.getId() : "" );
    }
}
