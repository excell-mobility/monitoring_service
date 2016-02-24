package monitoring.model;

import java.util.Date;
import java.util.List;

import beans.GeoPoint;

public class Report {

	private GeoPoint position;
	private List<Double[]> routeTotal;
	private List<Double[]> routeNext;
	private WorkStatus status;
	private TimeStatus timeStatus;
	private Date expectedTimeOfArrival;
	private int delayInMin;
	
	public Report () {
		
	}
	
	public Report(
			GeoPoint position,
			List<Double[]> routeTotal,
			List<Double[]> routeNext,
			WorkStatus status,
			TimeStatus timeStatus,
			Date expectedTimeOfArrival,
			int delayInMin) {
		this.position = position;
		this.routeTotal = routeTotal;
		this.routeNext = routeNext;
		this.status = status;
		this.timeStatus = timeStatus;
		this.expectedTimeOfArrival = expectedTimeOfArrival;
		this.delayInMin = delayInMin;
	}
	
	public enum WorkStatus {
		AT_APPOINTMENT,
		ON_THE_MOVE,
		UNKNOWN
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

	public WorkStatus getStatus() {
		return status;
	}

	public void setStatus(WorkStatus status) {
		this.status = status;
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
	
}
