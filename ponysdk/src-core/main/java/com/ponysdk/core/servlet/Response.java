
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.OutputStream;

public interface Response {

    OutputStream getOutputStream() throws IOException;

}
