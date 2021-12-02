package edu.rogachova.server.util;

import edu.rogachova.common.net.CommandResult;
import edu.rogachova.common.net.Request;
import edu.rogachova.common.net.ResultStatus;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;


public class RequestHandler extends Thread
{
    private ExecuteCommandService executeCommandService;
    private Request<?> request;
    private static ForkJoinPool forkJoinPool = new ForkJoinPool();
    private SocketChannel socketChannel;

    public RequestHandler(Request<?> request, ExecuteCommandService executeCommandService, SocketChannel socketChannel){
        this.request = request;
        this.executeCommandService = executeCommandService;
        this.socketChannel = socketChannel;
    }

    @Override
    public void run()
    {
        CommandResult result = executeCommandService.executeCommand(request);
        if (result.status == ResultStatus.OK) {
            System.out.println("Команда выполнена успешно");
        }else{
            System.out.println("Команда выполнена неуспешно");
        }
        forkJoinPool.invoke(new RecursiveTask<Boolean>() {
            @Override
            protected Boolean compute() {
                try {
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socketChannel.socket().getOutputStream());
                    objectOutputStream.writeObject(result);
                    objectOutputStream.flush();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        });
    }
}
