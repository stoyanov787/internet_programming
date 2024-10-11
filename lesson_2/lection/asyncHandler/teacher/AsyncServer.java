package lesson_2.lection.asyncHandler.teacher;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AsyncServer {
    public static void main(String[] args) throws Exception {
        // Create the server socket channel
        AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8090);
        serverSocketChannel.bind(hostAddress);

        System.out.println("Server listening on port 8090...");

        // Accept the first client connection
        // when the client connects CompletionHandler will be called
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
                // Accept the next client connection
                serverSocketChannel.accept(null, this);

                // Allocate a buffer to read data from the client
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                // Read data from the client
                clientChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer bytesRead, ByteBuffer buffer) {
                        if (bytesRead == -1) {
                            try {
                                clientChannel.close(); // Close channel if client disconnects
                            } catch (Exception e) {
                                System.err.println("Failed to close client channel: " + e.getMessage());
                            }
                            return;
                        }

                        buffer.flip();
                        String message = new String(buffer.array(), 0, buffer.limit());
                        System.out.println("Received message from client: " + message);
                        buffer.clear();

                        // Echo the message back to the client
                        ByteBuffer responseBuffer = ByteBuffer.wrap(("Server received: " + message).getBytes());
                        clientChannel.write(responseBuffer, responseBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                            @Override
                            public void completed(Integer result, ByteBuffer responseBuffer) {
                                if (responseBuffer.hasRemaining()) {
                                    clientChannel.write(responseBuffer, responseBuffer, this); // Continue writing if not done
                                } else {
                                    System.out.println("Response sent to client.");
                                }
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer responseBuffer) {
                                System.err.println("Failed to send response to client: " + exc.getMessage());
                            }
                        });
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        System.err.println("Failed to read from client: " + exc.getMessage());
                    }
                });
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                System.err.println("Failed to accept a connection: " + exc.getMessage());
            }
        });

        // Prevent the server from exiting
        Thread.sleep(Long.MAX_VALUE);
    }
}