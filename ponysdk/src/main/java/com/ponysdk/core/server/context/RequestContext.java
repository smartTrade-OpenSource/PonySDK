package com.ponysdk.core.server.context;

import java.util.List;
import java.util.Map;

public record RequestContext(Map<String, List<String>> parameterMap, Map<String, List<String>> headers,
                             Object session) {
}