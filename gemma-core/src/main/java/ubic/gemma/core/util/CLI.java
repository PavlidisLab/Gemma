package ubic.gemma.core.util;

import ubic.gemma.core.apps.GemmaCLI;

public interface CLI {
    /**
     * A short memorable name for the command that can be used to locate this class.
     *
     * @return name; if null, this will not be available as a shortcut command.
     */
    String getCommandName();

    /**
     * A short description of what the command does.
     * @return if null, this description will not be displayed
     */
    String getShortDesc();

    /**
     * A group this command is part of.
     * @return the command group for this CLI
     */
    GemmaCLI.CommandGroup getCommandGroup();

    int executeCommand( String[] args );

}
