package ubic.gemma.core.loader.expression;

/**
 * Configures a {@link DataLoader} from a {@link DataLoaderConfig}.
 * <p>
 * The main reason we have this is to allow for different data loading strategies for a given configuration.
 * @author poirigui
 * @param <T> the type of {@link DataLoader} this configurer produces
 * @param <C> the type of {@link DataLoaderConfig} this configurer consumes
 */
public interface DataLoaderConfigurer<T extends DataLoader, C extends DataLoaderConfig> {

    /**
     * Create a {@link DataLoader} from the given configuration.
     */
    T configureLoader( C config );
}
