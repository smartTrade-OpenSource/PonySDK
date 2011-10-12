/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.generator;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GeneratorHelper {

    // TODO nciaravola must be the client package name
    public static String getCommandPackage(Domain domain) {
        return domain.getPackage() + ".command." + domain.getName().toLowerCase();
    }

    public static String getServerServicePackage(Domain domain) {
        return domain.getPackage() + ".service." + domain.getName().toLowerCase();
    }

    public static String getServicePackage(Domain domain) {
        return domain.getPackage() + ".service." + domain.getName().toLowerCase();
    }

    public static String getEventPackage(Domain domain) {
        return domain.getPackage() + ".event." + domain.getName().toLowerCase();
    }

    public static String getDAOPackage(Domain domain) {
        return domain.getPackage() + ".dao." + domain.getName().toLowerCase();
    }

    public static String getFactoryPackage(String packageName, String entityName) {
        return packageName + "." + ".factory." + entityName.toLowerCase();
    }

    // userService
    public static String getServletName(String entityName) {
        return entityName.toLowerCase() + "Service";
    }

    // RemoteUserServiceImpl
    public static String getRemoteServiceImplClassName(Domain domain) {
        return "Remote" + domain.getName() + "ServiceImpl";
    }

    // userServiceImpl
    public static String getServiceImplClassName(Domain domain) {
        return domain.getName() + "ServiceImpl";
    }

    // Event
    public static String getEventClassName(Event event) {
        return event.getName() + "Event";
    }

    // Handler
    public static String getHandlerClassName(Event event) {
        return event.getName() + "Handler";
    }

    // Master Handler
    public static String getMasterHandlerClassName(Domain domain) {
        return domain.getName() + "EventsHandler";
    }

    public static String[] getBeforeEventName() {
        final String[] beforeEvent = { "Create", "Show", "Update", "Delete" };
        return beforeEvent;
    }

    public static String[] getAfterEventName() {
        final String[] afterEvent = { "Created", "Updated", "Deleted" };
        return afterEvent;
    }

    public static String getFirstCharToLower(String name) {
        return name.replaceFirst(String.valueOf(name.charAt(0)), String.valueOf(name.charAt(0)).toLowerCase());
    }

    // DAO
    public static String getDAOClassName(Domain domain) {
        return domain.getName() + "DAO";
    }

    // Business event extend
    public static String getBusinessEventExtends(Event event) {
        if (event.isBusiness())
            return "com.ponysdk.core.event.BusinessEvent <" + getHandlerClassName(event) + ">";
        else
            return "com.ponysdk.core.event.SystemEvent<" + getHandlerClassName(event) + ">";
    }

    // com.ponysdk.smartcc.services.user.RemoteUserServiceImpl
    public static String getRemoteServiceImplFullClassName(Domain domain) {
        return getServerServicePackage(domain) + "." + getRemoteServiceImplClassName(domain);
    }

    // UserService
    public static String getServiceClassName(Domain domain) {
        return domain.getName() + "Service";
    }

    // com.ponysdk.smartcc.client.services.user.UserService
    public static String getServiceFullClassName(Domain domain) {
        return getServicePackage(domain) + "." + getServiceClassName(domain);
    }

    // RemoteUserServiceAsync
    public static String getRemoteServiceAsyncClassName(Domain domain) {
        return "Remote" + domain.getName() + "ServiceAsync";
    }

    // com.ponysdk.smartcc.services.user.RemoteUserServiceAsync
    public static String getRemoteServiceAsyncFullClassName(Domain domain) {
        return getServicePackage(domain) + "." + getRemoteServiceAsyncClassName(domain);
    }

    // com.ponysdk.smartcc.services.user.RemoteUserService
    public static String getRemoteServiceClassName(Domain domain) {
        return "Remote" + domain.getName() + "Service";
    }

    // com.ponysdk.smartcc.services.user.RemoteUserService
    public static String getRemoteServiceFullClassName(Domain domain) {
        return getServicePackage(domain) + "." + getRemoteServiceClassName(domain);
    }

    // UserComponentFactory
    public static String getComponentFactoryClassName(String entityName) {
        return entityName + "ComponentFactory";
    }

    // com.ponysdk.smartcc.user.UserComponentFactory
    public static String getComponentFactoryFullClassName(String packageName, String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getComponentFactoryClassName(entityName);
    }

    // UserFormFieldFactory
    public static String getEditFormFieldFactoryClassName(String entityName) {
        return entityName + "EditFormFieldFactory";
    }

    // com.ponysdk.smartcc.user.UserFormFieldFactory
    public static String getEditFormFieldFactoryFullClassName(String packageName, String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getEditFormFieldFactoryClassName(entityName);
    }

    public static String getSearchFormFieldFactoryClassName(String entityName) {
        return entityName + "SearchFormFieldFactory";
    }

    public static String getSearchFormFieldFactoryFullClassName(String packageName, String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getSearchFormFieldFactoryClassName(entityName);
    }

    // UserCriteriaFieldFactory
    public static String getCriteriaFieldFactoryClassName(String entityName) {
        return entityName + "CriteriaFieldFactory";
    }

    // com.ponysdk.smartcc.user.UserCriteriaFieldFactory
    public static String getCriteriaFieldFactoryFullClassName(String packageName, String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getCriteriaFieldFactoryClassName(entityName);
    }

    // UserListFieldFactory
    public static String getListFieldFactoryClassName(String entityName) {
        return entityName + "ListFieldFactory";
    }

    // com.ponysdk.smartcc.user.UserListFieldFactory
    public static String getListFieldFactoryFullClassName(String packageName, String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getListFieldFactoryClassName(entityName);
    }

    // UserExportFieldFactory
    public static String getExportFieldFactoryClassName(String entityName) {
        return entityName + "ExportFieldFactory";
    }

    // com.ponysdk.smartcc.user.UserExportFieldFactory
    public static String getExportFieldFactoryFullClassName(String packageName, String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getExportFieldFactoryClassName(entityName);
    }

    public static String parameterToString(Collection<Parameter> parameters, String separator) {
        String result = "";
        final Iterator<Parameter> iterator = parameters.iterator();
        while (iterator.hasNext()) {
            final Parameter param = iterator.next();
            result += param.getName();
            if (iterator.hasNext())
                result += separator;
        }
        return result;
    }

    public static String collectionToString(Collection<?> elements, String separator) {
        String result = "";
        final Iterator<?> iterator = elements.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            result += object.toString();
            if (iterator.hasNext())
                result += separator;
        }
        return result;
    }

    public static List<Field> getAllFields(Domain entity) {
        return entity.getUi().getField();
    }

    public static String getParameterToString(Method method) throws Exception {
        String parameters = "";
        final Iterator<Parameter> it = method.getParameter().iterator();
        while (it.hasNext()) {
            final Parameter param = it.next();
            parameters += getClassName(param) + " " + param.getName();
            if (it.hasNext())
                parameters += " ,";
        }
        return parameters;
    }

    public static String getParameterNamesToString(Method serviceMethod) {
        String parameters = "";
        final Iterator<Parameter> it = serviceMethod.getParameter().iterator();
        while (it.hasNext()) {
            final Parameter param = it.next();
            parameters += param.getName();
            if (it.hasNext())
                parameters += " ,";
        }
        return parameters;
    }

    public static String getClassName(Parameter param) throws Exception {
        return getClassName(param.getClazz(), param.getCollection());
    }

    public static String getClassName(Return returnClass) throws Exception {
        if (returnClass.getValue() != null)
            return returnClass.getValue();
        return getClassName(returnClass.getClazz(), returnClass.getCollection());
    }

    public static String getClassName(String className, CollectionType collectionType) throws Exception {
        if (className.equals(""))
            return "void";

        if (className.equalsIgnoreCase("void")) {
            return className;
        }

        if (className.contains(".") || className.contains("boolean") || className.contains("int") || className.contains("float") || className.contains("long") || className.contains("double")
                || className.contains("char")) {
        } else {
            className = "com.ponysdk.messages.structures." + className;
        }
        if (collectionType == CollectionType.SINGLE)
            return className;
        if (collectionType == CollectionType.LIST)
            return List.class.getCanonicalName() + "<" + className + ">";
        if (collectionType == CollectionType.SET)
            return Set.class.getCanonicalName() + "<" + className + ">";
        throw new Exception("Unknown collection type");
    }

}
