package ubic.gemma.core.util;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import ubic.gemma.core.apps.GemmaCLI;

import javax.annotation.Nullable;

/**
 * Interface for CLI tools.
 * <p>
 * Implementing this interface will make the tool detectable by {@link GemmaCLI}.
 *
 * @author poirigui
 */
public interface CLI {

    /**
     * Copyright notice to be used by all the CLI tools.
     */
    String COPYRIGHT_NOTICE = "The Gemma project, Copyright (c) 2007-2021 University of British Columbia.";

    /**
     * Exit code used for a successful {@link #executeCommand(String[])}.
     */
    int SUCCESS = 0;

    /**
     * Exit code used for a failed {@link #executeCommand(String[])}.
     */
    int FAILURE = 1;

    /**
     * A short memorable name for the command that can be used to locate this class.
     */
    String getCommandName();

    /**
     * Obtain a short description for this command explaining what it does.
     * @return a short description, or null if unavailable
     */
    @Nullable
    String getShortDesc();

    /**
     * Obtain the command group.
     */
    CommandGroup getCommandGroup();

    /**
     * Execute the given command given CLI arguments.
     */
    int executeCommand( String[] args );

    @Nullable
    Exception getLastException();

    /**
     * Command group to which the CLI belongs.
     * <p>
     * Order is significant.
     */
    enum CommandGroup {
        EXPERIMENT, PLATFORM, ANALYSIS, METADATA, PHENOTYPES, SYSTEM, MISC, DEPRECATED
    }
}
