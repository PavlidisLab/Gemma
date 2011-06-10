<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
            + path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<head>
		<base href="<%=basePath%>">

		<title>My JSP 'contactUs.jsp' starting page</title>

		<meta http-equiv="pragma" content="no-cache">
		<meta http-equiv="cache-control" content="no-cache">
		<meta http-equiv="expires" content="0">
		<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
		<meta http-equiv="description" content="This is my page">
		<!--
	<link rel="stylesheet" type="text/css" href="styles.css">
	-->

	</head>


	<body>
		<div style="margin-bottom: 10px;">
			<div id="contact">
				<h3>
					Contacting us
				</h3>
				<p class="emphasized">
					To get emails about updates to the Gemma software, subscribe to the
					<a href="http://lists.chibi.ubc.ca/mailman/listinfo/gemma-announce">Gemma-announce
						mailing list</a>.
				</p>
				<p>
					For feature requests and bug reports, send us an email
					<a href="mailto:gemma@chibi.ubc.ca">here</a>.
				</p>
				<p>
					Please also keep an eye out for the feedback button-tab on the left
					of some pages in Gemma. It looks like
					<a class="thumbnail" href="javascript:void(0)">this<span><img
								src="/Gemma/images/feedbackButton.png" width="30px"
								height="103px" border="0" />
							<br />
					</span>
					</a> and is an easy way to give us in-context feedback!
				</p>


			</div>
		</div>
	</body>
</html>
