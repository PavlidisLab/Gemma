Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * This provides a summary of the differential analyses done for a particular dataset/expression experiment.
 * It is structured as a tree with each analysis as a node and its result sets as its children
 * 
 * @param ee {ExpressionExperimentValueObject} the expression experiment for which to display the analyses
 * @class Gemma.DifferentialExpressionAnalysesSummaryTree
 * @extends Ext.tree.TreePanel
 */

Gemma.DifferentialExpressionAnalysesSummaryTree = Ext.extend(Ext.tree.TreePanel, {
	
		animate: true,
		id:'differentialExpressionAnalysesSummaryTreeEEPage',
		rootVisible: false,
		enableDD: false,
		cls: 'x-tree-noicon',
		singleClickExpand:true,
		lines:false,
		containerScroll:false,
		//panel
		autoScroll:true,
		border:false,
		layout: 'fit',
		root:{text:'root'},
		// for drawing charts
		percentUpProbes:[],
		listeners:{
			afterrender: function(){this.drawPieCharts();},
			expandnode: function(){this.drawPieCharts();}
		},
				
		constructor : function(ee) {
			Ext.apply(this,{ee: ee});
			Gemma.DifferentialExpressionAnalysesSummaryTree.superclass.constructor.call(this);
		},

		initComponent : function(){
			Gemma.DifferentialExpressionAnalysesSummaryTree.superclass.initComponent.call(this);
			this.build();
		},
		build:function(){
				var analyses = this.ee.differentialExpressionAnalyses;

				//console.log("in build" + this.ee.differentialExpressionAnalyses);
				// set the root node
				var root = new Ext.tree.TreeNode({
					expanded: true,
					id: 'diffExRoot',
					text: 'root'
				});
				this.setRootNode(root);
				// just show "Not Avalailable" root if no analyses
				if(analyses.size() == 0){
					root.appendChild(new Ext.tree.TreeNode({
											id: 'nodeNA',
											expanded: false,
											leaf:true,
											text: 'Not Available'
					}));
					return
				}
				var subsetTracker = {}; //used to keep subset nodes adjacent
				var nodeId = 0; // used to keep track of nodes and give each a specific div in which to draw a pie chart
				for (var j = 0; j < analyses.size(); j++) {
					var analysis = analyses[j];
					//console.log(analysis.protocol);
					
					//console.log("ANALYSIS " + (j + 1) + " of " + analyses.size());
					
					var parentNode = null;
					var parentText = null;
					var interaction = 0;
					
					// prepare subset text if applicable
					var subsetText = '';
					var subsetIdent = '';
					var neighbourNode = null;
					if (analysis.subsetFactorValue) {
						var subsetFactor = (analysis.subsetFactorValue.factor) ? analysis.subsetFactorValue.factor : analysis.subsetFactorValue.category;
						subsetText = '<span ext:qtip="Analysis was run using only the subset of samples with ' + subsetFactor + " = " + analysis.subsetFactorValue.value + '" >' +
						"<b>subset</b> where " +
						subsetFactor +
						" = " +
						analysis.subsetFactorValue.value +
						' </span>';
						subsetIdent = subsetFactor;
						//console.log("susbetIdent: " + subsetIdent);
						//if a similar subset node has already been created, insert this node adjacent to it
						neighbourNode = root.findChild('subsetIdent', subsetIdent);
					}
					
					// prepare download link
					var downloadDiffDataLink = String.format("<span style='cursor:pointer' ext:qtip='Download all differential expression data in a tab delimited  format' onClick='fetchDiffExpressionData({0})' > &nbsp; <img src='/Gemma/images/download.gif'/> &nbsp;  </span>", this.ee.id); // FIXME
					
					// make node for analysis
					parentNode = new Ext.tree.TreeNode({
						id: 'node'+ (nodeId++),
						expanded: false,
						singleClickExpand: true,
						text: downloadDiffDataLink,
						subsetIdent: subsetIdent
					});
					
					// add node to tree
					if (neighbourNode) {
						root.insertBefore(parentNode, neighbourNode);
					}
					else {
						root.appendChild(parentNode);
					}
					
					// if analysis has only one result set, don't give it children and put all info in parent node
					if(analysis.resultSets.size()==1){
						var resultSet = analysis.resultSets[0];
							// get experimental factor string and build analysis parent node text
							var analysisName = this.getFactorNameText(resultSet);
							var nodeText= '';
							if (resultSet.numberOfDiffExpressedProbes == 0) {
								nodeText = "&nbsp;&nbsp;No expressed probes.";
							}
							else {
								nodeText += subsetText;
								nodeText += this.getBaseline(resultSet);
								nodeText += this.getActionLinks(resultSet,analysisName[0],this.ee.id, nodeId);
								//nodeText += this.getExpressionNumbers(resultSet);
							}
						parentText = '<b>' +analysisName[0]+'</b> '+nodeText;
					}
					// if analysis has >1 result set, create result set children
					else{
						for (var i = 0; i < analysis.resultSets.size(); i++) {
							//console.log("RESULT SET " + (i + 1) + " of " + analysis.resultSets.size());
							var resultSet = analysis.resultSets[i];
																					
							// get experimental factor string and build analysis parent node text
							var analysisName = this.getFactorNameText(resultSet);
							var factor = '<b>'+analysisName[0]+'</b>';
							interaction += analysisName[1];
							
							if (resultSet.experimentalFactors.size() == 1) {
								parentText = (!parentText) ? factor : (parentText + " & " + factor);
							}

							var nodeText= '';
							if (resultSet.numberOfDiffExpressedProbes == 0) {
								nodeText = "&nbsp;&nbsp;No expressed probes.";
							}
							else {
								nodeText += subsetText;
								nodeText += this.getBaseline(resultSet);
								nodeText += this.getActionLinks(resultSet,factor,this.ee.id,(nodeId+1));
								//nodeText += this.getExpressionNumbers(resultSet);
							}
							
							//make child nodes for each analysis and add them to parent factor node
							var analysisNode = new Ext.tree.TreeNode({
								id: 'node'+ (nodeId++),
								expanded: true,
								singleClickExpand: true,
								text: factor + nodeText
							});
							
							parentNode.appendChild(analysisNode);
							
						
						}
					}
					// figure out type of a ANOVA
					var numberOfFactors = 0;
					for (var i = 0; i < analysis.resultSets.size(); i++) {
						var resultSet = analysis.resultSets[i];
						// ignore the result sets where interactions are being looked at
						if(resultSet.experimentalFactors.size()==1){
							numberOfFactors++;} 
					}
					
					var analysisDesc = '';
					if(numberOfFactors==1){
						analysisDesc = 'One-way ANOVA on ';
					}else if(numberOfFactors==2){
						analysisDesc = 'Two-way ANOVA' + ((interaction>0) ? ' with interactions on ' : ' on ');
					}else if(numberOfFactors==3){
						analysisDesc = 'Three-way ANOVA' + ((interaction>0) ? ' with interactions on ' : ' on ');
					}else if(numberOfFactors==4){
						analysisDesc = 'Four-way ANOVA' + ((interaction>0) ? ' with interactions on ' : ' on ');
					}else if(numberOfFactors==5){
						analysisDesc = 'Five-way ANOVA' + ((interaction>0) ? ' with interactions on ' : ' on ');
					}else if(numberOfFactors==6){
						analysisDesc = 'Six-way ANOVA' + ((interaction>0) ? ' with interactions on ' : ' on ');
					}else{
						analysisDesc = 'n-way ANOVA' + ((interaction>0) ? ' with interactions on ' : ' on ');
					}//just being overly safe here
					
					parentNode.setText(analysisDesc + parentText + " " + parentNode.text);
				}
	},
	/**
	 * get the number of probes that are differentially expressed and the number of 'up' and 'down' probes
	 * @return String text with numbers
	 */
	getExpressionNumbers:function(resultSet, nodeId){
		/* Show how many probes are differentially expressed; */
				
		 var numbers = resultSet.numberOfDiffExpressedProbes +  ' probes' ;
		
		 //if (resultSet.upregulatedCount != 0) {
			 numbers += ':&nbsp;' + resultSet.upregulatedCount
			 + "&nbsp;Up";
		 //}
		
		 //if (resultSet.downregulatedCount != 0) {
			 numbers += ';&nbsp;' + resultSet.downregulatedCount
			 + "&nbsp;Down";
		// }
		numbers += '. <br>Threshold value = '+resultSet.threshold;
		numbers += (resultSet.qValue)?(', qvalue = '+resultSet.qValue):'';
		
		// save number of up regulated probes for drawing as chart after tree has been rendered
		// if there are no up or down regulated probes, draw an empty circle
		if(resultSet.downregulatedCount== 0 && resultSet.upregulatedCount == 0){
			this.percentUpProbes[nodeId]=null;	
		}else{
			this.percentUpProbes[nodeId]=resultSet.upregulatedCount/resultSet.numberOfDiffExpressedProbes;
		}
		return numbers;	 
	},
	getBaseline: function(resultSet){
		// get baseline info
		if (resultSet.baselineGroup) {
			return ' with baseline&nbsp;=&nbsp;' + resultSet.baselineGroup.value;
		}
		return '';
	},
	getActionLinks:function(resultSet, factor, eeID, nodeId){
		/*link for details*/
		var numbers = this.getExpressionNumbers(resultSet, nodeId);
		var linkText = '&nbsp;' +
		'<span class="link" onClick="Ext.Msg.alert(\'Probe Contrast Details\', \''+numbers+'\')" ext:qtip=\"'+numbers+'\">' +
		'&nbsp;<canvas height=20 width=20 id="chartDiv'+nodeId+'"></canvas></span>';
		/*+ '&nbsp;<img src="/Gemma/images/magnifier.png">&nbsp;</span>';*/
		
		/* provide link for visualization.*/
		linkText += 
		'<span class="link" onClick="Ext.getCmp(\'ee-details-panel\').visualizeDiffExpressionHandler(\'' +
		eeID +
		'\',\'' + resultSet.resultSetId + '\',\'' + factor +
		'\')" ext:qtip="Click to visualize differentially expressed probes for: ' +
		factor + ' (FDR threshold=' + resultSet.threshold +
		')">&nbsp;<img src="/Gemma/images/icons/chart_curve.png">&nbsp;</span>';
		return linkText;
	},
	/**
	 * get experimental factor string and build analysis parent node text
	 * @param {Object} resultSet
	 * @return {[String,int]} an array with the first element being the factor text and the second a flag marking interaction
	 */
	getFactorNameText:function(resultSet){
		var factor ='';
		var interaction = 0;
		if (resultSet.experimentalFactors == null || resultSet.experimentalFactors.size() == 0) {
			factor = "n/a";
		}
		else {
			factor = resultSet.experimentalFactors[0].name /*+ " (" + resultSet.experimentalFactors[0].category + ")"*/;
			
			for (var k = 1; k < resultSet.experimentalFactors.size(); k++) {
				//console.log("!expFac " + k + ": " + resultSet.experimentalFactors[k].name);
				factor = factor + " x " + resultSet.experimentalFactors[k].name /*+ " (" + resultSet.experimentalFactors[k].category + ")"*/;
				interaction = 1;
			}
		}
		return [ factor, interaction];
	},
	drawPieCharts: function(){
		var ctx, up;
		for (i = 0; i < this.percentUpProbes.size(); i++) {
			if (Ext.get('chartDiv' + i)) {
				up = this.percentUpProbes[i];
				ctx = Ext.get('chartDiv' + i).dom.getContext("2d");
				if(up == null){
					drawTwoColourMiniPie(ctx, 12, 12, 14, 'white', 'white', up * 360, 'black');
				}else{
					drawTwoColourMiniPie(ctx, 12, 12, 14, 'lightgrey', 'SteelBlue', up * 360, 'black');
				}
				
			}
		}
	}
});

// register panel as xtype
Ext.reg('differentialExpressionAnalysesSummaryTree',Gemma.DifferentialExpressionAnalysesSummaryTree);
