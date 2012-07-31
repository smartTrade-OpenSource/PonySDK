
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.Reader;

public interface Request {

    Session getSession();

    Reader getReader() throws IOException;

}
