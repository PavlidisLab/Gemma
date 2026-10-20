package ubic.gemma.persistence.util;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.hibernate.SessionFactory;

/**
 * Stash the {@link SessionFactoryImplementor} onto {@link AclQueryUtils#sessionFactory}
 * at Spring init. {@link AclQueryUtils} is a static utility that needs an open session
 * to resolve {@code acl_class.id} from a Securable class name (see
 * {@code resolveAclClassId}). A Hibernate-internal {@code Query.unwrap} doesn't
 * give us a workable session reference, so we cache the factory once at startup
 * and open a fresh {@link org.hibernate.StatelessSession} for the lookup.
 *
 * <p>{@code jakarta.annotation.PostConstruct} isn't on the gemma-core classpath
 * (the {@code @PostConstruct} dep was dropped in the Phase 3 Jakarta cleanup);
 * Spring's {@link InitializingBean} provides the same lifecycle slot.
 *
 * <p>{@code @Lazy(false)} is required: nothing injects this bean, so under the CLI's
 * {@link ubic.gemma.core.context.LazyInitByDefaultPostProcessor} it would be defined but never
 * instantiated, and the first ACL-filtered query would fail with "AclQueryUtils.sessionFactory
 * not set". Same reasoning as {@code AclEventListenerConfig}.
 */
@Component
@Lazy(false)
public class AclClassIdInitializer implements InitializingBean {

    private final SessionFactory sessionFactory;

    @Autowired
    public AclClassIdInitializer( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void afterPropertiesSet() {
        AclQueryUtils.sessionFactory = sessionFactory.unwrap( SessionFactoryImplementor.class );
    }
}
