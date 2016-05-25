
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.io.Writer;

public interface Response {

    void write(String s) throws IOException;

    void flush();

    Writer getWriter() throws IOException;

}
