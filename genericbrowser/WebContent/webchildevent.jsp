<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<link rel="stylesheet" type="text/css" href="css/form.css" />
<link rel="stylesheet" type="text/css"
	href="css/jquery.autocomplete.css" />
<link rel="stylesheet" type="text/css" href="css/jquery-ui.css" />

<!-- <script src="js/jquery.autocomplete.js"></script> -->
<script src="js/json.autocompletion.js"></script>
<script src="js/jquery-ui.js"></script>
<script src="js/jquery-1.8.2.js"></script>

<title>Commonsense Event Browser</title>

</head>
<body>

	<form method="GET" action="webchildevent" id="searchbox">
		<!-- <h1 align="center">Comparative Commonsense Browser</h1> -->
		<table align="center" border="0" cellpadding="0" cellspacing="0"
			width="435">
			<tr>
				<td>&nbsp;</td>
				<td align="left" width="135" valign="top"><input tabindex="1"
					id="x" type="text" name="x" value="cook" size="35">
					&nbsp;&nbsp;&nbsp; x,y</td>
				<td align="left" valign="top" width="100"><input tabindex="3"
					id="go" type="submit" value="ctriples"></td>
			</tr>
		</table>
	</form>

	<script>
		$("#x").autocomplete("jsp/autocompleteEvent.jsp");
		/* 		myautocomplete('x', 'jsp/autocomplete.jsp');
		 */
	</script>
</body>
</html>