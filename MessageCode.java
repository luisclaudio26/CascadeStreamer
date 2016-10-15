
public enum MessageCode 
{ 
	REQUEST_REGISTRATION("R01"),
	ACCEPT_REGISTRATION("R02"),
	DENY_AND_SUGGEST("R03");
	
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
