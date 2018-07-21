
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class NoisyCricket extends DatagramSocket {
	private static String PROP = "net.ini";
	private static String logFile = "NoisyCricket.log";
  
	private int packet_mtu = 1500;                    // max size of packet
	private int packet_droprate = 0;                  // 0=> 0% packet loss, 25=> 25% packet loss. etc
	private int[] packet_drop = new int[0];           // array of packer numbers to drop.  e.g. every 3rd {3,6,9,12,15,...}
	private int packet_delay_min = 10;                // minimum packer delay milliseconds to wait before send
	private int packet_delay_max = 10;                // maximum packet delay  ''                      ''
	private Random rand = new Random(System.currentTimeMillis());
	private static boolean dump = true;               // verbose output - for verbose set to true
	
	private static long packet_counter = 1L;          // keep track of packets - enumerate them
	
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
	
	public NoisyCricket(SocketAddress paramSocketAddress) throws SocketException {
		super(paramSocketAddress);
		setuplogfile();
		loadProperties();
	}
	
	public NoisyCricket(int paramInt, InetAddress paramInetAddress) throws SocketException {
		super(paramInt, paramInetAddress);
		setuplogfile();
		loadProperties();
	}
	
	public NoisyCricket(DatagramSocketImpl paramDatagramSocketImpl) throws SocketException {
		super(paramDatagramSocketImpl);
		setuplogfile();
		loadProperties();
	}
	
	public NoisyCricket(int paramInt) throws SocketException {
		super(paramInt);
		setuplogfile();
		loadProperties();
	}
	
	public NoisyCricket() throws SocketException {
		super();
		setuplogfile();
		loadProperties();
	}
	
	public int getSendBufferSize() throws SocketException {
		return packet_mtu;
	}
	
	public void setSendBufferSize(int paramInt) throws SocketException {
		throw new SocketException("MTU is adjusted within the " + PROP + " file");   
	}
	
	
	private void rawSend(DatagramPacket paramDatagramPacket) throws IOException {
		super.send(paramDatagramPacket);
	}
	
	
	public void send(DatagramPacket paramDatagramPacket) throws IOException {
		synchronized (paramDatagramPacket) {
			if (paramDatagramPacket.getLength() > packet_mtu) {
				throw new IOException("Packet length exceeds MTU");
			}
			
			boolean dropPacket = false;
			int dropRoll = rand.nextInt(100)+1;
			
			dropPacket = (packet_droprate > dropRoll);
			
			for (int i = 0; i < packet_drop.length; i++) {
				if (packet_counter - 1 == packet_drop[i])
					dropPacket = true;
			}
			
			try {   
				Thread.sleep((long) 1);
			}  catch (Exception e) {;}
			
			new senderThread(this, paramDatagramPacket, packet_counter - 1L, dropPacket);
			packet_counter += 1L;
		}
	}
	
	private void setuplogfile(){
		try {
			File file = new File(logFile);
			FileOutputStream fos = new FileOutputStream(file);
			PrintStream ps = new PrintStream(fos);
			System.setErr(ps);
		} catch (FileNotFoundException e) {}
	}
	
	private void loadProperties()  {
		try {
			Properties localProperties = new Properties();
			localProperties.load(new FileInputStream(PROP));

			String[] arrayOfString = localProperties.getProperty("packet.droprate", "0").split(",");
			
			if (arrayOfString.length == 1) {
				packet_droprate = Integer.parseInt(arrayOfString[0]);
			} else {
				packet_drop = new int[arrayOfString.length];
				for (int i = 0; i < arrayOfString.length; i++) {
					packet_drop[i] = Integer.parseInt(arrayOfString[i]);
				}
			}
			packet_delay_min = Integer.parseInt(localProperties.getProperty("packet.delay.minimum", "0"));
			packet_delay_max = Integer.parseInt(localProperties.getProperty("packet.delay.maximum", "0"));
			packet_mtu = Integer.parseInt(localProperties.getProperty("packet.mtu", "1500"));
			packet_mtu = (packet_mtu <= 0 ? Integer.MAX_VALUE : packet_mtu);
			
			if(dump) {
				System.err.println("packet_droprate: " + packet_droprate);
				System.err.println("packet_delay_min: " + packet_delay_min);
				System.err.println("packet_delay_max: " + packet_delay_max);
				System.err.println("packet_mtu: " + packet_mtu);
				System.err.print("Dropping packets: ");
				for (int i = 0; i < packet_drop.length; i++) {
					System.err.print("drop_packet[" + i + "]:" + packet_drop[i] + ", ");
					System.err.print(packet_drop[i] + ", ");
				}
				System.err.println(" ");
				dump=false;
			}
			
		} catch (Exception e){ e.printStackTrace(); }
	}
	
	
	private class senderThread extends Thread {
		NoisyCricket socket;
		
		DatagramPacket packet;
		boolean drop;
		
		senderThread(NoisyCricket paramSocket, DatagramPacket paramDatagramPacket, long paramLong, boolean paramBoolean) {
			socket = paramSocket;
			packet = new DatagramPacket(paramDatagramPacket.getData(),
					paramDatagramPacket.getLength(),
					paramDatagramPacket.getAddress(),
					paramDatagramPacket.getPort());
			drop = paramBoolean;
			start();
		}
		public void run(){
			try {
				long delay = (long) (packet_delay_min + rand.nextFloat() * (packet_delay_max - packet_delay_min));
				if(delay != 0 ) {
					try {
						String payload = new String(((Packet) Serializer.toObject(packet.getData())).getPayload());
						System.err.println(simpleDateFormat.format(new Date()) + ": Delaying send " + delay + "ms for packet " + ((Packet) Serializer.toObject(packet.getData())) + payload);
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				sleep( delay );
				try {
					if(drop){
						String payload = new String(((Packet) Serializer.toObject(packet.getData())).getPayload());
						System.err.println(simpleDateFormat.format(new Date()) + ": Dropping datagram: " + ((Packet) Serializer.toObject(packet.getData())) + payload);
					} else
						socket.rawSend(packet);
				} catch (Exception localException) {
					localException.printStackTrace();
				}
			} catch (InterruptedException localInterruptedException) {}
		}
	}
}
