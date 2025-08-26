<%@ include file="/common/taglibs.jsp" %>
<head>
<title>Meta-analysis Heatmap</title>
<script type='text/javascript' src='${pageContext.request.contextPath}/static/heatmaplib.js'></script>
<script type='text/javascript'
        src='${pageContext.request.contextPath}/dwr/interface/DifferentialExpressionSearchController.js'></script>
</head>

<a href='<c:url value="/home.html"/>'>Start a new search</a><br /><br />
<div id="meta-heatmap-div"></div>

<script type="text/javascript">

//Ext.state.Manager.setProvider(new Ext.state.CookieProvider( ));
Ext.QuickTips.init();
//var data = {};
Ext.onReady( function() {
   if ( !document.createElement( "canvas" ).getContext ) {
      redirectToClassic = true;
      //not supported
      if ( Ext.isIE8 ) {
         // excanvas doesn't cover all functionality of new diff ex metaheatmap visualization
         Ext.DomHelper.append( 'meta-heatmap-div', {
            tag : 'p',
            cls : 'trouble',
            id : 'browserWarning',
            html : 'Advanced differential expression visualizations are not available in your browser (Internet Explorer 8). We suggest upgrading to  ' +
               '<a href="https://www.microsoft.com/en-us/edge/" target="_blank">Microsoft Edge</a>, ' +
               '<a href="https://www.firefox.com/en-CA/" target="_blank">Firefox</a> or ' +
               '<a href="https://www.google.com/chrome/" target="_blank">Chrome</a>.'
         } );
      } else if ( Ext.isIE ) {
         Ext.DomHelper.append( 'meta-heatmap-div', {
            tag : 'p',
            cls : 'trouble',
            id : 'browserWarning',
            html : 'This page may display improperly in older versions of Internet Explorer(IE). Please upgrade to ' +
               '<a href="https://www.microsoft.com/en-us/edge/" target="_blank">Microsoft Edge</a>, ' +
               '<a href="https://www.firefox.com/en-CA/" target="_blank">Firefox</a> or ' +
               '<a href="https://www.google.com/chrome/" target="_blank">Chrome</a>.' +
               ' If you are running IE 9 and you see this message, please make sure you are not in compatibility mode. '
         } );
      } else {
         Ext.DomHelper.append( 'meta-heatmap-div', {
            tag : 'p',
            cls : 'trouble',
            id : 'browserWarning',
            html : 'This page may not display properly in all browsers. (The \"canvas\" element is requried.)' +
               ' Please switch to ' +
               '<a href="https://www.microsoft.com/en-us/edge/" target="_blank">Microsoft Edge</a>, ' +
               '<a href="https://www.firefox.com/en-CA/" target="_blank">Firefox</a> or ' +
               '<a href="https://www.google.com/chrome/" target="_blank">Chrome</a>.'
         } );
      }
   } else {
      new Gemma.MetaHeatmapDataSelection().show();
   }
} );
</script>
