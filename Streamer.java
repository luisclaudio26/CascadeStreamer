
public class Streamer extends Thread 
{
	//-----------------------------
	//-------- Attributes ---------
	//-----------------------------
	private IStreamTarget target;
	private int period;
	private boolean running;
	
	//------------------------------
	//-------- Constructor ---------
	//------------------------------
	/**
	 * Builds a simple Streamer object, which will send
	 * data to @tgt at each @period seconds
	 * @param period Time between packet data
	 * @param tgt Object to which we'll send data
	 */
	public Streamer(int period, IStreamTarget tgt)
	{
		this.target = tgt;
		this.period = period;
		this.running = true;
		this.start();
	}
	
	//--------------------------
	//-------- Methods ---------
	//--------------------------
	public void shutdown()
	{
		this.running = false;
	}
	
	//------------------------------
	//-------- From thread ---------
	//------------------------------
	@Override
	public void run()
	{
		int count = 0;
		
		//send only 40 packets
		while(running && count < 30)
		{
			target.push_data("Stream packet no. " + count++);
			
			try {
				Thread.sleep(this.period);
			} catch (InterruptedException e) {
				System.err.println("Streamer object failed to sleep.");
				System.err.println( e.getMessage());
			}
		}
		
		//notify that we ended the transmission
		target.eot();
	}
}
