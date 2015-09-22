
package com.ponysdk.core.stm;

import javax.json.JsonReader;

import com.ponysdk.core.Application;
import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.core.useragent.UserAgent;

public interface TxnContext {

    void flush();

    Parser getParser();

    JsonReader getReader();

    UserAgent getUserAgent();

    String getRemoteAddr();

    Application getApplication();

    void setApplication(Application application);

    void setAttribute(String name, Object value);

    Object getAttribute(String name);

    void setRequest(Request request);

    void setResponse(Response response);

    int getSeqNum();

    String getHistoryToken();

    Request getRequest();

    UIContext getUIContext();

    void setUIContext(UIContext uiContext);

    void sendHeartBeat();

}
