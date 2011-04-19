Ext.namespace('Gemma');


// Gene group
Gemma.MetaHeatmapLabelGroup = Ext.extend(Ext.BoxComponent, {	
	initComponent: function() {
		Ext.apply(this, {
			applicationRoot: this.applicationRoot,
			geneNames: this.labels,
			autoEl: { tag: 'canvas',
			  		  width: 80,
			  		  height: this.labels.length*10
			},
			
			geneGroupName: this.geneGroupName,
			geneGroupId: this.geneGroupId,
			
			getIndexFromY : function (y) {
		    	return Math.floor(y/Gemma.MetaVisualizationConfig.cellHeight);
			},
			_drawLabels : function (highlightRow) {
				var ctx = this.el.dom.getContext("2d");
				ctx.clearRect(0, 0, this.el.dom.width, this.el.dom.height);		
				CanvasTextFunctions.enable(ctx);
				ctx.drawRotatedText(10, this.getHeight() - 10, 270.0, 9, 'black', this.geneGroupName);
				
				// Some genes can be hidden. Genes can be sorted in different ways.
				// Gene ordering is mapping that capture order and number of shown genes.
				for (var i = 0; i < this.applicationRoot.geneOrdering[this.geneGroupId].length; i++) {
					var geneName = this.geneNames[this.applicationRoot.geneOrdering[this.geneGroupId][i]];
					if (highlightRow == i) {
						ctx.save();
						ctx.strokeStyle = Gemma.MetaVisualizationConfig.geneLabelHighlightColor;
						ctx.drawTextRight( '', Gemma.MetaVisualizationConfig.geneLabelFontSize, 77,
										   (i+1)*Gemma.MetaVisualizationConfig.cellHeight,
										   geneName);
						ctx.restore();
					} else {
						ctx.drawTextRight(  '', Gemma.MetaVisualizationConfig.geneLabelFontSize, 77,
											(i+1)*Gemma.MetaVisualizationConfig.cellHeight,
											geneName);						
					}					
				}									
			}
		});
		Gemma.MetaHeatmapLabelGroup.superclass.initComponent.apply ( this, arguments );		
		
},
	
	onRender: function() {
		Gemma.MetaHeatmapLabelGroup.superclass.onRender.apply ( this, arguments );
		this._drawLabels();
		
		this.el.on('mousemove', function(e,t) { 						
			var index = this.getIndexFromY(e.getPageY() - Ext.get(t).getY());
			this._drawLabels(index);
			
			this.applicationRoot._hoverDetailsPanel.update({
				type: 'gene',
				geneSymbol: this.geneNames[this.applicationRoot.geneOrdering[this.geneGroupId][index]],
				geneId: this.applicationRoot.geneOrdering[this.geneGroupId][index],
				geneFullName: this.applicationRoot.visualizationData.geneFullNames[this.geneGroupId][index]
			});
		}, this );		
		
		this.el.on('click', function(e,t) {
			var index = this.getIndexFromY(e.getPageY() - Ext.get(t).getY());
			var geneId = this.applicationRoot.geneOrdering[this.geneGroupId][index];
			var geneName = this.geneNames[geneId];
			var	realGeneId = this.applicationRoot._visualizationData.geneIds[this.geneGroupId][geneId];
			var popup = Gemma.MetaVisualizationPopups.makeGeneInfoWindow(geneName, realGeneId);
			//popup.show();
		}, this);		
	},
	refresh: function() {
		this._drawLabels();		
	}
	
});


// Gene Labels
Gemma.MetaHeatmapLabelsColumn = Ext.extend(Ext.Panel, {	
	initComponent: function() {
		Ext.apply(this, {
			applicationRoot: this.applicationRoot,
 			layout: 'vbox',
 			layoutConfig: {
				defaultMargins: {top:0, right:0, bottom:4, left:0}
			},
			labels: this.labels,
			geneGroupNames: this.geneGroupNames,
			highlightGene: function (geneGroup, row) {
				this.items.get(geneGroup)._drawLabels(row);
			}			
		});
						
		Gemma.MetaHeatmapLabelsColumn.superclass.initComponent.apply(this, arguments);		
	},
	
	onRender: function() {		
		Gemma.MetaHeatmapLabelsColumn.superclass.onRender.apply(this, arguments);
		
		for (var groupIndex = 0; groupIndex < this.labels.length; groupIndex++) {
			this.add( new Gemma.MetaHeatmapLabelGroup(
								{ applicationRoot: this.applicationRoot,
								  labels: this.labels[groupIndex],
								  geneGroupName: this.geneGroupNames[groupIndex],
								  geneGroupId: groupIndex }) );
		}
	},
	
	refresh: function() {
		this.items.each(function() {this.refresh();} );
	}
				
});

Ext.reg('metaVizGeneLabels', Gemma.MetaHeatmapLabelsColumn);
