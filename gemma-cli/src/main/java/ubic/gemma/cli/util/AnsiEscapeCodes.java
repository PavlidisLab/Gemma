package ubic.gemma.cli.util;

import org.springframework.util.Assert;

/**
 * Generate various ANSI escape codes.
 * @author poirigui
 */
public class AnsiEscapeCodes {

    private static final String
            ESC = "\u001B",
            ST = ESC + "\\";

    /**
     * Indicate progress.
     * <p>
     * This is based on <a href="https://conemu.github.io/en/AnsiEscapeCodes.html#ConEmu_specific_OSC">ConEmu-specific OSC</a>.
     * It works on Linux with VTE and Windows (<a href="https://github.com/microsoft/terminal/pull/8055">microsoft/terminal#8055</a>).
     */
    public static String progress( int progress ) {
        Assert.isTrue( progress >= 0 && progress <= 100, "Progress must be between 0 and 100." );
        return String.format( "%s]9;4;1;%d%s", ESC, progress, ST );
    }

    /**
     * Indicate an indeterminate progress.
     */
    public static String indeterminateProgress() {
        return String.format( "%s]9;4;3;%s", ESC, ST );
    }

    /**
     * Clear a progress indicator.
     */
    public static String clearProgress() {
        return String.format( "%s]9;4;0;%s", ESC, ST );
    }
}
