package ubic.gemma.persistence.hibernate;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

import java.util.List;

public class MySQL57InnoDBDialect extends org.hibernate.dialect.MySQL57InnoDBDialect {

    public MySQL57InnoDBDialect() {
        super();
        registerFunction( "bitwise_and", new MySQLBitwiseAnd() );
    }

    private static class MySQLBitwiseAnd implements SQLFunction {

        @Override
        public boolean hasArguments() {
            return false;
        }

        @Override
        public boolean hasParenthesesIfNoArguments() {
            return false;
        }

        @Override
        public Type getReturnType( Type firstArgumentType, Mapping mapping ) throws QueryException {
            return firstArgumentType;
        }

        @Override
        public String render( Type firstArgumentType, List arguments, SessionFactoryImplementor factory ) throws QueryException {
            if ( arguments.size() != 2 ) {
                throw new QueryException( "The bitwise_and() function expects exactly two parameters." );
            }
            return "(" + arguments.get( 0 ) + " & " + arguments.get( 1 ) + ")";
        }
    }
}
