package monitoring.beans;

import java.io.Serializable;
import java.util.Date;

import io.swagger.annotations.ApiModelProperty;

public class WorkingStatus implements Serializable {
	
	private static final long serialVersionUID = 1L;
	@ApiModelProperty(notes="Current status of monitored device - either at destination or on the way", required = true)
	private LocationStatus locStat;
	@Deprecated
	@ApiModelProperty(notes="Date since when the status has not been changed (deprecated)", required = false)
	private Date since;
	
	public WorkingStatus() {
		this.locStat = LocationStatus.UNKNOWN;
		this.since = new Date();
	};
	
	public WorkingStatus(
			LocationStatus locStat,
			Date since
			) {
		this.locStat = locStat;
		this.since = since;		
	};
	
	public enum LocationStatus {
		AT_APPOINTMENT,
		ON_THE_MOVE,
		UNKNOWN
	}
	
	public LocationStatus getLocationStatus() {
		return locStat;
	}

	public void setLocationStatus(LocationStatus locStat) {
		this.locStat = locStat;
	}
	
	public Date getSince() {
		return since;
	}
	
	public void setSince(Date since) {
		this.since = since;
	}

	@Override
	public String toString() {
		return "WorkingStatus [locationStatus=" + locStat
				+ " since=" + since + "]";
	}

}
