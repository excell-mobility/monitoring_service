package monitoring.component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import rest.CalendarConnector;
import rest.IDMConnector;
import rest.RoutingConnector;
import rest.TrackingConnector;
import utility.DateAnalyser;
import utility.DistanceCalculator;
import utility.MeasureConverter;
import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.Report;
import beans.WorkingStatus;

import com.google.common.collect.Lists;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import extraction.AppointmentExtraction;

@Component
public class MonitoringService {
	
	private final Logger log;
	private final CalendarConnector calendarConnector;
 	private final RoutingConnector routingConnector;
 	private final TrackingConnector trackingConnector;
 	private final IDMConnector idmConnector;
 	private ClientConfig clientConfig;
 	private ClientNetworkConfig networkConfig;
 	private List<String> addresses;
 	private HazelcastInstance client;
 	
	public IMap<String,Report> reportMap;
	
	public MonitoringService() {
 		this.log = LoggerFactory.getLogger(this.getClass());
 		this.calendarConnector = new CalendarConnector();
 		this.routingConnector = new RoutingConnector();
 		this.trackingConnector = new TrackingConnector();
 		this.idmConnector = new IDMConnector();
 		// hazelcast configuration
	    clientConfig = new ClientConfig();
	    networkConfig = new ClientNetworkConfig();
	    addresses = new LinkedList<String>();
//	    addresses.add("127.0.0.1:5701");
	    addresses.add("141.64.5.201:5701");
	    addresses.add("141.64.5.202:5701");
	    addresses.add("141.64.5.203:5701");
	    networkConfig.setAddresses(addresses);
	    clientConfig.setNetworkConfig(networkConfig);
	    client = HazelcastClient.newHazelcastClient( clientConfig );
	    reportMap = client.getMap( "reportMap" );
	    reportMap.clear();
	}
	
	@Scheduled(fixedRate = 15000)
	public void update() {
		
		// get current time
		Date currentDate = new Date();
		
		// get users
		List<String> calendarUsers = getCalendarUsers();
		
		// loop over all users
		for(String calendarUser: calendarUsers) {	
			
			// set up report to null to avoid duplicate entries
			Report report = null;
			
			// get appointments of user
			List<CalendarAppointment> calendarAppointments = getCalendarAppointments(calendarUser);
			
			// only start monitoring if we have appointments
			if (calendarAppointments.size() > 0) {
				// get current position
				GeoPoint currentPosition = getUserPosition(calendarUser);
				
				// only start monitoring if we have a position
				if(currentPosition != null)
					report = createReport(calendarUser, calendarAppointments, currentPosition, currentDate);
			}
		
			// update reportMap
			if (report != null)
				reportMap.put(calendarUser, report);
			else
				if (reportMap.containsKey(calendarUser)) {
					log.info("Monitoring will be stopped for ID: " + calendarUser);
					reportMap.delete(calendarUser);
				}
			
		}
	}
	
