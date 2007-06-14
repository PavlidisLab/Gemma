package ubic.gemma.gemmaspaces;

import org.springframework.context.ApplicationContext;

import ubic.gemma.util.gemmaspaces.GemmaSpacesEnum;
import ubic.gemma.util.gemmaspaces.GemmaSpacesUtil;

public abstract class AbstractGemmaSpacesService {

    protected GemmaSpacesUtil gigaSpacesUtil = null;

    protected ApplicationContext updatedContext = null;

    /**
     * @return ApplicationContext
     */
    public ApplicationContext addGigaspacesToApplicationContext() {
        if ( gigaSpacesUtil == null ) gigaSpacesUtil = new GemmaSpacesUtil();

        return gigaSpacesUtil.addGemmaSpacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    /**
     * Controllers extending this class must implement this method. The implementation should call
     * injectGigaspacesUtil(GigaSpacesUtil gigaSpacesUtil) to "inject" a spring loaded GigaSpacesUtil into this abstract
     * class.
     * 
     * @param gigaSpacesUtil
     */
    abstract protected void setGigaSpacesUtil( GemmaSpacesUtil gigaSpacesUtil );

    /**
     * @param gigaSpacesUtil
     */
    protected void injectGigaspacesUtil( GemmaSpacesUtil gigaspacesUtil ) {
        this.gigaSpacesUtil = gigaspacesUtil;
    }

}
