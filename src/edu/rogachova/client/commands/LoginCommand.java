package edu.rogachova.client.commands;

import edu.rogachova.client.managers.Asker;
import edu.rogachova.client.managers.RequestSender;
import edu.rogachova.common.Crypt;
import edu.rogachova.common.exceptions.WrongAmountOfArgumentsException;
import edu.rogachova.common.model.User;
import edu.rogachova.common.net.CommandResult;
import edu.rogachova.common.net.Request;
import edu.rogachova.common.net.ResultStatus;

import java.util.Scanner;

public class LoginCommand implements Command
{
    RequestSender requestSender;
    Scanner scanner;

    public LoginCommand(RequestSender requestSender, Scanner scanner){
        this.requestSender = requestSender;
        this.scanner = scanner;
    }

    @Override
    public String getName()
    {
        return "login";
    }

    @Override
    public String getDescription()
    {
        return "войти в учетную запись";
    }

    @Override
    public void execute(String input) throws Exception
    {
        try {
            if (!input.isEmpty()) throw new WrongAmountOfArgumentsException();
            String username = Asker.askUsername(scanner);
            String password = Asker.askPassword(scanner);
            password = Crypt.hashPassword(password);
            User user = new User(username, password);
            Request<User> request = new Request<>(getName(), user, username);
            CommandResult result = requestSender.sendRequest(request);
            if (result.status == ResultStatus.OK) {
                System.out.println(result.message);
                requestSender.setUser(user);
            } else
                System.out.println(result.message);
        } catch (WrongAmountOfArgumentsException exception) {
            System.out.println("Использование: '" + getName() + "'");
        }
    }

    @Override
    public boolean doIfNotAuthorized()
    {
        return true;
    }
}
