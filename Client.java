import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;

// TODO: Create a common enumeration with ports and IPs

public class Client {

	//------------------------------------
	//----------- Attributes -------------
	//------------------------------------
	
	
	
	//------------------------------------
	//------------ Methods ---------------
	//------------------------------------
	public void require_register()
	{
		// Open TCP connection and send "R01___.___.___.___"
		Socket connection = null;
		try {
			connection = new Socket(InetAddress.getLocalHost(), 5000);
			
			DataOutputStream output = new DataOutputStream(connection.getOutputStream());
			
			output.writeBytes("R01");
			
			// Close socket
			try {
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
	
	//------------------------------------
	public static void main(String[] args) 
	{
		Client C = new Client();
		C.require_register();
		return;
	}

}
