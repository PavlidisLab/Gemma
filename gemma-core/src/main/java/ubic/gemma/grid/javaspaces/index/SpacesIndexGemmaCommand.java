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

import java.io.Serializable;

import ubic.gemma.grid.javaspaces.SpacesCommand;

/**
 * @author klc
 * @version $Id$
 * 
 */

public class SpacesIndexGemmaCommand extends SpacesCommand implements
		Serializable {

	private static final long serialVersionUID = -8994831072852393919L;

	private boolean compassOn = false;

	private boolean indexAD;

	private boolean indexEE;

	private boolean indexBibRef;

	private boolean indexProbe;

	private boolean indexBioSequence;

	private boolean indexGene;
	
	
	
	
	
	public SpacesIndexGemmaCommand(String taskId, boolean compassOn,
			boolean indexAD, boolean indexEE, boolean indexGene,
			boolean indexProbe, boolean indexBibRef, boolean indexBioSequence) {

		super(taskId);

		this.indexAD = indexAD;
		this.indexEE = indexEE;
		this.indexBibRef = indexBibRef;
		this.indexProbe = indexProbe;
		this.indexGene = indexGene;
		this.indexBioSequence = indexBioSequence;

		this.compassOn = compassOn;
	}

	public boolean isCompassOn() {
		return compassOn;
	}

	public void setCompassOn(boolean compassOn) {
		this.compassOn = compassOn;
	}

	public boolean isIndexAD() {
		return indexAD;
	}

	public void setIndexAD(boolean indexAD) {
		this.indexAD = indexAD;
	}

	public boolean isIndexBibRef() {
		return indexBibRef;
	}

	public void setIndexBibRef(boolean indexBibRef) {
		this.indexBibRef = indexBibRef;
	}

	public boolean isIndexEE() {
		return indexEE;
	}

	public void setIndexEE(boolean indexEE) {
		this.indexEE = indexEE;
	}

	public boolean isIndexGene() {
		return indexGene;
	}

	public void setIndexGene(boolean indexGene) {
		this.indexGene = indexGene;
	}

	public boolean isIndexBioSequence() {
		return indexBioSequence;
	}

	public void setIndexOntology(boolean indexBioSequence) {
		this.indexBioSequence = indexBioSequence;
	}

	public boolean isIndexProbe() {
		return indexProbe;
	}

	public void setIndexProbe(boolean indexProbe) {
		this.indexProbe = indexProbe;
	}

}
