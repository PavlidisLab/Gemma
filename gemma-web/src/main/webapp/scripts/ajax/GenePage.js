Ext.namespace('Gemma');

Gemma.DIFF_THRESHOLD = 0.01;
Gemma.MAX_DIFF_RESULTS = 75;
/**
 * 
 * Top level container for all sections of gene info
 * 
 * @class Gemma.GenePage
 * @extends Ext.TabPanel
 * 
 */


Gemma.GenePage =  Ext.extend(Ext.TabPanel, {

	height: 600,
	defaults: {
		autoScroll: true,
		width: 850
	},
	deferredRender: true,
	listeners: {
		'tabchange': function(tabPanel, newTab){
			newTab.fireEvent('tabChanged');
		},
		'beforetabchange': function(tabPanel, newTab, currTab){
			// if false is returned, tab isn't changed
			if (currTab) {
				return currTab.fireEvent('leavingTab');
			}
			return true;
		}
	},
	initComponent: function(){
	
		var geneId = this.geneId;
		
		if ((Ext.get("hasWritePermission")) && Ext.get("hasWritePermission").getValue() == 'true') {
			this.editable = true;
		}
		var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
		
		var windowPadding = 3;
		var minWidth = 800;
		var minHeight = 600;
		
		var pageHeight = window.innerHeight !== null ? window.innerHeight : document.documentElement &&
		document.documentElement.clientHeight ? document.documentElement.clientHeight : document.body !== null ? document.body.clientHeight : null;
		
		var pageWidth = window.innerWidth !== null ? window.innerWidth : document.documentElement &&
		document.documentElement.clientWidth ? document.documentElement.clientWidth : document.body !== null ? document.body.clientWidth : null;
		
		var adjPageWidth = ((pageWidth - windowPadding) > minWidth) ? (pageWidth - windowPadding - 70) : minWidth;
		var adjPageHeight = ((pageHeight - windowPadding) > minHeight) ? (pageHeight - windowPadding - 60) : minHeight;
		this.setSize(adjPageWidth, adjPageHeight);
		// resize all elements with browser window resize
		Ext.EventManager.onWindowResize(function(width, height){
			var adjWidth = ((width - windowPadding) > minWidth) ? (width - windowPadding - 70) : minWidth;
			var adjHeight = ((height - windowPadding) > minHeight) ? (height - windowPadding - 60) : minHeight;
			this.setSize(adjWidth, adjHeight);
			this.doLayout();
		}, this);
		
		Gemma.GenePage.superclass.initComponent.call(this);
		/*DETAILS TAB*/
		this.add(new Gemma.GeneDetails({
			title: 'Details',
			geneId: geneId
		}));
		
		/*ALLEN BRAIN ATLAS IMAGES*/
		this.add({
			xtype:'geneallenbrainatlasimages',
			geneId: geneId,
			title:'Expression Images'
		});
		
		// diff expression grid
		
		var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
			title: 'Differential Expression'
		});
		diffExGrid.on('render', function(){
			var visColumnIndex = diffExGrid.getColumnModel().getIndexById('visualize');
			diffExGrid.getColumnModel().setHidden(visColumnIndex, false);
			// this should go in grid itself, but it wasn't working properly (or at all)
			if (!this.loadMask) {
				this.loadMask = new Ext.LoadMask(this.getEl(), {
					msg : "Loading ...",
					msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
				});
			}
			this.loadMask.show();
			diffExGrid.getStore().load({
				params: [geneId, Gemma.DIFF_THRESHOLD, Gemma.MAX_DIFF_RESULTS]
			});
		});
		this.add(diffExGrid);
		
		
		// Coexpression grid.
		var coexpressedGeneGrid = new Gemma.CoexpressionGrid({
			title: 'Coexpression',
			colspan: 2,
			lite: true,
			noSmallGemma: true
		});
		coexpressedGeneGrid.on('render', function(){
			coexpressedGeneGrid.doSearch({
				geneIds: [geneId],
				quick: true,
				stringency: 2,
				forceProbeLevelSearch: false
			});
		});
		this.add(coexpressedGeneGrid);
		
		
		this.add({
			xtype: 'geneproductgrid',
			geneid: geneId,
			title: 'Gene Products'
		});
		
		this.on('render', function(){
			this.setActiveTab(0);
		});
	}
});
