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
package ubic.gemma.search;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.spi.InternalCompass;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

import ubic.gemma.util.CompassUtils;

/**
 * Services for updating the search indexes.
 * 
 * @author keshav
 * @version $Id$
 */
public class IndexService {

	/*
	 * Note: IndexService has not been configured with xdoclet tags because we
	 * it needs to reside in applicationContext-search.xml so we can choose
	 * whether or not to load this part of the application context at spring
	 * startup.
	 */

	private Log log = LogFactory.getLog(this.getClass());

	private CompassGpsInterfaceDevice expressionGps;

	private CompassGpsInterfaceDevice geneGps;

	private CompassGpsInterfaceDevice arrayGps;

	private CompassGpsInterfaceDevice probeGps;

	private CompassGpsInterfaceDevice biosequenceGps;

	private CompassGpsInterfaceDevice bibliographicGps;

	private InternalCompass compassExpression;

	private InternalCompass compassGene;

	private InternalCompass compassArray;

	private InternalCompass compassProbe;

	private InternalCompass compassBiosequence;

	private InternalCompass compassBibliographic;

	/**
	 * Indexes expression experiments, genes, array designs, probes and
	 * bibliographic references. This is a convenience method for Quartz to
	 * schedule indexing of the entire database.
	 */
	public void indexAll() {
		log.debug("rebuilding compass index");
		CompassUtils.rebuildCompassIndex(expressionGps);
		CompassUtils.rebuildCompassIndex(geneGps);
		CompassUtils.rebuildCompassIndex(arrayGps);
		CompassUtils.rebuildCompassIndex(probeGps);
		CompassUtils.rebuildCompassIndex(biosequenceGps);
		CompassUtils.rebuildCompassIndex(bibliographicGps);
	}

	/**
	 * Indexes array designs.
	 */
	public void indexArrayDesigns() {
		CompassUtils.rebuildCompassIndex(arrayGps);
	}

	/**
	 * Indexes bibliographic references.
	 */
	public void indexBibligraphicReferences() {
		CompassUtils.rebuildCompassIndex(bibliographicGps);
	}

	/**
	 * Indexes probes
	 */
	public void indexCompositeSequences() {
		CompassUtils.rebuildCompassIndex(probeGps);
	}

	/**
	 * Indexes expression experiments.
	 */
	public void indexExpressionExperiments() {
		CompassUtils.rebuildCompassIndex(expressionGps);
	}

	/**
	 * Indexes genes.
	 */
	public void indexGenes() {
		CompassUtils.rebuildCompassIndex(geneGps);
	}

	/**
	 * @param pathToNewIndex
	 * @throws IOException
	 */
	public void replaceExperimentIndex(String pathToNewIndex)
			throws IOException {
		CompassUtils.swapCompassIndex(compassExpression, pathToNewIndex);
	}

	/**
	 * @param pathToNewIndex
	 * @throws IOException
	 */
	public void replaceGeneIndex(String pathToNewIndex) throws IOException {
		CompassUtils.swapCompassIndex(compassGene, pathToNewIndex);
	}

	/**
	 * @param pathToNewIndex
	 * @throws IOException
	 */
	public void replaceProbeIndex(String pathToNewIndex) throws IOException {
		CompassUtils.swapCompassIndex(compassProbe, pathToNewIndex);
	}

	/**
	 * @param pathToNewIndex
	 * @throws IOException
	 */
	public void replaceArrayIndex(String pathToNewIndex) throws IOException {
		CompassUtils.swapCompassIndex(compassArray, pathToNewIndex);
	}

	/**
	 * @param pathToNewIndex
	 * @throws IOException
	 */
	public void replaceBiosequenceIndex(String pathToNewIndex)
			throws IOException {
		CompassUtils.swapCompassIndex(compassBiosequence, pathToNewIndex);
	}

	/**
	 * @param pathToNewIndex
	 * @throws IOException
	 */
	public void replaceBibliographicIndex(String pathToNewIndex)
			throws IOException {
		CompassUtils.swapCompassIndex(compassBibliographic, pathToNewIndex);
	}

	/**
	 * @param arrayGps
	 *            The arrayGps to set.
	 */
	public void setArrayGps(CompassGpsInterfaceDevice arrayGps) {
		this.arrayGps = arrayGps;
	}

	public void setBibliographicGps(CompassGpsInterfaceDevice bibliographicGps) {
		this.bibliographicGps = bibliographicGps;
	}

	/**
	 * @param expressionGps
	 *            The expressionGps to set.
	 */
	public void setExpressionGps(CompassGpsInterfaceDevice expressionGps) {
		this.expressionGps = expressionGps;
	}

	/**
	 * @param geneGps
	 *            The geneGps to set.
	 */
	public void setGeneGps(CompassGpsInterfaceDevice geneGps) {
		this.geneGps = geneGps;
	}

	/**
	 * @param probeGps
	 */
	public void setProbeGps(CompassGpsInterfaceDevice probeGps) {
		this.probeGps = probeGps;
	}

	/**
	 * @param biosequenceGps
	 */
	public void setBiosequenceGps(CompassGpsInterfaceDevice biosequenceGps) {
		this.biosequenceGps = biosequenceGps;
	}

	public void setCompassArray(InternalCompass compassArray) {
		this.compassArray = compassArray;
	}

	public void setCompassBibliographic(InternalCompass compassBibliographic) {
		this.compassBibliographic = compassBibliographic;
	}

	public void setCompassBiosequence(InternalCompass compassBiosequence) {
		this.compassBiosequence = compassBiosequence;
	}

	public void setCompassExpression(InternalCompass compassExpression) {
		this.compassExpression = compassExpression;
	}

	public void setCompassGene(InternalCompass compassGene) {
		this.compassGene = compassGene;
	}

	public void setCompassProbe(InternalCompass compassProbe) {
		this.compassProbe = compassProbe;
	}

}
