package monitoring;

import java.util.Date;
import java.util.List;

import org.json.JSONArray;

import rest.CalendarConnector;
import calendarextraction.AppointmentExtraction;

public class MonitoringService {

	public static void main(String[] args) {

		while(true) {
			
			try {
				AppointmentExtraction extraction = new AppointmentExtraction();
				Date currentDate = new Date();
				
				// 1: get calendars and observe the appointments
				JSONArray calendarUsers = CalendarConnector.getCalendarUsers();
				List<String> extractCalendarUsers = extraction.extractCalendarUsers(calendarUsers);
				for(String userCalendar: extractCalendarUsers) {
					
				}
				
				// 2: extract the current location of all staff members
				
				// 3: check the route to the next appointment
				
				// 4: remind the customer, when the staff arrives or if staff is too late
				
				// sleep for 10 seconds
				Thread.sleep(10000);
			} catch(Exception e) {
				System.out.println("API problems, let's try again");
				System.out.println(e);
			}
			
		}

	}

}
