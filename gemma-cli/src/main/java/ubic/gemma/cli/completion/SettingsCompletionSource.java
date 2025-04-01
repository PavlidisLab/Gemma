package ubic.gemma.cli.completion;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import ubic.gemma.core.config.SettingsConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provide completion for all available Gemma settings.
 * @author poirigui
 */
public class SettingsCompletionSource implements CompletionSource {

    private final boolean includeGemmaPrefix;

    /**
     * @param includeGemmaPrefix Force completions to include the "gemma." prefix.  This is necessary if providing
     *                           completions for system properties.
     */
    public SettingsCompletionSource( boolean includeGemmaPrefix ) {
        this.includeGemmaPrefix = includeGemmaPrefix;
    }

    @Override
    public List<Completion> getCompletions() {
        try {
            List<Completion> result = new ArrayList<>();
            Map<String, String> descriptions = SettingsConfig.settingsDescriptions();
            for ( PropertySource<?> ps : SettingsConfig.settingsPropertySources() ) {
                if ( ps instanceof EnumerablePropertySource ) {
                    for ( String pn : ( ( EnumerablePropertySource<?> ) ps ).getPropertyNames() ) {
                        if ( includeGemmaPrefix && !pn.startsWith( "gemma." ) ) {
                            result.add( new Completion( "gemma." + pn, descriptions.getOrDefault( pn, "" ) ) );
                        } else {
                            result.add( new Completion( pn, descriptions.getOrDefault( pn, "" ) ) );
                        }
                    }
                }
            }
            return result;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
