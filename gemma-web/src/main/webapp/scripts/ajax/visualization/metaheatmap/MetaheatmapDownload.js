Ext.namespace('Gemma');

/**
 * Takes the data structure from metaheatmap and parses it into a delimited text format
 * representing both the basic differential expression data and the contrast data (if a column is expanded)
 *
 * Metaheatmap data must be passed in as a config called "dataToFormat"
 * (should be = applicationRoot._imageArea._heatmapArea)
 *
 * Filters and sorting are taken into account.
 *
 * Fields are separated by tabs and delimited by double quotes (\")
 * Quote delimitation is required for displaying data properly in Chrome (it merges adjacent tabs (\t))
 * All text used in data rows (as opposed to header rows) are checked for double quotes. If found, each is replaced with a single quote.
 *
 */
Gemma.MetaHeatmapDownloadWindow= Ext.extend(Ext.Window, {
	// maybe should make a delim field so it's easily changed
	dataToFormat: null, // should be overridden by instantiation configs
	bodyStyle: 'padding: 7px',
	width:800,
	height: 400,
	layout:'fit',
	title: 'Text Display of Differential Expression Visualization',
	// use the data to make text
	getOrderedGeneNames: function(dataToFormat){
		var geneOrdering = dataToFormat.applicationRoot.geneOrdering;
		
		var unorderedGeneIds = dataToFormat.geneIds;
		var unorderedGeneNames = dataToFormat.geneNames;
		var orderedGeneIds = [];
		var orderedGeneNames = [];
		
		var i;
		var j;
		var k;
		var s;
		var l;
		var m;
		for (r = 0; r < geneOrdering.length; r++) { // for each gene group
			for (s = 0; s < geneOrdering[r].length; s++) { // for each gene
				orderedGeneIds.push(unorderedGeneIds[r][geneOrdering[r][s]]);
				orderedGeneNames.push(unorderedGeneNames[r][geneOrdering[r][s]]);
			}
		}
		return orderedGeneNames;
	},
	formatData: function (dataToFormat) {
		
		//dataToFormat = this._imageArea._heatmapArea
		var geneOrdering = dataToFormat.applicationRoot.geneOrdering;

		var unorderedGeneIds = dataToFormat.geneIds;
		var unorderedGeneNames = dataToFormat.geneNames;
		var unorderedQValues = [];
		var orderedGeneIds = [];
		var orderedGeneNames = [];
		var orderedQValues = [];
				
		var formattedData = [];
		
		var i;
		var j;
		var k;
		var r;
		var s;
		var l;
		var m;
		for (i = 0; i < dataToFormat.items.getCount(); i++) { // for every MetaheatmapDatasetGroupPanel
			if (!dataToFormat.get(i).isFiltered) {
				orderedGeneIds = [];
				for (r = 0; r < geneOrdering.length; r++) { // for each gene group
					for (s = 0; s < geneOrdering[r].length; s++) { // for each gene
						orderedGeneIds.push(unorderedGeneIds[r][geneOrdering[r][s]]);
						orderedGeneNames.push(unorderedGeneNames[r][geneOrdering[r][s]]);
					}
				}
				
				for (j = 0; j < dataToFormat.items.get(i).items.getCount(); j++) { // for every MetaheatmapDatasetColumnGroup
					if (!dataToFormat.items.get(i).items.get(j).isFiltered) {
						for (k = 0; k < dataToFormat.items.get(i).items.get(j).items.getCount(); k++) { // for every MetaheatmapAnalysisColumnGroup
							if (!dataToFormat.items.get(i).items.get(j).items.get(k).isFiltered) {
								for (l = 0; l < dataToFormat.items.get(i).items.get(j).items.get(k).items.getCount(); l++) { // for every MetaheatmapExpandableColumn
									if (!dataToFormat.items.get(i).items.get(j).items.get(k).items.get(l).isFiltered) {
										var column = dataToFormat.items.get(i).items.get(j).items.get(k).items.get(l);
										
											var dataColumn = column.dataColumn;
											
											unorderedQValues = dataColumn.qValues;
											orderedQValues = [];
											for (r = 0; r < geneOrdering.length; r++) { // for each gene group
												for (s = 0; s < geneOrdering[r].length; s++) { // for each gene
													orderedQValues.push(unorderedQValues[r][geneOrdering[r][s]]);
												}
											}
											// write q value row
											formattedData.push(this.createDiffRow(dataColumn, orderedQValues));
											
											// maybe write contrast row
											if (column.expandButton_.pressed) {
												formattedData = formattedData.concat(this.createContrastRows(column, dataColumn,orderedGeneIds));
											}										
									}
								}
							}
						}
					}
				}
			}
		}
		return formattedData;
	},
	createDiffRow: function (dataColumn, orderedQValues){
		var factorValues = "";
		for (var i = 0; i < dataColumn.contrastsFactorValueIds.length; i++) {
			factorValues += dataColumn.contrastsFactorValues [ dataColumn.contrastsFactorValueIds[i] ].trim() + ", ";
		}				
		return {
			type: 'dataRow',
			datasetShortName: (dataColumn.datasetShortName)? dataColumn.datasetShortName.replace('"',"'"):'',
			rowType: 'q value',
			factorCategory: (dataColumn.factorCategory) ? dataColumn.factorCategory.replace('"',"'") : ((dataColumn.factorName)?dataColumn.factorName.replace('"',"'"):''),
			factorValue: factorValues.replace('"',"'"), 
			baseline: '',
			perGeneData: ("\""+orderedQValues.join('\"\t\"')+'&quot').replace('"',"'")
		};
		
	},
	createContrastRows: function (column, dataColumn,orderedGeneIds){
	
		// one row for each factor value
		var n;
		var r;
		var geneId;
		var foldChanges = '';
		var contrastPValues = '';
		var rows = [];
		for (n = 0; n < column.factorValueIds.length; n++) {
			var factorValueName = column.factorValueNames[column.factorValueIds[n]];
			// for each gene id in proper order
			foldChanges = '';
			contrastPValues = '';
			var fc = ''; var pval = '';
			for (r = 0; r < orderedGeneIds.length; r++) {
				geneId = orderedGeneIds[r];
				if(column.contrastsData.contrasts[geneId] && column.contrastsData.contrasts[geneId][column.factorValueIds[n]]){
					fc = column.contrastsData.contrasts[geneId][column.factorValueIds[n]].foldChangeValue;
					pval = column.contrastsData.contrasts[geneId][column.factorValueIds[n]].contrastPvalue;
					foldChanges += "\""+ fc + "\"\t";
					contrastPValues += "\""+ pval + "\"\t";
				}else{
					foldChanges += "\"\"" + "\t";
					contrastPValues += "\"\"" + "\t";
				}
			}
			
			rows.push ({type: 'dataRow',
						datasetShortName: (dataColumn.datasetShortName)? dataColumn.datasetShortName.replace('"',"'"):'',
						rowType: 'contrast log2 fold change',
						factorCategory: (dataColumn.factorCategory) ? dataColumn.factorCategory.replace('"',"'") : ((dataColumn.factorName)? dataColumn.factorName.replace('"',"'"):''),
						factorValue: (factorValueName)? factorValueName.replace('"',"'"):'',
						baseline: (dataColumn.baselineFactorValue) ? dataColumn.baselineFactorValue.replace('"',"'"):'',
						perGeneData: foldChanges
			});
						
			rows.push ({type: 'dataRow',
						datasetShortName: (dataColumn.datasetShortName)? dataColumn.datasetShortName.replace('"',"'"):'',
						rowType: 'contrast q value',
						factorCategory: (dataColumn.factorCategory) ? dataColumn.factorCategory.replace('"',"'") : ((dataColumn.factorName)? dataColumn.factorName.replace('"',"'"):''),
						factorValue: (factorValueName)? factorValueName.replace('"',"'"):'',
						baseline: (dataColumn.baselineFactorValue) ? dataColumn.baselineFactorValue.replace('"',"'"):'',
						perGeneData: contrastPValues
			});
		}
		return rows;
	},
	textAreaPanel : new Ext.form.TextArea({
		//xtype: 'textarea',
		//width:750,
		//height:350,
		readOnly: true,
		// tabs don't show up in Chrome, but \t doesn't work any better than \t
		tpl: new Ext.XTemplate('<tpl for=".">' ,
				'<tpl if="type==\'headerRow\'">', '{text}', '</tpl>', '<tpl if="type==\'dataRow\'">', 
				'\"{datasetShortName}\"\t\"{rowType}\"\t\"{factorCategory}\"\t\"',
				'{factorValue}\"\t\"{baseline}\"\t{perGeneData}\n', 
				'</tpl>', '</tpl>'),
				tplWriteMode: 'append',
				bodyStyle: 'white-space: nowrap',
				style: 'white-space: nowrap',
				wordWrap: false,
				padding: 7,
				autoScroll: true
	}),
	initComponent: function(){
		Ext.apply(this, {	
			tbar:[{
				ref: 'selectAllButton',
				xtype: 'button',
				text: 'Select All',
				scope: this,
				handler: function(){
					this.textAreaPanel.selectText();
				}
			}],
			items:[new Ext.form.TextArea({
				ref: 'textAreaPanel',
                readOnly: true,
				// tabs don't show up in Chrome, but \t doesn't work any better than \t
                tpl: new Ext.XTemplate('<tpl for=".">' ,
                	'<tpl if="type==\'headerRow\'">', '{text}', '</tpl>', 
					'<tpl if="type==\'dataRow\'">', 
					'\"{datasetShortName}\"\t\"{rowType}\"\t\"{factorCategory}\"\t\"',
					'{factorValue}\"\t\"{baseline}\"\t{perGeneData}\r\n', 
					'</tpl>', '</tpl>'),
                tplWriteMode: 'append',
                bodyStyle: 'white-space: nowrap',
                style: 'white-space: nowrap',
                wordWrap: false,
                padding: 7,
                autoScroll: true
            })]
		});        

	this.loadData = function (data) {
		var timeStamp = new Date();
		// make minutes double digits
		var min = (timeStamp.getMinutes()<10)?'0'+timeStamp.getMinutes(): timeStamp.getMinutes();
		var timeStampString = timeStamp.getFullYear()+"/"+timeStamp.getMonth()+"/"+timeStamp.getDate()+" "+timeStamp.getHours()+":"+min;
		this.textAreaPanel.update({
			type: 'headerRow',
			text:'\# Generated by Gemma\n'+
					'\# '+timeStampString+'\n'+
					'\# \n'+
					'\# If you use this file for your research, please cite the Gemma web site\n'+
					'\# chibi.ubc.ca/Gemma \n'+
					'\# \n'+
					'\# This functionality is currently in beta. The file format may change in the near future. \n'+
					'\# Fields are separated by tabs and delimited with double quotes\n'+
					'\# \n'
		});
		
		var formattedData = this.formatData (data);
		var geneNames = this.getOrderedGeneNames (data).join('\"\t\"');
		geneNames = "\""+geneNames+"\"";
		
		this.textAreaPanel.update({
			type: 'dataRow',
			datasetShortName: 'Experiment',
			rowType: 'Row Type',
			factorCategory: 'Factor',
			factorValue: 'Factor Value(s)',
			baseline: 'Baseline',
			perGeneData: geneNames.replace('"',"'")
		});
		this.textAreaPanel.update (formattedData);
	};
	
		Gemma.MetaHeatmapDownloadWindow.superclass.initComponent.call (this);		
	},
	onRender: function(){
		Gemma.MetaHeatmapDownloadWindow.superclass.onRender.apply (this, arguments);
	}
    
});

// Override textArea to allow control of word wrapping
// just adds a wordWrap config field to textArea
// from here: http://www.sencha.com/forum/showthread.php?52122-preventing-word-wrap-in-textarea
Ext.override(Ext.form.TextArea, {
    initComponent: Ext.form.TextArea.prototype.initComponent.createSequence(function(){
        Ext.applyIf(this, {
            wordWrap: true
        });
    }),
    
    onRender: Ext.form.TextArea.prototype.onRender.createSequence(function(ct, position){
        this.el.setOverflow('auto');
        if (this.wordWrap === false) {
            if (!Ext.isIE) {
                this.el.set({
                    wrap: 'off'
                });
            }
            else {
                this.el.dom.wrap = 'off';
            }
        }
        if (this.preventScrollbars === true) {
            this.el.setStyle('overflow', 'hidden');
        }
    })
});
