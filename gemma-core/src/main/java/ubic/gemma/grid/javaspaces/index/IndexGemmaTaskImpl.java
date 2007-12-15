/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.grid.javaspaces.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.gps.spi.CompassGpsInterfaceDevice;
import org.springframework.beans.factory.InitializingBean;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author klc
 * @version $Id: IndexGemmaTaskImpl.java,v 1.2 2007/12/11 06:15:49
 *          paul Exp $
 */
public class IndexGemmaTaskImpl extends BaseSpacesTask implements
		IndexGemmaTask, InitializingBean {

	private Log log = LogFactory.getLog(this.getClass().getName());

	private String taskId = null;
	
	
	private CompassGpsInterfaceDevice geneGps;
	private CompassGpsInterfaceDevice expressionGps;
	private CompassGpsInterfaceDevice arrayGps;
	private CompassGpsInterfaceDevice ontologyGps;
	private CompassGpsInterfaceDevice bibliographicGps;
	private CompassGpsInterfaceDevice probeGps;

	
	public void setArrayGps(CompassGpsInterfaceDevice arrayGps) {
		this.arrayGps = arrayGps;
	}



	public void setBibliographicGps(CompassGpsInterfaceDevice bibliographicGps) {
		this.bibliographicGps = bibliographicGps;
	}



	public void setExpressionGps(CompassGpsInterfaceDevice expressionGps) {
		this.expressionGps = expressionGps;
	}



	public void setOntologyGps(CompassGpsInterfaceDevice ontologyGps) {
		this.ontologyGps = ontologyGps;
	}



	public void setProbeGps(CompassGpsInterfaceDevice probeGps) {
		this.probeGps = probeGps;
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(java.lang.String,
	 *      boolean, boolean)
	 */
	@SuppressWarnings("unchecked")
	public SpacesResult execute(SpacesIndexGemmaCommand indexCommand) {

		super.initProgressAppender(this.getClass());

        try {
            if ( indexCommand.isIndexGene() ) {
                rebuildIndex( geneGps, "Gene index" );
            }
            if ( indexCommand.isIndexEE() ) {
                rebuildIndex( expressionGps, "Expression Experiment index" );
            }
            if ( indexCommand.isIndexAD() ) {
                rebuildIndex( arrayGps, "Array Design index" );
            }
            if ( indexCommand.isIndexOntology() ) {
                rebuildIndex( ontologyGps, "Ontology Index" );
            }
            if ( indexCommand.isIndexBibRef() ) {
                rebuildIndex( bibliographicGps, "Bibliographic Reference Index" );
            }
            if ( indexCommand.isIndexProbe() ) {
                rebuildIndex( probeGps, "Probe Reference Index" );

            }

        } catch ( Exception e ) {
            log.error( e );          
        }
		
		return null;
	}
	
	
	
    protected void rebuildIndex( CompassGpsInterfaceDevice device, String whatIndexingMsg ) throws Exception {

        long time = System.currentTimeMillis();


        log.info( "Rebuilding " + whatIndexingMsg );
        // /device.index();
        CompassUtils.rebuildCompassIndex( device );

        time = System.currentTimeMillis() - time;

        log.info( "Finished rebuilding " + whatIndexingMsg + ".  Took (ms): " + time );
        log.info( " \n " );

    }

	/**
	 * @param gigaSpacesTemplate
	 */
	@Override
	public void setGigaSpacesTemplate(GigaSpacesTemplate gigaSpacesTemplate) {
		super.setGigaSpacesTemplate(gigaSpacesTemplate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		this.taskId = TaskRunningService.generateTaskId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ubic.gemma.grid.javaspaces.SpacesTask#getTaskId()
	 */
	public String getTaskId() {
		return taskId;
	}
}
