package org.springframework.social.facebook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.social.core.AccountNotConnectedException;
import org.springframework.social.oauth.OAuthSigningClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * This is the central class for interacting with Facebook.
 * 
 * @author Craig Walls
 */
public class FacebookTemplate implements FacebookOperations {
	private RestOperations restOperations;

	/**
	 * Create a new instance of FacebookTemplate.
	 * 
	 * Because many Facebook operations require an OAuth access token,
	 * FacebookTemplate must be constructed with an {@link RestOperations} that
	 * is able to sign requests with OAuth authorization details. If it is given
	 * a {@link RestTemplate} or some other implementation of RestOperations
	 * that is not OAuth-enabled, then some operations may work. Those that
	 * require authentication, however, will result in a
	 * {@link AccountNotConnectedException} being thrown.
	 * 
	 * @param restOperations
	 *            An {@link RestOperations} that will perform the calls against
	 *            Facebook's REST APIs.
	 */
	public FacebookTemplate(RestOperations restOperations) {
		this.restOperations = restOperations;
	}

	/**
	 * Create a new instance of FacebookTemplate.
	 * 
	 * This constructor creates the FacebookTemplate using a given access token.
	 * 
	 * @param accessToken
	 *            An access token given by Facebook after a successful OAuth 2
	 *            authentication (or through Facebook's JS library).
	 */
	public FacebookTemplate(String accessToken) {
		this.restOperations = new RestTemplate(new OAuthSigningClientHttpRequestFactory(new FacebookRequestSigner(
				accessToken)));
	}

	public FacebookUserInfo getUserInfo() {
		return restOperations.getForObject(OBJECT_URL, FacebookUserInfo.class, "me");
    }

	public List<String> getFriendIds() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = restOperations.getForEntity(CONNECTION_URL, Map.class, CURRENT_USER, FRIENDS);

		@SuppressWarnings("unchecked")
		Map<String, List<Map<String, String>>> resultsMap = response.getBody();
		List<Map<String, String>> friends = resultsMap.get("data");
		
		List<String> friendIds = new ArrayList<String>();
		for (Map<String, String> friendData : friends) {
	        friendIds.add(friendData.get("id"));
        }
	    return friendIds;
    }
	
	public void postToWall(String message) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.set("message", message);
		publish(CURRENT_USER, FEED, map);
	}
	
	public void postToWall(String message, FacebookLink link) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.set("link", link.getLink());
		map.set("name", link.getName());
		map.set("caption", link.getCaption());
		map.set("description", link.getDescription());
		map.set("message", message);
		publish(CURRENT_USER, FEED, map);
	}
	
	public void publish(String object, String connection, MultiValueMap<String, String> data) {
		MultiValueMap<String, String> requestData = new LinkedMultiValueMap<String, String>(data);
		restOperations.postForLocation(CONNECTION_URL, requestData, object, connection);
	}
	
	public byte[] getProfilePicture() {
		return getProfilePicture(CURRENT_USER);
	}

	public byte[] getProfilePicture(String profileId) {
		ResponseEntity<byte[]> imageBytes = restOperations.getForEntity(PROFILE_LARGE_PICTURE_URL, byte[].class,
				profileId);
		return imageBytes.getBody();
	}
	
	// to support unit testing
	void setRestTemplate(RestTemplate restTemplate) {
		this.restOperations = restTemplate;
	}
	
	static final String PROFILE_LARGE_PICTURE_URL = "https://graph.facebook.com/{profile}/picture?type=large";
	static final String OBJECT_URL = "https://graph.facebook.com/{objectId}";
	static final String CONNECTION_URL = OBJECT_URL + "/{connection}";
	
	static final String FRIENDS = "friends";
	static final String FEED = "feed";
	static final String CURRENT_USER = "me";
}
