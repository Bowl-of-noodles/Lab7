package edu.rogachova.server.util;

import edu.rogachova.common.net.CommandResult;
import edu.rogachova.common.net.Request;

public interface Executable {
    CommandResult execute(Request<?> request);
}
