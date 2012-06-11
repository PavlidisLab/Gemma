/**
 * $Id$
 */
Ext.namespace('Gemma');

/**
 * need to set geneId as config
 */
Gemma.GeneDetails =  Ext.extend(Ext.Panel, {

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
		homologues.sort(function(a,b){
			var A = a.taxonCommonName.toLowerCase();
		    var B = b.taxonCommonName.toLowerCase();
		    if (A < B) return -1;
		    if (A > B) return  1;
		    return 0;
		});
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
		geneSets.sort(function(a,b){
			var A = a.name.toLowerCase();
		    var B = b.name.toLowerCase();
		    if (A < B) return -1;
		    if (A > B) return  1;
		    return 0;
		});
		var geneSetLinks = []; 
		for (var i = 0; i < geneSets.length; i++) {
			if (geneSets[i] && geneSets[i].name && geneSets[i].id) {
				geneSetLinks.push('<a target="_blank" href="/Gemma/geneSet/showGeneSet.html?id=' + geneSets[i].id+'">' + geneSets[i].name + '</a>');
			}
		}
		if(geneSetLinks.length === 0){
			geneSetLinks.push('Not currently a member of any gene group');
		}
		return geneSetLinks;
	},
	
	/**
	 * 
	 * @param geneDetails
	 * @returns {String}
	 */
	renderMultifunctionality : function(geneDetails) {
		if (geneDetails.multifunctionalityRank) {
			return geneDetails.numGoTerms + " GO Terms; Overall multifunctionality " + geneDetails.multifunctionalityRank.toFixed(2);
		} else {
			return "[ Not available ]";
		}
	},
	
	/**
	 * 
	 * @param geneDetails
	 * @returns {String}
	 */
	renderPhenotypes : function(geneDetails) {
		if (geneDetails.phenotypes && geneDetails.phenotypes.length > 0) {
			var phenotypes = geneDetails.phenotypes;
			phenotypes.sort(function(a,b){
				var A = a.value.toLowerCase();
			    var B = b.value.toLowerCase();
			    if (A < B) return -1;
			    if (A > B) return  1;
			    return 0;
			});
			var i = 0;
			var text = '';
			var limit = Math.min(3,phenotypes.length);
			for(i = 0; i < limit ; i++){
				text += '<a target="_blank" href="'+Gemma.LinkRoots.phenotypePage + phenotypes[i].urlId +
				'">'+phenotypes[i].value+'</a>';
				if( (i+1) !== limit){
					text += ', ';
				}
			}
			if(limit < phenotypes.length){
				text += ', '+ (phenotypes.length-limit) +' more';
			}
			text += "<img style='cursor:pointer' src='/Gemma/images/magnifier.png' ext:qtip='See all associated phenotypes'"+
				"onClick='Ext.getCmp(&#39;"+this.id+"&#39;).changeTab(&#39;phenotypes&#39;)'>";
			return text;
		} else {
			return "[ None ]";
		}
	},

	changeTab:function( tabName ){
		this.fireEvent('changeTab', tabName);
	},
	/**
	 * 
	 * @param ncbiId
	 * @param count
	 * @returns {String}
	 */
	renderAssociatedExperiments : function(ncbiId, count) {
		return '<a href="/Gemma/searcher.html?query=http%3A//purl.org/commons/record/ncbi_gene/' + ncbiId +'&scope=E">'+count+'</a>';
	},
	
	renderNodeDegree : function(geneDetails) {
		if (geneDetails.nodeDegreeRank) {
			return geneDetails.nodeDegreeRank.toFixed(2);
		} else {
			return "[ Not available ]";
		}
	},
	renderAliases: function(aliases) {
		aliases.sort();
		return aliases.join(', ');
	},
	initComponent: function(){
		Gemma.GeneDetails.superclass.initComponent.call(this);
		
		// need to do this on render so we can show a load mask
		this.on('afterrender', function(){
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
						flex:1,
						defaults: {
							border: false
						},
						items: [{
							fieldLabel: 'Taxon',
							html: geneDetails.taxonCommonName
						}, {
							fieldLabel: 'Aliases',
							html: this.renderAliases(geneDetails.aliases)
						}, {
							fieldLabel: 'NCBI ID',
							html: geneDetails.ncbiId + ' <a target="_blank" title="NCBI Gene link"' +
							'href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids=' +
							geneDetails.ncbiId +
							'"><img alt="NCBI Gene Link" src="/Gemma/images/logo/ncbi.gif"/></a>'
						}, {
							fieldLabel: 'Homologues',
							html: this.renderHomologues(geneDetails.homologues, geneDetails.name)
						}, {
							fieldLabel: 'Gene Groups',
							html: this.renderGeneSets(geneDetails.geneSets).join(', ')
						}, {
							fieldLabel: 'Studies' + '&nbsp;<a class="helpLink" href="javascript: void(0)" onclick="showHelpTip(event, ' +
							'\''+ Gemma.HelpText.WidgetDefaults.GeneDetails.assocExpTT +'\'); return false">' +
							'<img src="/Gemma/images/help.png" /> </a>',
							html: this.renderAssociatedExperiments( geneDetails.ncbiId, geneDetails.associatedExperimentCount ) 
						}, {
							fieldLabel: 'Multifunc.' + '&nbsp;<a class="helpLink" href="javascript: void(0)" onclick="showHelpTip(event, ' +
							'\''+ Gemma.HelpText.WidgetDefaults.GeneDetails.multifuncTT +'\'); return false">' +
							'<img src="/Gemma/images/help.png" /> </a>',
							html: this.renderMultifunctionality( geneDetails ) 
						}, {
							fieldLabel: 'Hub score' + '&nbsp;<a class="helpLink" href="javascript: void(0)" onclick="showHelpTip(event, ' +
							'\''+ Gemma.HelpText.WidgetDefaults.GeneDetails.nodeDegreeTT +'\'); return false">' +
							'<img src="/Gemma/images/help.png" /> </a>',
							html: this.renderNodeDegree( geneDetails ) 
						}, {
							fieldLabel: 'Phenotypes' + '&nbsp;<a class="helpLink" href="javascript: void(0)" onclick="showHelpTip(event, ' +
							'\''+ Gemma.HelpText.WidgetDefaults.GeneDetails.phenotypeTT +'\'); return false">' +
							'<img src="/Gemma/images/help.png" /> </a>',
							html: this.renderPhenotypes( geneDetails ) 
						}, {
							fieldLabel: 'Probes' + '&nbsp;<a class="helpLink" href="javascript: void(0)" onclick="showHelpTip(event, ' +
								'\''+ Gemma.HelpText.WidgetDefaults.GeneDetails.probesTT+'\'); return false">' +
								'<img src="/Gemma/images/help.png" /> </a>', 
							html: geneDetails.compositeSequenceCount + 
								' <a target="_blank" href="/Gemma/gene/showCompositeSequences.html?id=' + geneDetails.id + '">' +
								'<img src="/Gemma/images/magnifier.png"> </a>'
						}, {
							fieldLabel: 'Notes',
							html: geneDetails.description
						}]
					}]);
				this.syncSize();
			}.createDelegate(this));
		});
	}
});