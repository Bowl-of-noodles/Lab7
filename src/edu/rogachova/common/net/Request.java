package edu.rogachova.common.net;

import edu.rogachova.common.model.User;

import java.io.Serializable;

public class Request<T> implements Serializable
{
    public final String command;
    public String user;
    /**
     * Передаваемая в запросе сущность
     */
    public final T entity;

    public Request(String command, T entity, String userLogin) {
        this.command = command;
        this.entity = entity;
        this.user = userLogin;
    }
}
