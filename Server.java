
import java.io.*;
import java.net.*;

class Server{
	private static final int BUFFER_SIZE = 1024;
    private static final int PORT = 6789;

	public static void main(String[] args) throws IOException {
		NoisyCricket socket = new NoisyCricket(PORT);
		Packet rPacket = new Packet(0, new String(new byte[BUFFER_SIZE]));
		byte[] rBuf = new byte[BUFFER_SIZE];
		int expected_seq = 0;
		String payload = "";
           
		while(socket.isBound()) {
			DatagramPacket rDatagram = new DatagramPacket(rBuf, rBuf.length);
			socket.receive(rDatagram);
			
			//RECIEVE DATA PACKET
			try {
            	rPacket = (Packet) new ObjectInputStream(new ByteArrayInputStream(rDatagram.getData())).readObject();
			} catch (ClassNotFoundException e) { e.printStackTrace(); }
			
			//PARSE DATA PACKET
            int seq_no = rPacket.getSeq();
            byte[] sData;
            if(seq_no == expected_seq) {
                payload = new String(rPacket.getPayload());
                System.out.println("FROM CLIENT: '" + payload + "' sequence number: " + seq_no);
                sData = generateAck(seq_no, payload);
                expected_seq++;
            }  else {
            	System.out.println("Unexpected packet: " + seq_no + ".  Expected: " + expected_seq);
            	sData = generateAck(expected_seq - 1, payload);
            }

            //SEND ACK
            DatagramPacket sDatagram = new DatagramPacket(sData, sData.length, rDatagram.getAddress(), rDatagram.getPort());
            socket.send(sDatagram);

       	}
    socket.close();
    }
	
	private static byte[] generateAck(int seq_no, String payload) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        objectOut.writeObject(new Packet(seq_no, payload, State.Acked));
        return byteOut.toByteArray();
	}
}
