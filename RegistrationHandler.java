import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;

/**
 * Here we effectively decide whether we'll accept peer or not
 */
public class RegistrationHandler extends Thread 
{
	//-----------------------------
	//-------- Attributes ---------
	//-----------------------------
	private Socket connection;
	private IServerData server;

	//-------------------------
	//-------- Methods --------
	//-------------------------
	public RegistrationHandler(IServerData server, Socket incoming_connection) 
	{
		this.connection = incoming_connection;
		this.server = server;
		this.start();
	}

	//------------------------------
	//-------- From Thread ---------
	//------------------------------
	@Override
	public void run() 
	{
		BufferedReader input = null;
		try {				
			// read message from socket
			input = new BufferedReader( 
						new InputStreamReader( connection.getInputStream()));				
			String message = input.readLine();
			
			System.out.println("Server got: " + message);
			
			// register requester IP address, if code was correct
			if( message.equals(MessageCode.REQUEST_REGISTRATION.code_string()) )
			{
				DataOutputStream output = null;
				try {
					output = new DataOutputStream( connection.getOutputStream());
				} catch(IOException e) {
					System.err.println("Error while trying to open output stream.");
					System.err.println( e.getMessage() );
				}
							
				// try to push it to the list of registered clients. If we fail,
				// answer peer with a list of available peers to connect.
				if( server.push_new_peer(connection.getInetAddress()))
				{
					System.out.println("Peer was registered in this server.");
					output.writeBytes(MessageCode.ACCEPT_REGISTRATION.code_string() + "\n");
				}
				else
				{
					System.out.println("This server is already full. Sending list of available peers.");
					
					//builder peer list
					Iterator<InetAddress> peer_it = server.peers();
					StringBuilder peer_list = new StringBuilder();
					peer_list.append( server.peer_count() + " " );
					while(peer_it.hasNext())
					{
						peer_list.append(peer_it.next().toString());
						peer_list.append(" ");
					}
					
					output.writeBytes(MessageCode.DENY_AND_SUGGEST.code_string() + " " + peer_list.toString() + "\n");
				}
			}
			else if( message.equals(MessageCode.SHUTDOWN_REGISTRATION_SERVER.code_string()))
				System.out.println("Dummy connection.");
			else if( message.equals(MessageCode.REMOVE_REGISTRATION.code_string()))
			{
				this.server.pop_peer( connection.getInetAddress() );
				System.out.println(connection.getInetAddress().getHostName() + " is removing its registration.");
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
