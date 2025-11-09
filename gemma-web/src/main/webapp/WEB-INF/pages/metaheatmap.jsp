<%@ include file="/WEB-INF/common/taglibs.jsp" %>
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
               '<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">Internet Explorer 9</a>, ' +
               '<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a> or ' +
               '<a href="http://www.google.com/chrome/" target="_blank">Chrome</a>.'
         } );
      } else if ( Ext.isIE ) {
         Ext.DomHelper.append( 'meta-heatmap-div', {
            tag : 'p',
            cls : 'trouble',
            id : 'browserWarning',
            html : 'This page may display improperly in older versions of Internet Explorer(IE). Please upgrade to ' +
               '<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">IE 9</a>, ' +
               '<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a> or ' +
               '<a href="http://www.google.com/chrome/" target="_blank">Chrome</a>.' +
               ' If you are running IE 9 and you see this message, please make sure you are not in compatibility mode. '
         } );
      } else {
         Ext.DomHelper.append( 'meta-heatmap-div', {
            tag : 'p',
            cls : 'trouble',
            id : 'browserWarning',
            html : 'This page may not display properly in all browsers. (The \"canvas\" element is requried.)' +
               ' Please switch to ' +
               '<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a>,' +
               '<a href="http://www.google.com/chrome/" target="_blank">Chrome</a> or' +
               '<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">Internet Explorer 9</a>.'
         } );
      }
   } else {
      new Gemma.MetaHeatmapDataSelection().show();
   }
} );
</script>
