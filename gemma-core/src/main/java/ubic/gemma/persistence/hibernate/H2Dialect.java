package ubic.gemma.persistence.hibernate;

import org.hibernate.dialect.function.StandardSQLFunction;

public class H2Dialect extends org.hibernate.dialect.H2Dialect {

    public H2Dialect() {
        super();
        registerFunction( "bitwise_and", new StandardSQLFunction( "BITAND" ) );
    }
}
