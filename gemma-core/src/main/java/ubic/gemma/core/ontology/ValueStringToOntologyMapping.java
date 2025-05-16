package ubic.gemma.core.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Attempt to identify a preset value (ontology term) for certain strings found in GEO data sets and other places.
 * <p>
 * The presets are stored in valueStringToOntologyTermMappings.txt.
 */
@CommonsLog
public class ValueStringToOntologyMapping {

    private static final Map<String, Map<String, Characteristic>> term2OntologyMappings = new ConcurrentHashMap<>();

    /**
     * Lookup a value for a given category.
     */
    @Nullable
    public static Characteristic lookup( String value, String category ) {
        if ( term2OntologyMappings.isEmpty() ) {
            initializeTerm2OntologyMappings();
        }

        if ( category == null || !term2OntologyMappings.containsKey( category ) ) {
            return null;
        }

        return term2OntologyMappings.get( category ).get( value.toLowerCase() );
    }

    /**
     * Lookup a value across all categories.
     */
    public static Collection<Characteristic> lookup( String value ) {
        if ( term2OntologyMappings.isEmpty() ) {
            initializeTerm2OntologyMappings();
        }
        String finalValue = value.toLowerCase();
        return term2OntologyMappings.values().stream()
                .map( m -> m.get( finalValue ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
    }

    /**
     * See also GeoChannel, in which we have canned values for some sample characteristics.
     * See also convertVariableType where we map some to some categories.
     */
    private static void initializeTerm2OntologyMappings() {
        try ( BufferedReader in = new BufferedReader( new InputStreamReader( new ClassPathResource( "/ubic/gemma/core/ontology/valueStringToOntologyTermMappings.txt" ).getInputStream() ) ) ) {
            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) ) {
                    continue;
                }
                if ( line.isEmpty() ) continue;

                String[] split = StringUtils.split( line, "\t" );

                if ( split.length < 5 ) {
                    log.warn( "Did not get expected fields for line: " + line );
                    continue;
                }

                String inputValue = split[0].toLowerCase();

                String value = split[1];
                String valueUri = split[2];
                String category = split[3];
                String categoryUri = split[4];

                if ( StringUtils.isBlank( value ) || StringUtils.isBlank( valueUri ) || StringUtils.isBlank( category ) || StringUtils.isBlank( categoryUri ) ) {
                    throw new IllegalArgumentException( "Invalid line had blank field(s): " + line );
                }

                if ( !term2OntologyMappings.containsKey( category ) ) {
                    term2OntologyMappings.put( category, new HashMap<>() );
                }

                if ( term2OntologyMappings.get( category ).containsKey( inputValue ) ) {
                    log.warn( "Duplicate value: " + inputValue + ", ignoring" );
                    continue;
                }

                // NOTE: extensions via modifiers is not to be supported here, as GEO only has key-value pairs.
                Characteristic c = Characteristic.Factory.newInstance( category, categoryUri, value, valueUri );
                term2OntologyMappings.get( category ).put( inputValue, c );
            }
        } catch ( IOException e ) {
            log.error( "Ontology terms mapped from strings failed to initialize from file" );
        }
    }
}
