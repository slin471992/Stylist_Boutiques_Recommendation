
package db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Boutique;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import yelp.YelpAPI;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

public class MongoDBConnection implements DBConnection {

	private static final int MAX_RECOMMENDED_BOUTIQUES = 10;

	private MongoClient mongoClient;
	private MongoDatabase db;

	public MongoDBConnection() {
		// Connects to local mongodb server.
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase(DBUtil.DB_NAME);

	}

	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Override
	public void setVisitedBoutiques(String userId, List<String> businessIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pushAll", new Document("visited", businessIds)));
	}

	@Override
	public void unsetVisitedBoutiques(String userId, List<String> businessIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pullAll", new Document("visited", businessIds)));

	}

	@Override
	public Set<String> getVisitedBoutiques(String userId) {
		Set<String> set = new HashSet<>();
		// db.users.find({user_id:1111})
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));

		if (iterable.first().containsKey("visited")) {
			List<String> list = (List<String>) iterable.first().get("visited");
			set.addAll(list);
		}
		return set;

	}

	@Override
	public JSONObject getBoutiquesById(String businessId, boolean isVisited) {
		FindIterable<Document> iterable = db.getCollection("boutiques").find(eq("business_id", businessId));
		try {
			JSONObject obj = new JSONObject(iterable.first().toJson());

			String cat = obj.getString("categories").replace("\"", "\\\"").replace("/", " or ");
			JSONArray categories = new JSONArray("[" + cat + "]");
			obj.put("categories", categories);
			obj.put("is_visited", isVisited);
			return obj;

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public JSONArray recommendBoutiques(String userId) {
		try {

			Set<String> visitedBoutiques = getVisitedBoutiques(userId);
			Set<String> allCategories = new HashSet<>();
			for (String boutique : visitedBoutiques) {
				allCategories.addAll(getCategories(boutique));
			}
			Set<String> allBoutiques = new HashSet<>();
			for (String category : allCategories) {
				Set<String> set = getBusinessId(category);
				allBoutiques.addAll(set);
			}
			Set<JSONObject> diff = new HashSet<>();
			int count = 0;
			for (String businessId : allBoutiques) {
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
	public JSONArray searchBoutiques(String userId, double lat, double lon, String term) {
		try {
			YelpAPI api = new YelpAPI();
			JSONObject response = new JSONObject(api.searchForBusinessesByLocation(lat, lon));
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
				// Question: why using upsert instead of insert directly?
				// Answer:
				// http://stackoverflow.com/questions/17319307/how-to-upsert-with-mongodb-java-driver
				UpdateOptions options = new UpdateOptions().upsert(true);

				db.getCollection("boutiques").updateOne(new Document().append("business_id", businessId),
						new Document("$set", new Document()
								.append("business_id", businessId)
								.append("name", name)
								.append("categories", categories)
								.append("city", city).append("state", state)
								.append("full_address", fullAddress)
								.append("stars", stars)
								.append("latitude", latitude)
								.append("longitude", longitude)
								.append("image_url", imageUrl)
								.append("url", url)),
						options);
				list.add(obj);
			}
			if (term == null || term.isEmpty()) {
				return new JSONArray(list);
			} else {
				// Use text search to perform better efficiency
				return filterBoutiques(term);
			}

//			return new JSONArray(list);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	private JSONArray filterBoutiques(String term) {
		try {
			Set<JSONObject> set = new HashSet<JSONObject>();
			FindIterable<Document> iterable = db.getCollection("boutiques").find(Filters.text(term));

			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					set.add(getBoutiquesById(document.getString("business_id"), false));
				}
			});
			return new JSONArray(set);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}


	@Override
	public Set<String> getCategories(String businessId) {
		Set<String> set = new HashSet<>();
		FindIterable<Document> iterable = db.getCollection("boutiques").find(eq("business_id", businessId));

		if (iterable.first().containsKey("categories")) {
			String[] categories = iterable.first().getString("categories").split(",");
			for (String category : categories) {
				set.add(category.trim());
			}
		}
		return set;
	}

	@Override
	public Set<String> getBusinessId(String category) {
		Set<String> set = new HashSet<>();
		// similar to LIKE %category% in MySQL
		FindIterable<Document> iterable = db.getCollection("boutiques").find(regex("categories", category));
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				set.add(document.getString("business_id"));
			}
		});
		return set;
	}

	@Override
	public Boolean verifyLogin(String userId, String password) {
		FindIterable<Document> iterable = db.getCollection("users").find(new Document("user_id", userId));
		Document document = iterable.first();
		return document.getString("password").equals(password);
	}

	@Override
	public Boolean registration(String userId, String password, String firstName, String lastName) {
		db.getCollection("users").insertOne(new Document()
						         .append("first_name", firstName)
						         .append("last_name", lastName)
						         .append("password", password)
						         .append("user_id", userId));
		return true;
	}
	
	@Override
	public String getFirstLastName(String userId) {
		FindIterable<Document> iterable = db.getCollection("users").find(new Document("user_id", userId));
		Document document = iterable.first();
		String firstName = document.getString("first_name");
		String lastName = document.getString("last_name");
		return firstName + " " + lastName;

	}
}
