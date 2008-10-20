/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import ubic.gemma.grid.javaspaces.TaskResult;

/**
 * 
 * @author klc
 * 
 */

public class IndexerResult extends TaskResult {


	private static final long serialVersionUID = -150285942553712429L;

	protected String pathToExpresionIndex = null;

	protected String pathToArrayIndex = null;

	protected String pathToGeneIndex = null;

	protected String pathToProbeIndex = null;

	protected String pathToBiosequenceIndex = null;

	protected String pathToBibliographicIndex = null;

	public String getPathToArrayIndex() {
		return pathToArrayIndex;
	}

	public void setPathToArrayIndex(String pathToArrayIndex) {
		this.pathToArrayIndex = pathToArrayIndex;
	}

	public String getPathToBibliographicIndex() {
		return pathToBibliographicIndex;
	}

	public void setPathToBibliographicIndex(String pathToBibliographicIndex) {
		this.pathToBibliographicIndex = pathToBibliographicIndex;
	}

	public String getPathToBiosequenceIndex() {
		return pathToBiosequenceIndex;
	}

	public void setPathToBiosequenceIndex(String pathToBiosequenceIndex) {
		this.pathToBiosequenceIndex = pathToBiosequenceIndex;
	}

	public String getPathToExpressionIndex() {
		return pathToExpresionIndex;
	}

	public void setPathToExpresionIndex(String pathToExpresionIndex) {
		this.pathToExpresionIndex = pathToExpresionIndex;
	}

	public String getPathToGeneIndex() {
		return pathToGeneIndex;
	}

	public void setPathToGeneIndex(String pathToGeneIndex) {
		this.pathToGeneIndex = pathToGeneIndex;
	}

	public String getPathToProbeIndex() {
		return pathToProbeIndex;
	}

	public void setPathToProbeIndex(String pathToProbeIndex) {
		this.pathToProbeIndex = pathToProbeIndex;
	}

}