	private List<String> getCalendarUsers() {
		
		AppointmentExtraction extraction = new AppointmentExtraction();
		JSONArray calendarUsers = null;
		
		try {
			calendarUsers = calendarConnector.getCalendarUsers();
		}
		catch (JSONException jEx) {
			//
		}
		catch (IOException ioEx) {
			//
		}
		
		return extraction.extractCalendarUsers(calendarUsers);
	}

	
	private GeoPoint getUserPosition(String calendarUser) {

		String deviceId = null;
		GeoPoint currentPosition = null;
		
		// get ID of tracking device from IDM
		try {
			deviceId = idmConnector.extractDeviceIdOfUser(calendarUser);
		} catch (IOException ioEx) {
			//ioEx.printStackTrace();
		}
		currentPosition = getTrackingPosition(deviceId);

		if (currentPosition == null) {
			if (reportMap.containsKey(calendarUser))
				// take last know position
				currentPosition = ((Report) reportMap.get(calendarUser)).getPosition();
			else
				currentPosition = idmConnector.getGeoCoordByUserId(calendarUser);
		}
		
		return currentPosition;
		
	}
	
	
	private GeoPoint getTrackingPosition(String deviceId) {
		
		GeoPoint trackingPosition = null;
		
		// get current position of monitored user from Tracking Service
		try {
			trackingPosition = trackingConnector.getCurrentPosition(deviceId);
		}
		catch (Exception ex) {
			//
		}
		
		return trackingPosition;
	}
	
	
	private List<CalendarAppointment> getCalendarAppointments(String calendarUser) {
		
		List<CalendarAppointment> appointments = Lists.newArrayList();
		AppointmentExtraction extraction = new AppointmentExtraction();
		
		// get appointments for the day 0:00 - 24:00
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS+00:00");
		LocalTime midnight = LocalTime.MIDNIGHT;
		LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
		ZonedDateTime todayStart = ZonedDateTime.of(today, midnight, ZoneId.of("GMT"));
		String todayMidnight = todayStart.minusHours(2).format(formatter);
		String tomorrowMidnight = todayStart.plusDays(1).minusHours(2).format(formatter);
				
		// construct time filter
		StringBuilder timeFilter = new StringBuilder("")
				.append("{\"begin\": \"").append(todayMidnight).append("\",")
				.append("\"end\": \"").append(tomorrowMidnight).append("\",")
				.append("\"completed\": false}");
		
		try {
			// get list of appointments of monitored user from Calendar Service
			JSONArray appointmentsForCalendar = calendarConnector.getAppointmentsForCalendar(
					calendarUser, timeFilter.toString());
				
			appointments = extraction.extractAppointments(appointmentsForCalendar);
		}
		catch (Exception ex) {
			//
		}
		
		return appointments;
	}
	
