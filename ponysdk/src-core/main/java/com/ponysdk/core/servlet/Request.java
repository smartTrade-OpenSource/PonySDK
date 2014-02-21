
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.Reader;

public interface Request {

    Session getSession();

    String getHeader(String header);

    Reader getReader() throws IOException;

    String getRemoteAddr();

}
