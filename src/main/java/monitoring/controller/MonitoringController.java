package monitoring.controller;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import monitoring.component.MonitoringService;
import monitoring.model.Report;

@RestController
@Api(value="/v1/scheduling")
public class MonitoringController {	
	
	@Autowired
	private MonitoringService monitoringService;
	
	@RequestMapping(value = "/v1/monitoring", method = RequestMethod.GET)
    @ApiOperation(
    		value = "Get status report of monitored sensor", 
    		response=Report.class,
    		produces = "application/json")
    @ResponseBody
    public JSONObject scheduling(
    		@ApiParam(name="calendarId", value="Id of monitored sensor", defaultValue="") 
    		@RequestParam(value="calendarId", defaultValue="") String calendarId) {
        
		return monitoringService.getReport(calendarId);
    }
    
    @ExceptionHandler(value = Exception.class)
    public String inputParameterError() {
    	return "Your input parameters for the monitoring service are invalid!";
    }
    
}