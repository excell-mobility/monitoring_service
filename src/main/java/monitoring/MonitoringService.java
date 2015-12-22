package monitoring;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import rest.CalendarConnector;
import utility.MeasureConverter;
import beans.CalendarAppointment;

import com.google.common.collect.Maps;

import extraction.AppointmentExtraction;

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
					// sort the appointments for delay test
					Collections.sort(extractAppointments);
					appointmentsPerCalendar.put(userCalendar, extractAppointments);
				}
				
				// check, if end date and start date is between current date
				for(String userCalendar: extractCalendarUsers) {
					
					List<CalendarAppointment> appointments = appointmentsPerCalendar.get(userCalendar);
					for(int index = 0; index <= appointments.size() - 2; index++) {
						
						CalendarAppointment first = appointments.get(index);
						CalendarAppointment second = appointments.get(index + 1);
						if(first.getEndDate().before(currentDate) 
								&& second.getStartDate().after(currentDate)) {
							// extract the minutes to the next appointment
							int minutesBetweenAppointments = MeasureConverter.
									getTimeInMinutes((int) (second.getStartDate().getTime() - currentDate.getTime()));
							// TODO 2: extract the current location of the staff member
							
							// TODO 3: check the route to the next appointment

						}
						
					}
					
				}
				
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
