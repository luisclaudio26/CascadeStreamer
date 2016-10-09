
public enum MessageCode 
{
	REQUEST_REGISTRATION("R01");
	
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
