<%@page import="model.Autocompletion"%>
<%@page import="java.util.List"%>
<%
  /*  when you start typing b bi bik bike bike, input typed is stored in param q
    Example from: viralpatel.net/blogs/tutorial-create-autocomplete-feature-with-java-jsp-jquery/
    $('#country').autocomplete("list.jsp", {extraParams: {state: California }} );
    This will generates the internal URL like
    /getdata.jsp?q=ourChar&state=California    
    */

    String x = request.getParameter("q");
    System.out.println("autocomplete jsp gets x= " + x);
    boolean asJson = false;
    int maxAutoCompletion = 15;
    List<String> autocompletions = Autocompletion.event(x, maxAutoCompletion);

    int i = 0;
    StringBuilder sb = new StringBuilder();
    for(String y: autocompletions){

  if(y != null && y.length() != 0){
    String yAsJson = "";
    if(i == 0)
      yAsJson += "[";
    else yAsJson += ",";

    yAsJson += ("{\"autocompletion\":\"" + y + "\"}\n");

    String yToAdd = !asJson ? (y + "\n") : yAsJson;
    sb.append(yToAdd);
  }
    }

    out.println(sb.toString());
%>
