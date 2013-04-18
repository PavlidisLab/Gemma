Ext.namespace('Gemma');

/**
 * Task progress window that wraps ObservableSubmittedTask.
 *
 * Config options;
 * @param {ObservableSubmittedTask} task
 * @param [displayOptions]
 *
 * @class Gemma.ProgressWindow
 * @extends Ext.Window
 */
Gemma.ProgressWindow = Ext.extend(Ext.Window, {

    modal: true,
    closable: false,
    resizable: false,
    stateful: false,
    showAllMessages: false,
    collapsible: false,
    autoHeight: true,

    width: 400,

    initComponent: function () {
        if (!Ext.isDefined( this.displayOptions )) { this.displayOptions = {}; }

        this.progressWidget = new Gemma.ProgressWidget({
            task: this.task,
            displayOptions: this.displayOptions
        });

        Ext.apply( this, {
            items: [this.progressWidget]
        });

        Gemma.ProgressWindow.superclass.initComponent.call( this );

        this.progressWidget.on('beforedestroy', function() {
            this.destroy();
        }, this);

        this.on("show", function() {
            this.progressWidget.show();
        }, this);
    }
});

/**
 * Config options:
 *
 * @param taskId
 *            (required)
 * @class Gemma.ProgressWidget
 * @extends Ext.Panel
 */
Gemma.ProgressWidget = Ext.extend(Ext.Panel, {
    resizable: false,
    bodyBorder: false,
    stateful: false,

    // Default options
    showLogOnTaskFailure: false,
    hideLogButton: true,
    hideCloseButton: true,
    closeOnTaskCompletion: true,

    initComponent: function () {
        if (!Ext.isDefined( this.task )) {
            throw {name:"IllegalArgument", message:"Task is a required argument."};
        }

        Ext.apply(this, this.displayOptions);

        Ext.apply(this, {
            items: [
                {
                    xtype:'progress',
                    ref: 'progressBar',
                    width: 400,
                    text: "Initializing ..."
                },
                {
                    xtype:'panel',
                    autoScroll: true,
                    hidden: true,
                    ref:'logPanel',
                    items: [
                        {
                            xtype:'label',
                            ref:'textLabel'
                        }
                    ]
                }
            ],
            buttons: [
                {
                    text: "Logs",
                    itemId: "progresslogsbutton",
                    tooltip: "Show log messages",
                    hidden: this.hideLogButton,
                    enableToggle: true,
                    toggleHandler: function (button, enabled) {
                        if (enabled) {
                            this.showLogMessages();
                        } else {
                            this.hideLogMessages();
                        }
                    },
                    scope: this
                },
                {
                    text: "Cancel Job",
                    itemId: "progresscancelbutton",
                    tooltip: "Attempt to stop the job.",
                    handler: function () {
                        Ext.Msg.show({
                            title: 'Cancel?',
                            msg: 'Are you sure?',
                            buttons: Ext.Msg.YESNO,
                            fn: function (btn) {
                                if (btn === 'yes') {
                                    this.task.cancel();
                                }
                            },
                            scope: this,
                            icon: Ext.MessageBox.QUESTION
                        });
                    },
                    scope: this
                },
                {
                    text: "Close",
                    itemId: "progresshidebutton",
                    tooltip: "Remove the progress bar and return to the page",
                    hidden: this.hideCloseButton,
                    handler: function () {
                        Ext.Msg.show({
                            title: "Run in background",
                            msg: "The job will continue to run. You can get an email on completion. ",
                            buttons: {
                                ok: 'OK',
                                emailMe: 'Email me'
                            },
                            fn: function (btn) {
                                if (btn === 'emailMe') {
                                    this.task.addEmailAlert();
                                }
                            },
                            scope: this
                        });
                        this.stopListening();
                    },
                    scope: this
                }
            ]
        });

        Gemma.ProgressWidget.superclass.initComponent.call(this);

        this.on('show', this.startListening, this, {single:true});
    },

    /**
     * Start the progressbar in motion.
     * @private
     */
    startProgressBarAnimation: function () {
        this.progressBar.wait({
            text: "Starting ..."
        });
    },

    /**
     * @private
     */
    stopProgressBarAnimation: function () {
        this.progressBar.reset();
    },

    /**
     *
     * @private
     */
    startListening: function () {
        this.startProgressBarAnimation();

        this.task.on("task-completed", function() {
            this.stopListening();
            this.destroy();
        }, this);

        this.task.on("task-failed", function() {
            this.stopListening();
        }, this);

        this.task.on('task-cancelling', function() {
            this.stopListening();
            this.destroy();
        }, this );

        this.task.on("log-message-received", function (message) {
            this.progressBar.updateText( message );
            if ( this.logPanel.rendered ) {
                this.logPanel.textLabel.setText( this.task.logs );
            }
        }, this);

        this.task.on("synchronization-error", function() {
            this.progressBar.updateText( "Couldn't get progress updates from the server." );
        });
    },

    /**
     *
     * @private
     */
    stopListening: function() {
       this.task.purgeListeners();
       this.stopProgressBarAnimation();
    },

    /**
     *
     * @private
     */
    showLogMessages: function () {
        this.logPanel.show();
        this.logPanel.textLabel.setText( this.task.logs );
    },

    /**
     *
     * @private
     */
    hideLogMessages: function() {
        this.logPanel.hide();
    },

    //TODO: handle at the call site
    findTaskId: function () {
        // try to get from query string

        var queryStart = document.URL.indexOf("?");
        if (queryStart > -1) {
            var param = Ext.urlDecode(document.URL.substr(queryStart + 1));
            if (param.taskId) {
                return param.taskId;
            }
        }

        // try to get from hidden input field.
        return dwr.util.getValue("taskId");
    }
});