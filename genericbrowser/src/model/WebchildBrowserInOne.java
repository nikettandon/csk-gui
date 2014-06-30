package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.DBConnector;
import view.UI;
import controller.InputToModel;
import controller.InputToView;
import controller.InputToView.Atom;
import controller.InputToView.AtomType;
import controller.InputToView.Autocomplete;
import controller.InputToView.Decoration;
import controller.InputToView.Decoration.background;
import controller.InputToView.Decoration.boldface;
import controller.InputToView.Decoration.color;
import controller.InputToView.Decoration.font;
import controller.InputToView.Decoration.fontsize;
import controller.InputToView.ResultRow;
import controller.InputToView.SubmitForm;
import controller.InputToView.SubmitForm.buttonInfo;

/**
 * Servlet implementation class Controller
 */
public class WebchildBrowserInOne extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			WebchildBrowserOld.variablesInit(false);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (Autocompletion.autoCompletionXY == null
				|| Autocompletion.autoCompletionX == null)
			// Autocompletion.initACSortedByNumAdj();
			Autocompletion.initACSortedByFreq();
		if (Autocompletion.autoCompletionX == null)
			try {
				Autocompletion
						.initAbsolCSKSortedByNumPatterns("/home/chariman/git/csk-gui/"
								+ "genericbrowser/WebContent/data/preloaded.x.nouns");
				WebchildBrowserOld.variablesInit(true);
			} catch (Exception e) {
				System.out.println("Exception in autocompletion setup!");
				e.printStackTrace();
			}
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Atoms
		List<Atom> atomsWithHeader = new ArrayList<>();
		atomsWithHeader.add(header());
		List<ResultRow> rows = new ArrayList<>();
		List<Atom> relatedWords = new ArrayList<>();
		/*
		 * String resultsSummary = modelProxy.dbfetch(formInputToModel(request),
		 * atomsWithHeader, rows);
		 */
		String resultsSummary = getResultSummary(request, atomsWithHeader, rows, relatedWords);
		atomsWithHeader.add(new Atom(AtomType.resultSummary, -2,
				resultsSummary, "", "disambiguation", "resultssummary",
				new Decoration()));
		// UI
		SubmitForm form = formInputToUI("GET", "webchild1");
		Autocomplete ac = new Autocomplete("x", "jsp/autocomplete.jsp");
		UI uiProxy = new UI(new InputToView(form, atomsWithHeader, rows, relatedWords, ac));
		// Construct response
		StringBuilder responseText = uiProxy.buildHTMLCode();
		PrintWriter out = response.getWriter();
		response.setContentType("text/html");
		out.println(responseText.toString());

	}

	protected String getResultSummary(HttpServletRequest request,
			List<Atom> atomsWithHeader, List<ResultRow> rows, List<Atom> relatedWords) {
		try{
			String x = request.getParameter("x");
			
			if (x.contains(",")) {

				return getCResult(request, atomsWithHeader, rows, relatedWords);
			} else {
				
				LogicNonComparative modelProxy = new LogicNonComparative();
				return modelProxy.dbfetch(formInputToModelNC(request),
					atomsWithHeader, rows, relatedWords);
			}
		}
		catch (NullPointerException el){
			return getCResult(request, atomsWithHeader, rows, relatedWords);
		}
	}
	
	private String getCResult(HttpServletRequest request,
			List<Atom> atomsWithHeader, List<ResultRow> rows, List<Atom> relatedWords){
		Logic modelProxy = new Logic();
		return modelProxy.dbfetch(formInputToModel(request),
			atomsWithHeader, rows, relatedWords);
	}
	
	private Atom header() {
		return new Atom(AtomType.header, 0, "Comparative Commonsense Browser",
				"#", "Would be part of WebChild Browser", "header",
				new Decoration(color.black, font.normal, boldface.normal,
						fontsize.normal, background.yellow));
	}

	private SubmitForm formInputToUI(String getOrSet, String action) {
		// Buttons and submit form.
		List<buttonInfo> buttons = new ArrayList<buttonInfo>();
		buttons.add(new buttonInfo("x", "text", "x", "",
				"e.g. car,bicycle OR car"));
		buttons.add(new buttonInfo("go", "submit", "go", "search", ""));
		SubmitForm form = new SubmitForm(getOrSet, action, buttons);
		return form;
	}

	private InputToModel formInputToModel(HttpServletRequest request) {
		String xAndY = request.getParameter("x");
		if (xAndY != null)
			xAndY = xAndY.trim();
		if (xAndY == null || xAndY.length() == 0)
			xAndY = "car,bike";

		String[] xAndYSplitted = xAndY.split(",");
		if (xAndYSplitted.length != 2)
			xAndYSplitted = new String[] { "car", "bike" };
		String x = xAndYSplitted[0].trim().toLowerCase();
		String y = xAndYSplitted[1].trim().toLowerCase();

		int maxDbResults = 300;
		return new InputToModel(Arrays.asList(new String[] { x, y }),
				maxDbResults);
	}

	private InputToModel formInputToModelNC(HttpServletRequest request) {
		String x = request.getParameter("x");
		if (x != null)
			x = x.trim();
		else
			x = "lily";
		if (x.length() == 0)
			x = "lily";
		int maxDbResults = 300;
		boolean isRel = x.startsWith("r_");
		boolean isAdj = x.startsWith("a_");
		String type = isAdj ? "a" : "n";
		type = isRel ? "r" : type;
		return new InputToModel(Arrays.asList(new String[] { x }),
				maxDbResults, Arrays.asList(new String[] { type }));
	}

	public static void main(String[] args) {
		Autocompletion.initACSortedByNumAdj();
		System.out.println(Autocompletion.y("tiger,", 10));
		System.out.println(Autocompletion.y("woman,", 10));
		System.out.println(Autocompletion.y("ball,c", 10));
	}

}
