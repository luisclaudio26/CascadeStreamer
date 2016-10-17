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
	/**
	 * This effectively unsets Running flag
	 * and closes socket, which should cause run()
	 * to end. It is safe to call this multiple times
	 * (which will happen often!), even if thread
	 * is already dead.
	 */
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
				System.out.print("Waiting packet...");
				this.socket.receive(packet);
				System.out.println("got it!");
				
				//unpack data and write to client. TODO: ensure that offset is really 0
				byte[] data = packet.getData();
				String data_s = new String(data, 0, packet.getLength());
				
				//get code (first three characters) of the packet
				String code = data_s.substring(0, 3);
				
				if(code.equals(MessageCode.STREAM_PACKET.code_string()))
					this.target.push_data(data_s);
				else if(code.equals(MessageCode.END_OF_TRANSMISSION.code_string()))
				{
					this.target.eot();
					
					//Well, after lots of deadlock problems and complicated solutions,
					//this is it: there's no reason for a DownloadStream thread to be
					//remain open if transmission has ended. So we just close it from
					//here.
					this.shutdown();
				}
				else
					System.err.println("Packet received is in wrong format.");
			} 
			catch (IOException e) 
			{
				//When we close the socket, an exception will be risen; is it safe to ignore
				//it, 'though, because RUNNING flag is set to false.
				if(!running)
				{
					System.out.println("DownloadStream is shutting down.");
					return;
				}
				
				//if we reach this point, it is so because exception was thrown
				//while thread was supposed to be running normally; log the error.
				System.err.println("Error while waiting for incoming packet on UDP socket.");
				System.err.println( e.getMessage());
			}
		}
	}
}
