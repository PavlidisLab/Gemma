package ubic.gemma.persistence.initialization;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.SchemaUpdateScript;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.util.Assert;
import ubic.gemma.persistence.hibernate.H2Dialect;
import ubic.gemma.persistence.hibernate.MySQL57InnoDBDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@CommonsLog
public class DatabaseSchemaUpdatePopulator implements DatabasePopulator {

    private final Configuration configuration;
    private final Dialect dialect;

    public DatabaseSchemaUpdatePopulator( Configuration configuration, String vendor ) {
        Assert.isTrue( vendor.equals( "mysql" ) || vendor.equals( "h2" ) );
        this.configuration = configuration;
        this.dialect = vendor.equals( "mysql" ) ? new MySQL57InnoDBDialect() : new H2Dialect();
    }

    @Override
    public void populate( Connection connection ) throws SQLException {
        DatabaseMetadata dm = new DatabaseMetadata( connection, dialect, configuration );
        List<String> sqlStatements = configuration
                .generateSchemaUpdateScriptList( dialect, dm )
                .stream()
                .map( SchemaUpdateScript::getScript )
                .collect( Collectors.toList() );
        for ( String sqlStatement : sqlStatements ) {
            try ( PreparedStatement ps = connection.prepareStatement( sqlStatement ) ) {
                ps.execute();
            }
        }
    }
}
