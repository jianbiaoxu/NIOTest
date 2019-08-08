package com.jokexu;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class NIOClient {

	public static void main(String[] args) {
		
		SocketChannel sc;
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.connect(new InetSocketAddress("127.0.0.1", 18080));
			sc.finishConnect();
			@SuppressWarnings("resource")
			Scanner scan = new Scanner(System.in);
			
			while(true) {
				
				String line = scan.next();
				byte[] bytes = line.getBytes();
				int length = bytes.length;
				
				ByteBuffer buf = ByteBuffer.allocate(2 + length);
				buf.putShort((short) length);
				buf.put(bytes);
				buf.flip();
				
				sc.write(buf);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