	private Report createReport(String calendarUser, 
			List<CalendarAppointment> appointments,
			GeoPoint currentPosition,
			Date currentDate) {
		
		Report report = null;
		boolean atLastAppointment = false;
		
		// first appointment in the list is set as next appointment
		CalendarAppointment nextAppointment = appointments.get(0);
		GeoPoint calendarPosition = nextAppointment.getPosition();
		
		// check if calendar position is set, otherwise monitoring can not be applied
		if (calendarPosition != null) {
			
			// init report
			report = new Report();
			
			// set total route for the day
			if (appointments.size() > 1) {
				if (!reportMap.containsKey(calendarUser)) {
					report.setRouteTotal(getRouteTotal(appointments));
				}
				else
					report.setRouteTotal(((Report) reportMap.get(calendarUser)).getRouteTotal());
			}
			
			// variable for time status
			int appointmentDuration = 0;
			
			// get distance to last known position
			double posDistance;
			if (reportMap.containsKey(calendarUser)) {
				posDistance = DistanceCalculator.getDistance(( (Report) reportMap.get(calendarUser)).getPosition(), currentPosition);
			}
			else { 
				posDistance = 0.0;
			}
			
			// SET WORK STATUS
			
			report.setPosition(currentPosition);
			WorkingStatus workingStatus = new WorkingStatus();
			
			// set working status to ON_THE_MOVE by default
			workingStatus.setLocationStatus(WorkingStatus.LocationStatus.ON_THE_MOVE);
			
			// check if there is a previous state and since when
			WorkingStatus status = null;
			if (reportMap.containsKey(calendarUser))
				status = ((Report) reportMap.get(calendarUser)).getWorkingStatus();
			
			if (status != null)
				workingStatus.setSince(status.getSince());
			else
				workingStatus.setSince(new Date());
			
			// check if position has not changed greatly
			if (posDistance < 100) {
				
				// check if currentPosition is also near to location of appointment
				posDistance = DistanceCalculator.getDistance(currentPosition, calendarPosition);
				
				// is sensor near to the next appointment?
				if (posDistance < 500)
					workingStatus.setLocationStatus(WorkingStatus.LocationStatus.AT_APPOINTMENT);
			}
			
			// reset "since" if status has changed
			if (reportMap.containsKey(calendarUser)) {
				status = ((Report) reportMap.get(calendarUser)).getWorkingStatus();
				if (status.getLocationStatus() != workingStatus.getLocationStatus())
					workingStatus.setSince(new Date());
			}
			
			// if status is AT_APPOINTMENT, some extra settings are necessary
			if (workingStatus.getLocationStatus() == WorkingStatus.LocationStatus.AT_APPOINTMENT) {
				// reduce duration of appointment (total duration minus time already passed at appointment)
				appointmentDuration = 
						DateAnalyser.getDurationBetweenDates(
								nextAppointment.getStartDate(), nextAppointment.getEndDate()
						) -
						DateAnalyser.getDurationBetweenDates(
								workingStatus.getSince(), new Date()
						);
				
				// update nextAppointment to look at the following appointment (route, arrival, delay)
				if (appointments.size() == 1)
					atLastAppointment = true;
				else
					nextAppointment = appointments.get(1);
			}
			
			report.setWorkingStatus(workingStatus);
			
			
			// SET TIME STATUS
			
			if (!atLastAppointment) {
				// get route to next appointment
				List<Double[]> routeNext = getRouteNext(currentPosition, nextAppointment.getPosition());
				
				if (routeNext != null && !routeNext.isEmpty()) {
					report.setRouteNext(routeNext);
					
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(currentDate);
					
					// get travel time to next appointment from current position and add it up to time estimation
					calendar.add(Calendar.MINUTE, getRouteTravelTime(currentPosition, nextAppointment.getPosition()));
					
					// add duration of current appointment
					if (workingStatus.getLocationStatus() == WorkingStatus.LocationStatus.AT_APPOINTMENT &&
							appointmentDuration > 0)
						calendar.add(Calendar.MINUTE, appointmentDuration);
					
					// set estimated time of arrival
					report.setExpectedTimeOfArrival(calendar.getTime());
					
					// calculate delay
					calculateDelay(report, report.getExpectedTimeOfArrival(), nextAppointment.getStartDate());
				}
			}
			else
				// only compare real start date and planned start date of last appointment
				calculateDelay(report, workingStatus.getSince(), nextAppointment.getStartDate());
		}
		
		return report;
	}
	
	
	private List<Double[]> getRouteTotal(List<CalendarAppointment> appointments) {
		
		List<Double[]> routeTotal = Lists.newArrayList();
		GeoPoint[] appPositions = new GeoPoint[appointments.size()];
		boolean calcRouteTotal = true;
		
		for(int i = 0; i < appointments.size(); i++) {
			GeoPoint pos = appointments.get(i).getPosition();
			if (pos != null)
				appPositions[i] = pos;
			else {
				calcRouteTotal = false;
				break;
			}
		}
		
		if (calcRouteTotal)
			//try {
				routeTotal = routingConnector.getRoute(appPositions);
			/*}
			catch (RoutingNotFoundException rEx) {
				return null;
			}*/
		
		return routeTotal;
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
	
	private void calculateDelay(Report report, Date is, Date should) {
		if (is.before(should)) {
			report.setTimeStatus(Report.TimeStatus.IN_TIME);
			report.setDelayInMin(0);
		}
		else {
			Long delay = TimeUnit.MILLISECONDS.toMinutes(is.getTime() - should.getTime());
			
			// always add 1 minute (rounding up the seconds)
			// only set time status to DELAYED if delay is greater than 5 min
			if (delay.intValue() + 1 >= 5)
				report.setTimeStatus(Report.TimeStatus.DELAYED);
			else
				report.setTimeStatus(Report.TimeStatus.IN_TIME);
			
			report.setDelayInMin(delay.intValue() + 1);
		}
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getReport(String calendarId) {
		JSONObject obj = new JSONObject();
		
		String calId = "";
		try {
			calId = URLDecoder.decode(URLEncoder.encode(calendarId,"UTF-8"),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			//
		}
		
		if (reportMap.containsKey(calId)) {
			Report report = (Report) reportMap.get(calId);
			
			JSONObject workingStatus = new JSONObject();
			workingStatus.put("locationStatus", report.getWorkingStatus().getLocationStatus());
			workingStatus.put("since", report.getWorkingStatus().getSince());
			
			obj.put("position", report.getPosition());
			obj.put("routeTotal", report.getRouteTotal());
			obj.put("routeNext", report.getRouteNext());
			obj.put("workingStatus", workingStatus);
			obj.put("timeStatus", report.getTimeStatus());
			obj.put("expectedTimeOfArrival", report.getExpectedTimeOfArrival());
			obj.put("delayInMin", report.getDelayInMin());
		}
		else {
			obj.put("Error", "Calendar not found!");
		}
		
		return obj;
	}
	
}
