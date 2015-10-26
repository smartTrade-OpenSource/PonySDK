/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.impl.main;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ponysdk.spring.servlet.SpringApplicationLoader;

public class MainSpring {

    public static void main(final String[] args) throws Exception {
        final String serverConfigLocation = System.getProperty(SpringApplicationLoader.SERVER_CONFIG_LOCATION, "classpath:server_application.xml");

        final ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(serverConfigLocation);

        classPathXmlApplicationContext.addApplicationListener(new ApplicationListener<ApplicationEvent>() {

            @Override
            public void onApplicationEvent(final ApplicationEvent event) {
                // TODO Auto-generated method stub

            }
        });

    }

}
