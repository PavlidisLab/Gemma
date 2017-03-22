Ext.namespace('Gemma');

/**
 *
 * Panel with curation tools for curatable objects.
 * Should be displayed in the curation tabs for ExpressionExperiment and ArrayDesign (aka. platform).
 *
 * @class Gemma.CurationTools
 * @extends Ext.Panel
 */
Gemma.CurationTools = Ext.extend(Ext.Panel, {
    curatable: null,

    border: false,
    defaultType: 'box',
    defaults: {
        border: false
    },
    padding: 10,

    initComponent: function () {
        Gemma.CurationTools.superclass.initComponent.call(this);

        this.add({
            html:
                '<div class="v-padded"><span class="bold width110">Troubled status: </span>' + this.getTroubleStatusHtml() + '</div>' +
                '<div class="v-padded"><span class="bold width110">Curation status: </span>' + this.getNeedsAttentionStatusHtml() + '</div>'

        });
    },


    /**
     * Composes the string for curation attention description
     * @returns {string} string containing the status with appropriate icon, color, date and performer.
     */
    getNeedsAttentionStatusHtml: function () {
        //Base status
        var str = this.curatable.needsAttention
            ?   '<span class="gold width130"><i class="fa fa-exclamation-circle fa-lg fa-fw"></i>Needs attention</span>'
            :   '<span class="green width130"><i class="fa fa-check-circle-o fa-lg fa-fw"></i>OK</span>';

        //Status description
        if (this.curatable.lastNeedsAttentionEvent) {
            str +=  '<span class="dark-gray">' +
                        ' <i class="fa fa-calendar"></i> ' +
                        'As of ' + this.curatable.lastNeedsAttentionEvent.date.toLocaleString() +
                        ' <i class="fa fa-user"></i> ' +
                        ' set by: ' + this.curatable.lastNeedsAttentionEvent.performer +
                        ' <i class="fa fa-pencil"></i>' +
                        ' Details: ' + this.getNeedsAttentionNonNullDescription(this.curatable.lastNeedsAttentionEvent) +
                    '</span>';
        }
        return str;
    },

    /**
     * Composes the string for trouble status description
     * @returns {string} string containing the status with appropriate icon, color, date and performer.
     */
    getTroubleStatusHtml: function () {
        //Base status
        var str = this.curatable.troubled
            ?   '<span class="red width130"><i class="fa fa-exclamation-triangle fa-lg fa-fw"></i>Troubled</span>'
            :   '<span class="green width130"><i class="fa fa-check-circle fa-lg fa-fw"></i>Not Troubled</span>';

        //Status details
        if (this.curatable.lastTroubledEvent) {
            str +=  '<span class="dark-gray">' +
                        ' <i class="fa fa-calendar"></i> ' +
                        'As of ' + this.curatable.lastTroubledEvent.date.toLocaleString() +
                        ' <i class="fa fa-user"></i> ' +
                        ' set by: ' + this.curatable.lastTroubledEvent.performer +
                        ' <i class="fa fa-pencil"></i>' +
                        ' Details: ' + this.curatable.lastTroubledEvent.detail +
                    '</span>'
        }

        return str;
    },

    /**
     * Iterates through the events detail and note, and creates an adequate description from the found strings.
     * @param NeedsAttentionEvent the event to be described.
     * @returns {string} description of the given event, or "Unspecified" if no suitable string is found.
     */
    getNeedsAttentionNonNullDescription: function(NeedsAttentionEvent){
        var str = "Unspecified";

        if(NeedsAttentionEvent.detail) str = NeedsAttentionEvent.detail;
        if(NeedsAttentionEvent.note) str = NeedsAttentionEvent.note;

        if(str.length > 100){
            str = str.substring(0,99) + "...";
        }

        return str;
    }

});