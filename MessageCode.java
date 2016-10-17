
public enum MessageCode 
{ 
	SHUTDOWN_REGISTRATION_SERVER("END"),
	END_OF_TRANSMISSION("EOT"),
	STREAM_PACKET("SPK"),
	REQUEST_REGISTRATION("R01"),
	ACCEPT_REGISTRATION("R02"),
	DENY_AND_SUGGEST("R03"),
	REMOVE_REGISTRATION("R04");
	
	// Attributes
	String code;
	
	// Operations
	public String code_string()
	{
		return code;
	}
	
	// Constructors
	MessageCode(String code)
	{
		this.code = code;
	}
}
