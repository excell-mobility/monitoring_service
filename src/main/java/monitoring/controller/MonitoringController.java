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
	
	/*
	@Deprecated
	@RequestMapping(value = "/v1/monitoring", method = RequestMethod.GET)
	@ApiOperation(
    		value = "Get status report by checking calendar service (deprecated)", 
    		response=Report.class,
    		produces = "application/json")
    @ResponseBody
    public JSONObject monitoring(
    		@ApiParam(name="calendarId", value="Id of calendar (see Calendar Service)", defaultValue="") 
    		@RequestParam(value="calendarId", defaultValue="") String calendarId
    		) throws InternalMonitoringErrorException {
        
		JSONObject response = monitoringService.getReport(calendarId);
		return response;
    }
	*/
	
	@RequestMapping(value = "/v1/monitoring", method = RequestMethod.GET)
    @ApiOperation(
    		value = "Get status report using current position of monitored device", 
    		response=Report.class,
    		produces = "application/json")
    @ResponseBody
    public JSONObject monitoring(
    		@ApiParam(name="deviceId", value="Id of monitored device (use 'demo' to see test result)", defaultValue="") 
    		@RequestParam(value="deviceId", defaultValue="") String deviceId,
    		
    		@ApiParam(name="timestamp", value="Unix timestamp of upcoming appointment", defaultValue="1483225200")
    		@RequestParam(value="timestamp", defaultValue="1483225200") Long timestamp,
    		
    		@ApiParam(name="appointmentLat", value="Latitude of upcoming appointment", defaultValue="51.029")
    		@RequestParam(value="appointmentLat", defaultValue="0.0") Double latitude,
    		
    		@ApiParam(name="appointmentLon", value="Longitude of upcoming appointment", defaultValue="13.736") 
    		@RequestParam(value="appointmentLon", defaultValue="0.0") Double longitude,
    		
    		@ApiParam(name="delay", value="Known current delay in minutes", defaultValue="5")
    		@RequestParam(value="delay", defaultValue="0") Integer delay
    		) throws InternalMonitoringErrorException {
        
		JSONObject response = monitoringService.getReport(deviceId, timestamp, latitude, longitude, delay);
		return response;
    }
    
    @ExceptionHandler(value = InternalMonitoringErrorException.class)
    public BodyBuilder monitoringError() {
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    
}