Ext.namespace('Gemma');


// Gene group
Gemma.MetaHeatmapLabelGroup = Ext.extend(Ext.BoxComponent, {	
	initComponent: function() {
		Ext.apply(this, {
			applicationRoot: this.applicationRoot,
			geneNames: this.labels,
			autoEl:'canvas',
			
			geneGroupName: this.geneGroupName,
			geneGroupId: this.geneGroupId,
			
			getIndexFromY : function (y) {
		    	return Math.floor(y/Gemma.MetaVisualizationConfig.cellHeight);
			},
			_drawLabels : function (highlightRow) {
				var	ctx = Gemma.MetaVisualizationUtils.getCanvasContext(this.el.dom);
				ctx.canvas.width = 80;
				ctx.canvas.height = this.labels.length * Gemma.MetaVisualizationConfig.cellHeight;
				ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);		
				CanvasTextFunctions.enable(ctx);
				var geneGroupNameLabelSize = this.geneGroupName.length * Gemma.MetaVisualizationConfig.geneLabelFontSize;
				var geneGroupNameLabelYPosition = ( geneGroupNameLabelSize > this.getHeight() ) ? this.getHeight() : this.getHeight()/2 + geneGroupNameLabelSize/2;
				
				ctx.drawRotatedText(10, geneGroupNameLabelYPosition, 270.0, 
					(Gemma.MetaVisualizationConfig.geneLabelFontSize), 'black', this.geneGroupName);
				
				// Some genes can be hidden. Genes can be sorted in different ways.
				// Gene ordering is a mapping that captures order and visibility of genes.
				for (var i = 0; i < this.applicationRoot.geneOrdering[this.geneGroupId].length; i++) {
					var geneName = this.geneNames[this.applicationRoot.geneOrdering[this.geneGroupId][i]];
					var geneId = this.applicationRoot.visualizationData.geneIds[this.geneGroupId][this.applicationRoot.geneOrdering[this.geneGroupId][i]];
					
					if( this.applicationRoot._selectedGenes.indexOf(geneId) != -1 ){
						ctx.fillStyle = Gemma.MetaVisualizationConfig.rowHighlightColor;
						ctx.fillRect(14, 
										i*Gemma.MetaVisualizationConfig.cellHeight+2, 
										77, 
										Gemma.MetaVisualizationConfig.cellHeight);
					}
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
			},
			_toggleSelectRow: function(row){
				// Some genes can be hidden. Genes can be sorted in different ways.
				// Gene ordering is mapping that capture order and number of shown genes.
				for (var i = 0; i < this.applicationRoot.geneOrdering[this.geneGroupId].length; i++) {
					if (row == i) {
						var geneId = this.applicationRoot.visualizationData.geneIds[this.geneGroupId][this.applicationRoot.geneOrdering[this.geneGroupId][i]];
						this.applicationRoot.fireEvent('geneSelectionChange', geneId);
					}					
				}		
			}		
		});
		Gemma.MetaHeatmapLabelGroup.superclass.initComponent.apply ( this, arguments );		
		
},
	
	onRender: function() {
		Gemma.MetaHeatmapLabelGroup.superclass.onRender.apply ( this, arguments );
		this._drawLabels();
		
		this.el.on('mouseover', function(e,t) {
			document.body.style.cursor = 'pointer';
		});
		this.el.on('mouseout', function(e,t) {
			document.body.style.cursor = 'default';
			this.applicationRoot._hoverDetailsPanel.hide();
				this._drawLabels();
		}, this);
		this.el.on('mousemove', function(e,t) { 						
			var index = this.getIndexFromY(e.getPageY() - Ext.get(t).getY());
			this._drawLabels(index);
			this.applicationRoot._hoverDetailsPanel.show();
			this.applicationRoot._hoverDetailsPanel.setPagePosition(e.getPageX()+20 , e.getPageY()+20 );
			this.applicationRoot._hoverDetailsPanel.update({
				type: 'gene',
				geneSymbol: this.geneNames[this.applicationRoot.geneOrdering[this.geneGroupId][index]],
				geneId: this.applicationRoot.visualizationData.geneIds[this.geneGroupId][this.applicationRoot.geneOrdering[this.geneGroupId][index]],
				geneFullName: this.applicationRoot.visualizationData.geneFullNames[this.geneGroupId][this.applicationRoot.geneOrdering[this.geneGroupId][index]]
			});
		}, this );		
		
		this.el.on('click', function(e,t) {
			var index = this.getIndexFromY(e.getPageY() - Ext.get(t).getY());
			
			// If user held down ctrl while clicking, select column or gene instead of popping up window.
			if (e.ctrlKey === true) {
				this._toggleSelectRow(index);
				this._drawLabels();
			}else{
				var geneId = this.applicationRoot.geneOrdering[this.geneGroupId][index];
				var geneName = this.geneNames[geneId];
				var	realGeneId = this.applicationRoot._visualizationData.geneIds[this.geneGroupId][geneId];
				var popup = Gemma.MetaVisualizationPopups.makeGeneInfoWindow(geneName, realGeneId);
			}
		}, this);		
	},
	refresh: function() {
		this._drawLabels();		
	}
	
});
Ext.reg('metaVizGeneLabelGroup', Gemma.MetaHeatmapLabelGroup);

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
			},
			unhighlightGene: function (geneGroup) {
				this.items.get(geneGroup)._drawLabels();
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
