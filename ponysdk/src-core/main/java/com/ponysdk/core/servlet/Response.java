
package com.ponysdk.core.servlet;

import java.io.IOException;

public interface Response {

    void write(String s) throws IOException;

    void flush() throws IOException;

}
