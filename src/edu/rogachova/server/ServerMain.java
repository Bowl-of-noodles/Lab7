package edu.rogachova.server;

import edu.rogachova.common.Config;
import edu.rogachova.common.DataManager;
import edu.rogachova.server.managers.CollectionManager;
import edu.rogachova.server.managers.DatabaseManager;
import edu.rogachova.server.util.ExecuteCommandService;
import edu.rogachova.server.managers.FileManager;
import edu.rogachova.server.util.ReadReqThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMain
{
    private static int port = Config.PORT;
    static String filePath = Config.filePath;

    public static void main(String[] args) {
        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception exception) {
                System.out.println("Не получается спарсить порт. Используется " + port);
            }
        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Отстутствует драйвер DB PostgreSQL!");
        }

        String[] loginData = FileManager.getLoginData();
        if (loginData == null) {
            return;
        }

        DatabaseManager databaseManager;
        try {
            databaseManager = new DatabaseManager(Config.heliosUrl, loginData[0], loginData[1]);
            databaseManager.connectToDatabase();
        } catch (Exception exception) {
            System.out.println("Не удалось подключится к БД");;
            return;
        }

        DataManager dataManager;
        try {
            dataManager = new CollectionManager(databaseManager);
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }

        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            System.out.println("Сервер запущен. Порт: " + port);
        } catch (IOException exception) {
            System.out.println("Ошибка запуска сервера!");
            System.out.println(exception.getMessage());
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Выход");
        }));

        ExecuteCommandService executeCommandService = new ExecuteCommandService(dataManager, databaseManager);

        AtomicBoolean exit = new AtomicBoolean(false);
        getInputHandler(dataManager, exit).start();

        while (!exit.get()) {
            try{
                SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel == null) continue;
                ReadReqThread readThread = new ReadReqThread(socketChannel, executeCommandService);
                readThread.start();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    private static void save(DataManager dataManager) {
        dataManager.save();
    }

    private static Thread getInputHandler(DataManager dataManager, AtomicBoolean exit){
        return new Thread(() -> {
            Scanner scanner = new Scanner(System.in);

            while (true){
                if(scanner.hasNextLine()){
                    String serverCommand = scanner.nextLine();

                    switch (serverCommand){
                        case "save":
                            save(dataManager);
                            break;
                        case "exit":
                            exit.set(true);
                            System.exit(0);
                            return;
                        default:
                            System.out.println("Такой команды не существует");
                            break;
                    }
                }
                else{
                    exit.set(true);
                    return;
                }
            }
        });
    }
}
