Ext.namespace( 'Gemma' );

/**
 *
 * Panel with curation tools for curatable objects.
 * Should be displayed in the curation tabs for ExpressionExperiment and ArrayDesign (aka. platform).
 *
 * @class Gemma.CurationTools
 * @extends Ext.Panel
 */
Gemma.CurationTools = Ext.extend( Ext.Panel, {
    curatable: null,

    border : false,
    defaultType : 'box',
    defaults : {
        border : false
    },
    padding : 10,

    initComponent : function() {
        Gemma.CurationTools.superclass.initComponent.call( this );

        this.add( {
            html :
            '<div><h4>Curation status: </h4>' + this.getNeedsAttentionStatusHtml() + '</div>' +
            '<div><h4>Troubled status: </h4>' + this.getTroubleStatusHtml() + '</div>'
        } );
    },


    getNeedsAttentionStatusHtml : function(){
        return this.curatable.needsAttention ? '<span class="red">Needs attention</span>' : '<span class="green">OK</span>';
    },

    getTroubleStatusHtml : function(){
        return this.curatable.troubled ? '<span class="red"><img src="/Gemma/images/icons/stop.png"/> Troubled</span>' : '<span class="green">Not Troubled</span>';
    }

});