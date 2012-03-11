package ubic.gemma.loader.expression.arrayDesign;

import java.io.InputStream;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;

public interface ArrayDesignProbeRenamingService {

    /**
     * When we encounter two probes with the same name, we add this string along with a unique identifier to the end of
     * the name. This comes into play when the probe name is the sequence name, and the same sequence is used multiple
     * times on the array design.
     */
    public static final String DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR = "___";

    /**
     * @param arrayDesign
     * @param newIdFile Two columns, where first column is old id, second column is new id. If second column is blank,
     *        old name will be retained.
     */
    public abstract void reName( ArrayDesign arrayDesign, InputStream newIdFile );

    public abstract void setArrayDesignService( ArrayDesignService arrayDesignService );

}