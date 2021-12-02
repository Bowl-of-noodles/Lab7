package edu.rogachova.client.commands;

import edu.rogachova.client.managers.RequestSender;

import java.util.Scanner;

public class RegisterCommand extends LoginCommand
{
    public RegisterCommand(RequestSender requestSender, Scanner scanner)
    {
        super(requestSender, scanner);
    }

    @Override
    public String getName()
    {
        return "register";
    }

    @Override
    public String getDescription()
    {
        return "зарегистрировать нового пользователя";
    }

    @Override
    public void execute(String input) throws Exception
    {
        super.execute(input);
    }

    @Override
    public boolean doIfNotAuthorized()
    {
        return true;
    }
}
