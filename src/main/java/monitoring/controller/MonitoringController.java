package monitoring.controller;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import exceptions.InternalMonitoringErrorException;
import exceptions.RoutingNotFoundException;
import beans.Report;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import monitoring.component.MonitoringService;

@CrossOrigin(origins = "*")
@RestController
@Api(value="/v1/monitoring")
public class MonitoringController {	
	
	@Autowired
	private MonitoringService monitoringService;
	
	@RequestMapping(value = "/v1/monitoring", method = RequestMethod.GET)
    @ApiOperation(
    		value = "Get status report of monitored sensor", 
    		response=Report.class,
    		produces = "application/json")
    @ResponseBody
    public JSONObject monitoring(
    		@ApiParam(name="calendarId", value="Id of monitored sensor", defaultValue="") 
    		@RequestParam(value="calendarId", defaultValue="") String calendarId) throws InternalMonitoringErrorException {
        
		JSONObject response = monitoringService.getReport(calendarId);
		return response;
    }
	
	@RequestMapping(value = "/v1/monitoringnew", method = RequestMethod.GET)
    @ApiOperation(
    		value = "Get status report of monitored sensor", 
    		response=Report.class,
    		produces = "application/json")
    @ResponseBody
    public JSONObject monitoringnew(
    		@ApiParam(name="deviceId", value="Id of monitored device", defaultValue="") 
    		@RequestParam(value="deviceId", defaultValue="") String deviceId,
    		
    		@ApiParam(name="timestamp", value="Timestamp of the appointment", defaultValue="")
    		@RequestParam(value="timestamp", defaultValue="0") Long timestamp,
    		
    		@ApiParam(name="appointmentLat", value="Latitude of new appointment", defaultValue="51.029")
    		@RequestParam(value="appointmentLat", defaultValue="0.0") Double latitude,
    		
    		@ApiParam(name="appointmentLon", value="Longitude of new appointment", defaultValue="13.736") 
    		@RequestParam(value="appointmentLon", defaultValue="0.0") Double longitude,
    		
    		@ApiParam(name="delay", value="delay of the appointment", defaultValue="0")
    		@RequestParam(value="delay", defaultValue="0") Integer delay) throws InternalMonitoringErrorException {
        
		JSONObject response = monitoringService.getReport(deviceId, timestamp, latitude, longitude, delay);
		return response;
    }
    
    @ExceptionHandler(value = InternalMonitoringErrorException.class)
    public BodyBuilder monitoringError() {
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    
}