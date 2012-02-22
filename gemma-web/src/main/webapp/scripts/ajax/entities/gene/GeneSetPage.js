Ext.namespace('Gemma');

/**
 *
 * Top level container for all sections of gene group info
 * Sections are:
 * 1. Summary (has editing tools)
 * TODO ...
 *
 * @class Gemma.GeneSetPage
 * @extends Ext.TabPanel
 *
 */
Gemma.GeneSetPage = Ext.extend(Ext.TabPanel, {

    defaults: {
        autoScroll: true
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
	invalidIdHandler: function(msg){
		this.items.add(new Ext.Panel({
			html: "Error in loading gene group due to invalid id. "+msg
		}));
	},
    initComponent: function(){
    
		// get id of set to show
        if (!this.geneSetId && document.URL.indexOf("?") > -1 && (document.URL.indexOf("id=") > -1 )) {
            var subsetDetails = document.URL.substr(document.URL.indexOf("?") + 1);
            var param = Ext.urlDecode(subsetDetails);
            if (param.id) {
                var ids = param.id.split(',');
				if(ids.length === 1){
					this.geneSetId = ids[0];
				}else{
					this.invalidIdHandler("Id was: "+param.id);
					Gemma.GeneSetPage.superclass.initComponent.call(this);
					return;
				}
            }if (param.goid) {
                var goids = param.goid.split(',');
				if(goids.length === 1){
					this.geneSetGOId = ids[0];
				}else{
					this.invalidIdHandler("GO Id was: "+param.goid);
					Gemma.GeneSetPage.superclass.initComponent.call(this);
					return;
				}
            } else{
				this.invalidIdHandler("Missing \"id\" or \"GO id\" parameter.");
				Gemma.GeneSetPage.superclass.initComponent.call(this);
				return;
        	}
		}
	

        var isAdmin = Ext.get("hasAdmin").getValue() == 'true';
        
        Gemma.GeneSetPage.superclass.initComponent.call(this);
        this.on('render', function(){
            if (!this.loadMask) {
                this.loadMask = new Ext.LoadMask(this.getEl(), {
                    msg: Gemma.StatusText.Loading.generic,
                    msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                });
            }
            this.loadMask.show();
            GeneSetController.load(this.geneSetId, function(geneSetVO){

				/* taken care of in jsp           
				document.title = geneSetVO.name + " Details | Gemma";
				if(this.ownerCt){
					this.ownerCt.setTitle(geneSetVO.name);
				}*/
				
                this.geneSet = geneSetVO;
                this.editable = geneSetVO.currentUserHasWritePermission;
                this.loadMask.hide();
				
                /*DETAILS TAB*/
                this.add(new Gemma.GeneSetSummary({
                    title: 'Summary',
                    geneSet: geneSetVO,
                    editable: this.editable,
                    admin: this.admin
                }));
                
                
                this.adjustForIsAdmin(isAdmin, this.editable);
                
                Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){
                    var appScope = this;
                    GeneController.canCurrentUserEditGroup(geneDetails.id, {
                        callback: function(editable){
                            appScope.adjustForIsAdmin(isAdmin, editable);
                        },
                        scope: appScope
                    });
                    
                }, this);
                
                Gemma.Application.currentUser.on("logOut", function(){
                    this.adjustForIsAdmin(false, false);
                    
                }, this);
                
                this.setActiveTab(0);
            }.createDelegate(this));
        });
    },
    adjustForIsAdmin: function(isAdmin, isEditable){
        /*HISTORY TAB*/
        
        /*ADMIN TOOLS TAB*/
       
    }
});
