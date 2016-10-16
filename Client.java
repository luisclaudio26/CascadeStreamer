import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

// TODO: Create a common enumeration with ports and IPs

public class Client implements IStreamTarget {

	//------------------------------------
	//----------- Attributes -------------
	//------------------------------------
	private InetAddress actual_server;
	private int registration_port;
	private InetAddress connected_server;
	private DownloadStream down_stream;
	
	//------------------------------------
	//------------ Methods ---------------
	//------------------------------------
	private List<InetAddress> parse_peer_list(String msg)
	{
		List<InetAddress> out = new ArrayList<InetAddress>();
		
		String[] token_list = msg.split(" ");
		
		int n_elem = Integer.parseInt(token_list[1]);
		for(int i = 0, j = 2; i < n_elem; i++, j++)
			try 
			{
				//Substring will just remove the '\' character in beginning
				out.add( InetAddress.getByName( token_list[j].substring(1) ));
			} 
			catch (UnknownHostException e) 
			{
				System.err.println("Error while trying to retrieve list of peers.");
				System.err.println( e.getMessage());
			}
		
		return out;
	}
	
	/**
	 * Tries to connect to the specified address, returning the peer list
	 * if it fails
	 * @param server_ip
	 * @return An empty list if connection was successful, list of potential peers otherwise
	 */
	private List<InetAddress> require_register(InetAddress server_ip)
	{
		List<InetAddress> potential_peers = new ArrayList<InetAddress>();
		Socket connection = null;
		
		try {
			System.out.println("Client is trying to connect...");
			
			// open socket and associated resources
			connection = new Socket(server_ip, this.registration_port);
			DataOutputStream output = new DataOutputStream(connection.getOutputStream());
			BufferedReader input = new BufferedReader( 
					new InputStreamReader( connection.getInputStream()));
			
			// 1) Send registration request
			try {
				output.writeBytes(MessageCode.REQUEST_REGISTRATION.code_string() + "\n");
				System.out.println("Client requested registration.");
			} catch(IOException e) {
				System.err.println("Error while sending request to server.");
				System.err.println( e.getMessage() );
			}
			
			// 2) Wait response
			String reply = input.readLine();
			System.out.println("Server replied with: " + reply);
			
			String reply_code = reply.substring(0, 3);
			if( reply_code.equals(MessageCode.ACCEPT_REGISTRATION.code_string()) )
			{
				// 3.1) connection accepted
				System.out.println("Server accepted connection.");
				this.connected_server = connection.getInetAddress();
			}
			else if( reply_code.equals(MessageCode.DENY_AND_SUGGEST.code_string()))
			{
				// 3.2) connection refused
				System.out.println("Server denied connection. Trying server-recommended peers.");
				potential_peers = parse_peer_list( reply );
			}
			
			// 4) close socket
			try {
				System.out.println("Client is shutting down.");
				
				connection.close();
				output.close();
				input.close();
			} catch(IOException e) {
				System.err.println("Error while closing registration socket in client.");
				System.err.println(e.getMessage());
			}
		} catch (IOException e) {
			System.err.println("Error while opening registration socket in client.");
			System.err.println(e.getMessage());
		}
		
		return potential_peers;
	}
	
	/**
	 * Explores in breadth all the possible peers to connect, starting by those
	 * in peer_list
	 * @param peer_list List of peers to explore. 
	 * It'll also recursively explore those returned by peer_list if it fails
	 */
	public void require_register(List<InetAddress> peer_list)
	{
		//This is actually a in-breadth exploration of
		//the possible peers, which avoids creating chains
		//of connections (which would be a waste of connection
		//and would increase latency times).
		Queue<InetAddress> q = new LinkedList<InetAddress>(peer_list);
		
		while(!q.isEmpty())
		{
			InetAddress cur = q.poll();
			
			LinkedList<InetAddress> ngbrs = 
					new LinkedList<InetAddress>( require_register(cur) );
			
			//if connection was successful, ngbrs will be empty;
			//if it is so, stop; otherwise, push ngbrs to the queue
			//so we'll try to connect to them after.
			if(ngbrs.isEmpty()) return;
			else 
				for(InetAddress n : ngbrs) q.offer(n);
		}
	}

	//--------------------------------------
	//----------- Constructors -------------
	//--------------------------------------
	public Client(InetAddress server)
	{
		this.registration_port = Parameters.DEFAULT_PORT.toInt();
		this.connected_server = null;
		this.actual_server = server;
		this.down_stream = null;
	}

	//----------------------------------------
	//---------- From IStreamTarget ----------
	//----------------------------------------
	@Override
	public void push_data(String data) 
	{
		System.out.println("Received stream data: " + data);
	}
	
	@Override
	public void eot()
	{
		//TODO: here we should shutdown
	}
	
	//---------------------------------------------
	//----------- External operations -------------
	//---------------------------------------------
	public void launch()
	{
		//register to some server
		this.require_register(Collections.singletonList(this.actual_server));
		
		//launch download thread
		this.down_stream = new DownloadStream(this);
	}
	
	public void shutdown()
	{
		this.down_stream.shutdown();
		
		//TODO: we must notify server that we disconnected via registration port.
		//This means we can't close TCP registration connection.
	}
	
	//------------------------------------
	public static void main(String[] args) throws UnknownHostException 
	{
		Client C = new Client( InetAddress.getLocalHost() );
		
		C.launch();	

		//TODO: Fix this. It's horrible.
		Scanner keystroke = new Scanner(System.in);
		keystroke.next();
		
		C.shutdown();
		
		return;
	}
}
