/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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


package ubic.gemma.web.controller.compass;

import java.io.Serializable;

/**
 * This class contains the options for indexing.  
 * 
 * @author klc
 *
 */


public class IndexGemmaCommand implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1710257727263881366L;
	
	
	private boolean indexArray;
	private boolean indexEE;
	private boolean indexProbe;
	private boolean indexBibliographic;
	private boolean indexGene;
	private boolean indexBioSequence;
	
	
	
	public boolean isIndexArray() {
		return indexArray;
	}
	public void setIndexArray(boolean indexArray) {
		this.indexArray = indexArray;
	}
	public boolean isIndexBibliographic() {
		return indexBibliographic;
	}
	public void setIndexBibliographic(boolean indexBibliographic) {
		this.indexBibliographic = indexBibliographic;
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
	public boolean isIndexProbe() {
		return indexProbe;
	}
	public void setIndexProbe(boolean indexProbe) {
		this.indexProbe = indexProbe;
	}
	public boolean isIndexBioSequence() {
		return indexBioSequence;
	}
	public void setIndexBioSequence(boolean indexBioSequence) {
		this.indexBioSequence = indexBioSequence;
	}
	
	
	

}
