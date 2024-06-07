package ubic.gemma.core.util;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts a text resource into a set of lines.
 * <p>
 * Lines starting with '#' are ignored.
 * @author poirigui
 */
public class TextResourceToSetOfLinesFactoryBean extends AbstractFactoryBean<Set<String>> {

    private final Resource resource;

    public TextResourceToSetOfLinesFactoryBean( Resource resource ) {
        this.resource = resource;
    }

    @Override
    protected Set<String> createInstance() throws Exception {
        return new BufferedReader( new InputStreamReader( resource.getInputStream(), StandardCharsets.UTF_8 ) )
                .lines()
                .filter( line -> !line.startsWith( "#" ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }
}
