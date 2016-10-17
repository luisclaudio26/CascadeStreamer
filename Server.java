import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class Server implements IServerData, IStreamTarget {

	//------------------------------------
	//----------- Attributes -------------
	//------------------------------------
	private int registration_port;
	private int streaming_port;
	private DatagramSocket stream_out_socket;
	
	//TODO: Maybe this should become a class itself? YES, implemented as a monitor.
	private int max_peers; 
	private ArrayList<InetAddress> peers;
	private final ReentrantLock peers_locker = new ReentrantLock();
	
	private RegistrationDesk waiter;
	private Streamer streamer;
	
	//--------------------------------------
	//----------- Internal ops -------------
	//--------------------------------------
	private String pack_stream_data(String data)
	{
		int hash = data.hashCode();
		return MessageCode.STREAM_PACKET.code_string() + " "
				+ hash + " " + data;
	}
	
	//TODO: Ideally, we should have one thread per
	//connection and a common buffer. The streamer
	//would write to the common buffer, thread-safe,
	//and then each thread would make a copy of the
	//data and send it.
	private void send_data_to_peers(String data)
	{	
		String msg = pack_stream_data(data);
		
		for(InetAddress peer : peers)
		{
			DatagramPacket packet = new DatagramPacket(msg.getBytes(), 
														msg.getBytes().length,
														peer,
														this.streaming_port);
			
			System.out.println("Delivering data to " + peer.getHostAddress() + " in port " + this.streaming_port);
			
			try
			{
				stream_out_socket.send(packet);
			} 
			catch (IOException e) 
			{
				System.err.println("Error while streaming data to peers.");
				System.err.println( e.getMessage());
			}
		}
	}
	
	private void send_eot_to_peers()
	{
		String data = MessageCode.END_OF_TRANSMISSION.code_string();
		for(InetAddress peer : peers)
		{
			DatagramPacket packet = new DatagramPacket(data.getBytes(), 
														data.getBytes().length,
														peer,
														this.streaming_port);
			
			System.out.println("Delivering EOT to " + peer.getHostAddress() + " in port " + this.streaming_port);
			
			try
			{
				stream_out_socket.send(packet);
			} 
			catch (IOException e) 
			{
				System.err.println("Error while streaming EOT to peers.");
				System.err.println( e.getMessage());
			}
		}
	}
	
	//---------------------------------------------
	//------------ From IServerData ---------------
	//---------------------------------------------
	@Override
	public boolean push_new_peer(InetAddress peer_ip) 
	{
		peers_locker.lock();
		
		if( peers.size() < max_peers)
		{
			peers.add(peer_ip);
			peers_locker.unlock();
			
			return true;
		}
		else
		{
			peers_locker.unlock();
			return false;
		}
	}

	@Override
	public int peer_count() { return peers.size(); }

	@Override //TODO: Return unmodifiable iterator. Also: THIS IS NOT THREAD-SAFE! Think solution for this.
	public Iterator<InetAddress> peers() { return peers.iterator(); }

	@Override
	public void pop_peer(InetAddress peer_ip) { peers.remove(peer_ip); }
	
	//-----------------------------------------------
	//------------ From IStreamTarget ---------------
	//-----------------------------------------------
	@Override
	public void push_data(String data) 
	{
		this.send_data_to_peers(data);
	}
	
	@Override
	public void eot()
	{
		System.out.println("Transmission has ended.");
		this.send_eot_to_peers();
	}
	
	//------------------------------------------------
	//------------ External operations ---------------
	//------------------------------------------------
	public void launch() 
	{
		// Launch thread that waits for incoming registration requests
		this.waiter = new RegistrationDesk(this, this.registration_port);
		
		// Launch thread that streams data
		this.streamer = new Streamer(500, this);
		
		// Open socket for UDP streaming
		try 
		{
			this.stream_out_socket = new DatagramSocket();
		} 
		catch (SocketException e) 
		{
			System.err.println("Error while opening streaming socket.");
			System.err.println( e.getMessage());
		}
	}
	
	public void shutdown()
	{		
		try 
		{
			//Server can close only after RegistrationDesk 
			//and Streamer objects are done.
			this.waiter.shutdown();
			this.streamer.shutdown();
			
			this.waiter.join();
			this.streamer.join();
		} 
		catch (InterruptedException e) 
		{
			System.err.println("Either RegistrationDesk or Streamer thread was interrupted and was not correctly closed.");
			System.err.println(e.getMessage());
		}
	}
	
	//------------------------------------
	//---------- Constructors ------------
	//------------------------------------
	public Server()
	{
		this.registration_port = Parameters.DEFAULT_PORT.toInt();
		this.streaming_port = Parameters.STREAMING_PORT.toInt();
		this.stream_out_socket = null;
		this.max_peers = 2;
		this.peers = new ArrayList<InetAddress>();
		this.waiter = null;
		this.streamer = null;
	}
	
	//------------------------------------
	public static void main(String[] args) throws InterruptedException 
	{
		Server s = new Server();
		s.launch();
		
		//TODO: Fix this. It's horrible.
		Scanner keystroke = new Scanner(System.in);
		keystroke.next();
		
		s.shutdown();
		
		return;
	}
}
