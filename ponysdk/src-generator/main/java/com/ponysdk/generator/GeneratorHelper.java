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
    public static String getCommandPackage(final Domain domain) {
        return domain.getPackage() + ".command." + domain.getName().toLowerCase();
    }

    public static String getServerServicePackage(final Domain domain) {
        return domain.getPackage() + ".service." + domain.getName().toLowerCase();
    }

    public static String getServicePackage(final Domain domain) {
        return domain.getPackage() + ".service." + domain.getName().toLowerCase();
    }

    public static String getEventPackage(final Domain domain) {
        return domain.getPackage() + ".event." + domain.getName().toLowerCase();
    }

    public static String getUiPackage(final Domain domain) {
        return domain.getPackage() + ".ui." + domain.getName().toLowerCase();
    }

    public static String getDAOPackage(final Domain domain) {
        return domain.getPackage() + ".dao." + domain.getName().toLowerCase();
    }

    public static String getFactoryPackage(final String packageName, final String entityName) {
        return packageName + "." + ".factory." + entityName.toLowerCase();
    }

    public static String getDirectoryFromPackage(final String packageName) {
        return packageName.replaceAll("\\.", "/");
    }

    // userService
    public static String getServletName(final String entityName) {
        return entityName.toLowerCase() + "Service";
    }

    // userServiceImpl
    public static String getServiceImplClassName(final Domain domain) {
        return domain.getName() + "ServiceImpl";
    }

    // Event
    public static String getEventClassName(final Event event) {
        return event.getName() + "Event";
    }

    // ListDescriptor
    public static String getListDescriptorClassName(final Domain domain) {
        return domain.getName() + "ListDescriptor";
    }

    // Handler
    public static String getHandlerClassName(final Event event) {
        return event.getName() + "Handler";
    }

    // Master Handler
    public static String getMasterHandlerClassName(final Domain domain) {
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

    public static String getFirstCharToLower(final String name) {
        return name.replaceFirst(String.valueOf(name.charAt(0)), String.valueOf(name.charAt(0)).toLowerCase());
    }

    // HibernateDAO
    public static String getDAOClassName(final Domain domain) {
        return domain.getName() + "DAO";
    }

    // Business event extend
    public static String getBusinessEventExtends(final Event event) {
        if (event.isBusiness()) return "com.ponysdk.core.event.BusinessEvent <" + getHandlerClassName(event) + ">";
        else return "com.ponysdk.core.event.SystemEvent<" + getHandlerClassName(event) + ">";
    }

    // UserService
    public static String getServiceClassName(final Domain domain) {
        return domain.getName() + "Service";
    }

    // com.ponysdk.smartcc.client.services.user.UserService
    public static String getServiceFullClassName(final Domain domain) {
        return getServicePackage(domain) + "." + getServiceClassName(domain);
    }

    // RemoteUserServiceAsync
    public static String getRemoteServiceAsyncClassName(final Domain domain) {
        return "Remote" + domain.getName() + "ServiceAsync";
    }

    // com.ponysdk.smartcc.services.user.RemoteUserServiceAsync
    public static String getRemoteServiceAsyncFullClassName(final Domain domain) {
        return getServicePackage(domain) + "." + getRemoteServiceAsyncClassName(domain);
    }

    // com.ponysdk.smartcc.services.user.RemoteUserService
    public static String getRemoteServiceClassName(final Domain domain) {
        return "Remote" + domain.getName() + "Service";
    }

    // com.ponysdk.smartcc.services.user.RemoteUserService
    public static String getRemoteServiceFullClassName(final Domain domain) {
        return getServicePackage(domain) + "." + getRemoteServiceClassName(domain);
    }

    // UserComponentFactory
    public static String getComponentFactoryClassName(final String entityName) {
        return entityName + "ComponentFactory";
    }

    // com.ponysdk.smartcc.user.UserComponentFactory
    public static String getComponentFactoryFullClassName(final String packageName, final String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getComponentFactoryClassName(entityName);
    }

    // UserFormFieldFactory
    public static String getEditFormFieldFactoryClassName(final String entityName) {
        return entityName + "EditFormFieldFactory";
    }

    // com.ponysdk.smartcc.user.UserFormFieldFactory
    public static String getEditFormFieldFactoryFullClassName(final String packageName, final String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getEditFormFieldFactoryClassName(entityName);
    }

    public static String getSearchFormFieldFactoryClassName(final String entityName) {
        return entityName + "SearchFormFieldFactory";
    }

    public static String getSearchFormFieldFactoryFullClassName(final String packageName, final String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getSearchFormFieldFactoryClassName(entityName);
    }

    // UserCriteriaFieldFactory
    public static String getCriteriaFieldFactoryClassName(final String entityName) {
        return entityName + "CriteriaFieldFactory";
    }

    // com.ponysdk.smartcc.user.UserCriteriaFieldFactory
    public static String getCriteriaFieldFactoryFullClassName(final String packageName, final String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getCriteriaFieldFactoryClassName(entityName);
    }

    // UserListFieldFactory
    public static String getListFieldFactoryClassName(final String entityName) {
        return entityName + "ListFieldFactory";
    }

    // com.ponysdk.smartcc.user.UserListFieldFactory
    public static String getListFieldFactoryFullClassName(final String packageName, final String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getListFieldFactoryClassName(entityName);
    }

    // UserExportFieldFactory
    public static String getExportFieldFactoryClassName(final String entityName) {
        return entityName + "ExportFieldFactory";
    }

    // com.ponysdk.smartcc.user.UserExportFieldFactory
    public static String getExportFieldFactoryFullClassName(final String packageName, final String entityName) {
        return getFactoryPackage(packageName, entityName) + "." + getExportFieldFactoryClassName(entityName);
    }

    public static String parameterToString(final Collection<Parameter> parameters, final String separator) {
        String result = "";
        final Iterator<Parameter> iterator = parameters.iterator();
        while (iterator.hasNext()) {
            final Parameter param = iterator.next();
            result += param.getName();
            if (iterator.hasNext()) result += separator;
        }
        return result;
    }

    public static String collectionToString(final Collection<?> elements, final String separator) {
        String result = "";
        final Iterator<?> iterator = elements.iterator();
        while (iterator.hasNext()) {
            final Object object = iterator.next();
            result += object.toString();
            if (iterator.hasNext()) result += separator;
        }
        return result;
    }

    public static String getParameterToString(final List<Parameter> params) throws Exception {
        String parameters = "";
        final Iterator<Parameter> it = params.iterator();
        while (it.hasNext()) {
            final Parameter param = it.next();
            parameters += getClassName(param) + " " + param.getName();
            if (it.hasNext()) parameters += " ,";
        }
        return parameters;
    }

    public static String getParameterNamesToString(final List<Parameter> params) {
        String parameters = "";
        final Iterator<Parameter> it = params.iterator();
        while (it.hasNext()) {
            final Parameter param = it.next();
            parameters += param.getName();
            if (it.hasNext()) parameters += " ,";
        }
        return parameters;
    }

    public static String getClassName(final Parameter param) throws Exception {
        return getClassName(param.getClazz(), param.getCollection());
    }

    public static String getClassName(final Return returnClass) throws Exception {
        if (returnClass.getValue() != null) return returnClass.getValue();
        return getClassName(returnClass.getClazz(), returnClass.getCollection());
    }

    public static String getClassName(String className, final CollectionType collectionType) throws Exception {
        if (className.equals("")) return "void";

        if (className.equalsIgnoreCase("void")) { return className; }

        if (className.contains(".") || className.contains("boolean") || className.contains("int") || className.contains("float") || className.contains("long") || className.contains("double") || className.contains("char")) {} else {
            className = "com.ponysdk.messages.structures." + className;
        }
        if (collectionType == CollectionType.SINGLE) return className;
        if (collectionType == CollectionType.LIST) return List.class.getCanonicalName() + "<" + className + ">";
        if (collectionType == CollectionType.SET) return Set.class.getCanonicalName() + "<" + className + ">";
        throw new Exception("Unknown collection type");
    }

    public static String toUpperUnderscore(final String in) {
        final String under = in.replaceAll("(.)([A-Z])", "$1_$2");
        return under.toUpperCase();
    }

}
