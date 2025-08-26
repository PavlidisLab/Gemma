package ubic.gemma.cli.completion;

public enum CompletionType {
    TAXON,
    PLATFORM,
    PROTOCOL,
    EESET,
    EXTERNAL_DATABASE,
    DATASET,
    EXPERIMENTAL_FACTOR,
    /**
     * Complete experimental factors and interactions suitable for DEA.
     * <p>
     * This is distinct from {@link #EXPERIMENTAL_FACTOR} as it will suggest interaction of factors and will replace
     * {@code :} with {@code _} in factor names and categories.
     */
    EXPERIMENTAL_FACTOR_OR_INTERACTION_FOR_DEA,
}
