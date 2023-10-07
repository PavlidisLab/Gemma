package ubic.gemma.core.util;

import ubic.gemma.core.apps.GemmaCLI;

import static java.util.Objects.requireNonNull;

/**
 * Interface for CLI tools.
 *
 * Implementing this interface will make the tool detectable by {@link GemmaCLI}.
 *
 * @author poirigui
 */
public interface CLI {

    /**
     * A short memorable name for the command that can be used to locate this class.
     *
     * @return name; if null, this will not be available as a shortcut command.
     * @deprecated use {@link Command} instead.
     */
    @Deprecated
    default String getCommandName() {
        return requireNonNull( this.getClass().getAnnotation( Command.class ),
                "Either annotate this CLI with @Command or implement getCommandName()." ).name();
    }

    /**
     * Obtain a short description for this command explaining what it does.
     * @deprecated use {@link Command} instead.
     */
    @Deprecated
    default String getShortDesc() {
        return requireNonNull( this.getClass().getAnnotation( Command.class ),
                "Either annotate this CLI with @Command or implement getShortDesc()." ).description();
    }

    /**
     * Obtain the command grup.
     *
     * @return the command group for this CLI
     * @deprecated use {@link Command} instead.
     */
    @Deprecated
    default GemmaCLI.CommandGroup getCommandGroup() {
        return requireNonNull( this.getClass().getAnnotation( Command.class ),
                "Either annotate this CLI with @Command or implement getCommandGroup()." ).group();
    }

    /**
     * Execute the given command given CLI arguments.
     */
    int executeCommand( String[] args );
}
