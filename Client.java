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

// TODO: Create a common enumeration with ports and IPs

public class Client {

	//------------------------------------
	//----------- Attributes -------------
	//------------------------------------
	private int registration_port;
	private InetAddress connected_server;
	
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
		
		// Open TCP connection and send "R01___.___.___.___"
		Socket connection = null;
		try {
			System.out.println("Client is trying to connect...");
			connection = new Socket(server_ip, this.registration_port);
			DataOutputStream output = new DataOutputStream(connection.getOutputStream());
			
			// Send request
			try {
				output.writeBytes(MessageCode.REQUEST_REGISTRATION.code_string() + "\n");
				System.out.println("Client requested registration.");
			} catch(IOException e) {
				System.err.println("Error while sending request to server.");
				System.err.println( e.getMessage() );
			}
			
			// wait reply
			BufferedReader input = new BufferedReader( 
					new InputStreamReader( connection.getInputStream()));
			String reply = input.readLine();
			
			System.out.println("Server replied with: " + reply);
			
			String reply_code = reply.substring(0, 3);
			
			if( reply_code.equals(MessageCode.ACCEPT_REGISTRATION.code_string()) )
			{
				System.out.println("Server accepted connection.");
				this.connected_server = connection.getInetAddress();
			}
			else if( reply_code.equals(MessageCode.DENY_AND_SUGGEST.code_string()))
			{
				System.out.println("Server denied connection. Trying server-recommended peers.");
				potential_peers = parse_peer_list( reply );
			}
			
			// Close socket
			try {
				System.out.println("Client is shutting down.");
				connection.close();
				output.close();
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
	public Client()
	{
		this.registration_port = Parameters.DEFAULT_PORT.toInt();
		this.connected_server = null;
	}
	
	//------------------------------------
	public static void main(String[] args) throws UnknownHostException 
	{
		Client C = new Client();
		C.require_register(Collections.singletonList(InetAddress.getLocalHost()));
		return;
	}

}
