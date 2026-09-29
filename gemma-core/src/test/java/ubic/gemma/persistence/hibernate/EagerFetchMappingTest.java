package ubic.gemma.persistence.hibernate;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the fetch mode of eager associations, which the hbm.xml to annotations migration changed.
 * <p>
 * In hbm.xml, {@code lazy="false"} with no {@code fetch} attribute meant {@code fetch="select"}. On an annotation,
 * {@code FetchType.EAGER} with no {@link org.hibernate.annotations.Fetch} means {@code FetchMode.JOIN}. The port
 * carried the first over as the second, naming no fetch mode, so 42 associations that hbm.xml loaded by a separate
 * statement became outer joins on every by-id load, proxy initialization and batch load.
 * <p>
 * Two shapes of that are costly, and this test forbids both:
 * <ul>
 *     <li>An eager collection fetched by join multiplies the rows of the statement that loads its owner, and every
 *     row carries every selected column. Two such collections on one entity multiply each other.
 *     {@code SampleCoexpressionMatrix} was loaded with one row per bioassay, each carrying the whole matrix; for
 *     GSE260875 that is 1,090 copies of a 9.5 MB blob per load.</li>
 *     <li>A many-to-one fetched by join repeats the target's columns once per owner row. When the target carries a
 *     blob and owners are batch-loaded (128 per statement), the same blob is sent once per owner:
 *     {@code SingleCellExpressionDataVector.singleCellDimension} and the dimension's {@code CELL_IDS}.</li>
 * </ul>
 * HQL is not affected either way: it ignores the mapped fetch mode and loads eager associations it does not
 * {@code join fetch} with a follow-up select.
 * <p>
 * An association that genuinely needs a join goes in the allow-list below, with its reason.
 *
 * @see ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionMatrixLoadSqlTest
 */
@ContextConfiguration
public class EagerFetchMappingTest extends BaseDatabaseTest5 {

    /**
     * Eager collections deliberately fetched by join, as {@code Entity.attribute}.
     */
    private static final Set<String> JOIN_FETCHED_COLLECTIONS = Set.of();

    @Test
    public void eagerCollectionsAreNotFetchedByJoin() {
        Set<String> violations = new TreeSet<>();
        forEachAssociation( ( owner, am ) -> {
            if ( am instanceof PluralAttributeMapping
                    && am.getMappedFetchOptions().getTiming() == FetchTiming.IMMEDIATE
                    && am.getMappedFetchOptions().getStyle() == FetchStyle.JOIN ) {
                String name = owner + "." + am.getAttributeName();
                if ( !JOIN_FETCHED_COLLECTIONS.contains( name ) ) {
                    violations.add( name );
                }
            }
        } );
        assertThat( violations )
                .as( "eager collections fetched by join; add @Fetch(FetchMode.SELECT)" )
                .isEmpty();
    }

    @Test
    public void manyToOnesFetchedByJoinDoNotTargetAnEntityWithABlob() {
        Set<String> violations = new TreeSet<>();
        forEachAssociation( ( owner, am ) -> {
            if ( am instanceof ToOneAttributeMapping
                    && ( ( ToOneAttributeMapping ) am ).getCardinality() == ToOneAttributeMapping.Cardinality.MANY_TO_ONE
                    && am.getMappedFetchOptions().getStyle() == FetchStyle.JOIN ) {
                EntityMappingType target = ( ( ToOneAttributeMapping ) am ).getEntityMappingType();
                Set<String> blobs = new TreeSet<>();
                collectBlobColumns( target, blobs );
                target.getSubMappingTypes().forEach( sub -> collectBlobColumns( sub, blobs ) );
                if ( !blobs.isEmpty() ) {
                    violations.add( owner + "." + am.getAttributeName() + " -> " + blobs );
                }
            }
        } );
        assertThat( violations )
                .as( "many-to-ones fetched by join whose target carries a blob; add @Fetch(FetchMode.SELECT)" )
                .isEmpty();
    }

    private void forEachAssociation( BiConsumer<String, AttributeMapping> consumer ) {
        ( ( SessionFactoryImplementor ) sessionFactory ).getMappingMetamodel().forEachEntityDescriptor( p ->
                visit( p, p.getJavaType().getJavaTypeClass().getSimpleName(), consumer ) );
    }

    private void visit( ManagedMappingType type, String owner, BiConsumer<String, AttributeMapping> consumer ) {
        type.forEachAttributeMapping( am -> {
            if ( am instanceof EmbeddableValuedModelPart ) {
                visit( ( ( EmbeddableValuedModelPart ) am ).getEmbeddableTypeDescriptor(), owner + "." + am.getAttributeName(), consumer );
            } else {
                consumer.accept( owner, am );
            }
        } );
    }

    private void collectBlobColumns( ManagedMappingType type, Set<String> blobs ) {
        type.forEachAttributeMapping( am -> {
            if ( am instanceof EmbeddableValuedModelPart ) {
                collectBlobColumns( ( ( EmbeddableValuedModelPart ) am ).getEmbeddableTypeDescriptor(), blobs );
            } else if ( am instanceof SelectableMapping && isBlob( ( SelectableMapping ) am ) ) {
                blobs.add( ( ( SelectableMapping ) am ).getSelectionExpression() );
            }
        } );
    }

    /**
     * Binary large columns only. {@code TEXT} columns such as {@code Characteristic.DESCRIPTION} are capped at 64 KB
     * on MySQL, and the join-fetched many-to-ones that reach them were {@code fetch="join"} in hbm.xml too.
     */
    private boolean isBlob( SelectableMapping sm ) {
        String def = sm.getColumnDefinition() == null ? "" : sm.getColumnDefinition().toUpperCase( Locale.ROOT );
        int code = sm.getJdbcMapping().getJdbcType().getDdlTypeCode();
        return def.contains( "BLOB" ) || code == SqlTypes.BLOB || code == SqlTypes.LONGVARBINARY || code == SqlTypes.LONG32VARBINARY;
    }

    @Configuration
    @TestComponent
    static class Config extends BaseDatabaseTestContextConfiguration {
    }
}
