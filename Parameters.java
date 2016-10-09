// TODO: Refactor this!
public enum Parameters 
{
	DEFAULT_PORT(5000);
	
	int port;
	
	Parameters(int port)
	{
		this.port = port;
	}
	
	public int toInt()
	{
		return port;
	}
}
