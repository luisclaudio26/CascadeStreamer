// TODO: Refactor this!
public enum Parameters 
{
	//Buffer size for streaming must be defined on protocol!
	STREAMING_BUFFER_SIZE(1024),
	
	STREAMING_PORT(5001),
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
