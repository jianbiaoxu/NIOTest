package com.jokexu;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NIOServer {

	volatile static Map<SocketAddress, ByteBuffer> socketMap = new ConcurrentHashMap<>();
	static final int HEAD_LENGTH = 2;
	
	public static void main(String[] args) throws IOException {
		
		Selector selector = Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(18080));
		ssc.configureBlocking(false);
		
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		
		while(true) {
			
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			
			Iterator<SelectionKey> it = selectedKeys.iterator();
			while(it.hasNext()) {
				
				SelectionKey selectionKey = it.next();
				it.remove();
				if(selectionKey.isValid()) {
					
					if(selectionKey.isAcceptable()) {
						
						ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
						SocketChannel socketChannel = channel.accept();
						socketChannel.configureBlocking(false);
						socketChannel.register(selector, SelectionKey.OP_READ);
						
					} if(selectionKey.isReadable()) {
						
						SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
						processRead(socketChannel);
						
					}
				}
				
			}
		}
	}

	private static void processRead(SocketChannel socketChannel) throws IOException {
		
		SocketAddress remoteAddress = socketChannel.getRemoteAddress();
		ByteBuffer byteBuffer = socketMap.get(remoteAddress);
		ByteBuffer readBuf = ByteBuffer.allocate(128);
		int readLength = -1;
		List<ByteBuffer> list = new ArrayList<>();
		readLength = socketChannel.read(readBuf);
		
		do {
			
			int dataLength = readLength;
			readBuf.flip();
			
			if(byteBuffer != null) {
				
				dataLength += byteBuffer.remaining();
				ByteBuffer tmp = ByteBuffer.allocate(dataLength);
				
				tmp.put(byteBuffer.array());
				tmp.put(readBuf.array(), 0, readBuf.remaining());
				tmp.position(0);
				byteBuffer = tmp;
			} else {
				byteBuffer = ByteBuffer.allocate(readLength);
				byteBuffer.put(readBuf.array(), 0, readBuf.remaining());
				byteBuffer.flip();
			}
			
			if(dataLength > HEAD_LENGTH) {
				
				readFrame(dataLength, byteBuffer, list, remoteAddress);
				
			} else {
				
				socketMap.put(remoteAddress, byteBuffer);
				break;
			}
			
			readBuf.clear();
		} while((readLength = socketChannel.read(readBuf)) > 0);
		
		list.forEach(buf -> {
			
			byte[] data = buf.array();
			System.out.println("接收到数据包：" + new String(data));
			
		});
	}

	private static void readFrame(int dataLength, ByteBuffer byteBuffer, List<ByteBuffer> list, SocketAddress remoteAddress) {
		
		byte[] headData = new byte[HEAD_LENGTH];
		byteBuffer.get(headData);
		
		int frameLength = ByteBuffer.wrap(headData).asShortBuffer().get();
		
		if(frameLength <= dataLength - HEAD_LENGTH) {
			
			byte[] data = new byte[frameLength];
			byteBuffer.get(data);
			
			ByteBuffer wrap = ByteBuffer.wrap(data);
			list.add(wrap);
			
			//还有可读数据
			if(byteBuffer.remaining() > 0) {
				byte[] tmp = new byte[byteBuffer.remaining()];
				byteBuffer.get(tmp);
				
				byteBuffer = ByteBuffer.wrap(tmp);
				readFrame(byteBuffer.remaining(), byteBuffer, list, remoteAddress);
				
			} else {
				byteBuffer = null;
				socketMap.remove(remoteAddress);
			}
		} else {
			byteBuffer.position(0);
			socketMap.put(remoteAddress, byteBuffer);
		}
		
	}
	
}
