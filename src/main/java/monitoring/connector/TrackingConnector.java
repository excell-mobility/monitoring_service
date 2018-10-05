package monitoring.connector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import monitoring.beans.GeoPoint;
import monitoring.connector.HttpConnector;
import monitoring.connector.HttpConnectorInterface.RestRequestType;

@Component
public class TrackingConnector {

	private HttpConnector connector;

	@Value("${url.trackingservice}")
	private String urlRoutingService;

	@Value("${url.routingservice.auth}")
	private boolean requiresToken;

	@Value("${url.authlayer}")
	private String urlAuthLayer;

	@Value("${url.authlayer.user}")
	private String urlAuthLayerUser;

	@Value("${url.authlayer.pw}")
	private String urlAuthLayerUserPw;
	public TrackingConnector() {
		this.connector = new HttpConnector();
	}
	
	public synchronized String setCurrentPosition(String trackingId, GeoPoint pos) throws Exception {
		
		String 	urlStr = doRequest(createUrlString(trackingId, "put"));
		ZonedDateTime trackingTime = new Date().toInstant().atZone(ZoneId.of("Europe/Berlin"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		
		// set up request body and escape parameters for json
		String urlParameters = "[{"
				+ "\"accuracy\": 0,"
				+ "\"altitude\": 0,"
				+ "\"bearing\": 0,"
				+ "\"date\": \"" + trackingTime.format(formatter) + "\","
				+ "\"latitude\": 52.5537197,"
				+ "\"locationProvider\": \"ExCELL\","
				+ "\"longitude\": 13.3595911,"
				+ "\"speed\": 0,"
				+ "\"tmode\": \"office_chair\""
				+ "}]";
		
		return this.connector.getConnectionString(urlStr, urlParameters, RestRequestType.PUT);
	}
	
	public synchronized HashMap<String,GeoPoint> getCurrentPositions() throws Exception {
		
		HashMap<String,GeoPoint> positions = new HashMap<String,GeoPoint>();
		String urlStr = doRequest("url.trackingservice");
		String result = this.connector.getConnectionString(urlStr);
		
		if (result != null && !result.equals("")) {	
			JSONArray jsonResult = new JSONArray(result);
			
			for (int i = 0; i < jsonResult.length(); i++) {
				JSONObject obj = jsonResult.getJSONObject(i);
				String trackId = obj.getString("id");
				GeoPoint point = new GeoPoint(obj.getDouble("latitude"),obj.getDouble("longitude"));
				positions.put(trackId, point);
			}
		}
		
		return positions;
	}
	
	public synchronized GeoPoint getCurrentPosition(String trackingId) throws Exception {
		String urlStr = createUrlString(trackingId, "get");
		String result = this.connector.getConnectionString(urlStr);

		if(result == null) {
			return null;
		} else if(result.length() < 10) {
			return null;
		}
		
		JSONArray positionArray = new JSONArray(result);
		JSONObject positions = (JSONObject) positionArray.get(0);
		
		// check if tracking really returns a current timestamp
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMAN);
		Date trackingTime = format.parse(positions.getString("date"));
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.HOUR,-12);
		
		Date controlTime = cal.getTime();
		
		if (controlTime.after(trackingTime))
			return null;
		
		return new GeoPoint(positions.getDouble("latitude"), positions.getDouble("longitude"));
	}



	private String createUrlString(String trackingId, String mode) {

		String idParameter;
		if (mode.equals("get"))
			idParameter = "?idList=" + trackingId;
		else
			idParameter = "/beuth/" + trackingId;
		
		return doRequest("url.trackingservice") + mode + "PositionData" + idParameter;
	}

	private String doRequest(String urlStr){
		String result = null;
		try {
			if (requiresToken) {
				// get authentication token first
				String POST_PAYLOAD = "{" + "\"username\"" + ":" + "\"" + urlAuthLayerUser
						+ "\"" + "," + "\"password\"" + ":" + "\"" + urlAuthLayerUserPw + "\"" + "}";
				String token = "";

				try {
					String jsonResponse = connector.getConnectionAuthenticationString(urlAuthLayer,
							POST_PAYLOAD);
					JSONObject tokenJSON = new JSONObject(jsonResponse);
					if(tokenJSON.has("token")) {
						token = tokenJSON.getString("token");
					} else {
						return null;
					}
				} catch (KeyManagementException | NoSuchAlgorithmException| IOException e1) {
					e1.printStackTrace();
				}
				token = "Token " + token;

				try {
					result = connector.getConnectionStringWithToken(urlStr, token);
				} catch (KeyManagementException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			else {
				result = this.connector.getConnectionString(urlStr);
			}
		} catch (IOException e) {
			System.out.println("ERROR: Could not update graph with recent travel times: " + e.getClass().getSimpleName() + " - " +  e.getMessage());
		}
		
		return result;
	}

}
