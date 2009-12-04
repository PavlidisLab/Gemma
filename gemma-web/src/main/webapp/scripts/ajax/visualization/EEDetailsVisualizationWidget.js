/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace('Gemma');

/**
 * Basic form to allow user to search for genes or show 'random' set of vectors for the experiment.
 * 
 * @author paul, based on older code.
 * @version $Id$
 */
Gemma.EEDetailsVisualizationWidget = Ext.extend(Gemma.GeneGrid, {

	height : 120,
	width : 420,

	name : 'eedvw',

	vizButtonId : "visualizeButton-" + Ext.id(),

	initComponent : function() {

		// has to be done after constructor is done creating the handler...
		Ext.apply(this, {
			extraButtons : [new Ext.Button({
				id : this.vizButtonId,
				text : "Show",
				tooltip : "Click to display data for selected genes, or a 'random' selection of data from this experiment",
				handler : this.showButHandler,
				scope : this
			})]
		});

		Gemma.EEDetailsVisualizationWidget.superclass.initComponent.call(this);

		this.on('ready', function(taxon) {
					/*
					 * taxon is the one filled in by staterestore; we need to enforce that we pick an exact taxon. Cant
					 * use getToolbar() at this point.
					 */
					var foundTaxon = this.getTopToolbar().taxonCombo.setTaxonByCommonName(this.taxon);
					this.taxonChanged(foundTaxon, false);
					this.getTopToolbar().taxonCombo.disable(false);
				});

	},

	showButHandler : function() {

		if (this.visWindow) {
			this.visWindow.close();
			this.visWindow.destroy();
		}

		var geneList = this.getGeneIds();
		var eeId = Ext.get("eeId").getValue();
		var title = '';
		var downloadLink = '';
		if (geneList.length > 0) {
			title = "Data for selected genes";
			downloadLink = String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&g={1}", eeId, geneList.join(','));
		} else {
			geneList = [];
			title = "Data for a 'random' sampling of probes", downloadLink = String.format(
					"/Gemma/dedv/downloadDEDV.html?ee={0}", eeId);

		}

		this.visWindow = new Gemma.VisualizationWithThumbsWindow({
					title : title,
					thumbnails : false,
					downloadLink : downloadLink
				});

		this.visWindow.show({
					params : [[eeId], geneList]
				});
	}

});