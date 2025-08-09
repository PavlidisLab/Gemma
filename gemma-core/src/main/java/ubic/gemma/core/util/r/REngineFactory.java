package ubic.gemma.core.util.r;

import org.rosuda.REngine.REngine;

public interface REngineFactory {

    REngine createREngine() throws Exception;
}
