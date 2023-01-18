package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(type = "string")
public class StringArg extends AbstractArg<String> {

    public StringArg( String s ) {
        super( s );
    }

    public StringArg valueOf( String s ) {
        return new StringArg( s );
    }
}
