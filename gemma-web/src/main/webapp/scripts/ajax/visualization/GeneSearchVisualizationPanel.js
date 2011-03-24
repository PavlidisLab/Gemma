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
 * Form to allow user to search for genes or show 'random' set of vectors for the experiment.
 */
Gemma.EEDetailsVisualizationWidget = Ext.extend(Ext.Panel, {

	layout : 'border',
	width : 390,
	height : 360,
	frame : true,

	visualizeHandler : function() {

		if (this.visWindow) {
			this.visWindow.close();
			this.visWindow.destroy();
		}

		var geneList = this.geneChooserPanel.getGeneIds();
		var eeId = Ext.get("eeId").getValue();
		var title = '';
		if (geneList.length > 0) {
			title = "Data for selected genes";
		} else {
			geneList = [];
			title = "Data for a 'random' sampling of probes";
		}

		this.visWindow = new Gemma.VisualizationWithThumbsWindow({
					title : title,
					thumbnails : false
				});

		this.visWindow.show({
					params : [[eeId], geneList]
				});
	},

	initComponent : function() {

		this.geneChooserPanel = new Gemma.GeneChooserPanel({
			height : 100,
			width : 400,
			region : 'center',
			id : 'gene-chooser-panel',
			extraButtons : [new Ext.Button({
				id : "visualizeButton",
				text : "Show",
				tooltip : "Click to display data for selected genes, or a 'random' selection of data from this experiment",
				handler : this.visualizeHandler.createDelegate(this, [], false)
			})]
		});

		var allPanel = new Ext.Panel({
					renderTo : 'visualization',
					layout : 'table',
					baseCls : 'x-plain-panel',
					autoHeight : true,
					width : 400,
					items : [this.geneChooserPanel],
					enabled : false
				});

		Gemma.EEDetailsVisualizationWidget.superclass.initComponent.call(this);

		this.geneChooserPanel.on("addgenes", function(geneids) {
					if (this.geneChooserPanel.getGeneIds().length > 1) {
						var cmp = Ext.getCmp("visualizeButton");
						cmp.enable();
					}

				}, this);

		this.geneChooserPanel.on("removegenes", function() {
					if (this.geneChooserPanel.getGeneIds().length < 1) {
						var cmp = Ext.getCmp("visualizeButton");
						cmp.disable();
					}
				}, this);

		/*
		 * We listen to taxon ready event and filter the presets on the taxon.
		 */
		this.geneChooserPanel.toolbar.taxonCombo.on("ready", function(taxon) {
					var foundTaxon = this.geneChooserPanel.toolbar.taxonCombo.setTaxonByCommonName(this.taxon.taxon);
					this.geneChooserPanel.taxonChanged(foundTaxon, false);

				}.createDelegate(this), this);
	}

});