import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

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
	public void require_register()
	{
		// Open TCP connection and send "R01___.___.___.___"
		Socket connection = null;
		try {
			System.out.println("Client is trying to connect...");
			connection = new Socket(InetAddress.getLocalHost(), this.registration_port);
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
			
			System.out.println("Server replied: " + reply);
			
			String reply_code = reply.substring(0, 3);
			
			if( reply_code.equals(MessageCode.ACCEPT_REGISTRATION.code_string()) )
			{
				System.out.println("Server accepted connection.");
				this.connected_server = connection.getInetAddress();
			}
			else if( reply_code.equals(MessageCode.DENY_AND_SUGGEST.code_string()))
			{
				System.out.println("Server denied connection. Trying server-recommended peers.");
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
	}

	//--------------------------------------
	//----------- Constructors -------------
	//--------------------------------------
	public Client()
	{
		this.registration_port = Parameters.DEFAULT_PORT.toInt();
	}
	
	//------------------------------------
	public static void main(String[] args) 
	{
		Client C = new Client();
		C.require_register();
		return;
	}

}
