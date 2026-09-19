package ubic.gemma.cli.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * {@link Options} that refuse a second option with the same short or long name.
 * <p>
 * {@link Options#addOption(Option)} replaces an existing option with the same key without complaint. When a subclass
 * CLI reused a name its base class had already registered, the subclass's definition won in the parser while the base
 * class still read that key as its own option: {@code platformRepeatScan -f} was read as a list of platforms, and
 * {@code loadSingleCellData} read its cell type URI column as the cell type name column.
 * <p>
 * Every other {@code add*} method of {@link Options}, {@link Options#addOptionGroup} included, goes through
 * {@link #addOption(Option)}, so overriding it covers all of them.
 */
class DuplicateRejectingOptions extends Options {

    private final String owner;

    /**
     * @param owner the CLI building these options, named in the error message
     */
    DuplicateRejectingOptions( String owner ) {
        this.owner = owner;
    }

    @Override
    public Options addOption( Option opt ) {
        // the key is the short name, or the long name of an option that has none; either way it is stored with the
        // short names, which is where a second option with the same key replaces the first
        if ( hasShortOption( opt.getKey() ) ) {
            throw new IllegalStateException( String.format( "%s defines the option '%s' twice: %s would replace %s.",
                    owner, opt.getKey(), opt, getOption( opt.getKey() ) ) );
        }
        if ( opt.hasLongOpt() && hasLongOption( opt.getLongOpt() ) ) {
            throw new IllegalStateException( String.format( "%s defines the long option '%s' twice: %s would replace %s.",
                    owner, opt.getLongOpt(), opt, getOption( opt.getLongOpt() ) ) );
        }
        return super.addOption( opt );
    }
}
