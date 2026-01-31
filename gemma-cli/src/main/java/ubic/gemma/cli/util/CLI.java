package ubic.gemma.cli.util;

import org.apache.commons.cli.Options;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface for CLI tools.
 * <p>
 * Implementing this interface will make the tool detectable by {@link ubic.gemma.cli.main.GemmaCLI}.
 *
 * @author poirigui
 */
public interface CLI {

    /**
     * A short memorable name for the command that can be used to locate this class.
     *
     * @return name; if null or blank, this will not be available as a shortcut command.
     */
    @Nullable
    String getCommandName();

    /**
     * A list of aliases for the command that can be used to locate this class.
     */
    List<String> getCommandAliases();

    /**
     * Obtain a short description for this command explaining what it does.
     */
    @Nullable
    String getShortDesc();

    /**
     * Obtain the command group for this CLI.
     */
    CommandGroup getCommandGroup();

    /**
     * Obtain the options for the CLI.
     */
    Options getOptions();

    /**
     * Indicate if this CLI allows positional arguments.
     */
    boolean allowPositionalArguments();

    /**
     * Execute the given command given CLI arguments.
     */
    void executeCommand( CLIContext ctx );

    // order here is significant.
    enum CommandGroup {
        EXPERIMENT, PLATFORM, ANALYSIS, METADATA, SYSTEM, MISC, DEPRECATED
    }
}
