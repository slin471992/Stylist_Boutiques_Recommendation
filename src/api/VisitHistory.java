package api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.MySQLDBConnection;
import db.MongoDBConnection;

/**
 * Servlet implementation class VisitHistory
 */
@WebServlet("/history")
public class VisitHistory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final DBConnection connection = new MySQLDBConnection();
	//private static DBConnection connection = new MongoDBConnection();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public VisitHistory() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}
//		try {
//			// DBConnection connection = new MySQLDBConnection();
//			JSONArray array = null;
//			if (request.getParameterMap().containsKey("user_id")) {
//				String userId = request.getParameter("user_id");
//				Set<String> visited_business_id = connection.getVisitedBoutiques(userId);
//				array = new JSONArray();
//				for (String id : visited_business_id) {
//					array.put(connection.getBoutiquesById(id, true));
//				}
//				RpcParser.writeOutput(response, array);
//			} else {
//				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
		
		JSONArray array = null;
		String userId = (String) session.getAttribute("user");
		Set<String> visited_business_id = connection.getVisitedBoutiques(userId);
		array = new JSONArray();
		for (String id : visited_business_id) {
			array.put(connection.getBoutiquesById(id, true));
		}
		RpcParser.writeOutput(response, array);				
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}

//		try {
//			JSONObject input = RpcParser.parseInput(request);
//			if (input.has("user_id") && input.has("visited")) {
//				String userId = (String) input.get("user_id");
//				JSONArray array = (JSONArray) input.get("visited");
//				List<String> visitedBoutiques = new ArrayList<>();
//				for (int i = 0; i < array.length(); i++) {
//					String businessId = (String) array.get(i);
//					visitedBoutiques.add(businessId);
//				}
//				connection.setVisitedBoutiques(userId, visitedBoutiques);
//				RpcParser.writeOutput(response, new JSONObject().put("status", "OK"));
//			} else {
//				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
		try {
			JSONObject input = RpcParser.parseInput(request);
			String userId = (String) session.getAttribute("user");
			if (input.has("visited")) {
				JSONArray array = (JSONArray) input.get("visited");
				List<String> visitedBoutiques = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					String businessId = (String) array.get(i);
					visitedBoutiques.add(businessId);
				}
				connection.setVisitedBoutiques(userId, visitedBoutiques);
				RpcParser.writeOutput(response, new JSONObject().put("status", "OK"));
			} else {
				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// allow access only if session exists
		HttpSession session = request.getSession();
		if (session.getAttribute("user") == null) {
			response.setStatus(403);
			return;
		}
//		try {
//			JSONObject input = RpcParser.parseInput(request);
//			if (input.has("user_id") && input.has("visited")) {
//				String userId = (String) input.get("user_id");
//				JSONArray array = (JSONArray) input.get("visited");
//				List<String> visitedBoutiques = new ArrayList<>();
//				for (int i = 0; i < array.length(); i++) {
//					String businessId = (String) array.get(i);
//					visitedBoutiques.add(businessId);
//				}
//				connection.unsetVisitedBoutiques(userId, visitedBoutiques);
//				RpcParser.writeOutput(response, new JSONObject().put("status", "OK"));
//			} else {
//				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
		try {
			JSONObject input = RpcParser.parseInput(request);
			String userId = (String) session.getAttribute("user");
			if (input.has("visited")) {
				JSONArray array = (JSONArray) input.get("visited");
				List<String> visitedBoutiques = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					String businessId = (String) array.get(i);
					visitedBoutiques.add(businessId);
				}
				connection.unsetVisitedBoutiques(userId, visitedBoutiques);
				RpcParser.writeOutput(response, new JSONObject().put("status", "OK"));
			} else {
				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
