package com.ponysdk.spring.servlet;

import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.application.ApplicationManagerOption;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class SpringApplicationManager extends AbstractApplicationManager {
    private String[] configurations;


    SpringApplicationManager(ApplicationManagerOption options) {
        super(options);

        final List<String> files = new ArrayList<>();

        final String clientConfigFile = getOptions().getClientConfigFile();
        if (StringUtils.isEmpty(clientConfigFile))
            files.addAll(Arrays.asList("conf/client_application.inc.xml", "etc/client_application.xml"));
        else
            files.add(clientConfigFile);

        configurations = files.toArray(new String[0]);
    }

    @Override
    protected EntryPoint initializeUIContext(UIContext ponySession) {
        try (ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(configurations)) {
            EntryPoint entryPoint = applicationContext.getBean(EntryPoint.class);

            final Map<String, InitializingActivity> initializingPages = applicationContext.getBeansOfType(InitializingActivity.class);
            if (!initializingPages.isEmpty()) {
                initializingPages.values().forEach(InitializingActivity::afterContextInitialized);
            }
            return entryPoint;
        }
    }
}
