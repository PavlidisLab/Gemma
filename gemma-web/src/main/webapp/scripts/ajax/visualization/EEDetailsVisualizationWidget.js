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

	height : 220,
	width : 550,
	usingPanel: false, //whether we're using vizualisation window or panel
	name : 'eedvw',

	vizButtonId : "visualizeButton-" + Ext.id(),

	initComponent : function() {

		// has to be done after constructor is done creating the handler...
		this.geneGroupCombo = new Gemma.GeneGroupCombo({
					id : "visGeneGroupCombo",
					listeners : {
						'select' : {
							fn : function(combo, record, index) {

								var loadMask = new Ext.LoadMask(this.getEl(), {
											msg : "Loading Genes for " + record.get('name') + " ..."
										});
								loadMask.show();

								this.loadGenes(record.get('geneIds'), function() {
											loadMask.hide();
										});
							},
							scope : this
						}
					}
				});
		Ext.apply(this, {
			extraButtons : [this.geneGroupCombo, new Ext.Button({
						text : "Clear",
						tooltip : "Clear gene selection",
						handler : this.clearButHandler,
						scope : this
					}),{
						xtype : 'tbfill'
					},
					
					new Ext.Button({
						id : this.vizButtonId,
						text : "Visualize",
						tooltip : "Click to display data for selected genes, or a 'random' selection of data from this experiment",
						handler : this.showButHandler,
						scope : this,
						cls: 'x-toolbar-standardbutton'
					}), new Ext.Button({
						icon : Gemma.ICONURL + 'information.png',
						// text : 'help',
						tootltip : "Get help",
						handler : function() {
							Ext.Msg.show({
								title : 'Visualization',
								msg : 'Use the search fields to find individual genes, or groups of genes. ' +
										'Gene group searches work for GO terms and other groups in Gemma. ' +
										'To create groups use the <a href=\"/Gemma/geneGroupManager.html\">gene group manager</a>.' +
										' Click "show" to view the data for those genes. ' +
										'Note that when viewing gene groups, not all genes in the group are necessarily in the data set.',
								buttons : Ext.Msg.OK,
								icon : Ext.MessageBox.INFO
							});
						}.createDelegate(this)
					})]
		});

		Gemma.EEDetailsVisualizationWidget.superclass.initComponent.call(this);

		this.on('ready', function() {
					/*
					 * Taxon is passed in during construction.
					 */
					var foundTaxon = this.getTopToolbar().taxonCombo.setTaxonByCommonName(this.taxon.commonName);
					this.getTopToolbar().taxonCombo.hide();
					this.geneGroupCombo.taxon = this.taxon;
					this.taxonChanged(foundTaxon, false);
					this.getTopToolbar().taxonCombo.disable(false);
				});

	},

	clearButHandler: function(){
		this.removeAllGenes();
	},
	showButHandler : function() {

		if (this.visWindow) {
			this.visWindow.close();
			this.visWindow.destroy();
		}

		var geneList = this.getGeneIds();
		var eeId = (Ext.get("eeId"))?Ext.get("eeId").getValue():this.eeId;
		var title = '';
		var downloadLink = '';
		if (geneList.length > 0) {
			title = "Data for selected genes";
			downloadLink = String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&g={1}", eeId, geneList.join(','));
		} else {
			geneList = [];
			title = "Data for a 'random' sampling of probes"; 
			downloadLink = String.format("/Gemma/dedv/downloadDEDV.html?ee={0}", eeId);
		}
		if (this.usingPanel && this.visPanel) {
			this.visPanel.loadFromParam({
					params: [[eeId], geneList]
				});
		}
		else {
			this.visWindow = new Gemma.VisualizationWithThumbsWindow({
				title: title,
				thumbnails: false,
				downloadLink: downloadLink
			});
			
			this.visWindow.show({
				params: [[eeId], geneList]
			});
		}
	}

});