package monitoring;

import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Date;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import beans.Report;
import beans.WorkingStatus;
import exceptions.InputParameterErrorException;
import exceptions.InternalMonitoringErrorException;
import exceptions.RoutingNotFoundException;
import monitoring.component.MonitoringService;


public class TestMonitoringService {
	
	@Autowired
	private MonitoringService monitoringService;
	
	private JSONObject monitoringReport;
	private Long currentTime;
	
	@Before
	public void initialize() {
		monitoringService = new MonitoringService();
		currentTime = new Date().getTime();
	}
	
	@Test
	public void testMonitoringInTime() throws RoutingNotFoundException, InputParameterErrorException, InternalMonitoringErrorException, ParseException {
		
		monitoringReport = monitoringService.getReport("demo", currentTime+15*60*1000, 51.029, 13.736, 0);
		
		JSONObject workingStatus = (JSONObject) monitoringReport.get("workingStatus");
		assertTrue(workingStatus.get("locationStatus").equals(WorkingStatus.LocationStatus.ON_THE_MOVE));
		assertTrue(monitoringReport.get("timeStatus").equals(Report.TimeStatus.IN_TIME));
	}
	
	@Test
	public void testMonitoringDelayed() throws RoutingNotFoundException, InputParameterErrorException, InternalMonitoringErrorException {
		
		monitoringReport = monitoringService.getReport("demo", currentTime+10*60*1000, 51.029, 13.736, 20);
		
		JSONObject workingStatus = (JSONObject) monitoringReport.get("workingStatus");
		assertTrue(workingStatus.get("locationStatus").equals(WorkingStatus.LocationStatus.ON_THE_MOVE));
		assertTrue(monitoringReport.get("timeStatus").equals(Report.TimeStatus.DELAYED));
		
		int delay = (int) monitoringReport.get("delayInMin");
		assertTrue(delay >= 10);
		
	}
	
	@Test
	public void testMonitoringArrived() throws RoutingNotFoundException, InputParameterErrorException, InternalMonitoringErrorException {
		
		monitoringReport = monitoringService.getReport("demo", currentTime+15*60*1000, 51.0399, 13.73, 0);
		JSONObject workingStatus = (JSONObject) monitoringReport.get("workingStatus");
		assertTrue(workingStatus.get("locationStatus").equals(WorkingStatus.LocationStatus.AT_APPOINTMENT));
		assertTrue(monitoringReport.get("timeStatus").equals(Report.TimeStatus.IN_TIME));
		
	}
}
