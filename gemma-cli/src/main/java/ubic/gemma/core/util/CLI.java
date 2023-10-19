package ubic.gemma.core.util;

import ubic.gemma.core.apps.GemmaCLI;

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
     */
    String getCommandName();

    /**
     * Obtain a short description for this command explaining what it does.
     */
    String getShortDesc();

    /**
     * Obtain the command grup.
     *
     * @return the command group for this CLI
     */
    GemmaCLI.CommandGroup getCommandGroup();

    /**
     * Execute the given command given CLI arguments.
     */
    int executeCommand( String... args );
}
