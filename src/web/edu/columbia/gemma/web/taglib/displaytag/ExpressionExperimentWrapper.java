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
package edu.columbia.gemma.web.taglib.displaytag;

import org.displaytag.decorator.TableDecorator;

import edu.columbia.gemma.expression.experiment.ExpressionExperiment;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and
 * http://displaytag.sourceforge.net/10/tut_links.html for explanation of how
 * this works.
 * 
 * @author pavlidis
 * @version $Id: ExpressionExperimentWrapper.java,v 1.3 2006/02/12 00:01:12
 *          pavlidis Exp $
 */
public class ExpressionExperimentWrapper extends TableDecorator {

	/**
	 * @return String
	 */
	public String getDetailsLink() {
		ExpressionExperiment object = (ExpressionExperiment) getCurrentRowObject();
		if (object.getAccession() != null) {
			return "<a href=\"expressionExperimentDetails.html?id="
					+ object.getId() + "\">"
					+ object.getAccession().getAccession() + "</a>";
		}
		return "No accession";
	}

	/**
	 * @return String
	 */
	public String getAssaysLink() {
		ExpressionExperiment object = (ExpressionExperiment) getCurrentRowObject();
		if (object.getBioAssays() != null) {
			return "<a href=\"expressionExperimentDetails.html?id="
					+ object.getId() + "\">" + object.getBioAssays().size()
					+ "</a>";
		}
		return "No bioassays";
	}

	/**
	 * @return String
	 */
	public String getDesignsLink() {
		ExpressionExperiment object = (ExpressionExperiment) getCurrentRowObject();
		if (object.getExperimentalDesigns() != null) {
			return "<a href=\"expressionExperimentDetails.html?id="
					+ object.getId() + "\">"
					+ object.getExperimentalDesigns().size() + "</a>";
		}
		return "No design";
	}

	public String getNameLink() {
		ExpressionExperiment object = (ExpressionExperiment) getCurrentRowObject();
		if (object.getExperimentalDesigns() != null) {
			return "<a href=\"showExpressionExperiment.html?name="
					+ object.getName() + "\">" + object.getName() + "</a>";
		}
		return "No design";
	}

}
