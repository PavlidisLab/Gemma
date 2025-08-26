package ubic.gemma.cli.util;

public class EnumeratedByCommandStringConverter implements EnumeratedByCommandConverter<String, Exception> {

    public static EnumeratedByCommandStringConverter of( String... command ) {
        return new EnumeratedByCommandStringConverter( command );
    }

    private final String[] command;

    private EnumeratedByCommandStringConverter( String[] command ) {
        this.command = command;
    }

    @Override
    public String[] getPossibleValuesCommand() {
        return command;
    }

    @Override
    public String apply( String string ) {
        return string;
    }
}
