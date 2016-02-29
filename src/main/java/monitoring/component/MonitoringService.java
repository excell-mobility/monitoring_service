package monitoring.component;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import beans.CalendarAppointment;
import beans.GeoPoint;
//import extraction.AppointmentExtraction;
//import extraction.GeocodingExtraction;
import monitoring.model.Report;
import rest.RoutingConnector;
import rest.SimulatorConnector;
import rest.TrackingConnector;
import utility.MeasureConverter;

@Component
public class MonitoringService {
	
	private final Logger log;
	public ConcurrentHashMap<String,Report> reportMap;
	
	// variables for simulation
	private List<String> extractCalendarUsers;
	private HashMap<String,List<CalendarAppointment>> simAppointments;
	
	public MonitoringService() {
		log = LoggerFactory.getLogger(this.getClass());
		reportMap = new ConcurrentHashMap<String,Report>();
		
		initSim();
	}
	
	private void initSim() {
		extractCalendarUsers = new ArrayList<String>();
		extractCalendarUsers.add("Track1");
		extractCalendarUsers.add("Track2");
		extractCalendarUsers.add("Track3");
		
		DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
		Date startDate = null;
		
		try {
			startDate = format.parse("January 1, 2010");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		Calendar simCalendar = Calendar.getInstance();
		simCalendar.setTime(startDate);
		
		simAppointments = new HashMap<String,List<CalendarAppointment>>();
		
		simAppointments.put("Track1", Lists.newArrayList(
				new CalendarAppointment(new GeoPoint(51.029943, 13.718802),addTime(simCalendar,0,5),addTime(simCalendar,0,10),"Track1"),
				new CalendarAppointment(new GeoPoint(51.029230, 13.707544),addTime(simCalendar,5,45),addTime(simCalendar,0,15),"Track1"),
				new CalendarAppointment(new GeoPoint(51.042258, 13.722375),addTime(simCalendar,10,35),addTime(simCalendar,0,20),"Track1"),
				new CalendarAppointment(new GeoPoint(51.071538, 13.730782),addTime(simCalendar,12,50),addTime(simCalendar,30,0),"Track1")));
		
		simCalendar.setTime(startDate);
		
		simAppointments.put("Track2", Lists.newArrayList(
				new CalendarAppointment(new GeoPoint(51.044603, 13.802338),addTime(simCalendar,0,5),addTime(simCalendar,0,10),"Track2"),
				new CalendarAppointment(new GeoPoint(51.044262, 13.690923),addTime(simCalendar,24,45),addTime(simCalendar,35,0),"Track2")));
		
		simCalendar.setTime(startDate);
		
		simAppointments.put("Track3", Lists.newArrayList(
				new CalendarAppointment(new GeoPoint(51.090607, 13.715340),addTime(simCalendar,0,5),addTime(simCalendar,0,10),"Track3"),
				new CalendarAppointment(new GeoPoint(51.051607, 13.797407),addTime(simCalendar,29,45),addTime(simCalendar,60,0),"Track3")));
		
	}
	
	@Scheduled(fixedRate = 2000)
	public void update() {
		
		try {
//			AppointmentExtraction extraction = new AppointmentExtraction();
//			GeocodingExtraction geoExtraction = new GeocodingExtraction();
			
			// get current time
			//Date currentDate = new Date();
			//DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
			//Date currentDate = format.parse("February 24, 2016");//new Date();
			Date currentDate = SimulatorConnector.getCurrentSimTime();
			
			// get simulated tracking positions
			HashMap<String,GeoPoint> positions = TrackingConnector.getCurrentPositions();
			
			/*
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			LocalTime midnight = LocalTime.MIDNIGHT;
			LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
			ZonedDateTime todayStart = ZonedDateTime.of(today, midnight, ZoneId.of("Europe/Berlin"));
			String todayMidnight = todayStart.format(formatter);
			String tomorrowMidnight = todayStart.plusDays(1).format(formatter);
			
			// construct time filter
			StringBuilder timeFilter = new StringBuilder("")
					.append("{\"begin\": \"").append(todayMidnight).append("\",")
					.append("\"end\": \"").append(tomorrowMidnight).append("\"}");
			*/
			// loop over users
			//JSONArray calendarUsers = CalendarConnector.getCalendarUsers();
			//List<String> extractCalendarUsers = extraction.extractCalendarUsers(calendarUsers);
			
			
			for(String userCalendar: extractCalendarUsers) {
				
				Report report = new Report();
				
				// get current position of monitored user from Tracking Service
				//GeoPoint currentPosition = TrackingConnector.getCurrentPosition(userCalendar);
				GeoPoint currentPosition = positions.get(userCalendar);
				
				CalendarAppointment appointment = null;
				GeoPoint calendarPosition = null;
				boolean duringAppointment = false;
				boolean lastAppointment = false;
				boolean doTimeCheck = true;
				
				// get list of appointments of monitored user from Calendar Service
				/*JSONArray appointmentsForCalendar = CalendarConnector.getAppointmentsForCalendar(
						userCalendar, timeFilter.toString());
				
				List<CalendarAppointment> appointments = extraction.extractAppointments(appointmentsForCalendar);*/
				List<CalendarAppointment> appointments = simAppointments.get(userCalendar);
				int appointmentCount = appointments.size();
				
				// TODO get entire route for the day
				// List<Double> routeTotal = RouteConnector.getRoute(positions[]);
				//report.setRouteTotal(routeTotal);
				
				// find matching appointment for current time
				for(int index = 0; index < appointmentCount; index++) {
					
					appointment = appointments.get(index);
					
					// if appointment lies already completely in the past, continue with next one
					if (appointment.getEndDate().before(currentDate))
						continue;
					
					// check if currentDate is before firstAppointment
					if (appointment.getStartDate().after(currentDate)) {
						calendarPosition = appointment.getPosition();
						break;
					}
					else {
						calendarPosition = appointment.getPosition();
						
						// currentDate is during firstAppointment
						if (appointment.getStartDate().before(currentDate) &&
								appointment.getEndDate().after(currentDate))
							duringAppointment = true;
							
						// check for next appointment
						if (index < appointmentCount-1) {
							// get next appointment to compare
							CalendarAppointment secondAppointment = appointments.get(index + 1);
					
							// currentData is already after secondAppointment, so continue
							if (secondAppointment.getEndDate().before(currentDate))
								continue;
						
							// use secondAppointment as appointment in the following
							// in order to get variables such as route, estimatedTimeOfArrival, delay
							if (duringAppointment) {
								appointment = secondAppointment;
								break;
							}
							else
								continue;
						}
						else {
							// no more appointments left
							lastAppointment = true;
						}
					}
				}
					
				// check if calendar position is set, otherwise monitoring can not be applied
				if (calendarPosition != null) {
					double posDistance;
					
					// get distance to last known position
					if (reportMap.containsKey(userCalendar)) {
						posDistance = getDistance(reportMap.get(userCalendar).getPosition(), currentPosition);
					}
					else
						posDistance = 0.0;
					
					// SET POSITION AND WORK STATUS
					
					// check if position has changed
					if (posDistance < 100) {
						// if not, check if an appointment is taking place at the moment
						if (duringAppointment) {
							// if yes, check if currentPosition is also near to location of appointment
							posDistance = getDistance(currentPosition, calendarPosition);
													
							if (posDistance < 500) {
								// sensor is not moving and near appointment
								report.setPosition(calendarPosition);
								report.setStatus(Report.WorkStatus.AT_APPOINTMENT);
								
								// if it's the last appointment stop time check
								if (lastAppointment)
									doTimeCheck = false;
							}
							else {
								//sensor is not moving but also > 500 away from appointment
								report.setPosition(currentPosition);
								report.setStatus(Report.WorkStatus.UNKNOWN);
							}
						}
						else {
							// position has not changed >100m but no appointment at the moment
							// consider this as ON_THE_MOVE
							report.setPosition(currentPosition);
							report.setStatus(Report.WorkStatus.ON_THE_MOVE);
						}
					}
					else {
						// sensor position has changed >100m; consider this as ON_THE_MOVE
						report.setPosition(currentPosition);
						report.setStatus(Report.WorkStatus.ON_THE_MOVE);
					}
					
					// SET ROUTE AND TIME STATUS
					
					if (doTimeCheck) {
						// get route to next appointment
						List<Double[]> routeNext = RoutingConnector.getGPSCoordinates(currentPosition, appointment.getPosition());
						report.setRouteNext(routeNext);
						
						// get travel time to next appointment from current time
						int minutesToNextAppointment = MeasureConverter.getTimeInMinutes(RoutingConnector.
								getTravelTime(currentPosition, appointment.getPosition()));
						
						// compare estimated time of arrival with startTime of next appointment and set delay
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(currentDate);
						calendar.add(Calendar.MINUTE, minutesToNextAppointment);
						Date expectedTimeOfArrival = calendar.getTime();
						
						if (expectedTimeOfArrival.before(appointment.getStartDate())) {
							report.setTimeStatus(Report.TimeStatus.IN_TIME);
							report.setExpectedTimeOfArrival(appointment.getStartDate());
							report.setDelayInMin(0);
						}
						else {
							report.setTimeStatus(Report.TimeStatus.DELAYED);
							report.setExpectedTimeOfArrival(expectedTimeOfArrival);
							Long delay = TimeUnit.MILLISECONDS.toMinutes(
									expectedTimeOfArrival.getTime() - appointment.getStartDate().getTime());
							// always add 1 minute (rounding up the seconds)
							report.setDelayInMin(delay.intValue() + 1);
						}
					}
					else {
						if (reportMap.containsKey(userCalendar)) {
							Report lastReport = reportMap.get(userCalendar);
							report.setRouteNext(lastReport.getRouteNext());
							report.setTimeStatus(lastReport.getTimeStatus());
							report.setExpectedTimeOfArrival(lastReport.getExpectedTimeOfArrival());
							report.setDelayInMin(lastReport.getDelayInMin());
						}
					}
					
					// update reportMap
					reportMap.put(userCalendar, report);
				}
				else {
					log.error("Could not resolve location from calendar! Monitoring will be stopped for ID: " + userCalendar);
					if (reportMap.containsKey(userCalendar))
						reportMap.remove(userCalendar);
				}
			}
			/*
			// map for all appointments of all calendars
			
			Map<String, List<CalendarAppointment>> appointmentsPerCalendar = Maps.newHashMap();
			
			// this set is necessary to avoid duplicate reminders
			//Set<CalendarAppointment> reminderSetForSMS = Sets.newHashSet();
			
			// first: get calendars and observe the appointments
			JSONArray calendarUsers = CalendarConnector.getCalendarUsers();
			List<String> extractCalendarUsers = extraction.extractCalendarUsers(calendarUsers);
			for(String userCalendar: extractCalendarUsers) {
				JSONArray appointmentsForCalendar = CalendarConnector.getAppointmentsForCalendar(userCalendar);
				List<CalendarAppointment> extractAppointments = extraction.extractAppointments(appointmentsForCalendar);
				// sort the appointments for delay test
				Collections.sort(extractAppointments);
				appointmentsPerCalendar.put(userCalendar, extractAppointments);
			}
			
			// second: check, if end date and start date is between current date
			for(String userCalendar: extractCalendarUsers) {
				
				List<CalendarAppointment> appointments = appointmentsPerCalendar.get(userCalendar);
				for(int index = 0; index <= appointments.size() - 2; index++) {
					
					CalendarAppointment firstAppointment = appointments.get(index);
					CalendarAppointment secondAppointment = appointments.get(index + 1);
					
					// precondition, appointments are sorted correctly and there is time between the appointments
					if(firstAppointment.getEndDate().before(currentDate) 
							&& secondAppointment.getStartDate().after(currentDate)) {
						
						// extract the minutes to the next appointment
						int minutesBetweenAppointments = MeasureConverter.
								getTimeInMinutes((int) (secondAppointment.getStartDate().getTime() - currentDate.getTime()));
						
						// TODO extract the current location of the staff member
						GeoPoint currentStaffPosition = new GeoPoint(51.05, 13.7333);
						
						// check the route to the next appointment
						int minutesToNextAppointment = MeasureConverter.getTimeInMinutes(RoutingConnector.
								getTravelTime(currentStaffPosition, secondAppointment.getPosition()));
						
						// check, if the staff can not reach the customer on time
						if(minutesToNextAppointment > minutesBetweenAppointments 
								&& !reminderSetForSMS.contains(secondAppointment)) {
							// TODO remind the customer, when staff is too late
							BroadcastingConnector.sendMessageToCustomer("sms", "12345", "Wir kommen später");
							// save the appointment in reminder set
							reminderSetForSMS.add(secondAppointment);
						}
						

					}
					
				}
				
			}*/
			
		} catch(Exception e) {
			log.error("API problems, let's try again");
			log.error(e.toString());
		}
	}
	
	private double getDistance(GeoPoint pointA, GeoPoint pointB) {
		double lonDelta = pointA.getLongitude() - pointB.getLongitude();
		double posDistance = (
				Math.acos(
				Math.sin(pointA.getLatitude() * Math.PI / 180.0) * 
				Math.sin(pointB.getLatitude() * Math.PI / 180.0) + 
				Math.cos(pointA.getLatitude() * Math.PI / 180.0) * 
				Math.cos(pointB.getLatitude() * Math.PI / 180.0) * 
				Math.cos(lonDelta * Math.PI / 180.0)) * 180 / Math.PI) * 
				60 * 1.1515 * 1609.344;
		
		return posDistance;
	}
	
	private Date addTime(Calendar cal, int minutes, int seconds) {
		cal.add(Calendar.MINUTE,minutes);
		cal.add(Calendar.SECOND,seconds);
		return cal.getTime();
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getReport(String calendarId) {
		JSONObject obj = new JSONObject();
		
		if (reportMap.containsKey(calendarId)) {
			Report report = reportMap.get(calendarId);
		
			obj.put("position", report.getPosition());
			obj.put("routeTotal", report.getRouteTotal());
			obj.put("routeNext", report.getRouteNext());
			obj.put("status", report.getStatus());
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
