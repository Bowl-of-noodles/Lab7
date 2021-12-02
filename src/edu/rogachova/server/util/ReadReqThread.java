package edu.rogachova.server.util;

import edu.rogachova.common.net.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.channels.SocketChannel;

public class ReadReqThread extends Thread
{
    private SocketChannel socketChannel;
    private ExecuteCommandService executeCommandService;


    public ReadReqThread(SocketChannel socketChannel, ExecuteCommandService executeCommandService){
        this.socketChannel = socketChannel;
        this.executeCommandService = executeCommandService;
    }

    @Override
    public void run()
    {
        try{
            ObjectInputStream objectInputStream = new ObjectInputStream(socketChannel.socket().getInputStream());
            Request<?> request = (Request<?>) objectInputStream.readObject();
            System.out.println(socketChannel.getRemoteAddress() + ": " + request.command);
            RequestHandler requestHandler = new RequestHandler(request, executeCommandService, socketChannel);
            requestHandler.start();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
