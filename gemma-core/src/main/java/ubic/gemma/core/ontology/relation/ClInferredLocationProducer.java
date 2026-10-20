package ubic.gemma.core.ontology.relation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Cell type &rarr; anatomical structure relations that CL <b>entails</b> rather than asserts.
 *
 * <p>{@link OntologyRelationProducerImpl} reads {@code getDirectRestrictions()}, so a cell type whose
 * only location comes from an {@code is_a} ancestor produces nothing there — deliberately, because
 * the entailment is valid OWL and frequently useless. {@code Mueller cell} inherits
 * {@code part of photoreceptor array} from {@code retinal cell}; {@code microglial cell} inherits
 * {@code tissue}, {@code central nervous system} and {@code immune system} at once.</p>
 *
 * <p>These are the survivors of that set, judged one at a time and kept only where the claim is both
 * accurate and <i>exclusive</i> — the cell type is found essentially only there, which is the
 * question the curation rule turns on. Of 1,584 entailed rows, 440 survive: the cortical neuron
 * subtypes inheriting {@code cerebral cortex}, the thymocytes inheriting {@code thymus}, the retinal
 * subtypes inheriting {@code retina}. What is rejected is almost entirely the immune tree, where
 * every cell inherits {@code immune system} and none is restricted to it.</p>
 *
 * <p>🛑 <b>A reviewed file, not a computation.</b> The judgement is the artifact, so it is checked in
 * and readable rather than re-derived at run time from a model that would answer differently. It
 * carries its own {@code SOURCE} — a caller that wants only what CL literally states filters to
 * {@code CL}, and one that wants the wider set takes both. Do NOT merge the two: the claim is
 * weaker, and {@code VIA} names the ancestor each row came from so any of them can be argued with.</p>
 *
 * <p>The predicate is the one the ANCESTOR asserts and is not uniformly {@code part of}: 234 of the
 * 440 are {@code has soma location}, which is how CL locates most neurons. Both are already
 * registered in {@code Relation.terms.txt}, {@code RelationInferenceDirection} and
 * {@code RelationTopicality}, so this source needs no new registration.</p>
 *
 * @author gembro
 * @see OntologyRelationSource#CL
 */
public class ClInferredLocationProducer {

    private static final Log log = LogFactory.getLog( ClInferredLocationProducer.class );

    public static final String SOURCE = "CL_INFERRED";

    private static final String RESOURCE = "/ubic/gemma/core/ontology/CL_inferred_locations.tsv";
    private static final String OBO = "http://purl.obolibrary.org/obo/";
    private static final int VALUE_MAX = 255;

    private final AnnotationRelationDao annotationRelationDao;
    private final TransactionTemplate transactionTemplate;

    /**
     * 🛑 Constructor-injected and declared as a {@code @Bean} in {@code OntologyConfig}, NOT a
     * {@code @Service}. The two other file-backed producers are wired that way and the deviation is
     * not cosmetic: a component-scanned {@code @Service} here needs a {@code TransactionTemplate}
     * bean that gemma-rest's test context does not have, and the whole context then fails to load —
     * 190-odd tests erroring on one missing bean, none of them mentioning this class.
     */
    public ClInferredLocationProducer( AnnotationRelationDao annotationRelationDao,
            TransactionTemplate transactionTemplate ) {
        this.annotationRelationDao = annotationRelationDao;
        this.transactionTemplate = transactionTemplate;
    }

    public int produce() throws IOException {
        try ( InputStream in = getClass().getResourceAsStream( RESOURCE ) ) {
            if ( in == null ) {
                throw new IOException( "missing classpath resource " + RESOURCE );
            }
            return produce( in );
        }
    }

    int produce( InputStream in ) throws IOException {
        List<AnnotationRelation> rows = read( in );
        log.info( "Read " + rows.size() + " entailed CL location relations." );
        // 🛑 The delete is narrowed to this SOURCE. It shares BASIS with CL's asserted rows, and a
        // basis-wide delete would take those with it -- 1,206 rows this job cannot rebuild.
        Integer written = transactionTemplate.execute( status -> {
            int removed = annotationRelationDao.removeByBasis( AnnotationRelationBasis.ONTOLOGY, null, SOURCE );
            if ( !rows.isEmpty() ) {
                annotationRelationDao.create( rows );
            }
            log.info( "Removed " + removed + " and wrote " + rows.size()
                    + " ONTOLOGY relation rows for " + SOURCE + "." );
            return rows.size();
        } );
        return written != null ? written : 0;
    }

    private List<AnnotationRelation> read( InputStream in ) throws IOException {
        List<AnnotationRelation> rows = new ArrayList<>();
        Date generatedAt = new Date();
        try ( BufferedReader r = new BufferedReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) ) ) {
            String line;
            boolean header = true;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.isEmpty() || line.charAt( 0 ) == '#' ) {
                    continue;
                }
                if ( header ) {
                    header = false;   // the first non-comment line names the columns
                    continue;
                }
                String[] f = line.split( "\t", -1 );
                if ( f.length < 7 ) {
                    log.warn( "Skipping a row with " + f.length + " columns: " + line );
                    continue;
                }
                rows.add( build( f[0], f[1], f[2], f[3], f[4], f[5], f[6], generatedAt ) );
            }
        }
        return rows;
    }

    private AnnotationRelation build( String subject, String subjectUri, String predicate, String predicateUri,
            String object, String objectUri, String via, Date generatedAt ) {
        AnnotationRelation r = new AnnotationRelation();
        r.setSubjectValue( truncate( subject ) );
        r.setSubjectValueUri( subjectUri );
        r.setSubjectCategory( Categories.CELL_TYPE.getCategory() );
        r.setSubjectCategoryUri( Categories.CELL_TYPE.getCategoryUri() );
        r.setPredicate( truncate( predicate ) );
        r.setPredicateUri( predicateUri );
        r.setObjectValue( truncate( object ) );
        r.setObjectValueUri( objectUri );
        Category objectCategory = categoryOf( objectUri );
        if ( objectCategory != null ) {
            r.setObjectCategory( objectCategory.getCategory() );
            r.setObjectCategoryUri( objectCategory.getCategoryUri() );
        }
        r.setBasis( AnnotationRelationBasis.ONTOLOGY );
        r.setSource( SOURCE );
        // the ancestor the axiom actually sits on, so a reader can see WHY the row exists
        r.setEvidence( truncate( via.isEmpty() ? null : "entailed from " + via ) );
        // software derived it from an axiom, with nobody having checked this particular row IN GEMMA
        r.setEvidenceCode( GOEvidenceCode.IEA );
        // a claim about a term, not about anything Gemma holds; the read path's ACL clause lets a
        // null experiment through untouched
        r.setExpressionExperiment( null );
        r.setAclIsAuthenticatedAnonymouslyMask( 0 );
        r.setGeneratedAt( generatedAt );
        return r;
    }

    /**
     * The target's own vocabulary decides what kind of thing it is; a fixed category would file the
     * CL-targeted rows as anatomical structures.
     */
    @Nullable
    private static Category categoryOf( String uri ) {
        String local = uri.substring( Math.max( uri.lastIndexOf( '/' ), uri.lastIndexOf( '#' ) ) + 1 );
        if ( local.startsWith( "UBERON_" ) ) {
            return Categories.ORGANISM_PART;
        }
        if ( local.startsWith( "CL_" ) ) {
            return Categories.CELL_TYPE;
        }
        return null;
    }

    @Nullable
    private static String truncate( @Nullable String s ) {
        return s == null || s.length() <= VALUE_MAX ? s : s.substring( 0, VALUE_MAX );
    }
}
