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

    public ServiceGenerator(final Domain domain) {
        super(domain);
    }

    public void generate() throws Exception {
        generateCRUDMethos();
        generateCommands();
        generatePushCommands();
        generateService();
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

            // Create the implementation class of these CRUD methods if the HibernateDAO is set
            if (domain.getService().getDao() != null) {
                final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getServerServicePackage(domain), GeneratorHelper.getServiceImplClassName(domain));

                classWriter.addImplements(GeneratorHelper.getServiceClassName(domain));
                classWriter.setGenerateGetter(true);
                classWriter.setGenerateSetter(true);

                // Add static logger
                classWriter.addConstants("private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(" + GeneratorHelper.getServiceImplClassName(domain) + ".class);");

                // Add service HibernateDAO object
                final Parameter daoParameter = new Parameter();
                daoParameter.setClazz(GeneratorHelper.getDAOPackage(domain) + "." + GeneratorHelper.getDAOClassName(domain));
                daoParameter.setName(GeneratorHelper.getFirstCharToLower(GeneratorHelper.getDAOClassName(domain)));
                classWriter.addClassMembers(daoParameter);

                for (final Method method : domain.getService().getMethod()) {
                    final String resultClass = GeneratorHelper.getClassName(method.getReturn());

                    classWriter.addNewLine();
                    classWriter.addLine("@Override");
                    classWriter.addLine("public " + resultClass + " " + method.getName() + "(" + GeneratorHelper.getParameterToString(method.getParameter()) + ") throws Exception {");
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
                classWriter.addLine("public " + GeneratorHelper.getClassName(createMethod.getReturn()) + " " + createMethod.getName() + " (" + GeneratorHelper.getParameterToString(createMethod.getParameter()) + ")  throws Exception {");
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
                classWriter.addLine("public " + GeneratorHelper.getClassName(createMethod.getReturn()) + " " + readMethod.getName() + " (" + GeneratorHelper.getParameterToString(readMethod.getParameter()) + ")  throws Exception {");
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
                classWriter.addLine("public " + GeneratorHelper.getClassName(updateMethod.getReturn()) + " " + updateMethod.getName() + " (" + GeneratorHelper.getParameterToString(updateMethod.getParameter()) + ")  throws Exception {");
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
                classWriter.addLine("public " + GeneratorHelper.getClassName(deleteMethod.getReturn()) + " " + deleteMethod.getName() + " (" + GeneratorHelper.getParameterToString(deleteMethod.getParameter()) + ")  throws Exception {");
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

    private Method createCRUDMethod(final String name, final Parameter crudParameter, final Return crudReturn) {
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
        final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getServicePackage(domain), className);

        classWriter.setInterface(true);
        classWriter.addExtend("com.ponysdk.core.service.PonyService");

        if (domain.getService() != null) {
            for (final Method method : domain.getService().getMethod()) {
                final String returnClass = GeneratorHelper.getClassName(method.getReturn());
                classWriter.addLine("public " + returnClass + " " + method.getName() + "(" + GeneratorHelper.getParameterToString(method.getParameter()) + ") throws Exception;");
            }

            for (final Pushmethod method : domain.getService().getPushmethod()) {
                if (method.getParameter().isEmpty()) {
                    classWriter.addLine("public com.ponysdk.core.event.HandlerRegistration " + method.getName() + "Registration(com.ponysdk.core.command.PushListener<" + method.getType() + "> listener);");
                } else {
                    classWriter.addLine("public com.ponysdk.core.event.HandlerRegistration " + method.getName() + "Registration(" + GeneratorHelper.getParameterToString(method.getParameter()) + ", com.ponysdk.core.command.PushListener<"
                            + method.getType() + "> listener);");
                }
            }

            final Dao dao = domain.getService().getDao();
            if (dao != null) {
                switch (dao.getDaoLayer()) {
                    case HIBERNATE:
                        generateHibernateDAO(dao);
                        break;
                    case MONGODB:
                        generateMongoDBDAO(dao);
                        break;
                    default:
                        System.err.println(dao.getDaoLayer() + " HibernateDAO generation ignored for class" + domain.getService().getDao().getClazz() + ". Only " + DaoLayer.HIBERNATE + " is supported");
                }
            }
        }

        classWriter.generateContentAndStore();
    }

    /*
     * Services : HibernateDAO
     */

    private void generateHibernateDAO(final Dao dao) throws Exception {
        final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getDAOPackage(domain), GeneratorHelper.getDAOClassName(domain));

        classWriter.addExtend("com.ponysdk.hibernate.dao.HibernateDAO");

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
        classWriter.addLine("       " + dao.getClazz() + " instance = (" + dao.getClazz() + ") sessionFactory.getCurrentSession().get(" + dao.getClazz() + ".class, id);");
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
        classWriter.addLine("       final java.util.List<" + dao.getClazz() + "> results = sessionFactory.getCurrentSession().createQuery(\"FROM " + domain.getName() + "\").list();");
        classWriter.addLine("       return results;");
        classWriter.addLine("   } catch (final RuntimeException re) {");
        classWriter.addLine("       log.error(\"finding all " + domain.getName() + " failed\", re);");
        classWriter.addLine("       throw re;");
        classWriter.addLine("   }");
        classWriter.addLine("}");

        classWriter.generateContentAndStore();
    }

    /*
     * Services : MongoDBDAO
     */
    private void generateMongoDBDAO(final Dao dao) throws Exception {
        final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getDAOPackage(domain), GeneratorHelper.getDAOClassName(domain));

        classWriter.addImport("com.fasterxml.jackson.databind.ObjectMapper");
        classWriter.addImport("com.mongodb.BasicDBObject");
        classWriter.addImport("com.mongodb.DBCollection");
        classWriter.addImport("com.mongodb.DBObject");
        classWriter.addImport("com.mongodb.DBCursor");
        classWriter.addImport("java.util.List");
        classWriter.addImport("java.util.ArrayList");

        classWriter.addExtend("com.ponysdk.mongodb.dao.MongoDAO");

        // Add static logger
        classWriter.addConstants("private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(" + GeneratorHelper.getDAOClassName(domain) + ".class);");

        // Create constructor
        final List<Parameter> parameters = new ArrayList<Parameter>();
        final Parameter sessionFactoryParameter = new Parameter();
        sessionFactoryParameter.setName("sessionFactory");
        sessionFactoryParameter.setClazz("org.hibernate.SessionFactory");
        // parameters.add(sessionFactoryParameter);
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

        classWriter.addLine("       DBCollection collection = db.getCollection(\"" + domain.getName().toLowerCase() + "\");");
        classWriter.addLine("       BasicDBObject basicDBObject = new BasicDBObject(\"id\"," + "id);");
        classWriter.addLine("       final DBObject foundInstance = collection.findOne(basicDBObject);");
        classWriter.addLine("       " + dao.getClazz() + " instance = null;");
        classWriter.addLine("       return toModel(foundInstance);");
        classWriter.addLine("   } catch (final Exception re) {");
        classWriter.addLine("       log.error(\"getting " + domain.getName() + " by id failed\", re);");
        classWriter.addLine("       throw new RuntimeException(re);");
        classWriter.addLine("   }");
        classWriter.addLine("}");

        classWriter.addLine("private " + dao.getClazz() + " toModel(DBObject dbObject) {");
        classWriter.addLine("   if (dbObject == null) return null;");
        classWriter.addLine("   final ObjectMapper mapper = new ObjectMapper();");
        classWriter.addLine("   try{");
        classWriter.addLine("       " + dao.getClazz() + " model = mapper.readValue(dbObject.toString(), " + dao.getClazz() + ".class);");
        classWriter.addLine("       model.setID(dbObject.get(\"_id\"));");
        classWriter.addLine("       return model;");
        classWriter.addLine("   } catch (final Exception e) {");
        classWriter.addLine("       log.error(\"toModel " + domain.getName() + " failed\", e);");
        classWriter.addLine("       throw new RuntimeException(e);");
        classWriter.addLine("   }");
        classWriter.addLine("}");

        // Create findAll method
        classWriter.addNewLine();
        classWriter.addLine("@SuppressWarnings(\"unchecked\")");
        classWriter.addLine("public java.util.List<" + dao.getClazz() + "> findAll() {");
        classWriter.addLine("   if (log.isDebugEnabled()) {");
        classWriter.addLine("       log.debug(\"finding all " + domain.getName() + "\");");
        classWriter.addLine("   }");
        classWriter.addNewLine();
        classWriter.addLine("   DBCursor cursor = null;");
        classWriter.addLine("   try {");
        classWriter.addLine("       DBCollection collection = db.getCollection(\"" + domain.getName().toLowerCase() + "\");");
        classWriter.addLine("       cursor = collection.find();");
        classWriter.addLine("       List<" + dao.getClazz() + "> result = new ArrayList<" + dao.getClazz() + ">();");
        classWriter.addLine("       while(cursor.hasNext()){");
        classWriter.addLine("           result.add(toModel(cursor.next()));");
        classWriter.addLine("       }");
        classWriter.addLine("       return result;");
        classWriter.addLine("   } catch (final RuntimeException re) {");
        classWriter.addLine("       log.error(\"finding all " + domain.getName() + " failed\", re);");
        classWriter.addLine("       throw re;");
        classWriter.addLine("   } finally{");
        classWriter.addLine("       cursor.close();");
        classWriter.addLine("   }");
        classWriter.addLine("}");

        // Create find by query method
        classWriter.addNewLine();
        classWriter.addLine("@SuppressWarnings(\"unchecked\")");
        classWriter.addLine("@Override");
        classWriter.addLine("public java.util.List<" + dao.getClazz() + "> find(Object query) {");
        classWriter.addLine("   if (log.isDebugEnabled()) {");
        classWriter.addLine("       log.debug(\"finding " + domain.getName() + "  by query\");");
        classWriter.addLine("   }");
        classWriter.addNewLine();
        classWriter.addLine("   DBCursor cursor = null;");
        classWriter.addLine("   try {");
        classWriter.addLine("       DBCollection collection = db.getCollection(\"" + domain.getName().toLowerCase() + "\");");
        classWriter.addLine("       cursor = collection.find((DBObject)query);");
        classWriter.addLine("       List<" + dao.getClazz() + "> result = new ArrayList<" + dao.getClazz() + ">();");
        classWriter.addLine("       while(cursor.hasNext()){");
        classWriter.addLine("           result.add(toModel(cursor.next()));");
        classWriter.addLine("       }");
        classWriter.addLine("       return result;");
        classWriter.addLine("   } catch (final RuntimeException re) {");
        classWriter.addLine("       log.error(\"find " + domain.getName() + " failed\", re);");
        classWriter.addLine("       throw re;");
        classWriter.addLine("   } finally{");
        classWriter.addLine("       cursor.close();");
        classWriter.addLine("   }");
        classWriter.addLine("}");

        classWriter.generateContentAndStore();
    }

    /*
     * Services : commands
     */

    private void generatePushCommands() throws Exception {
        if (domain.getService() != null) {
            for (final Pushmethod method : domain.getService().getPushmethod()) {
                final ClassWriter classWriter = generatePushCommandX(method, method.getType());
                classWriter.addExtend("com.ponysdk.core.command.AbstractPushCommand<" + method.getType() + ">");
                classWriter.generateContentAndStore();
            }
        }
    }

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

    private ClassWriter generatePushCommandX(final Pushmethod method, final String type) throws Exception {
        final String className = method.getName().substring(0, 1).toUpperCase() + method.getName().substring(1) + "Command";
        final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getCommandPackage(domain), className);

        final Parameter pushListener = new Parameter();
        pushListener.setName("listener");
        pushListener.setClazz("com.ponysdk.core.command.PushListener<" + type + ">");

        final List<Parameter> parameters = method.getParameter();

        final List<Parameter> clonedParameters = new ArrayList<Parameter>(parameters);
        clonedParameters.add(pushListener);

        final Constructor constructor = new Constructor();
        constructor.setConstructorParameters(clonedParameters);
        constructor.setSuperConstructorParameters(Arrays.asList(pushListener));

        classWriter.addConstructor(constructor);

        for (final Parameter param : parameters) {
            classWriter.addClassMembers(param);
        }
        classWriter.addConstants("private static " + GeneratorHelper.getServiceFullClassName(domain) + " service;");

        final StringBuilder template = new StringBuilder();
        template.append("@Override\n");
        template.append("public com.ponysdk.core.event.HandlerRegistration execute(){\n");
        template.append("   if (service == null) {\n");
        template.append("       service = com.ponysdk.core.service.PonyServiceRegistry.getPonyService(" + GeneratorHelper.getServiceFullClassName(domain) + ".class);");
        template.append("   }\n");
        if (method.getParameter().isEmpty()) template.append("   return service.%1$sRegistration(this);\n");
        else template.append("   return service.%1$sRegistration(%2$s,this);\n");
        template.append("}\n");

        classWriter.addMethod(template.toString(), method.getName(), GeneratorHelper.getParameterNamesToString(method.getParameter()));

        return classWriter;
    }

    private ClassWriter generateCommandX(final Method method, final String resultClass) throws Exception {
        final String className = method.getName().substring(0, 1).toUpperCase() + method.getName().substring(1) + "Command";

        final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getCommandPackage(domain), className);

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
        classWriter.addConstants("private static " + GeneratorHelper.getServiceFullClassName(domain) + " service;");

        final StringBuilder template = new StringBuilder();
        template.append("@Override\n");
        if (resultClass.equals("void") && method.getReturn().getValue() == null) {
            template.append("protected java.lang.Void  execute0() throws Exception {\n");
        } else {
            template.append("protected " + resultClass + " execute0() throws Exception {\n");
        }
        template.append("   if (service == null) {\n");
        template.append("       service = com.ponysdk.core.service.PonyServiceRegistry.getPonyService(" + GeneratorHelper.getServiceFullClassName(domain) + ".class);");
        template.append("   }\n");
        if (resultClass.equals("void") && method.getReturn().getValue() == null) {
            template.append("	service.%1$s(%2$s);\n");
            template.append("	return null;\n");
        } else {
            template.append("	return service.%1$s(%2$s);\n");
        }
        template.append("}\n");

        classWriter.addMethod(template.toString(), method.getName(), GeneratorHelper.getParameterNamesToString(method.getParameter()));

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

    private void generateEvent(final Event event) throws Exception {
        final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getEventPackage(domain), GeneratorHelper.getEventClassName(event));

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

    private void generateHandler(final Event event) throws Exception {
        final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getEventPackage(domain), GeneratorHelper.getHandlerClassName(event));

        classWriter.setInterface(true);
        classWriter.addExtend("com.ponysdk.core.event.EventHandler");

        // Build event method
        classWriter.addLine("public void on" + event.getName() + "(" + GeneratorHelper.getEventClassName(event) + " event);");

        classWriter.generateContentAndStore();
    }

    private void generateEventsHandler() throws Exception {
        if (domain.getCrudevent() != null || (domain.getEvent() != null && !domain.getEvent().isEmpty())) {
            final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), GeneratorHelper.getEventPackage(domain), GeneratorHelper.getMasterHandlerClassName(domain));

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

    public void setSrcGeneratedDirectory(final String srcGeneratedDirectory) {
        this.srcGeneratedDirectory = srcGeneratedDirectory;
    }
}
