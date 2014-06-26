package model;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import view.UI;
import controller.InputToModel;
import controller.InputToView;
import controller.InputToView.Atom;
import controller.InputToView.AtomType;
import controller.InputToView.Autocomplete;
import controller.InputToView.Decoration;
import controller.InputToView.ResultRow;
import controller.InputToView.Decoration.background;
import controller.InputToView.Decoration.boldface;
import controller.InputToView.Decoration.color;
import controller.InputToView.Decoration.font;
import controller.InputToView.Decoration.fontsize;
import controller.InputToView.SubmitForm;
import controller.InputToView.SubmitForm.buttonInfo;

/**
 * Servlet implementation class Controller
 */
public class WebchildBrowserNonComparative extends HttpServlet {
private static final long serialVersionUID = 1L;

public void init(ServletConfig config) throws ServletException{
  super.init(config);
  if(Autocompletion.autoCompletionX == null)
    try{
      Autocompletion.initAbsolCSKSortedByNumPatterns("/var/tmp/git-repository/"
        + "genericbrowser/WebContent/data/preloaded.x.nouns");
      WebchildBrowserOld.variablesInit(true);
    } catch (Exception e){
      System.out.println("Exception in autocompletion setup!");
      e.printStackTrace();
    }
}

protected void doGet(HttpServletRequest request,HttpServletResponse response)
  throws ServletException,IOException{
  // Atoms
  LogicNonComparative modelProxy = new LogicNonComparative();
  List<Atom> atomsWithHeader = new ArrayList<Atom>();
  atomsWithHeader.add(header());
  List<ResultRow> rows = new ArrayList<>();
  String resultsSummary =
    modelProxy.dbfetch(formInputToModel(request), atomsWithHeader, rows);
  atomsWithHeader.add(new Atom(AtomType.resultSummary, -2, resultsSummary, "",
    "disambiguation", "resultssummary", new Decoration()));
  // UI
  SubmitForm form = formInputToUI("GET", "webchildnc");
  Autocomplete ac = new Autocomplete("x", "jsp/autocomplete.jsp");
  UI uiProxy = new UI(new InputToView(form, atomsWithHeader, rows, ac));
  // Construct response
  StringBuilder responseText = uiProxy.buildHTMLCode();
  PrintWriter out = response.getWriter();
  response.setContentType("text/html");
  out.println(responseText.toString());
}

private Atom header(){
  return new Atom(AtomType.header, 0, "Commonsense Browser", "#",
    "Would be part of WebChild Browser", "header", new Decoration(color.black,
      font.normal, boldface.normal, fontsize.normal, background.yellow));
}

private SubmitForm formInputToUI(String getOrSet,String action){
  // Buttons and submit form.
  List<buttonInfo> buttons = new ArrayList<buttonInfo>();
  buttons.add(new buttonInfo("x", "text", "x", "", "e.g. lily"));
  buttons.add(new buttonInfo("go", "submit", "go", "search", ""));
  SubmitForm form = new SubmitForm(getOrSet, action, buttons);
  return form;
}

private InputToModel formInputToModel(HttpServletRequest request){
  String x = request.getParameter("x");
  if(x != null)
    x = x.trim();
  else x = "lily";
  if(x.length() == 0) x = "lily";
  int maxDbResults = 300;
  boolean isRel = x.startsWith("r_");
  boolean isAdj = x.startsWith("a_");
  String type = isAdj ? "a" : "n";
  type = isRel ? "r" : type;
  return new InputToModel(Arrays.asList(new String[] { x }), maxDbResults,
    Arrays.asList(new String[] { type }));
}

public static void main(String[] args){
  Autocompletion.initACSortedByNumAdj();
  System.out.println(Autocompletion.y("tiger", 10));
  System.out.println(Autocompletion.y("woman", 10));
  System.out.println(Autocompletion.y("ball", 10));
}

}
