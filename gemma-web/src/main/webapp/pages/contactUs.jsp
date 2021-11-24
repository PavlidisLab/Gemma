<!DOCTYPE html>
<%-- Decorator for the home page --%>


<%-- Include common set of tag library declarations for each layout --%>
<%@ include file="/common/taglibs.jsp"%>
<html>
<title>Contact Us</title>
	<body>
		<div style="margin-bottom: 10px;">
			<div id="contact">
				<h3>
					Contacting us
				</h3>
				<p>
					For feature requests and bug reports, send us an email
					<a href="mailto:pavlab-support@msl.ubc.ca">here</a>.
				</p>
				<p>
					Please also keep an eye out for the feedback button-tab on the left
					of some pages in Gemma. It looks like
					<a class="thumbnail" href="javascript:void(0)">this<span><img
                            src="${pageContext.request.contextPath}/images/feedbackButton.png" width="30px"
                            height="103px" border="0" />
							<br />
					</span>
					</a> and is an easy way to give us in-context feedback!
				</p>

			</div>
		</div>
	</body>
</html>
