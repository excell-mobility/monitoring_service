package monitoring.component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import beans.CalendarAppointment;
import beans.GeoPoint;
import extraction.AppointmentExtraction;
//import extraction.GeocodingExtraction;
import monitoring.model.Report;
import rest.CalendarConnector;
import rest.RoutingConnector;
import utility.MeasureConverter;

@Component
public class MonitoringService {
	
	public ConcurrentHashMap<String,Report> reportMap;
	
	public MonitoringService() {
		reportMap = new ConcurrentHashMap<String,Report>();
	}
	
	@Scheduled(fixedRate = 5000)
	public void update() {
		
		try {
			AppointmentExtraction extraction = new AppointmentExtraction();
			//GeocodingExtraction geoExtraction = new GeocodingExtraction();
			
			// get current time
			DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
			Date currentDate = format.parse("February 24, 2016");//new Date();
			
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
			
			// loop over users
			JSONArray calendarUsers = CalendarConnector.getCalendarUsers();
			List<String> extractCalendarUsers = extraction.extractCalendarUsers(calendarUsers);
			for(String userCalendar: extractCalendarUsers) {
				if (userCalendar.equals("Andre"))
					continue;
				
				Report report = new Report();					
				
				// get current position of monitored user from Tracking Service
				//JSONObject trackingPosition = TrackingConnector.getCurrentPosition(userCalendar.getCar().getTrackingId());
				//GeoPoint currentPosition = geoExtraction.extractCoordinates(trackingPosition);
				GeoPoint currentPosition = new GeoPoint(51.05, 13.7333);
				
				// get list of appointments of monitored user from Calendar Service
				JSONArray appointmentsForCalendar = CalendarConnector.getAppointmentsForCalendar(
						userCalendar, timeFilter.toString());
				
				List<CalendarAppointment> appointments = extraction.extractAppointments(appointmentsForCalendar);
				int appointmentCount = appointments.size();
				
				// TODO get entire route for the day
				// List<Double> routeTotal = RouteConnector.getRoute(positions[]);
				//report.setRouteTotal(routeTotal);
				
				// find matching appointment for current time
				for(int index = 0; index < appointmentCount; index++) {
					
					CalendarAppointment appointment = appointments.get(index);
					GeoPoint calendarPosition = null;
					boolean duringAppointment = false;
					
					// if appointment lies already completely in the past, continue with next one
					if (appointment.getEndDate().before(currentDate))
						continue;
					
					// check if currentDate is before firstAppointment
					if (appointment.getStartDate().after(currentDate)) {
						calendarPosition = appointment.getPosition();
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
							if (duringAppointment)
								appointment = secondAppointment;
							else
								continue;
						}
					}
						
					double posDistance;
					
					// get distance to last known position
					if (reportMap.contains(userCalendar))
						posDistance = getDistance(reportMap.get(userCalendar).getPosition(), currentPosition);
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
						// sensor positoin has changed >100m; consider this as ON_THE_MOVE
						report.setPosition(currentPosition);
						report.setStatus(Report.WorkStatus.ON_THE_MOVE);
					}
					
					// SET ROUTE AND TIME STATUS
					
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
						report.setDelayInMin((int) (expectedTimeOfArrival.getTime() - appointment.getStartDate().getTime()));
					}
												
					// update reportMap
					reportMap.put(userCalendar, report);
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
			System.out.println("API problems, let's try again");
			System.out.println(e);
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
				60 * 1.1515 * 0.001609344;
		
		return posDistance;
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
