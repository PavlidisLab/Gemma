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
 * Fields are separated by tabs and delimited by double quotes (&quot;) 
 * Quote delimitation is required for displaying data properly in Chrome (it merges adjacent tabs (&#09;))
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
	formatData: function(dataToFormat){
		
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
		for (i = 0; i < dataToFormat.items.items.length; i++) { // for every MetaheatmapDatasetGroupPanel
			if (!dataToFormat.items.items[i].isFiltered) {
				orderedGeneIds = [];
				for (r = 0; r < geneOrdering.length; r++) { // for each gene group
					for (s = 0; s < geneOrdering[r].length; s++) { // for each gene
						orderedGeneIds.push(unorderedGeneIds[r][geneOrdering[r][s]]);
						orderedGeneNames.push(unorderedGeneNames[r][geneOrdering[r][s]]);
					}
				}
				
				for (j = 0; j < dataToFormat.items.items[i].items.items.length; j++) { // for every MetaheatmapDatasetColumnGroup
					if (!dataToFormat.items.items[i].items.items[j].isFiltered) {
						for (k = 0; k < dataToFormat.items.items[i].items.items[j].items.items.length; k++) { // for every MetaheatmapAnalysisColumnGroup
							if (!dataToFormat.items.items[i].items.items[j].items.items[k].isFiltered) {
								for (l = 0; l < dataToFormat.items.items[i].items.items[j].items.items[k].items.items.length; l++) { // for every MetaheatmapExpandableColumn
									if (!dataToFormat.items.items[i].items.items[j].items.items[k].items.items[l].isFiltered) {
										var column = dataToFormat.items.items[i].items.items[j].items.items[k].items.items[l];
										
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
	createDiffRow: function(dataColumn, orderedQValues){
			
		// in case factorDescription is empty
		var factorValues
		if (dataColumn.factorDescription) {
			factorValues = dataColumn.factorDescription;
		}
		else {
			var fieldContents;
			for (var field in dataColumn.contrastsFactorValues) {
				fieldContents = dataColumn.contrastsFactorValues[field];
				if (typeof(fieldContents) !== "function" && typeof(fieldContents) !== "undefined") {
					factorValues += fieldContents + ", ";
				}
			}
		}
		
		return {
			type: 'dataRow',
			datasetShortName: dataColumn.datasetShortName.replace('"',"'"),
			rowType: 'basic diff ex',
			factorCategory: (dataColumn.factorCategory) ? dataColumn.factorCategory.replace('"',"'") : dataColumn.factorName.replace('"',"'"),
			factorValue: factorValues.replace('"',"'"), 
			baseline: '',
			perGeneData: ("&quot;"+orderedQValues.join('&quot;&#09;&quot;')+'&quot').replace('"',"'")
		};
		
	},
	createContrastRows: function(column, dataColumn,orderedGeneIds){
	
		// one row for each factor value
		var n;
		var r;
		var geneId;
		var foldChanges;
		var rows = [];
		for (n = 0; n < column.factorValueIds.length; n++) {
			var factorValueName = column.factorValueNames[column.factorValueIds[n]];
			// for each gene id in proper order
			foldChanges = '';
			for (r = 0; r < orderedGeneIds.length; r++) {
				geneId = orderedGeneIds[r];
				if(column.contrastsData.contrasts[geneId] && column.contrastsData.contrasts[geneId][column.factorValueIds[n]]){
					foldChanges += "&quot;"+column.contrastsData.contrasts[geneId][column.factorValueIds[n]].foldChangeValue + "&quot;&#09;";
				}else{
					foldChanges += "&quot;&quot;" + "&#09;";
				}
			}
			
			rows.push( {
			type: 'dataRow',
				datasetShortName: dataColumn.datasetShortName.replace('"',"'"),
				rowType: 'contrast',
				factorCategory: (dataColumn.factorCategory) ? dataColumn.factorCategory.replace('"',"'") : dataColumn.factorName.replace('"',"'"),
				factorValue: factorValueName.replace('"',"'"),
				baseline: column.baselineFactorValue.replace('"',"'"),
				perGeneData: foldChanges.replace('"',"'")
			});
		}
		return rows;
	},
	initComponent: function(){
	Ext.apply(this,{
		items:[{
			ref: 'textAreaPanel',
			xtype:'textarea',
			//width:750,
			//height:350,
			selectOnFocus:true,
			readOnly: true,
			tpl: new Ext.XTemplate('<tpl for=".">'+
				'<tpl if="type==\'headerRow\'">',
				'{text}',
				'</tpl>',
				'<tpl if="type==\'dataRow\'">',
				'&quot;{datasetShortName}&quot;&#09;&quot;{rowType}&quot;&#09;&quot;{factorCategory}&quot;&#09;&quot;{factorValue}&quot;&#09;&quot;{baseline}&quot;&#09;{perGeneData}\n',
				'</tpl>',
				'</tpl>'),
			tplWriteMode: 'append',
			bodyStyle:'white-space: nowrap',
			style:'white-space: nowrap',
			wordWrap:false,
			padding: 7,
			autoScroll:true
		}]
	});
	this.loadData = function(dataToFormat){
		var date = new Date();
		var dateStr = date.getFullYear()+"/"+date.getMonth()+"/"+date.getDate()+" "+date.getHours()+":"+date.getMinutes();
		var queryStart = document.URL.indexOf("Gemma/")+5;
		var url = queryStart > -1 ? document.URL.substr(0, queryStart) : document.URL;
		this.textAreaPanel.update({
			type: 'headerRow',
			text:'\# Generated by Gemma\n'+
					'\# '+dateStr+'\n'+
					'\# \n'+
					'\# If you use this file for your research, please cite the Gemma web site\n'+
					'\# '+url+'\n'+
					'\# \n'+
					'\# This functionality is currently in beta. The file format may change in the near future. \n'+
					'\# Fields are separated by tabs and delimited with double quotes\n'+
					'\# \n'+
					'\# \n'


		});
		
			html:
		var formattedData = this.formatData(dataToFormat);
		var geneNames = this.getOrderedGeneNames(dataToFormat).join('&quot;&#09;&quot;');
		geneNames = "&quot;"+geneNames+"&quot;";
		
		this.textAreaPanel.update({
			type: 'dataRow',
						datasetShortName: 'Experiment',
						rowType: 'Row Type',
						factorCategory: 'Factor',
						factorValue: 'Factor Value(s)',
						baseline: 'Baseline',
						perGeneData: geneNames.replace('"',"'")
					});
		this.textAreaPanel.update(formattedData);
	};
		Gemma.MetaHeatmapDownloadWindow.superclass.initComponent.call(this);
		
	},
	onRender: function(){
		Gemma.MetaHeatmapDownloadWindow.superclass.onRender.apply(this, arguments);
	}
});

// override textArea to allow control of word wrapping
Ext.override(Ext.form.TextArea, {
	initComponent: Ext.form.TextArea.prototype.initComponent.createSequence(function() {
		Ext.applyIf(this, {wordWrap: true});
	}),
	
	onRender: Ext.form.TextArea.prototype.onRender.createSequence(function(ct, position){ 
		this.el.setOverflow('auto');
		if (this.wordWrap === false) {
			if (!Ext.isIE) {
				this.el.set({wrap:'off'})
			} else {
				this.el.dom.wrap = 'off';
			}
		}
		if (this.preventScrollbars === true) {
			this.el.setStyle('overflow', 'hidden');
        }	            	
    })
});