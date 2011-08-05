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
			homologueStr = "No aliases defined";
		}
		return homologueStr;
	},
	
	renderGeneSets:function(geneSets){
		var geneSetBtns = [];
		var i, geneSet;
		for (i = 0; i < geneSets.length; i++) {
			if (geneSets[i] && geneSets[i].name) {
				geneSetBtns.push({
					xtype: 'button',
					text: geneSets[i].name,
					ctCls: 'right-align-btn transparent-btn transparent-btn-link',
					geneSet: geneSets[i],
					listeners: {
						click: function(){
							var grid = new Gemma.GeneMembersSaveGrid({
								geneGroupId: this.geneSet.id,
								selectedGeneGroup: this.geneSet,
								groupName: this.geneSet.name,
								taxonId: this.geneSet.taxonId,
								taxonName: this.geneSet.taxonName,
								geneIds: this.geneSet.geneIds,
								hideHeaders: true,
								frame: false,
								allowSaveToSession: false
							});
							
							var win = new Ext.Window({
								closable: true,
								layout: 'fit',
								width: 450,
								height: 500,
								items: grid,
								title: this.geneSet.name
							});
							grid.on('doneModification', function(){
								win.close();
							}, this);
							win.show();
						}
					}
				});
			}
		}
		if(geneSetBtns.length === 0){
			geneSetBtns.push({html: 'Not currently a member of any gene group', border:false});
		}
		return geneSetBtns;
	},
	initComponent: function(){
		Gemma.GeneDetails.superclass.initComponent.call(this);
		
		this.on('render', function(){
			if (!this.loadMask) {
				this.loadMask = new Ext.LoadMask(this.getEl(), {
					msg: "Loading ...",
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
							html: geneDetails.aliases.join(', ') + ' <a  target="_blank" title="NCBI Gene link"' +
							'href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=' +
							geneDetails.ncbiId +
							'"><img alt="NCBI Gene Link" src="/Gemma/images/logo/ncbi.gif"/></a>'
						}, {
							fieldLabel: 'Homolgues',
							html: this.renderHomologues(geneDetails.homologues, geneDetails.name)
						}, {
							fieldLabel: 'Gene Groups',
							layout:'hbox',
							items: this.renderGeneSets(geneDetails.geneSets)
						}, {
							fieldLabel: 'Probes' + '<a class="helpLink" href="javascript: void(0)" onclick="showHelpTip(event, ' +
							'\'Number of probes for this gene on expression platforms in Gemma\'); return false">' +
							'<img src="/Gemma/images/help.png" /> </a>',
							html: geneDetails.compositeSequenceCount + ' <a target="_blank" href="/Gemma/gene/showCompositeSequences.html?id=' + geneDetails.id + '">' +
							'<img src="/Gemma/images/magnifier.png"> </a>'
						}, {
							fieldLabel: 'Notes',
							html: geneDetails.description
						}]
					}, {
						html: '<h4>Gene Ontology Terms</h4>'
					}, {
						layout: 'fit', // need extra panel level to make grid resize with window
						flex:1,
						items: {
							xtype: 'genegogrid',
							border: true,
							geneid: this.geneId
						//,height: 200
						}
				}]);
				this.doLayout();
			}.createDelegate(this));
		});
	}
});