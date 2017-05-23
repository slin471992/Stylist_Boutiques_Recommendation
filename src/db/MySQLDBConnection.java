
package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Boutique;

import org.json.JSONArray;
import org.json.JSONObject;

import yelp.YelpAPI;


public class MySQLDBConnection implements DBConnection {
	// May ask for implementation of other methods. Just add empty body to them.

	private Connection conn = null;
	private static final int MAX_RECOMMENDED_BOUTIQUES = 10;

	public MySQLDBConnection() {
		this(DBUtil.URL);
	}

	public MySQLDBConnection(String url) {
		try {
			// Forcing the class representing the MySQL driver to load and
			// initialize.
			// The newInstance() call is a work around for some broken Java
			// implementations
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) { /* ignored */
			}
		}
	}

	@Override
	public JSONArray searchBoutiques(String userId, double lat, double lon, String term) {
		try {
			YelpAPI api = new YelpAPI();
			JSONObject response = new JSONObject(
					api.searchForBusinessesByLocation(lat, lon));
			JSONArray array = (JSONArray) response.get("businesses");

			List<JSONObject> list = new ArrayList<JSONObject>();
			Set<String> visited = getVisitedBoutiques(userId);

			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				Boutique boutique = new Boutique(object);
				String businessId = boutique.getBusinessId();
				String name = boutique.getName();
				String categories = boutique.getCategories();
				String city = boutique.getCity();
				String state = boutique.getState();
				String fullAddress = boutique.getFullAddress();
				double stars = boutique.getStars();
				double latitude = boutique.getLatitude();
				double longitude = boutique.getLongitude();
				String imageUrl = boutique.getImageUrl();
				String url = boutique.getUrl();
				JSONObject obj = boutique.toJSONObject();
				if (visited.contains(businessId)) {
					obj.put("is_visited", true);
				} else {
					obj.put("is_visited", false);
				}
				String sql = "INSERT IGNORE INTO boutiques VALUES (?,?,?,?,?,?,?,?,?,?,?)";
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, businessId);
				statement.setString(2, name);
				statement.setString(3, categories);
				statement.setString(4, city);
				statement.setString(5, state);
				statement.setDouble(6, stars);
				statement.setString(7, fullAddress);
				statement.setDouble(8, latitude);
				statement.setDouble(9, longitude);
				statement.setString(10, imageUrl);
				statement.setString(11, url);
				statement.execute();
				// Perform filtering if term is specified.
				if (term == null || term.isEmpty()) {
					list.add(obj);
				} else {
					if (categories.contains(term) || fullAddress.contains(term) || name.contains(term)) {
						list.add(obj);
					}
				}
			}
			return new JSONArray(list);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}


	@Override
	public void setVisitedBoutiques(String userId, List<String> businessIds) {
		String query = "INSERT INTO history (user_id, business_id) VALUES (?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String businessId : businessIds) {
				statement.setString(1,  userId);
				statement.setString(2, businessId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void unsetVisitedBoutiques(String userId, List<String> businessIds) {
		String query = "DELETE FROM history WHERE user_id = ? and business_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String businessId : businessIds) {
				statement.setString(1,  userId);
				statement.setString(2, businessId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public Set<String> getVisitedBoutiques(String userId) {
		Set<String> visitedBoutiques = new HashSet<String>();
		try {
			String sql = "SELECT business_id from history WHERE user_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String visitedBoutique = rs.getString("business_id");
				visitedBoutiques.add(visitedBoutique);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return visitedBoutiques;

	}

	@Override
	public JSONObject getBoutiquesById(String businessId, boolean isVisited) {
		try {
			String sql = "SELECT * from boutiques where business_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, businessId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				Boutique boutique = new Boutique(
						rs.getString("business_id"), rs.getString("name"),
						rs.getString("categories"), rs.getString("city"),
						rs.getString("state"), rs.getFloat("stars"),
						rs.getString("full_address"), rs.getFloat("latitude"),
						rs.getFloat("longitude"), rs.getString("image_url"),
						rs.getString("url"));
				JSONObject obj = boutique.toJSONObject();
				obj.put("is_visited", isVisited);
				return obj;
			}
		} catch (Exception e) { /* report an error */
			System.out.println(e.getMessage());
		}
		return null;

	}

	@Override
	public JSONArray recommendBoutiques(String userId) {
		try {
			if (conn == null) {
				return null;
			}

			Set<String> visitedBoutiques = getVisitedBoutiques(userId);//step 1
			Set<String> allCategories = new HashSet<>();// why hashSet? //step 2
			for (String boutique : visitedBoutiques) {
				allCategories.addAll(getCategories(boutique));
			}
			Set<String> allBoutiques = new HashSet<>();//step 3
			for (String category : allCategories) {
				Set<String> set = getBusinessId(category);
				allBoutiques.addAll(set);
			}
			Set<JSONObject> diff = new HashSet<>();//step 4
			int count = 0;
			for (String businessId : allBoutiques) {
				// Perform filtering
				if (!visitedBoutiques.contains(businessId)) {
					diff.add(getBoutiquesById(businessId, false));
					count++;
					if (count >= MAX_RECOMMENDED_BOUTIQUES) {
						break;
					}
				}
			}
			return new JSONArray(diff);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;

	}

	@Override
	public Set<String> getCategories(String businessId) {
		try {
			String sql = "SELECT categories from boutiques WHERE business_id = ? ";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, businessId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				Set<String> set = new HashSet<>();
				String[] categories = rs.getString("categories").split(",");
				for (String category : categories) {
					// ' Shoes ' -> 'Shoes'
					set.add(category.trim());
				}
				return set;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return new HashSet<String>();

	}

	@Override
	public Set<String> getBusinessId(String category) {
		Set<String> set = new HashSet<>();
		try {
			// if category = Woman's, categories = Woman's, Men's, Shoes,
			// it's a match
			String sql = "SELECT business_id from boutiques WHERE categories LIKE ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, "%" + category + "%");
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String businessId = rs.getString("business_id");
				set.add(businessId);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return set;

	}

	@Override
	public Boolean verifyLogin(String userId, String password) {
		try {
			if (conn == null) {
				return false;
			}

			String sql = "SELECT user_id from users WHERE user_id = ? and password = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				return true;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return false;

	}
	
	@Override
	public Boolean registration(String userId, String password, String firstName, String lastName) {
		try {
			if (conn == null) {
				return false;
			}

			String sql = "INSERT INTO users (user_id, password, first_name, last_name) VALUES (?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			statement.setString(3, firstName);
			statement.setString(4, lastName);
			int i = statement.executeUpdate();
			if (i > 0) {
				return true;
			}			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	@Override
	public String getFirstLastName(String userId) {
		String name = "";
		try {
			if (conn != null) {
				String sql = "SELECT first_name, last_name from users WHERE user_id = ?";
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, userId);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					name += rs.getString("first_name") + " "
							+ rs.getString("last_name");
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return name;

	}
}
