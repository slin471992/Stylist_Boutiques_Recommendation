
package db;

import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public interface DBConnection {

    /**
     * Close the connection.
     */
    public void close() ;
    
    /**
     * Insert the visited boutiques for a user.
     * @param userId
     * @param businessIds
     */
    public void setVisitedBoutiques(String userId, List<String> businessIds);

    /**
     * Delete the visited boutiques for a user.
     * @param userId
     * @param businessIds
     */
    public void unsetVisitedBoutiques(String userId, List<String> businessIds);

    /**
     * Get the visited boutiques for a user.
     * @param userId
     * @return
     */
    public Set<String> getVisitedBoutiques(String userId);

    /**
     * Get the boutique json by id.
     * @param businessId
     * @param isVisited, set the visited field in json.
     * @return
     */
    public JSONObject getBoutiquesById(String businessId, boolean isVisited);

    /**
     * Recommend boutiques based on userId
     * @param userId
     * @return
     */
    public JSONArray recommendBoutiques(String userId);
    
    /**
     * Gets categories based on business id
     * @param businessId
     * @return
     */
    public Set<String> getCategories(String businessId);

    /**
     * Gets business id based on category
     * @param category
     * @return
     */
    public Set<String> getBusinessId(String category);
    
    /**
     * Search boutiques near a geolocation.
     * @param userId
     * @param lat
     * @param lon
     * @return
     */
    public JSONArray searchBoutiques(String userId, double lat, double lon, String term);

   /**
     * Verify if the userId matches the password.
     * @param userId
     * @param password
     * @return
     */
    public Boolean verifyLogin(String userId, String password);
    
    /**
     * Register new user
     * @param userId
     * @param password
     * @param firstName
     * @param lastName
     * @return
     */
    public Boolean registration(String userId, String password, String firstName, String lastName);

    /**
     * Get user's name for the userId.
     * @param userId
     * @return First and Last Name
     */
    public String getFirstLastName(String userId);

}
