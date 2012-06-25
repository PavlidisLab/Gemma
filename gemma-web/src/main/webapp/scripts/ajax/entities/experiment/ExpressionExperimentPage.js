Ext.namespace('Gemma');

/**
 *
 * Top level container for all sections of expression experiment info
 * Sections are:
 * 1. Details (has editing tools)
 * 2. Experimental design
 * 3. Expression visualisation
 * 4. Diagnostics
 * 5. Quantitation Types ?
 * 6. History (admin only)
 * 7. Admin (running analyses)
 * 
 * 
 * To open the page at a specific tab, include ?tab=[tabName] suffix in the URL. Tab names are each tab's itemId.
 *
 * @class Gemma.ExpressionExperimentPage
 * @extends Ext.TabPanel
 *
 */
Gemma.ExpressionExperimentPage = Ext.extend(Ext.TabPanel, {

    height: 600,
    defaults: {
        autoScroll: true,
        width: 850
    },
    initialTab: 'details',
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
    checkURLforInitialTab: function(){
    	this.loadSpecificTab = (document.URL.indexOf("?") > -1 && (document.URL.indexOf("tab=") > -1));
        if ( this.loadSpecificTab ) {
        	var param = Ext.urlDecode(document.URL.substr(document.URL.indexOf("?") + 1));
        	if (param.tab) {
        		if(this.getComponent( param.tab ) != undefined){
        			this.initialTab =  param.tab;
        		}
        	}
        }
    },
    initComponent: function(){
    
        var eeId = this.eeId;

        var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
                
        Gemma.ExpressionExperimentPage.superclass.initComponent.call(this);
        this.on('render', function(){
            if (!this.loadMask) {
                this.loadMask = new Ext.LoadMask(this.getEl(), {
                    msg: Gemma.StatusText.Loading.generic,
                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                });
            }
            this.loadMask.show();  
        });

        ExpressionExperimentController.loadExpressionExperimentDetails(eeId, function(experimentDetails){
        	this.initFromExperimentValueObject(experimentDetails, isAdmin);

            this.checkURLforInitialTab();
            this.setActiveTab( this.initialTab );
            
        }.createDelegate(this));

   	
        Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){
            var appScope = this;
            ExpressionExperimentController.canCurrentUserEditExperiment(experimentDetails.id, {
                callback: function(editable){
                    //console.log(this);
                    appScope.adjustForIsAdmin(isAdmin, editable);
                },
                scope: appScope
            });
            
        }, this);
        
        Gemma.Application.currentUser.on("logOut", function(){
        
            this.adjustForIsAdmin(false, false);
            
        }, this);
    },
    initFromExperimentValueObject: function( experimentDetails, isAdmin ){
        
        this.experimentDetails = experimentDetails;
        this.editable = experimentDetails.canCurrentUserEditExperiment;
        this.ownedByCurrentUser = experimentDetails.doesCurrentUserOwnExperiment;
        
        if(this.loadMask){
        	this.loadMask.hide();
        }
        
        // DETAILS TAB
        this.add(this.makeDetailsTab( experimentDetails ));
        
        // EXPERIMENT DESIGN TAB
                        
        this.add(this.makeDesignTab(experimentDetails));
        
        // VISUALISATION TAB
        this.add(this.makeVisualisationTab(experimentDetails, isAdmin));
        
        // DIAGNOSTICS TAB
        this.add(this.makeDiagnosticsTab(experimentDetails, isAdmin));
                        
        // QUANTITATION TYPES TAB
        this.add(new Gemma.ExpressionExperimentQuantitationTypeGrid({
            title: 'Quantitation Types',
            itemId: 'quantitation',
            eeid: experimentDetails.id
        }));
        
        this.adjustForIsAdmin(isAdmin, this.editable);
       
    },
    makeDetailsTab : function(experimentDetails){
    	return new Gemma.ExpressionExperimentDetails({
    		title: 'Details',
            itemId: 'details',
            id : 'ee-details-panel',
            experimentDetails: experimentDetails,
            editable: this.editable,
			owned: this.ownedByCurrentUser,
            admin: this.admin,
            listeners:{
            	'experimentDetailsReloadRequired': function(){
            		var myMask = new Ext.LoadMask(Ext.getBody(), {msg:"Refreshing..."});
            		myMask.show();
            		window.location.reload(false); // could do something fancier like reloading just the component
            	},
            	scope:this
            }
        });
    },
    makeDesignTab: function(experimentDetails){
    	var batchInfo = '';
        if (experimentDetails.hasBatchInformation) {
            batchInfo = '<span style="font-size: smaller">This experimental design also ' +
            'has information on batches, not shown.</span>' +
            '<br />' +
            '<span style="color:#DD2222;font-size: smaller"> ' +
            experimentDetails.batchConfound +
            ' </span>' +
            '<span style="color:#DD2222;font-size: smaller"> ' +
            experimentDetails.batchEffect +
            ' </span>';
        }
        
        return {
            title: 'Experimental Design',
            tbar: [{
                text: 'Show Details',
                itemId: 'design',
                tooltip: 'Go to the design details',
                icon: '/Gemma/images/magnifier.png',
                handler: function(){
                    window.open("/Gemma/experimentalDesign/showExperimentalDesign.html?eeid=" + experimentDetails.id);
                }
            }],
            html: batchInfo + '<div id="eeDesignMatrix" style="height:100%">Loading...</div>',
            layout: 'absolute',
            //items -> bar chart and table?
            listeners: {
                render: function(){
                    DesignMatrix.init({
                        id: experimentDetails.id
                    });
                }
            }
        };
    },
    makeVisualisationTab: function(experimentDetails, isAdmin){
        var eeId = this.eeId;
    	var title = "Data for a 'random' sampling of probes";
        var downloadLink = '';
        var geneList = [];
        downloadLink = String.format("/Gemma/dedv/downloadDEDV.html?ee={0}", eeId);
        var viz = new Gemma.VisualizationWithThumbsPanel({
            thumbnails: false,
            downloadLink: downloadLink,
            params: [[eeId], geneList]
        });
        viz.on('render', function(){
            viz.loadFromParam({
                params: [[eeId], geneList]
            });
        });
        var geneTBar = new Gemma.VisualizationWidgetGeneSelectionToolbar({
                eeId: eeId,
                visPanel: viz,
                taxonId: experimentDetails.parentTaxonId,
                showRefresh: ( isAdmin || this.editable )
            });
        geneTBar.on('refreshVisualisation', function(){
        	viz.loadFromParam({
                params: [[eeId], geneList]
            });
        });
        return {
            items: viz,
            itemId: 'visualize',
            layout: 'fit',
            padding: 0,
            title: 'Visualize Expression',
            tbar: geneTBar
        };
    },
    makeDiagnosticsTab: function(experimentDetails, isAdmin){

        var refreshDiagnosticsLink = '';
        if (this.editable || isAdmin) {
            refreshDiagnosticsLink = '<a href="refreshCorrMatrix.html?id=' + experimentDetails.id + '"><img ' +
            'src="/Gemma/images/icons/arrow_refresh_small.png" title="refresh" ' +
            'alt="refresh" />Refresh</a><br>';
        }
		this.refreshDiagnosticsBtn = new Ext.Button({
				icon: '/Gemma/images/icons/arrow_refresh_small.png',
				text: 'Refresh',
				handler: function(){
					window.location = "refreshCorrMatrix.html?id=" + experimentDetails.id;
				},
				hidden: (this.editable || isAdmin)
			});
        return {
            title: 'Diagnostics',
            itemId: 'diagnostics',
			items: [this.refreshDiagnosticsBtn,{html:experimentDetails.QChtml,border:false}]
        };
    },
    adjustForIsAdmin: function(isAdmin, isEditable){
        // hide/show 'refresh' link to diagnostics tab
		this.refreshDiagnosticsBtn.setVisible(isAdmin || isEditable);
		
		/*HISTORY TAB*/
        if ((isAdmin || isEditable) && !this.historyTab) {
            this.historyTab = new Gemma.AuditTrailGrid({
                title: 'History',
                itemId: 'history',
                bodyBorder: false,
                collapsible: false,
                viewConfig: {
                    forceFit: true
                },
                auditable: {
                    id: this.experimentDetails.id,
                    classDelegatingFor: "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
                },
				loadOnlyOnRender:true
            });
            this.add(this.historyTab);
        } else if (this.historyTab) {
                this.historyTab.setVisible((isAdmin || isEditable));
        }
        
        /*ADMIN TOOLS TAB*/
        if ((isAdmin || isEditable) && !this.toolTab) {
            this.toolTab = new Gemma.ExpressionExperimentTools({
                experimentDetails: this.experimentDetails,
                title: 'Admin',
                itemId: 'admin',
                editable: isEditable,
                listeners:{
                	'reloadNeeded': function(){
                		var myMask = new Ext.LoadMask(Ext.getBody(), {msg:"Refreshing..."});
                		myMask.show();
                		var reloadToAdminTab = document.URL;
                		reloadToAdminTab = reloadToAdminTab.replace(/&*tab=\w*/, '');
                		reloadToAdminTab += '&tab=admin';
                		window.location.href = reloadToAdminTab;

                	}
                }
            });
            this.add(this.toolTab);
        } else if (this.toolTab) {
            this.toolTab.setVisible((isAdmin || isEditable));
        }
    }
});

/**
 * Used to make the correlation heatmap clickable. See ExperimentQCTag.java
 *
 * @param {Object}
 *            bigImageUrl
 */
var popupImage = function(url, width, height){
    url = url + "&nocache=" + Math.floor(Math.random() * 1000);
    var b = new Ext.Window({
        modal: true,
        stateful: false,
        resizable: true,
        autoScroll: true,
        height: height, // or false.
        width: width || 200,
        padding: 10,
        html: '<img src=\"' + url + '"\" />'
    });
    b.show();
};
