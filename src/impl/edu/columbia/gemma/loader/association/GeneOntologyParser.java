package edu.columbia.gemma.loader.association;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

public interface GeneOntologyParser {
public abstract Method findParseLineMethod( String string ) throws NoSuchMethodException;

    public abstract Map parse( InputStream is, Method m ) throws IOException;

    public abstract Map parseFile( String filename ) throws IOException;
}
