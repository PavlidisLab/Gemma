
Ext.namespace('Gemma');

/**
 * Window for user to set coex search refinements
 * 
 * you must implement handling for 'Re-run search' button 
 * click: an event is fired with param: stringency, probe-level query, coex among query genes only
 * @author thea
 * @version $Id: AnalysisResultsSearchForm.js,v 1.34 2011/05/06 04:02:25 paul
 *          Exp $
 */
Gemma.CoexpressionSearchOptions = Ext.extend(Ext.Window,{
	title: 'Coexpression search refinements',
	layout: 'form',
	width: 275,
	height: 250,
	labelWidth: 130,
	padding: 10,
	
	initComponent: function(){
		Ext.apply(this,{
			buttons: [{
		text: 'Re-run search',
		scope: this,
		handler: function(){
			this.fireEvent('rerunSearch', this.stringencyfield.getValue(), 
			this.forceProbeLevelSearch.getValue(), this.querygenesonly.getValue());
		}
	}, {
		text: 'Cancel',
		scope: this,
		handler: function(){
			this.hide();
		}
	}],
	items: [{
		html: '<h4>Run your <u>original</u> coexpression search with: </h4><br>',
		bodyStyle: 'background-color: transparent',
		border: false
	}, {
		xtype: 'spinnerfield',
		decimalPrecision: 1,
		incrementValue: 1,
		accelerate: true,
		ref: 'stringencyfield',
		allowBlank: false,
		allowDecimals: false,
		allowNegative: false,
		minValue: Gemma.MIN_STRINGENCY,
		maxValue: 999,
		fieldLabel: 'Stringency ',
		value: 2,
		width: 60,
		fieldTip:"The minimum number of datasets that must show coexpression for a result to appear"
	}, {
		xtype: 'checkbox',
		style: 'margin-top:7px',
		disabled : !this.admin,
		hidden : !this.admin,
		hideLabel : !this.admin,
		ref: 'forceProbeLevelSearch',
		fieldLabel: 'Probe-level query',
		fieldTip: 'Run this query at the level of probes instead of genes. ' +
		'May be slower but always gets most current information from newly-processed data sets.'
	}, {
		xtype: 'checkbox',
		ref: 'querygenesonly',
		style: 'margin-top:10px',
		fieldLabel: 'Coexpression among query genes only ',
		fieldTip:"Restrict the output to include only links among the listed query genes"
	}]});
		Gemma.CoexpressionSearchOptions.superclass.initComponent.call(this);
	}
});