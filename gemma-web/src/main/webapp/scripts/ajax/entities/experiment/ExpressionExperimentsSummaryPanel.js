
Ext.namespace('Gemma');

/* 
 * A panel for displaying by-taxon counts of all existing, new and updated experiments in Gemma, 
 * with a column for each count
 * 
 * a similar table appeared on the classic Gemma's front page
 */


Gemma.ExpressionExperimentsSummaryPanel = Ext.extend(Ext.Panel, {
	title: 'Summary & Updates',
	collapsible: true,
	titleCollapse: true,
	animCollapse: false, // vbox collapse fix isn't animated
	
	listeners: {
		render: function(){
			if (!this.collapsed) {
				this.loadCounts();
			}
		},
		expand: function(){
			if (!this.countsLoaded) {
				this.loadCounts();
			}
		}
	},
	
	stateful: true,
	stateId: 'showAllExpressionExperimentsSummaryGridState',
	// what describes the state of this panel - in this case it is the "collapsed" field
	getState: function(){
		return {
			collapsed: this.collapsed
		};
	},
	// specify when the state should be saved - in this case after panel was collapsed or expanded
	stateEvents: ['collapse', 'expand'],
	
	constructor: function(config){
        Gemma.ExpressionExperimentsSummaryPanel.superclass.constructor.call(this, config);
    },
	initComponent: function(){
		Gemma.ExpressionExperimentsSummaryPanel.superclass.initComponent.call(this);
	}, // end of initComponent
	
	loadCounts: function(){
		if (this.getEl() && !this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
				msg: "Loading summary ...",
				msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
			});
		}if(this.loadMask){
			this.loadMask.show();
		}
		
		ExpressionExperimentController.loadCountsForDataSummaryTable(function(json){
			// update the panel with counts
			json.cmpId = Ext.id(this);
			this.update(json);
			this.countsLoaded = true;
			
			if(this.loadMask){
				this.loadMask.hide();
			}
			
		}.createDelegate(this));
	},
	/*tpl: new Ext.XTemplate(
		'<table>', 
		'<tr><td></td><td>Total</td><td>Updated</td><td>New</td></tr>',
		'<tr><td><a href="/Gemma/expressionExperiment/showAllExpressionExperiments.html">', 
		'Expression Experiments</a></td><td><b>{expressionExperimentCount}</td>',
		'<td>{updatedExpressionExperimentCount}</td>',
		'<td>{newExpressionExperimentCount}</b></td></tr>',
		'<tpl for="sortedCountsPerTaxon">', 
		'<tr><td><a href="/Gemma/expressionExperiment/showAllExpressionExperiments.html?taxonId={taxonId}">', 
		'{taxonName}</a></td><td>{totalCount}</td><td>{updatedCount}</td><td>{newCount}</td></tr>', '</tpl>', 
		'</table>'),*/
		
	// this is so long because it is lifted from the original version of the summary table
	tpl: new Ext.XTemplate('<div id="dataSummaryTable">'+
	'<div class="roundedcornr_box_777249" style="margin-bottom: 15px; padding: 10px; -moz-border-radius: 15px; border-radius: 15px;">'+
		'<div class="roundedcornr_content_777249">'+
			'<div style="font-size: small; padding-bottom: 5px;">'+
				'<b> <a target="_blank" href="http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma/All+news">'+
				'Updates in the last week</a> </b>'+
			'</div>'+

			'<div id="dataSummary" style="margin-left: 15px; margin-right: 15px">'+
				'<table style="white-space: nowrap">'+
					'<tr>'+
						'<td style="padding-right: 10px">'+
							'<span style="white-space: nowrap">'+
							'<strong>Data Summary</strong> </span>'+
						'</td>'+
						'<td style="padding-right: 10px" align="right">'+
							'Total'+
						'</td>'+
						'<tpl if="drawUpdatedColumn == true">'+
							'<td align="right" style="padding-right: 10px">'+
								'Updated'+
							'</td>'+
						'</tpl>'+
						'<tpl if="drawNewColumn == true">'+
							'<td align="right" style="padding-right: 10px">'+
								'New'+
							'</td>'+
						'</tpl>'+
					'</tr><tr>'+
						'<td style="padding-right: 10px">'+
							'<span style="white-space: nowrap"> <!-- for IE --> '+
							'Expression Experiments:</span>'+
						'</td>'+
						'<td align="right" style="padding-right: 10px">'+
							'<b><a href="/Gemma/expressionExperiment/showAllExpressionExperiments.html">{expressionExperimentCount}</b>'+
						'</td>'+
						'<td align="right" style="padding-right: 10px">'+
							'<b><a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{updatedExpressionExperimentIds}],\'{cmpId}\');">'+
							'{updatedExpressionExperimentCount}</a></b>&nbsp;&nbsp;'+
						'</td>'+
						'<td align="right">'+
							'<b><a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{newExpressionExperimentIds}],\'{cmpId}\');">'+
							'{newExpressionExperimentCount}</a></b>&nbsp;'+
						'</td>'+
					'</tr>'+
					'<tpl for="sortedCountsPerTaxon">'+ 
						'<tr>'+ 
							'<td style="padding-right: 10px">'+ 
								'<span style="white-space: nowrap"> <!-- for IE --> &emsp;'+ 
								'{taxonName}'+
							'</td><td align="right" style="padding-right: 10px">'+
								'<a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleTaxonLink({taxonId},\'{parent.cmpId}\');">'+
								'{totalCount}</a>'+
							'</td><td align="right" style="padding-right: 10px">'+
								'<b><a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{updatedIds}],\'{parent.cmpId}\');">'+
								'{updatedCount}</a></b>&nbsp;&nbsp;'+
								'</a>'+
							'</td><td align="right">'+
								'<b><a style="cursor:pointer" onClick="Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink([{newIds}],\'{parent.cmpId}\');">'+
								'{newCount}</a></b>&nbsp;'+
								'</a>'+
							'</td>'+
						'</tr>'+
					'</tpl>'+
					'<tr>'+
						'<td style="padding-right: 10px">'+
							'<span style="white-space: nowrap"> <!-- for IE -->'+ 
									'Array Designs:  </span>'+
						'</td>'+
						'<td align="right" style="padding-right: 10px">'+
							'<a href="/arrays/showAllArrayDesigns.html">'+
							'<b>{arrayDesignCount}</b></a>'+
						'</td>'+
						'<td align="right" style="padding-right: 10px">'+
							'<b>{updatedArrayDesignCount}</b>&nbsp;&nbsp;'+
						'</td>'+
						'<td align="right">'+
							'<b>{newArrayDesignCount}</b>&nbsp;'+
						'</td>'+
					'</tr>'+
					'<tr>'+
						'<td style="padding-right: 10px">'+

							'<span style="white-space: nowrap"> <!-- for IE --> Assays:'+
							'</span>'+
						'</td>'+
						'<td align="right" style="padding-right: 10px">'+
							'<b>{bioAssayCount}</b>'+
						'</td>'+
						'<td align="right" style="padding-right: 10px">'+
							'&nbsp;&nbsp;'+
						'</td>'+
						'<td align="right">'+
							'<b>{newBioAssayCount}</b>&nbsp;'+
						'</td>'+
					'</tr>'+
				'</table>'+
			'</div>'+
		'</div>'+
	'</div>'+
'</div>')
});

Gemma.ExpressionExperimentsSummaryPanel.handleIdsLink = function(ids, cmpId){
		Ext.getCmp(cmpId).fireEvent('showExperimentsByIds', ids);
	};
Gemma.ExpressionExperimentsSummaryPanel.handleTaxonLink = function(id, cmpId){
		Ext.getCmp(cmpId).fireEvent('showExperimentsByTaxon', id);
	};