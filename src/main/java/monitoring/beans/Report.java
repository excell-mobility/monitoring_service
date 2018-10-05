package monitoring.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import monitoring.beans.GeoPoint;
import io.swagger.annotations.ApiModelProperty;

public class Report implements Serializable {

	private static final long serialVersionUID = 1L;
	
    @ApiModelProperty(notes = "The current position of the monitored device", required = true)
	private GeoPoint position;
    @Deprecated
    @ApiModelProperty(notes = "The planned route for the entire day. Worked only in conjuction with calendar service (deprecated)", dataType = "[Ljava.lang.Double;", required = false)
	private List<Double[]> routeTotal;
    @ApiModelProperty(notes = "Route to given location (appointment)", dataType = "[Ljava.lang.Double;", required = true)
	private List<Double[]> routeNext;
    @ApiModelProperty(notes = "Consists of two values: A text classifier and a date (more details below)", required = true)
	private WorkingStatus workingStatus;
    @ApiModelProperty(notes = "If delay is bigger than 5min, monitored device is classified as delayed", required = true)
	private TimeStatus timeStatus;
    @ApiModelProperty(notes = "Timestamp for expected arrival at destination", required = true)
	private Date expectedTimeOfArrival;
    @ApiModelProperty(notes = "Calculated delay in minutes + delay by user input", required = true)
	private int delayInMin;
    @Deprecated
    @ApiModelProperty(notes = "Similar to 'since' attribute in working status (deprecated)", required = false)
	private int minAtAppointment;
    
	public Report () {
		
	}
	
	public Report(
			GeoPoint position,
			List<Double[]> routeTotal,
			List<Double[]> routeNext,
			WorkingStatus workingStatus,
			TimeStatus timeStatus,
			Date expectedTimeOfArrival,
			int delayInMin,
			int minAtAppointment) {
		this.position = position;
		this.routeTotal = routeTotal;
		this.routeNext = routeNext;
		this.workingStatus = workingStatus;
		this.timeStatus = timeStatus;
		this.expectedTimeOfArrival = expectedTimeOfArrival;
		this.delayInMin = delayInMin;
		this.setMinAtAppointment(minAtAppointment);
	}
	
	public enum TimeStatus {
		IN_TIME,
		DELAYED,
		UNKNOWN
	}
	
	public GeoPoint getPosition() {
		return position;
	}

	public void setPosition(GeoPoint position) {
		this.position = position;
	}

	public List<Double[]> getRouteTotal() {
		return routeTotal;
	}

	public void setRouteTotal(List<Double[]> routeTotal) {
		this.routeTotal = routeTotal;
	}

	public List<Double[]> getRouteNext() {
		return routeNext;
	}

	public void setRouteNext(List<Double[]> routeNext) {
		this.routeNext = routeNext;
	}

	public WorkingStatus getWorkingStatus() {
		return workingStatus;
	}

	public void setWorkingStatus(WorkingStatus workingStatus) {
		this.workingStatus = workingStatus;
	}

	public TimeStatus getTimeStatus() {
		return timeStatus;
	}

	public void setTimeStatus(TimeStatus timeStatus) {
		this.timeStatus = timeStatus;
	}

	public Date getExpectedTimeOfArrival() {
		return expectedTimeOfArrival;
	}

	public void setExpectedTimeOfArrival(Date expectedTimeOfArrival) {
		this.expectedTimeOfArrival = expectedTimeOfArrival;
	}
	
	public int getDelayInMin() {
		return delayInMin;
	}

	public void setDelayInMin(int delayInMin) {
		this.delayInMin = delayInMin;
	}
	
	public int getMinAtAppointment() {
		return minAtAppointment;
	}

	public void setMinAtAppointment(int minAtAppointment) {
		this.minAtAppointment = minAtAppointment;
	}

	@Override
	public String toString() {
		return "Report [position=" + position + ", routeTotal=" + routeTotal
				+ ", routeNext=" + routeNext + ", workingStatus=" + workingStatus.toString()
				+ ", timeStatus=" + timeStatus + ", expectedTimeOfArrival="
				+ expectedTimeOfArrival + ", delayInMin=" + delayInMin + "]";
	}
	
}
