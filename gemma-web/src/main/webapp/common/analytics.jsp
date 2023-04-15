<%@ include file="/common/taglibs.jsp" %>

<c:if test='${ appConfig["ga.tracker"] != null}'>

    <!-- Google tag (gtag.js) -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=${appConfig["ga.tracker"]}"></script>
    <script>
       window.dataLayer = window.dataLayer || [];

       function gtag() {
          dataLayer.push( arguments );
       }

       gtag( 'js', new Date() );

       gtag( 'config', ${appConfig["ga.tracker"]}, {
          send_page_view: false
       } );

       <!-- End Google Analytics -->

       if ( typeof googleAnalyticsTrackPageviewIfConfigured === 'undefined' ) {
          var googleAnalyticsTrackPageviewIfConfigured = googleAnalyticsTrackPageviewIfConfigured || function( pageURL ) {
             if ( typeof ga !== 'undefined' ) {
                gtag( 'event', 'page_view', {
                   page_location : pageURL
                } );
             }
          };
       }
    </script>
</c:if>

<c:if test='${ appConfig["ga.tracker"] == null}'>
    <script type="text/javascript">
       if ( typeof googleAnalyticsTrackPageviewIfConfigured === 'undefined' ) {
          var googleAnalyticsTrackPageviewIfConfigured = function( pageURL ) {/* no op, for sandbox and development */
          };
       }
    </script>
</c:if>
