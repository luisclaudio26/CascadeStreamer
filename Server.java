import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server {

	//------------------------------------
	//----------- Attributes -------------
	//------------------------------------
	private boolean running;
	private int registration_port;
	
	private int max_peers; 
	private ArrayList<InetAddress> peers;
	
	private WaitRegistration waiter;
	
	//----------------------------------------
	//----------- Multithreading -------------
	//----------------------------------------
	private class ProcessRegistration extends Thread
	{
		private Socket connection;
		
		public ProcessRegistration(Socket incoming_connection) 
		{
			this.connection = incoming_connection;
			this.start();
		}
		
		/**
		 * Here we effectively decide whether we'll 
		 * accept peer or not;
		 */
		public void run() 
		{
			BufferedReader input = null;
			try {
				// read message from socket
				input = new BufferedReader( 
							new InputStreamReader( connection.getInputStream()));
				String message = input.readLine();
				
				// register requester IP address, if code was correct
				if( message.equals(MessageCode.REQUEST_REGISTRATION.code_string()) )
				{
					System.out.println("Client was registered.");
					peers.add( this.connection.getInetAddress() );
				}
				else
					System.out.println("Client registration request has wrong code.");
				
			} catch(IOException e) {
				System.err.println("Error while reading from registration socket.");
				System.err.println(e.getMessage());				
			} finally {
				try {
					input.close();
					connection.close();
				} catch(IOException e) {
					System.err.println("Error while closing registration socket.");
				}
			}
		}	
	}
	
	private class WaitRegistration extends Thread
	{
		public WaitRegistration()
		{
			this.start();
		}
		
		/**
		 * Here we wait for someone to request a connection
		 * with the server; when request is made, we launch
		 * a new thread to treat it.
		 */
		public void run() 
		{
			// Holds all processes launched so we 
			// can wait for them to die in peace.
			List<ProcessRegistration> children = new ArrayList<ProcessRegistration>();
			
			// Open listening socket
			ServerSocket listener_socket = null;
			
			try {
				listener_socket = new ServerSocket(registration_port);
				
				// accept incoming connections and launch them
				// on separate threads while server is running
				while(running)
				{
					System.out.println("Waiting for connection: ");
					
					Socket incoming_connection = listener_socket.accept();
					ProcessRegistration p = new ProcessRegistration(incoming_connection);
					children.add(p);
				}
				
				// Wait for children to die
				for(ProcessRegistration p : children)
					try {
						p.join();
					} catch (InterruptedException e1) {
						System.err.println("Error joining children thread in WaitRegistration.");
						System.err.println(e1.getMessage());
					}
				
				// shutdown socket
				try {
						listener_socket.close();
				} catch(IOException e) {
					System.err.println("Error while closing socket: ");
					System.err.println(e.getMessage());
				}
			} catch(IOException e) {
				System.err.println("Error while opening socket: ");
				System.err.println(e.getMessage());
			}
		}	
	}
	
	//------------------------------------------
	//------------ Access method ---------------
	//------------------------------------------
	/**
	 * @param peer_address IP address of peer, as a plain string object
	 * @return True if we push it to the peers list, false otherwise
	 */
	private boolean push_new_peer(String peer_address)
	{
		// Should use lockers here to ensure
		// integrity of max_peers and peers[].
		return true;
	}
	
	//------------------------------------------------
	//------------ External operations ---------------
	//------------------------------------------------
	public void launch() 
	{
		// Launch thread that waits for incoming registration requests
		this.waiter = new WaitRegistration();
	}
	
	public void shutdown()
	{
		System.out.println("Server is shutting down after next connection.");
		this.running = false;
		
		// TODO: We should send a dummy message to
		// server, so it will unblock .accept()
		// method and effectively shutdown
		
		try {
			//Server can close only
			waiter.join();
		} catch (InterruptedException e) {
			System.err.println("Waiter thread was interrupted and was not correctly closed.");
			System.err.println(e.getMessage());
		}
	}
	
	//------------------------------------
	//---------- Constructors ------------
	//------------------------------------
	public Server()
	{
		this.registration_port = Parameters.DEFAULT_PORT.toInt();
		this.max_peers = 2;
		this.peers = new ArrayList<InetAddress>();
		this.running = true;
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
