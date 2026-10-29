/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.servlet;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ponysdk.core.server.application.Drain;

/**
 * A load balancer's health check: 200 "UP" while the server takes pages, 503 "DRAINING" while it moves them to the
 * others ({@link Drain}). Cheaper than the bootstrap page, which the server builds for each request; no session, not
 * cached.
 */
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final boolean draining = Drain.isDraining();
        response.setStatus(draining ? HttpServletResponse.SC_SERVICE_UNAVAILABLE : HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(draining ? "DRAINING" : "UP");
    }
}
