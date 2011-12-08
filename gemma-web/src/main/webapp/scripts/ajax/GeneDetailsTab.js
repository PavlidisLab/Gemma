Ext.namespace('Gemma');

/**
 * need to set geneId as config
 */
Gemma.GeneDetails =  Ext.extend(Ext.Panel, {

	height: 600,
	padding:10,
	defaults:{
		border:false,
		flex:0
	},
	layoutConfig:{
		align:'stretch'
	},
	layout:'vbox',
	renderHomologues: function(homologues, mainGeneSymbol){
		var homologueStr = '';
		var j, homologue;
		for (j = 0; j < homologues.length; j++) {
			homologue = homologues[j];
				homologueStr += "<a title=\"View this homologous gene in Gemma\" href=\"/Gemma/gene/showGene.html?id=" +
				homologue.id +
				"\">" +
				homologue.officialSymbol +
				"&nbsp;[" +
				homologue.taxonCommonName +
				"]</a>&nbsp;&nbsp;&nbsp;";
		}
		if(homologueStr === ''){
			homologueStr = "No homologues defined";
		}
		return homologueStr;
	},
	
	renderGeneSets:function(geneSets){
		var geneSetLinks = [];
		var i, geneSet;
		for (i = 0; i < geneSets.length; i++) {
			if (geneSets[i] && geneSets[i].name && geneSets[i].id) {
				geneSetLinks.push('<a target="_blank" href="/Gemma/geneSet/showGeneSet.html?id='+geneSets[i].id+'">'+geneSets[i].name+'</a>');
			}
		}
		if(geneSetLinks.length === 0){
			geneSetLinks.push('Not currently a member of any gene group');
		}
		return geneSetLinks;
	},
	initComponent: function(){
		Gemma.GeneDetails.superclass.initComponent.call(this);
		
		this.on('render', function(){
			if (!this.loadMask && this.getEl()) {
				this.loadMask = new Ext.LoadMask(this.getEl(), {
					msg: Gemma.StatusText.Loading.generic,
					msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
				});
			}
			this.loadMask.show();
			
			GeneController.loadGeneDetails(this.geneId, function(geneDetails){
			
			this.loadMask.hide();
				this.add([
				{
						html: '<div style="font-weight: bold; font-size:1.3em;">' + geneDetails.name + '<br>' +
						geneDetails.officialName +
						'<br><br></div>'
					}, {
						layout: 'form',
						defaults: {
							border: false
						},
						items: [{
							fieldLabel: 'Taxon',
							html: geneDetails.taxonCommonName
						}, {
							fieldLabel: 'Aliases',
							html: geneDetails.aliases.join(', ')
						}, {
							fieldLabel: 'NCBI ID',
							html: geneDetails.ncbiId + ' <a  target="_blank" title="NCBI Gene link"' +
							'href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=' +
							geneDetails.ncbiId +
							'"><img alt="NCBI Gene Link" src="/Gemma/images/logo/ncbi.gif"/></a>'
						}, {
							fieldLabel: 'Homologues',
							html: this.renderHomologues(geneDetails.homologues, geneDetails.name)
						}, {
							fieldLabel: 'Gene Groups',
							html: this.renderGeneSets(geneDetails.geneSets).join(',')
						}, {
							fieldLabel: 'Probes' + '<a class="helpLink" href="javascript: void(0)" onclick="showHelpTip(event, ' +
								'\''+Gemma.HelpText.WidgetDefaults.GeneDetails.probesTT+'\'); return false">' +
								'<img src="/Gemma/images/help.png" /> </a>', 
							html: geneDetails.compositeSequenceCount + 
								' <a target="_blank" href="/Gemma/gene/showCompositeSequences.html?id=' + geneDetails.id + '">' +
								'<img src="/Gemma/images/magnifier.png"> </a>'
						}, {
							fieldLabel: 'Notes',
							html: geneDetails.description
						}]
					}]);
				this.doLayout();
			}.createDelegate(this));
		});
	}
});