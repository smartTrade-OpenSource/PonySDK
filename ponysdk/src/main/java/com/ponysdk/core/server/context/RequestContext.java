package com.ponysdk.core.server.context;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

public record RequestContext(Map<String, List<String>> parameterMap, Map<String, List<String>> headers, HttpSession session) {

}