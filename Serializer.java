
import java.io.*;

public class Serializer {
	
	public static byte[] toBytes(Object obj) throws IOException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		new ObjectOutputStream(byteOut).writeObject(obj);
		return byteOut.toByteArray();
	}

	public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
		return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
	}
}
