package ubic.gemma.persistence.hibernate;

/**
 * H2 dialect override for Gemma tests.
 * <p>
 * Pre-phase-2 this class registered a {@code bitwise_and} SQL function via
 * {@code Dialect.registerFunction}, which Hibernate 6 removed. ACL native SQL no
 * longer routes through the dialect's function registry (see
 * {@code AclQueryUtils}), so nothing custom is needed here anymore. The class is
 * preserved as a subclass so existing wiring (test configs, schema populator)
 * keeps working without changes.
 */
public class H2Dialect extends org.hibernate.dialect.H2Dialect {
}
