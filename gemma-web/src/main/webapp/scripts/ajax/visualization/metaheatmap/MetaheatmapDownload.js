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
Gemma.Metaheatmap.DownloadWindow = Ext.extend (Ext.Window, {
	// maybe should make a delim field so it's easily changed
	dataToFormat: null, // should be overridden by instantiation configs
	bodyStyle: 'padding: 7px',
	width:800,
	height: 400,
	layout:'fit',
	title: 'Text Display of Differential Expression Visualization',
	
//	formatData: function (dataToFormat) {
//		var formattedData = [];
//		
//		for (i = 0; i < dataToFormat.items.getCount(); i++) { // for every MetaheatmapDatasetGroupPanel
//			if (!dataToFormat.get(i).isFiltered) {
//				orderedGeneIds = [];
//				for (r = 0; r < geneOrdering.length; r++) { // for each gene group
//					for (s = 0; s < geneOrdering[r].length; s++) { // for each gene
//						orderedGeneIds.push(unorderedGeneIds[r][geneOrdering[r][s]]);
//						orderedGeneNames.push(unorderedGeneNames[r][geneOrdering[r][s]]);
//					}
//				}
//				
//				for (j = 0; j < dataToFormat.items.get(i).items.getCount(); j++) { // for every MetaheatmapDatasetColumnGroup
//					if (!dataToFormat.items.get(i).items.get(j).isFiltered) {
//						for (k = 0; k < dataToFormat.items.get(i).items.get(j).items.getCount(); k++) { // for every MetaheatmapAnalysisColumnGroup
//							if (!dataToFormat.items.get(i).items.get(j).items.get(k).isFiltered) {
//								for (l = 0; l < dataToFormat.items.get(i).items.get(j).items.get(k).items.getCount(); l++) { // for every MetaheatmapExpandableColumn
//									if (!dataToFormat.items.get(i).items.get(j).items.get(k).items.get(l).isFiltered) {
//										var column = dataToFormat.items.get(i).items.get(j).items.get(k).items.get(l);
//										
//											var dataColumn = column.dataColumn;
//											
//											unorderedQValues = dataColumn.qValues;
//											orderedQValues = [];
//											for (r = 0; r < geneOrdering.length; r++) { // for each gene group
//												for (s = 0; s < geneOrdering[r].length; s++) { // for each gene
//													orderedQValues.push(unorderedQValues[r][geneOrdering[r][s]]);
//												}
//											}
//											// write q value row
//											formattedData.push(this.createDiffRow(dataColumn, orderedQValues));
//											
//											// maybe write contrast row
//											if (column.expandButton_.pressed) {
//												formattedData = formattedData.concat(this.createContrastRows(column, dataColumn,orderedGeneIds));
//											}										
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		return formattedData;
//	},
	
//	createContrastRows: function (column, dataColumn, orderedGeneIds){
//	
//		// one row for each factor value
//		var n;
//		var r;
//		var geneId;
//		var foldChanges = '';
//		var contrastPValues = '';
//		var rows = [];
//		for (n = 0; n < column.factorValueIds.length; n++) {
//			var factorValueName = column.factorValueNames[column.factorValueIds[n]];
//			// for each gene id in proper order
//			foldChanges = '';
//			contrastPValues = '';
//			var fc = ''; var pval = '';
//			for (r = 0; r < orderedGeneIds.length; r++) {
//				geneId = orderedGeneIds[r];
//				if(column.contrastsData.contrasts[geneId] && column.contrastsData.contrasts[geneId][column.factorValueIds[n]]){
//					fc = column.contrastsData.contrasts[geneId][column.factorValueIds[n]].foldChangeValue;
//					pval = column.contrastsData.contrasts[geneId][column.factorValueIds[n]].contrastPvalue;
//					foldChanges += "\""+ fc + "\"\t";
//					contrastPValues += "\""+ pval + "\"\t";
//				}else{
//					foldChanges += "\"\"" + "\t";
//					contrastPValues += "\"\"" + "\t";
//				}
//			}
//			
//			rows.push ({type: 'dataRow',
//						datasetShortName: (dataColumn.datasetShortName)? dataColumn.datasetShortName.replace('"',"'"):'',
//						rowType: 'contrast log2 fold change',
//						factorCategory: (dataColumn.factorCategory) ? dataColumn.factorCategory.replace('"',"'") : ((dataColumn.factorName)? dataColumn.factorName.replace('"',"'"):''),
//						factorValue: (factorValueName)? factorValueName.replace('"',"'"):'',
//						baseline: (dataColumn.baselineFactorValue) ? dataColumn.baselineFactorValue.replace('"',"'"):'',
//						perGeneData: foldChanges
//			});
//						
//			rows.push ({type: 'dataRow',
//						datasetShortName: (dataColumn.datasetShortName)? dataColumn.datasetShortName.replace('"',"'"):'',
//						rowType: 'contrast q value',
//						factorCategory: (dataColumn.factorCategory) ? dataColumn.factorCategory.replace('"',"'") : ((dataColumn.factorName)? dataColumn.factorName.replace('"',"'"):''),
//						factorValue: (factorValueName)? factorValueName.replace('"',"'"):'',
//						baseline: (dataColumn.baselineFactorValue) ? dataColumn.baselineFactorValue.replace('"',"'"):'',
//						perGeneData: contrastPValues
//			});
//		}
//		return rows;
//	},
	
//	textAreaPanel : new Ext.form.TextArea({
//		//xtype: 'textarea',
//		//width:750,
//		//height:350,
//		readOnly: true,
//		// tabs don't show up in Chrome, but \t doesn't work any better than \t
////		tpl: new Ext.XTemplate('<tpl for=".">' ,
////				'<tpl if="type==\'headerRow\'">',
////				'{text}',
////				'</tpl>',
////				'<tpl if="type==\'dataRow\'">', 
////				'\"{datasetShortName}\"\t\"{rowType}\"\t\"{factorCategory}\"\t\"',
////				'{factorValue}\"\t\"{baseline}\"\t{perGeneData}\n', 
////				'</tpl>',
////				'</tpl>'),
////				tplWriteMode: 'append',
//				bodyStyle: 'white-space: nowrap',
//				style: 'white-space: nowrap',
//				wordWrap: false,
//				padding: 7,
//				autoScroll: true
//	}),

	fixQuotes : function (str) {
		return str.replace ('"',"'");
	},
	
	timeToString : function (timeStamp) {
		// Make minutes double digits.
		var min = (timeStamp.getMinutes()<10)?'0'+timeStamp.getMinutes(): timeStamp.getMinutes();
		return timeStamp.getFullYear()+"/"+timeStamp.getMonth()+"/"+timeStamp.getDate()+" "+timeStamp.getHours()+":"+min;
	},
		
	makeHeaderRow : function (conditionTree) {
		var row = [];		
		row.push ("Gene");
		for (var i = 0; i < conditionTree.items.length; i++) {		
			var condition = conditionTree.items[0];
				row.push (condition.contrastFactorValue + " vs " + condition.baselineFactorValue + " : " + condition.datasetShortName);				
		}
		return row.join('|')+"\n";		
	},
	
	makeGeneRow : function (gene, conditionTree, cells) {
		var row = [];		
		row.push (gene.name);
		for (var i = 0; i < conditionTree.items.length; i++) {		
			var condition = conditionTree.items[i];
			var cell = cells.getCell (gene, condition);
			if (cell === null) {				
				row.push ("NA/NA");
			} else {
				row.push (cell.pValue + "/" + cell.logFoldChange);				
			}
		}
		return row.join('|')+"\n";
	},
	
	convertToText : function (geneTree, conditionTree, cells) {
		var text = '# Generated by Gemma\n'+
				   '# ' + this.timeToString (new Date()) + '\n'+
				   '# \n'+
				   '# If you use this file for your research, please cite the Gemma web site\n'+
				   '# chibi.ubc.ca/Gemma \n'+
				   '# \n'+
				   '# This functionality is currently in beta. The file format may change in the near future. \n'+
				   '# Fields are separated by tabs and delimited with double quotes\n'+
				   '# \n';
		
		text += this.makeHeaderRow (conditionTree);
		
		for (var i = 0; i < geneTree.items.length; i++) {		
			var gene = geneTree.items[i];
			text += this.makeGeneRow (gene, conditionTree, cells);
		}
		this.show();
		this.textAreaPanel.update({
			text: text
		});
	},

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
                tpl: new Ext.XTemplate('<tpl>','{text}','</tpl>'),
				//tplWriteMode: 'append',
                bodyStyle: 'white-space: nowrap',
                style: 'white-space: nowrap',
                wordWrap: false,
                padding: 7,
                autoScroll: true
            })]
		});        

	
		Gemma.Metaheatmap.DownloadWindow.superclass.initComponent.call (this);		
	},
	
	onRender: function(){
		Gemma.Metaheatmap.DownloadWindow.superclass.onRender.apply (this, arguments);
	}
    
});
