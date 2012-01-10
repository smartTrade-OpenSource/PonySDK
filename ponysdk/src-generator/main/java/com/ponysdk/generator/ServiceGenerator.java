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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServiceGenerator extends BaseGenerator {

    private String srcGeneratedDirectory = "src-generated-application";

    public ServiceGenerator(Domain domain) {
        super(domain);
    }

    public void generate() throws Exception {
        generateCRUDMethos();
        generateCommands();
        generateService();
        generateRemoteServiceImpl();
        generateCRUDEvents();
        generateEvents();
        generateEventsHandler();
    }

    /*
     * Services : CRUD Methods
     */

    private void generateCRUDMethos() throws Exception {
        // Insert CRUD methods
        if (domain.getService() != null && domain.getService().getCrudmethod() != null) {
            // Return
            final Return crudReturn = new Return();
            crudReturn.setClazz(domain.getService().getCrudmethod().getClazz());
            // Parameters
            final Parameter crudParameter = new Parameter();
            crudParameter.setClazz(domain.getService().getCrudmethod().getClazz());
            crudParameter.setName(GeneratorHelper.getFirstCharToLower(domain.getName()));
            final Parameter crudIDParameter = new Parameter();
            crudIDParameter.setClazz("long");
            crudIDParameter.setName(GeneratorHelper.getFirstCharToLower(domain.getName()) + "ID");

            // Add CRUD methods
            final Method createMethod = createCRUDMethod("create" + domain.getName(), crudParameter, crudReturn);
            final Method readMethod = createCRUDMethod("read" + domain.getName(), crudIDParameter, crudReturn);
            final Method updateMethod = createCRUDMethod("update" + domain.getName(), crudParameter, crudReturn);
            final Method deleteMethod = createCRUDMethod("delete" + domain.getName(), crudIDParameter, new Return());

            // Create the implementation class of these CRUD methods if the DAO is set
            if (domain.getService().getDao() != null) {
                final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getServerServicePackage(domain), GeneratorHelper.getServiceImplClassName(domain));
                classWriter.addImplements(GeneratorHelper.getServiceClassName(domain));
                classWriter.setGenerateGetter(true);
                classWriter.setGenerateSetter(true);

                // Add static logger
                classWriter.addConstants("private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(" + GeneratorHelper.getServiceImplClassName(domain) + ".class);");

                // Add service DAO object
                final Parameter daoParameter = new Parameter();
                daoParameter.setClazz(GeneratorHelper.getDAOPackage(domain) + "." + GeneratorHelper.getDAOClassName(domain));
                daoParameter.setName(GeneratorHelper.getFirstCharToLower(GeneratorHelper.getDAOClassName(domain)));
                classWriter.addClassMembers(daoParameter);

                for (final Method method : domain.getService().getMethod()) {
                    final String resultClass = GeneratorHelper.getClassName(method.getReturn());

                    classWriter.addNewLine();
                    classWriter.addLine("@Override");
                    classWriter.addLine("public " + resultClass + " " + method.getName() + "(" + GeneratorHelper.getParameterToString(method) + ") throws Exception {");
                    if (!"void".equals(resultClass)) {
                        classWriter.addLine("   return null;");
                    }
                    classWriter.addLine("}");
                }

                final String domainClass = domain.getName();
                final String domainDAOParameter = GeneratorHelper.getFirstCharToLower(GeneratorHelper.getDAOClassName(domain));
                final String domainParameter = GeneratorHelper.getFirstCharToLower(domain.getName());
                final String domainParameterID = domainParameter + "ID";

                /*
                 * CRUD method implementation
                 */

                // Create
                classWriter.addLine("@Override");
                classWriter.addLine("public " + GeneratorHelper.getClassName(createMethod.getReturn()) + " " + createMethod.getName() + " (" + GeneratorHelper.getParameterToString(createMethod) + ")  throws Exception {");
                classWriter.addLine("   " + domainDAOParameter + ".beginTransaction();");
                classWriter.addLine("   try {");
                classWriter.addLine("       " + domainDAOParameter + ".save(" + domainParameter + ");");
                classWriter.addLine("       " + domainDAOParameter + ".commit();");
                classWriter.addLine("   } catch (final Exception e) {");
                classWriter.addLine("       log.error(\"final Cannot create " + domainClass + " in database\", e);");
                classWriter.addLine("       " + domainDAOParameter + ".rollback();");
                classWriter.addLine("       throw new Exception(\"Cannot create " + domainClass + " in database\", e);");
                classWriter.addLine("   }");
                classWriter.addLine("   return " + domainParameter + ";");
                classWriter.addLine("}");

                // Read
                classWriter.addLine("@Override");
                classWriter.addLine("public " + GeneratorHelper.getClassName(createMethod.getReturn()) + " " + readMethod.getName() + " (" + GeneratorHelper.getParameterToString(readMethod) + ")  throws Exception {");
                classWriter.addLine("   " + GeneratorHelper.getClassName(createMethod.getReturn()) + " " + domainParameter + " = null;");
                classWriter.addLine("   " + domainDAOParameter + ".beginTransaction();");
                classWriter.addLine("   try {");
                classWriter.addLine("       " + domainParameter + " = " + domainDAOParameter + ".findById(" + domainParameterID + ");");
                classWriter.addLine("       " + domainDAOParameter + ".commit();");
                classWriter.addLine("   } catch (final Exception e) {");
                classWriter.addLine("       log.error(\"final Cannot find " + domain.getName() + " in database\", e);");
                classWriter.addLine("       " + domainDAOParameter + ".rollback();");
                classWriter.addLine("       throw new Exception(\"Cannot find " + domain.getName() + " in database\", e);");
                classWriter.addLine("   }");
                classWriter.addLine("   return " + domainParameter + ";");
                classWriter.addLine("}");

                // Update
                classWriter.addLine("@Override");
                classWriter.addLine("public " + GeneratorHelper.getClassName(updateMethod.getReturn()) + " " + updateMethod.getName() + " (" + GeneratorHelper.getParameterToString(updateMethod) + ")  throws Exception {");
                classWriter.addLine("   " + domainDAOParameter + ".beginTransaction();");
                classWriter.addLine("   try {");
                classWriter.addLine("       " + domainDAOParameter + ".saveOrUpdate(" + domainParameter + ");");
                classWriter.addLine("       " + domainDAOParameter + ".commit();");
                classWriter.addLine("   } catch (final Exception e) {");
                classWriter.addLine("       log.error(\"final Cannot update " + domainClass + " in database\", e);");
                classWriter.addLine("       " + domainDAOParameter + ".rollback();");
                classWriter.addLine("       throw new Exception(\"Cannot update " + domainClass + " in database\", e);");
                classWriter.addLine("   }");
                classWriter.addLine("   return " + domainParameter + ";");
                classWriter.addLine("}");

                // Delete
                classWriter.addLine("@Override");
                classWriter.addLine("public " + GeneratorHelper.getClassName(deleteMethod.getReturn()) + " " + deleteMethod.getName() + " (" + GeneratorHelper.getParameterToString(deleteMethod) + ")  throws Exception {");
                classWriter.addLine("   " + domainDAOParameter + ".beginTransaction();");
                classWriter.addLine("   try {");
                classWriter.addLine("       " + GeneratorHelper.getClassName(createMethod.getReturn()) + " " + domainParameter + " = " + domainDAOParameter + ".findById(" + domainParameterID + ");");
                classWriter.addLine("       " + domainDAOParameter + ".delete(" + domainParameter + ");");
                classWriter.addLine("       " + domainDAOParameter + ".commit();");
                classWriter.addLine("   } catch (final Exception e) {");
                classWriter.addLine("       log.error(\"final Cannot delete " + domain.getName() + " in database\", e);");
                classWriter.addLine("       " + domainDAOParameter + ".rollback();");
                classWriter.addLine("       throw new Exception(\"Cannot delete " + domain.getName() + " in database\", e);");
                classWriter.addLine("   }");
                classWriter.addLine("}");

                classWriter.generateContentAndStore();
            }

            domain.getService().getMethod().add(createMethod);
            domain.getService().getMethod().add(readMethod);
            domain.getService().getMethod().add(updateMethod);
            domain.getService().getMethod().add(deleteMethod);
        }
    }

    private Method createCRUDMethod(String name, Parameter crudParameter, Return crudReturn) {
        final Method method = new Method();
        method.setName(name);
        method.setReturn(crudReturn);
        method.getParameter().add(crudParameter);

        return method;
    }

    /*
     * Services : Interface & RemoteService
     */

    private void generateService() throws Exception {
        final String className = GeneratorHelper.getServiceClassName(domain);
        final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getServicePackage(domain), className);

        classWriter.setInterface(true);

        if (domain.getService() != null) {
            for (final Method method : domain.getService().getMethod()) {
                final String returnClass = GeneratorHelper.getClassName(method.getReturn());

                classWriter.addLine("public " + returnClass + " " + method.getName() + "(" + GeneratorHelper.getParameterToString(method) + ") throws Exception;");
            }

            if (domain.getService().getDao() != null) {
                generateDAO(domain.getService().getDao());
            }
        }

        classWriter.generateContentAndStore();
    }

    private void generateRemoteServiceImpl() throws Exception {

        final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getServerServicePackage(domain), GeneratorHelper.getRemoteServiceImplClassName(domain));

        classWriter.addImplements(GeneratorHelper.getServiceClassName(domain));

        classWriter.addClassAnnotation("@SuppressWarnings(\"serial\")");
        final Parameter parameter = new Parameter();
        parameter.setClazz(GeneratorHelper.getServiceFullClassName(domain));
        parameter.setName("service");
        classWriter.addClassMembers(parameter);

        classWriter.addImport(GeneratorHelper.getServiceFullClassName(domain));

        // add logger
        classWriter.addNewLine();
        classWriter.addLine("private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(" + GeneratorHelper.getRemoteServiceImplClassName(domain) + ".class);");

        classWriter.addLine("private static " + GeneratorHelper.getRemoteServiceImplClassName(domain) + " INSTANCE;");

        classWriter.addNewLine();

        classWriter.addLine("public static " + GeneratorHelper.getRemoteServiceImplClassName(domain) + " getInstance() {");
        classWriter.indentBlock();
        classWriter.addLine("if (INSTANCE == null) INSTANCE = new " + GeneratorHelper.getRemoteServiceImplClassName(domain) + "();");
        classWriter.addLine("this.service = com.ponysdk.core.service.PonyServiceRegistry.getPonyService(" + GeneratorHelper.getServiceFullClassName(domain) + ".class);");
        classWriter.addLine("return INSTANCE;");
        classWriter.unindentBlock();
        classWriter.addLine("}");

        classWriter.addNewLine();

        // Build methods
        if (domain.getService() != null) {

            for (final Method method : domain.getService().getMethod()) {
                final String returnClass = GeneratorHelper.getClassName(method.getReturn());
                classWriter.addLine("@Override");
                classWriter.addLine("public " + returnClass + " " + method.getName() + "(" + GeneratorHelper.getParameterToString(method) + ") throws Exception {");
                classWriter.addLine("try{");
                classWriter.addLine("final long start = System.nanoTime();");

                if (method.getReturn().getClazz().equals("void") && method.getReturn().getValue() == null) {
                    classWriter.addLine("service." + method.getName() + "(" + GeneratorHelper.parameterToString(method.getParameter(), " ,") + ");");
                } else {
                    classWriter.addLine(GeneratorHelper.getClassName(method.getReturn()) + " result = service." + method.getName() + "(" + GeneratorHelper.parameterToString(method.getParameter(), " ,") + ");");
                }

                classWriter.addLine("final long end = System.nanoTime();");
                classWriter.addLine("log.debug(\"execution time = \" + ((end - start)* 0.000000001f) + \" ms \");");
                if (method.getReturn().getValue() != null || !method.getReturn().getClazz().equals("void")) {
                    classWriter.addLine("return result;");
                }
                classWriter.addLine("} catch (final Throwable throwable) {");
                classWriter.addLine("    log.error(\"\", throwable);");
                classWriter.addLine("    throw new Exception(throwable.getMessage(), throwable);");
                classWriter.addLine("}");
                classWriter.addLine("}");
            }

        }

        classWriter.generateContentAndStore();
    }

    /*
     * Services : DAO
     */

    private void generateDAO(Dao dao) throws Exception {
        final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getDAOPackage(domain), GeneratorHelper.getDAOClassName(domain));

        classWriter.addExtend("com.ponysdk.core.database.DAO");

        // Add static logger
        classWriter.addConstants("private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(" + GeneratorHelper.getDAOClassName(domain) + ".class);");

        // Create constructor
        final List<Parameter> parameters = new ArrayList<Parameter>();
        final Parameter sessionFactoryParameter = new Parameter();
        sessionFactoryParameter.setName("sessionFactory");
        sessionFactoryParameter.setClazz("org.hibernate.SessionFactory");
        parameters.add(sessionFactoryParameter);
        final Constructor constructor = new Constructor(parameters, parameters);
        classWriter.addConstructor(constructor);

        // Create findById method
        classWriter.addNewLine();
        classWriter.addLine("final public " + dao.getClazz() + " findById(final long id) {");
        classWriter.addLine("   if (log.isDebugEnabled()) {");
        classWriter.addLine("       log.debug(\"getting " + domain.getName() + " instance with id: \" + id);");
        classWriter.addLine("   }");
        classWriter.addNewLine();
        classWriter.addLine("   try {");
        classWriter.addLine("       final " + dao.getClazz() + " instance = (" + dao.getClazz() + ") sessionFactory.getCurrentSession().get(" + dao.getClazz() + ".class, id);");
        classWriter.addLine("       return instance;");
        classWriter.addLine("   } catch (final RuntimeException re) {");
        classWriter.addLine("       log.error(\"getting " + domain.getName() + " by id failed\", re);");
        classWriter.addLine("       throw re;");
        classWriter.addLine("   }");
        classWriter.addLine("}");

        // Create findAll method
        classWriter.addNewLine();
        classWriter.addLine("@SuppressWarnings(\"unchecked\")");
        classWriter.addLine("public java.util.List<" + dao.getClazz() + "> findAll() {");
        classWriter.addLine("   if (log.isDebugEnabled()) {");
        classWriter.addLine("       log.debug(\"finding " + domain.getName() + " instance by example\");");
        classWriter.addLine("   }");
        classWriter.addNewLine();
        classWriter.addLine("   try {");
        classWriter.addLine("       final java.util.List<" + dao.getClazz() + "> results = sessionFactory.getCurrentSession().createQuery(\"final FROM " + domain.getName() + "\").list();");
        classWriter.addLine("       return results;");
        classWriter.addLine("   } catch (final RuntimeException re) {");
        classWriter.addLine("       log.error(\"finding all " + domain.getName() + " failed\", re);");
        classWriter.addLine("       throw re;");
        classWriter.addLine("   }");
        classWriter.addLine("}");

        classWriter.generateContentAndStore();
    }

    /*
     * Services : commands
     */

    private void generateCommands() throws Exception {
        if (domain.getService() != null) {
            for (final Method method : domain.getService().getMethod()) {
                final String returnClass = GeneratorHelper.getClassName(method.getReturn());
                final ClassWriter classWriter = generateCommandX(method, returnClass);

                if (returnClass.equals("void") && method.getReturn().getValue() == null) {
                    classWriter.addExtend("com.ponysdk.core.command.AbstractServiceCommand<java.lang.Object>");
                } else {
                    classWriter.addExtend("com.ponysdk.core.command.AbstractServiceCommand<" + returnClass + ">");
                }

                classWriter.generateContentAndStore();
            }
        }
    }

    private ClassWriter generateCommandX(Method method, String resultClass) throws Exception {
        final String className = method.getName().substring(0, 1).toUpperCase() + method.getName().substring(1) + "Command";

        final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getCommandPackage(domain), className);
        classWriter.setAbstract(true);

        final Constructor constructor = new Constructor();
        final Parameter eventBusParameter = new Parameter();
        eventBusParameter.setName("eventBus");
        eventBusParameter.setClazz("com.ponysdk.core.event.EventBus");

        final List<Parameter> parameters = method.getParameter();
        final List<Parameter> clonedParameters = new ArrayList<Parameter>();
        for (final Parameter parameter : parameters) {
            final Parameter clonedParameter = new Parameter();
            clonedParameter.setClazz(parameter.getClazz());
            clonedParameter.setName(parameter.getName());
            clonedParameter.setCollection(parameter.getCollection());
            clonedParameters.add(clonedParameter);
        }

        final Constructor constructor2 = new Constructor();
        constructor2.setConstructorParameters(new ArrayList<Parameter>(clonedParameters));

        clonedParameters.add(0, eventBusParameter);
        constructor.setConstructorParameters(clonedParameters);
        constructor.setSuperConstructorParameters(Arrays.asList(eventBusParameter));

        classWriter.addConstructor(constructor);
        classWriter.addConstructor(constructor2);

        for (final Parameter param : parameters) {
            classWriter.addClassMembers(param);
        }
        classWriter.addConstants("public static " + GeneratorHelper.getServiceFullClassName(domain) + " service;");

        final StringBuilder template = new StringBuilder();
        template.append("@Override\n");
        if (resultClass.equals("void") && method.getReturn().getValue() == null) {
            template.append("protected java.lang.Void  execute0() throws Exception {\n");
        } else {
            template.append("protected " + resultClass + " execute0() throws Exception {\n");
        }
        template.append("   if (service == null) {\n");
        template.append("       service = " + GeneratorHelper.getRemoteServiceImplFullClassName(domain) + ".getInstance();\n");
        template.append("   }\n");
        if (resultClass.equals("void") && method.getReturn().getValue() == null) {
            template.append("	service.%1$s(%2$s);\n");
            template.append("	return null;\n");
        } else {
            template.append("	return service.%1$s(%2$s);\n");
        }
        template.append("}\n");

        classWriter.addMethod(template.toString(), method.getName(), GeneratorHelper.getParameterNamesToString(method));

        return classWriter;
    }

    /*
     * Events
     */

    private void generateEvents() throws Exception {
        if (domain.getEvent() != null) {
            for (final Event event : domain.getEvent()) {
                generateEvent(event);
                generateHandler(event);
            }
        }
    }

    public void generateCRUDEvents() throws Exception {
        if (domain.getCrudevent() != null) {
            for (final String before : GeneratorHelper.getBeforeEventName()) {
                final Event event = new Event();
                event.name = before + domain.getName();
                event.parameter = domain.getCrudevent().getParameter();
                generateEvent(event);
                generateHandler(event);
            }
            for (final String after : GeneratorHelper.getAfterEventName()) {
                final Event event = new Event();
                event.name = domain.getName() + after;
                event.business = true;
                event.parameter = domain.getCrudevent().getParameter();
                generateEvent(event);
                generateHandler(event);
            }
        }
    }

    private void generateEvent(Event event) throws Exception {
        final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getEventPackage(domain), GeneratorHelper.getEventClassName(event));

        if (event.getParameter() != null) {
            for (final Parameter parameter : event.getParameter()) {
                classWriter.addClassMembers(parameter);
            }
        }

        classWriter.addExtend(GeneratorHelper.getBusinessEventExtends(event));

        // Constant
        classWriter.addConstants("public static final com.ponysdk.core.event.Event.Type<" + GeneratorHelper.getHandlerClassName(event) + "> TYPE = new com.ponysdk.core.event.Event.Type<" + GeneratorHelper.getHandlerClassName(event)
                + ">();");

        // Build constructor
        final Parameter sourceComponentParameter = new Parameter();
        sourceComponentParameter.setName("sourceComponent");
        sourceComponentParameter.setClazz("java.lang.Object");
        final List<Parameter> superConstructorParameters = new ArrayList<Parameter>();
        superConstructorParameters.add(sourceComponentParameter);

        final List<Parameter> constructorParameters = new ArrayList<Parameter>(event.getParameter());
        constructorParameters.add(0, sourceComponentParameter);

        final Constructor constructor = new Constructor(constructorParameters, superConstructorParameters);
        classWriter.addConstructor(constructor);

        // Build methods
        classWriter.addLine("@Override");
        classWriter.addLine("protected void dispatch(" + GeneratorHelper.getHandlerClassName(event) + " handler) {");
        classWriter.addLine("   handler.on" + event.getName() + "(this);");
        classWriter.addLine("}");
        classWriter.addNewLine();

        classWriter.addLine("@Override");
        classWriter.addLine("public com.ponysdk.core.event.Event.Type<" + GeneratorHelper.getHandlerClassName(event) + "> getAssociatedType() {");
        classWriter.addLine("   return TYPE;");
        classWriter.addLine("}");

        // Adding
        classWriter.setGenerateGetter(true);
        classWriter.generateContentAndStore();
    }

    private void generateHandler(Event event) throws Exception {
        final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getEventPackage(domain), GeneratorHelper.getHandlerClassName(event));

        classWriter.setInterface(true);
        classWriter.addExtend("com.ponysdk.core.event.EventHandler");

        // Build event method
        classWriter.addLine("public void on" + event.getName() + "(" + GeneratorHelper.getEventClassName(event) + " event);");

        classWriter.generateContentAndStore();
    }

    private void generateEventsHandler() throws Exception {
        if (domain.getCrudevent() != null || (domain.getEvent() != null && !domain.getEvent().isEmpty())) {
            final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getEventPackage(domain), GeneratorHelper.getMasterHandlerClassName(domain));

            classWriter.setInterface(true);

            if (domain.getCrudevent() != null) {
                for (final String before : GeneratorHelper.getBeforeEventName()) {
                    classWriter.addExtend(before + domain.getName() + "Handler");
                }
                for (final String after : GeneratorHelper.getAfterEventName()) {
                    classWriter.addExtend(domain.getName() + after + "Handler");
                }
            }

            if (domain.getEvent() != null) {
                for (final Event event : domain.getEvent()) {
                    classWriter.addExtend(GeneratorHelper.getHandlerClassName(event));
                }
            }

            classWriter.generateContentAndStore();
        }
    }

    public String getSrcGeneratedDirectory() {
        return srcGeneratedDirectory;
    }

    public void setSrcGeneratedDirectory(String srcGeneratedDirectory) {
        this.srcGeneratedDirectory = srcGeneratedDirectory;
    }
}
