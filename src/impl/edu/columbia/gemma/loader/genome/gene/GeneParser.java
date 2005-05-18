package edu.columbia.gemma.loader.genome.gene;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;

public interface GeneParser {

    public abstract Map parseFile( String filename ) throws IOException, ConfigurationException;

}