package ubic.gemma.persistence.hibernate;

import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

/**
 * The sole purpose of this class is to limit usages of {@code org.springframework.orm} to this package.
 *
 * @author poirigui
 */
public class LocalSessionFactoryBuilder extends org.springframework.orm.hibernate4.LocalSessionFactoryBuilder {

    public LocalSessionFactoryBuilder( DataSource dataSource, ResourceLoader resourceLoader ) {
        super( dataSource, resourceLoader );
    }
}
