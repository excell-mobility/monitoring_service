package monitoring.component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.common.collect.Lists;

import monitoring.connector.RoutingConnector;
import monitoring.connector.TrackingConnector;
import monitoring.utility.DistanceCalculator;
import monitoring.utility.MeasureConverter;
import monitoring.beans.GeoPoint;
import monitoring.beans.Report;
import monitoring.beans.WorkingStatus;
import monitoring.exceptions.InternalMonitoringErrorException;

@Component
public class MonitoringService {
	
	@Autowired
 	private RoutingConnector routingConnector;
	@Autowired
 	private TrackingConnector trackingConnector;
	
	public MonitoringService() {
 		this.routingConnector = new RoutingConnector();
 		this.trackingConnector = new TrackingConnector();
	}
	
	public JSONObject getReport(
			String deviceId,
			long timestamp, 
			double latitude,
			double longitude,
			int delay
			) throws InternalMonitoringErrorException {
		
		JSONObject obj = new JSONObject();
		Date currentDate = new Date();		
		Date appointmentTime = new Date(timestamp);
		GeoPoint appointmentLocation = new GeoPoint(latitude, longitude);
		
		// default position to enable a demo output for monitoring
		GeoPoint userPosition = new GeoPoint(51.04, 13.73);
		if (!deviceId.equals("demo"))
			userPosition = getTrackingPosition(deviceId);
		
		if (userPosition == null) {
			throw new InternalMonitoringErrorException("No current tracking position found. Unable to create report");
		}
		
		// calculate route between current location and next appointment
		List<Double[]> routeNext = getRouteNext(userPosition, appointmentLocation);
		
		// set up location status
		JSONObject workingStatus = new JSONObject();
		workingStatus.put("since", null);
		double posDistance = DistanceCalculator.getDistance(userPosition, appointmentLocation);
		if (posDistance < 100) {
			workingStatus.put("locationStatus", WorkingStatus.LocationStatus.AT_APPOINTMENT);
		} else {
			workingStatus.put("locationStatus", WorkingStatus.LocationStatus.ON_THE_MOVE);
		}
		
		// calculate time between current date and start date of appointment
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		
		// add known delay
		calendar.add(Calendar.MINUTE, delay);
		
		// get travel time to next appointment from current position and add it up to time estimation
		calendar.add(Calendar.MINUTE, getRouteTravelTime(userPosition, appointmentLocation));
		
		// set up delay and time status
		if (calendar.getTime().before(appointmentTime)) {
			obj.put("timeStatus", Report.TimeStatus.IN_TIME);
			obj.put("delayInMin", 0);
		}
		else {
			Long delayInMinutes = TimeUnit.MILLISECONDS.toMinutes(calendar.getTime().getTime() 
					- appointmentTime.getTime());
			
			// always add 1 minute (rounding up the seconds)
			// only set time status to DELAYED if delay is greater than 5 minutes
			if (delayInMinutes.intValue() + 1 >= 5) {
				obj.put("timeStatus", Report.TimeStatus.DELAYED);
			}
			else {
				obj.put("timeStatus", Report.TimeStatus.IN_TIME);
			}
			obj.put("delayInMin", delayInMinutes.intValue() + 1);
		}
		
		obj.put("position", userPosition);
		obj.put("routeTotal", null);
		obj.put("routeNext", routeNext);
		obj.put("workingStatus", workingStatus);
		obj.put("expectedTimeOfArrival", calendar.getTime());
		return obj;
		
	}
	
	private GeoPoint getTrackingPosition(String deviceId) throws InternalMonitoringErrorException {
		
		GeoPoint trackingPosition = null;
		
		// get current position of monitored user from Tracking Service
		try {
			trackingPosition = trackingConnector.getCurrentPosition(deviceId);
		}
		catch (Exception ex) {
			throw new InternalMonitoringErrorException("Error while getting tracking data for a device "
					+ "id from the tracking service");
		}
		
		return trackingPosition;
	}
	
	private List<Double[]> getRouteNext(GeoPoint currentPosition, GeoPoint nextAppointmentPosition) {
		
		List<Double[]> routeNext = Lists.newArrayList();
		
		//try {
			routeNext = routingConnector.getGPSCoordinates(currentPosition, nextAppointmentPosition);
		/*}
		catch (RoutingNotFoundException rEx) {
			return null;
		}*/	return routeNext;
	}
	
	
	private int getRouteTravelTime(GeoPoint currentPosition, GeoPoint nextAppointmentPosition) {
		
		int minutesToNextAppointment;
		
		try {
			// get travel time to next appointment from current time
			minutesToNextAppointment = MeasureConverter.getTimeInMinutes(routingConnector.
					getTravelTime(currentPosition, nextAppointmentPosition));
		}
		catch (Exception ex) {
			return 0;
		}
		
		return minutesToNextAppointment;
	}
	
}
