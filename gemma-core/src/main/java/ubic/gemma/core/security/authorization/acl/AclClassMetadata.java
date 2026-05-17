package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.domain.AclObjectIdentity;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Provide ACL-related metadata.
 *
 * @author poirigui
 */
@Component
public class AclClassMetadata {

    /**
     * A pre-defined mapping of all known secured child classes to their parent securable class.
     */
    private static final Map<Class<? extends SecuredChild<?>>, Class<? extends Securable>> securedChildToParentTypeMap = new HashMap<>();

    /**
     * A snippet of HQL that resolves the parent identifier for a given secured child.
     * <p>
     * The query has access to the following bindings: {@code aoi} (an {@link AclObjectIdentity}), {@code :type} and
     * {@code :parentType}.
     */
    private static final Map<Class<? extends SecuredChild<?>>, List<String>> securedChildToParentIdQueryMap = new HashMap<>();

    /**
     *
     * @param clazz              a secured child class
     * @param parentClazz        an expected parent class
     * @param childToParentIdHql optional HQL snippets to resolve the parent identifier from the child, a parent is
     *                           considered incorrect if any of the queries produce a mismatch. A query may produce an
     *                           empty result which will not be considered a mismatch.
     */
    private static void addSecuredChildToParent( Class<? extends SecuredChild<?>> clazz, Class<? extends Securable> parentClazz, String... childToParentIdHql ) {
        securedChildToParentTypeMap.put( clazz, parentClazz );
        if ( childToParentIdHql.length > 0 ) {
            securedChildToParentIdQueryMap.put( clazz, Arrays.asList( childToParentIdHql ) );
        }
    }

    static {
        // FIXME: handle sub-assays and sub-biomaterials in the child to parent query, or recursively resolve parents as
        //        a special case
        // this cover cases where the BioAssay is attached to a EE or a subset of an EE
        addSecuredChildToParent( BioAssay.class, ExpressionExperiment.class,
                //language=HQL
                "select ee.id from ExpressionExperiment ee join ee.bioAssays ba where ba.id = :identifier group by ee",
                //language=HQL
                "select eess.sourceExperiment.id from ExpressionExperimentSubSet eess join eess.bioAssays ba where ba.id = :identifier group by eess.sourceExperiment" );
        addSecuredChildToParent( BioMaterial.class, ExpressionExperiment.class,
                //language=HQL
                "select ee.id from ExpressionExperiment ee join ee.bioAssays ba where ba.sampleUsed.id = :identifier group by ee",
                //language=HQL
                "select eess.sourceExperiment.id from ExpressionExperimentSubSet eess join eess.bioAssays ba where ba.sampleUsed.id = :identifier group by eess.sourceExperiment" );
        addSecuredChildToParent( ExpressionExperimentSubSet.class, ExpressionExperiment.class,
                //language=HQL
                "select ears.analysis.id from ExpressionAnalysisResultSet ears where ears.id = :identifier" );
        addSecuredChildToParent( MeanVarianceRelation.class, ExpressionExperiment.class,
                //language=HQL
                "select ears.analysis.id from ExpressionAnalysisResultSet ears where ears.id = :identifier" );
        addSecuredChildToParent( ExperimentalDesign.class, ExpressionExperiment.class,
                //language=HQL
                "select ears.analysis.id from ExpressionAnalysisResultSet ears where ears.id = :identifier" );
        addSecuredChildToParent( ExperimentalFactor.class, ExpressionExperiment.class,
                //language=HQL
                "select ee.id from ExpressionExperiment ee join ee.experimentalDesign ed join ed.experimentalFactors ef where ef.id = :identifier group by ee" );
        addSecuredChildToParent( FactorValue.class, ExpressionExperiment.class,
                //language=HQL
                "select ee.id from ExpressionExperiment ee join ee.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues fv where fv.id = :identifier group by ee" );
        addSecuredChildToParent( DifferentialExpressionAnalysis.class, ExpressionExperiment.class,
                //language=HQL
                "select coalesce(ea.sourceExperiment.id, ea.id) from DifferentialExpressionAnalysis dea join dea.experimentAnalyzed ea where dea.id = :identifier" );
        // result set belong to the analysis which in turn belong to the EE
        addSecuredChildToParent( ExpressionAnalysisResultSet.class, DifferentialExpressionAnalysis.class,
                //language=HQL
                "select ears.analysis.id from ExpressionAnalysisResultSet ears where ears.id = :identifier" );
        addSecuredChildToParent( SampleCoexpressionAnalysis.class, ExpressionExperiment.class,
                //language=HQL
                "select coalesce(ea.sourceExperiment.id, ea.id) from SampleCoexpressionAnalysis sca join sca.experimentAnalyzed ea where sca.id = :identifier" );
        addSecuredChildToParent( PrincipalComponentAnalysis.class, ExpressionExperiment.class,
                //language=HQL
                "select coalesce(ea.sourceExperiment.id, ea.id) from PrincipalComponentAnalysis pca join pca.experimentAnalyzed ea where pca.id = :identifier" );
    }

    @Autowired
    public AclClassMetadata( SessionFactory sessionFactory ) {
        // validate that all SecuredChild are registered
        // TODO: this should be part of the Hibernate class metadata, either via annotation of cutom XML entries if that
        //       is allowed
        // Hibernate 5: getAllClassMetadata() throws UnsupportedOperationException; use the JPA metamodel.
        for ( jakarta.persistence.metamodel.EntityType<?> et : sessionFactory.getMetamodel().getEntities() ) {
            Class<?> mappedClass = et.getJavaType();
            if ( SecuredChild.class.isAssignableFrom( mappedClass ) ) {
                if ( Modifier.isAbstract( mappedClass.getModifiers() ) ) {
                    continue;
                }
                //noinspection unchecked
                Class<? extends SecuredChild<?>> scc = ( Class<? extends SecuredChild<?>> ) mappedClass;
                if ( !securedChildToParentTypeMap.containsKey( scc ) ) {
                    throw new IllegalStateException( scc.getName() + " is not configured in the AclClassMetadata, it must have an entry indicating its parent type and query(ies) for resolving its parent ID." );
                }
            }
        }
    }

    /**
     * Obtain the parent class for a given secured child.
     */
    public Class<? extends Securable> getSecurityOwnerClass( Class<? extends SecuredChild<?>> clazz ) {
        return requireNonNull( securedChildToParentTypeMap.get( clazz ), clazz.getName() + " is not registered." );
    }

    /**
     * Obtain the parent ID query(ies) for a given secured child.
     * <p>
     * This usually returns a single query, but in some cases the parent is not uniquely defined. For example,
     * {@link BioAssay} may either belong to an {@link ExpressionExperiment} or an {@link ExpressionExperimentSubSet}.
     *
     * @param aoiIdColumn the column representing the secured child ID in the ACL object identity table, this can also
     *                    be a placeholder (e.g. :identifier).
     */
    public List<String> getSecurityOwnerIdQueries( Class<? extends SecuredChild<?>> clazz, String aoiIdColumn ) {
        return Objects.requireNonNull( securedChildToParentIdQueryMap.get( clazz ), clazz.getName() + " is not registered." ).stream()
                .map( q -> q.replace( ":identifier", aoiIdColumn ) )
                .collect( Collectors.toList() );
    }
}
