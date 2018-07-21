
import java.io.*;
import java.net.*;
import java.lang.Math;

class Client{
    private static final int BUFFER_SIZE = 1024;
    private static final int PORT = 6789;
    private static final int WIN_SIZE = 5;
    
    private static final String SERVER = "localhost";
    private static final int TIMEOUT = 3000;
	
    public static void main(String args[]) throws Exception{
    	NoisyCricket socket = new NoisyCricket();
    	BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
    	String sentence;
    	int seq_no = 0;
    	int last_ack = -1;
    	
    	socket.setSoTimeout(TIMEOUT);
		InetAddress serverIP = InetAddress.getByName(SERVER);
    	
		while(socket.isBound()) {
			sentence = inFromUser.readLine();
			Packet[] sPacketBuffer = new Packet[sentence.length()];
    		
			//POPULATE THE PACKET BUFFER
    		for(int i=0; i<sPacketBuffer.length; i++) {
    			String sData = sentence.substring(i, i+1);
    			sPacketBuffer[i] = new Packet(seq_no, sData);
    			seq_no++;
    		}
    		
    		for(int i=0; last_ack < sPacketBuffer[sPacketBuffer.length - 1].getSeq();) {
    			//SEND WINDOW
    			if(i < sPacketBuffer.length && sPacketBuffer[i].getSeq() <= last_ack + WIN_SIZE) {
    				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        			ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        			objectOut.writeObject(sPacketBuffer[i]);
        			byte[] sBuf = byteOut.toByteArray();
        			DatagramPacket sDatagram = new DatagramPacket(sBuf, sBuf.length, serverIP, PORT);
        			System.out.println( "Sending Packet (seq_n: " + sPacketBuffer[i] + ") Payload: '" + sPacketBuffer[i].getData() + "'"); 
    				socket.send(sDatagram);
    				//Thread.sleep(30);
    				i++;
    				continue;
    			}
    			
    			//WAIT FOR ACKS
    			try {
    				byte[] rData = new byte[BUFFER_SIZE];
    				DatagramPacket rDatagram = new DatagramPacket(rData, rData.length, serverIP, PORT);
    				socket.receive(rDatagram);
    				byte[] rPayload = rDatagram.getData();
    				
    	    		ByteArrayInputStream byteIn = new ByteArrayInputStream(rPayload);
    	    		ObjectInputStream objectIn = new ObjectInputStream(byteIn);
    	    		Packet rPacket = new Packet(0, new String(new byte[BUFFER_SIZE]));
    	    		
    	    		try {
    	    			rPacket = (Packet) objectIn.readObject();
    	    		} catch (ClassNotFoundException e) { e.printStackTrace(); }
    	    		
    	    		last_ack = Math.max(last_ack, rPacket.getSeq());
    	    		System.out.println(" Received Packet (seq_n: "  + rPacket.getSeq() + ") Payload: '" + rPacket.getPayload() + "'");
    	    		
    			} catch(SocketTimeoutException e) {
    				System.out.println("Socket timeout.  Last ACK was " + last_ack + ".");
    				i = Math.max(i-(sPacketBuffer[i-1].getSeq() - last_ack), 0);
    			}
    		}
		}
    	socket.close();
    }
}
