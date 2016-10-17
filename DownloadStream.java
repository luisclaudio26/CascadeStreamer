import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class DownloadStream extends Thread
{
	//-----------------------------
	//-------- Attributes ---------
	//-----------------------------
	private IStreamTarget target;
	private boolean running;
	private DatagramSocket socket;
	
	//------------------------------
	//-------- Constructor ---------
	//------------------------------
	/**
	 * Builds a simple DownloadStream object, which will 
	 * read from UDP socket and write to target
	 * @param tgt Object to which we'll send data
	 */
	public DownloadStream(IStreamTarget tgt)
	{
		this.target = tgt;
		this.running = true;
		
		try {
			this.socket = new DatagramSocket(Parameters.STREAMING_PORT.toInt());
		} 
		catch (SocketException e) 
		{
			System.err.println("Error while creating client streaming socket.");
			System.err.println( e.getMessage() );
		}
		
		this.start();
	}
	
	//--------------------------
	//-------- Methods ---------
	//--------------------------
	public void shutdown()
	{
		this.running = false;
		this.socket.close();
	}
	
	//------------------------------
	//-------- From thread ---------
	//------------------------------
	@Override
	public void run()
	{	
		//Keep waiting for incoming packets,
		//write to Target once we receive something.
		while(running)
		{			
			byte[] buffer = new byte[Parameters.STREAMING_BUFFER_SIZE.toInt()];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			
			//this blocks until something is received on socket
			try 
			{
				this.socket.receive(packet);
				
				//unpack data and write to client. TODO: ensure that offset is really 0
				byte[] data = packet.getData();
				String data_s = new String(data, 0, packet.getLength());
				
				//TODO: Here we should analyze the header and decide whether to push_data
				//or to send a eot() signal.
				this.target.push_data(data_s);
			} 
			catch (IOException e) 
			{
				//When we close the socket, an exception will be risen; is it safe to ignore
				//it, 'though, because RUNNING flag is set to false.
				if(!running) return;
				
				//if we reach this point, it is so because exception was thrown
				//while thread was supposed to be running normally; log the error.
				System.err.println("Error while waiting for incoming packet on UDP socket.");
				System.err.println( e.getMessage());
			}
		}
	}

}
