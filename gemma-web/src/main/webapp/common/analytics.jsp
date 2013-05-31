<%@ include file="/common/taglibs.jsp"%>

<c:if test='${ appConfig["ga.tracker"] != null}'>

	<script type="text/javascript">
      var _gaq = _gaq || [];
      var pluginUrl = '//www.google-analytics.com/plugins/ga/inpage_linkid.js';
      _gaq.push([ '_require', 'inpage_linkid', pluginUrl ]);
      _gaq.push([ '_setAccount', '${appConfig["ga.tracker"]}' ]);
      _gaq.push([ '_setDomainName', '${appConfig["ga.domain"]}' ]);
      _gaq.push([ '_trackPageview' ]);

      (function() {
         var ga = document.createElement('script');
         ga.type = 'text/javascript';
         ga.async = true;
         ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www')
               + '.google-analytics.com/ga.js';
         var s = document.getElementsByTagName('script')[0];
         s.parentNode.insertBefore(ga, s);
      })();

      if ( typeof googleAnalyticsTrackPageviewIfConfigured === 'undefined' ) {
         var googleAnalyticsTrackPageviewIfConfigured = googleAnalyticsTrackPageviewIfConfigured || function(pageURL) {
            if ( typeof _gaq !== 'undefined' ) {
               _gaq.push([ '_setAccount', '${appConfig["ga.tracker"]}' ]);
               _gaq.push([ '_trackPageview', pageURL ]);
            }
         };
      }
   </script>
</c:if>
