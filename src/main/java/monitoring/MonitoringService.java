package monitoring;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import rest.CalendarConnector;
import beans.CalendarAppointment;
import calendarextraction.AppointmentExtraction;

import com.google.common.collect.Maps;

public class MonitoringService {

	public static void main(String[] args) {

		while(true) {
			
			try {
				
				AppointmentExtraction extraction = new AppointmentExtraction();
				Date currentDate = new Date();
				Map<String, List<CalendarAppointment>> appointmentsPerCalendar = Maps.newHashMap();
				
				// 1: get calendars and observe the appointments
				JSONArray calendarUsers = CalendarConnector.getCalendarUsers();
				List<String> extractCalendarUsers = extraction.extractCalendarUsers(calendarUsers);
				for(String userCalendar: extractCalendarUsers) {
					JSONArray appointmentsForCalendar = CalendarConnector.getAppointmentsForCalendar(userCalendar);
					List<CalendarAppointment> extractAppointments = extraction.extractAppointments(appointmentsForCalendar);
					appointmentsPerCalendar.put(userCalendar, extractAppointments);
				}
				
				// 2: extract the current location of all staff members
				
				// 3: check the route to the next appointment
				
				// 4: remind the customer, when the staff arrives or if staff is too late
				
				// sleep for 5 minutes
				Thread.sleep(300000);
			} catch(Exception e) {
				System.out.println("API problems, let's try again");
				System.out.println(e);
			}
			
		}

	}

}
