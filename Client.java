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
				System.out.println("Client is closing registration socket.");
				
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

	/**
	 * Here we request connected_server to remove our registration.
	 */
	public void remove_register()
	{
		//If we're not connected to anyone, then
		//no need to disconnect.
		if(this.connected_server == null) return;
		
		try 
		{
			//open connection to server using registration port,
			//write a Remove Registration code and close. We don't
			//need to expect any reply of any type.
			
			System.out.println("Client is registering itself out of connected server.");
			
			Socket connection = new Socket(this.connected_server, this.registration_port);
			DataOutputStream output = new DataOutputStream(connection.getOutputStream());
			
			output.writeBytes( MessageCode.REMOVE_REGISTRATION.code_string() + "\n" );
			
			this.connected_server = null;
			
			try {
				connection.close();
			} catch(IOException e) {
				System.err.println("Error while closing socket to remove registration.");
				System.err.println( e.getMessage());
			}
		} 
		catch (IOException e) 
		{
			System.err.println("Error while opening socket to remove registration.");
			System.err.println( e.getMessage() );
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
		System.out.println("Transmission has ended.");
		
		//we disconnect from this streaming server
		this.disconnect();
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
		
		//it is not always the case that we want to
		//shutdown completely after a streaming (we
		//could use the client to connect to another
		//server, for example), but in this simple
		//implementation we'll just do this.
		//Ideally, the main thread should keep listening
		//to events (incoming connections from peers,
		//keyboard input).
		try {
			this.down_stream.join();
		} catch (InterruptedException e) {
			System.err.println("Error while joining DownloadStream thread.");
			System.err.println( e.getMessage() );
		}
	}
	
	public void disconnect()
	{
		//notify server we're disconnecting
		this.remove_register();

		//Another thread must be spawned to close
		//DownloadStream. Calling join() here
		//will cause deadlock (as DownloadStream calls
		//eot(), eot() calls shutdown(). If we join()
		//in this point we're effectively telling
		//DownloadStream to wait for itself to finish).
		//But why don't just finish DownloadStream after.
		//
		//UPDATE: Finally, we'll won't spawn any new thread
		//to close down_stream (because it is closing itself
		//after a EOT). Be careful while using this! This allows
		//us 'though to close client from terminal while
		//streaming goes on (which was not possible in the
		//later solution).
		this.down_stream.shutdown();
	}
	
	//------------------------------------
	public static void main(String[] args) throws UnknownHostException 
	{
		Client C = new Client( InetAddress.getLocalHost() );
		C.launch();
				
		return;
	}
}
