package ubic.gemma.core.search.source;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Search source based on Hibernate Search.
 * @author poirigui
 */
@Component
public class HibernateSearchSource implements SearchSource {

    private static final String[] PUBLICATION_FIELDS = new String[] { "name", "description", "abstractText",
            "annotatedAbstract", "authorList", "chemicals.name", "chemicals.description", "chemicals.registryNumber",
            "fullTextUri", "keywords.term", "meshTerms.term", "pubAccession.accession", "title" };

    private static String[] DATASET_FIELDS = {
            "shortName", "name", "description", "bioAssays.name", "bioAssays.description", "bioAssays.accession.accession",
            "bioAssays.sampleUsed.name", "bioAssays.sampleUsed.description", "bioAssays.sampleUsed.characteristics.value",
            "bioAssays.sampleUsed.characteristics.valueUri", "characteristics.value", "characteristics.valueUri",
            "experimentalDesign.name", "experimentalDesign.description", "experimentalDesign.experimentalFactors.name",
            "experimentalDesign.experimentalFactors.description",
            "experimentalDesign.experimentalFactors.category.categoryUri",
            "experimentalDesign.experimentalFactors.category.category",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.value",
            "experimentalDesign.experimentalFactors.factorValues.characteristics.valueUri" };

    private static final String[] GENE_FIELDS = {
            "name", "description", "accessions.accession", "aliases.alias",
            "ensemblId", "ncbiGeneId", "officialName", "officialSymbol", "products.name", "products.description",
            "products.ncbiGi", "products.accessions.accession", "products.previousNcbiId"
    };

    private static String[] GENE_SET_FIELDS = {
            "name", "description", "characteristics.value", "characteristics.valueUri", "sourceAccession.accession"
    };

    private static String[] EXPERIMENT_SET_FIELDS = { "name", "description" };

    private static final String[] BIO_SEQUENCE_FIELDS = { "name", "description", "sequenceDatabaseEntry.accession" };

    private static String[] COMPOSITE_SEQUENCE_FIELDS = { "name", "description" };

    private static String[] prefix( String p, String... fields ) {
        return Arrays.stream( fields ).map( f -> p + f ).toArray( String[]::new );
    }

    static {
        DATASET_FIELDS = ArrayUtils.addAll( DATASET_FIELDS, prefix( "primaryPublication.", PUBLICATION_FIELDS ) );
        DATASET_FIELDS = ArrayUtils.addAll( DATASET_FIELDS, prefix( "otherRelevantPublications.", PUBLICATION_FIELDS ) );
        // TODO: EXPERIMENT_SET_FIELDS = ArrayUtils.addAll( EXPERIMENT_SET_FIELDS, prefix( "experiments.", DATASET_FIELDS ) );
        GENE_SET_FIELDS = ArrayUtils.addAll( GENE_SET_FIELDS, prefix( "literatureSources.", PUBLICATION_FIELDS ) );
        GENE_SET_FIELDS = ArrayUtils.addAll( GENE_SET_FIELDS, prefix( "members.gene.", GENE_FIELDS ) );
        COMPOSITE_SEQUENCE_FIELDS = ArrayUtils.addAll( COMPOSITE_SEQUENCE_FIELDS, prefix( "biologicalCharacteristic.", BIO_SEQUENCE_FIELDS ) );
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) {
        return searchFor( settings, ArrayDesign.class, "shortName", "name", "description", "alternateNames.name", "externalReferences.accession" );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) {
        return searchFor( settings, BibliographicReference.class, PUBLICATION_FIELDS );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) {
        return searchFor( settings, ExpressionExperimentSet.class, EXPERIMENT_SET_FIELDS );
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) {
        return searchFor( settings, BioSequence.class, BIO_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) {
        return searchFor( settings, CompositeSequence.class, COMPOSITE_SEQUENCE_FIELDS );
    }

    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) {
        return searchFor( settings, ExpressionExperiment.class, DATASET_FIELDS );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) {
        return searchFor( settings, Gene.class, GENE_FIELDS );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) {
        return searchFor( settings, GeneSet.class, GENE_SET_FIELDS );
    }

    private <T extends Identifiable> Collection<SearchResult<T>> searchFor( SearchSettings settings, Class<T> clazz, String... fields ) {
        FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.getCurrentSession() );
        QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( clazz )
                .get();
        //noinspection unchecked
        List<Object[]> results = fullTextSession
                .createFullTextQuery( queryBuilder.keyword().onFields( fields ).matching( settings.getQuery() ).createQuery(), clazz )
                .setProjection( settings.isFillResults() ? FullTextQuery.THIS : FullTextQuery.ID, FullTextQuery.SCORE )
                .list();
        return results.stream()
                .map( r -> searchResultFromRow( r, settings, clazz ) )
                .collect( Collectors.toList() );
    }

    private <T extends Identifiable> SearchResult<T> searchResultFromRow( Object[] row, SearchSettings settings, Class<T> clazz ) {
        if ( settings.isFillResults() ) {
            //noinspection unchecked
            return SearchResult.from( clazz, ( T ) row[0], ( Float ) row[1], "hibernateSearch" );
        } else {
            return SearchResult.from( clazz, ( Long ) row[0], ( Float ) row[1], "hibernateSearch" );
        }
    }
}
