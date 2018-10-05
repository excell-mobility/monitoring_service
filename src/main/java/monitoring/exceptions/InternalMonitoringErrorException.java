package monitoring.exceptions;


public class InternalMonitoringErrorException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InternalMonitoringErrorException() {
        super();

    }

    public InternalMonitoringErrorException(String message) {
        super(message);
    }

}
