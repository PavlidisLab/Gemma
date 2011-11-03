Ext.namespace('Gemma.Metaheatmap');

	  // a window for displaying details as elements of the image are
	  // hovered over

Gemma.Metaheatmap.HoverWindow = Ext.extend ( Ext.Window, {
				  
	height : 200,
	width  : 350,
	//	autoScroll : true,
	closable   : false,
	shadow 	   : false,
	border 	   : false,
	bodyBorder : false,
	//hidden	   : true,
	//	bodyStyle  : 'padding: 7px',
	
	tplWriteMode : 'overwrite',

	initComponent : function () {
		Gemma.Metaheatmap.HoverWindow.superclass.initComponent.apply ( this, arguments );

		this.tpl = this.initTemplate_();
		
	},		
		
	initTemplate_ : function () {
		return new Ext.XTemplate (
				'<span style="font-size: 12px ">',
				'<tpl for=".">',
				'<tpl if="type==\'condition\'">',   //Experiment
				'<b>Experiment</b>: {datasetName}<br><br>',
				'<b>Factor</b>:{factorCategory} - {factorDescription}<br><br> ',
				'</tpl>',
				'<tpl if="type==\'minipie\'">',     //minipie
				'{percentProbesDiffExpressed} of probes are differentially expressed.<br><br>',
				'({numberOfProbesDiffExpressed} of {numberOfProbesTotal}) Click for details.',
				'</tpl>',
				'<tpl if="type==\'gene\'">',		//gene
				'<b>Gene</b>: {geneSymbol} {geneFullName}<br>',
				'</tpl>',
				'<tpl if="type==\'contrastCell\'">',  //contrast
				'<b>Gene</b>: {geneSymbol} {geneFullName}<br><br> ',
				'<b>Experiment</b>: {datasetShortName}</a> {datasetName}<br><br>',
				'<b>Factor</b>:{factorCategory} - {factorDescription}<br><br> ', '<b>Log2 fold change</b>: {foldChange}<br>', '<b>pValue</b>: {contrastPvalue} <br>',
				'</tpl>',
				'<tpl if="type==\'cell\'">',		 //cell
				'<b>Gene</b>: {geneSymbol} {geneFullName}<br><br> ',
				'<b>Experiment</b>: {datasetName}<br><br>',
				'<b>Factor</b>:{factorCategory} - {factorDescription}<br><br> ',
				'<b>p-value</b>: {pvalue}<br><br>',
				'<b>fold change</b>: {foldChange}',
				'</tpl>', '</tpl></span>' );
	},

	onRender: function() {
		Gemma.Metaheatmap.HoverWindow.superclass.onRender.apply ( this, arguments );
	}
	
});

Ext.reg('Metaheatmap.HoverWindow',Gemma.Metaheatmap.HoverWindow);
