package transitive;

import javassist.CtClass;
import javassist.CtMethod;
import org.apache.commons.beanutils.PropertyUtils;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.enhancers.Enhancer;
import play.data.binding.Binder;
import play.data.binding.NoBinding;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.db.Model;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransitivePlugin extends PlayPlugin {

    private Enhancer findersEnhancer = new Enhancer() {
        public void enhanceThisClass(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
            CtClass ctClass = makeClass(applicationClass);

            if (!ctClass.subtypeOf(classPool.get("transitive.GenericTransitiveModel"))) {
                return;
            }

            // Enhance only JPA entities
            if (!hasAnnotation(ctClass, "javax.persistence.Entity")) {
                return;
            }

            String entityName = ctClass.getName();

            // count
            CtMethod count = CtMethod.make("public static long count() { return transitive.jpa.JPQL.instance.count(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(count);

            // count2
            CtMethod count2 = CtMethod.make("public static long count(String query, Object[] params) { return transitive.jpa.JPQL.instance.count(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(count2);

            // findAll
            CtMethod findAll = CtMethod.make("public static java.util.List findAll() { return transitive.jpa.JPQL.instance.findAll(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(findAll);

            // findById
            CtMethod findById = CtMethod.make("public static transitive.GenericTransitiveModel findById(Object id) { return transitive.jpa.JPQL.instance.findById(\"" + entityName + "\", id); }", ctClass);
            ctClass.addMethod(findById);

            // find
            CtMethod find = CtMethod.make("public static transitive.GenericTransitiveModel.JPAQuery find(String query, Object[] params) { return transitive.jpa.JPQL.instance.find(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(find);

            // find
            CtMethod find2 = CtMethod.make("public static transitive.GenericTransitiveModel.JPAQuery find() { return transitive.jpa.JPQL.instance.find(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(find2);

            // all
            CtMethod all = CtMethod.make("public static transitive.GenericTransitiveModel.JPAQuery all() { return transitive.jpa.JPQL.instance.all(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(all);

            // delete
            CtMethod delete = CtMethod.make("public static int delete(String query, Object[] params) { return transitive.jpa.JPQL.instance.delete(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(delete);

            // deleteAll
            CtMethod deleteAll = CtMethod.make("public static int deleteAll() { return transitive.jpa.JPQL.instance.deleteAll(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(deleteAll);

            // findOneBy
            CtMethod findOneBy = CtMethod.make("public static transitive.GenericTransitiveModel findOneBy(String query, Object[] params) { return transitive.jpa.JPQL.instance.findOneBy(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(findOneBy);


            // Done.
            applicationClass.enhancedByteCode = ctClass.toBytecode();
            ctClass.defrost();
        }
    };

    public void enhance(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
        findersEnhancer.enhanceThisClass(applicationClass);
    }

//    @Override
//    public Object bind(RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
//        if (TransitiveModel.class.isAssignableFrom(clazz)) {
//            ParamNode node = rootParamNode.getChild(name, true);
//            ParamNode idNode = node.getChild("id");
//            if(idNode!=null) {
//                TransitiveModel transitiveModel = JPA.em().find((Class<TransitiveModel>) clazz, Long.valueOf(idNode.getFirstValue(String.class)));
//                // TODO: bind other param values to attributes of transitiveModel object
//                return transitiveModel;
//            }
//        }
//        return null;
//    }



    @Override
    public Object bind(RootParamNode rootParamNode, String name, Class clazz, java.lang.reflect.Type type, Annotation[] annotations) {
        // TODO need to be more generic in order to work with JPASupport
        if (TransitiveModel.class.isAssignableFrom(clazz)) {

            ParamNode paramNode = rootParamNode.getChild(name, true);

            String[] keyNames = new JPAModelLoader(clazz).keyNames();
            ParamNode[] ids = new ParamNode[keyNames.length];
            // Collect the matching ids
            int i = 0;
            for (String keyName : keyNames) {
                ids[i++] = paramNode.getChild(keyName, true);
            }
            if (ids != null && ids.length > 0) {
                try {
                    EntityManager em = JPA.em();
                    StringBuilder q = new StringBuilder().append("from ").append(clazz.getName()).append(" o where");
                    int keyIdx = 1;
                    for (String keyName : keyNames) {
                        q.append(" o.").append(keyName).append(" = ?").append(keyIdx++).append(" and ");
                    }
                    if (q.length() > 4) {
                        q = q.delete(q.length() - 4, q.length());
                    }
                    Query query = em.createQuery(q.toString());
                    // The primary key can be a composite.
                    Class[] pk = new JPAModelLoader(clazz).keyTypes();
                    int j = 0;
                    for (ParamNode id : ids) {
                        if (id.getValues() == null || id.getValues().length == 0 || id.getFirstValue(null)== null || id.getFirstValue(null).trim().length() <= 0 ) {
                            // We have no ids, it is a new entity
                            return TransitiveModel.create(rootParamNode, name, clazz, annotations);
                        }
                        query.setParameter(j + 1, Binder.directBind(id.getOriginalKey(), annotations, id.getValues()[0], pk[j++], null));

                    }
                    Object o = query.getSingleResult();
                    return TransitiveModel.edit(rootParamNode, name, o, annotations);
                } catch (NoResultException e) {
                    // ok
                } catch(RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
            return TransitiveModel.create(rootParamNode, name, clazz, annotations);
        }
        return null;
    }

    @Override
    public Object bindBean(RootParamNode rootParamNode, String name, Object bean) {
        if (bean instanceof TransitiveModel) {
            return TransitiveModel.edit(rootParamNode, name, bean, null);
        }
        return null;
    }




    public static class JPAModelLoader implements play.db.Model.Factory {

        private Class<? extends play.db.Model> clazz;
        private Map<String, Model.Property> properties;


        public JPAModelLoader(Class<? extends play.db.Model> clazz) {
            this.clazz = clazz;
        }

        public play.db.Model findById(Object id) {
            try {
                if (id == null) {
                    return null;
                }
                return JPA.em().find(clazz, id);
            } catch (Exception e) {
                // Key is invalid, thus nothing was found
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public List<Model> fetch(int offset, int size, String orderBy, String order, List<String> searchFields, String keywords, String where) {
            String q = "from " + clazz.getName();
            if (keywords != null && !keywords.equals("")) {
                String searchQuery = getSearchQuery(searchFields);
                if (!searchQuery.equals("")) {
                    q += " where (" + searchQuery + ")";
                }
                q += (where != null ? " and " + where : "");
            } else {
                q += (where != null ? " where " + where : "");
            }
            if (orderBy == null && order == null) {
                orderBy = "id";
                order = "ASC";
            }
            if (orderBy == null && order != null) {
                orderBy = "id";
            }
            if (order == null || (!order.equals("ASC") && !order.equals("DESC"))) {
                order = "ASC";
            }
            q += " order by " + orderBy + " " + order;
            Query query = JPA.em().createQuery(q);
            if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
                query.setParameter(1, "%" + keywords.toLowerCase() + "%");
            }
            query.setFirstResult(offset);
            query.setMaxResults(size);
            return query.getResultList();
        }

        public Long count(List<String> searchFields, String keywords, String where) {
            String q = "select count(*) from " + clazz.getName() + " e";
            if (keywords != null && !keywords.equals("")) {
                String searchQuery = getSearchQuery(searchFields);
                if (!searchQuery.equals("")) {
                    q += " where (" + searchQuery + ")";
                }
                q += (where != null ? " and " + where : "");
            } else {
                q += (where != null ? " where " + where : "");
            }
            Query query = JPA.em().createQuery(q);
            if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
                query.setParameter(1, "%" + keywords.toLowerCase() + "%");
            }
            return Long.decode(query.getSingleResult().toString());
        }

        public void deleteAll() {
            JPA.em().createQuery("delete from " + clazz.getName()).executeUpdate();
        }

        public List<play.db.Model.Property> listProperties() {
            List<play.db.Model.Property> properties = new ArrayList<Model.Property>();
            Set<Field> fields = new LinkedHashSet<Field>();
            Class<?> tclazz = clazz;
            while (!tclazz.equals(Object.class)) {
                Collections.addAll(fields, tclazz.getDeclaredFields());
                tclazz = tclazz.getSuperclass();
            }
            for (Field f : fields) {
                if (Modifier.isTransient(f.getModifiers())) {
                    continue;
                }
                if (f.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                if (f.isAnnotationPresent(NoBinding.class)) {
                    NoBinding a = f.getAnnotation(NoBinding.class);
                    List<String> values = Arrays.asList(a.value());
                    if (values.contains("*")) {
                        continue;
                    }
                }
                play.db.Model.Property mp = buildProperty(f);
                if (mp != null) {
                    properties.add(mp);
                }
            }
            return properties;
        }

        public String keyName() {
            return keyField().getName();
        }

        public Class<?> keyType() {
            return keyField().getType();
        }

        public Class<?>[] keyTypes() {
            Field[] fields = keyFields();
            Class<?>[] types = new Class<?>[fields.length];
            int i = 0;
            for (Field field : fields) {
                types[i++] = field.getType();
            }
            return types;
        }

        public String[] keyNames() {
            Field[] fields = keyFields();
            String[] names = new String[fields.length];
            int i = 0;
            for (Field field : fields) {
                names[i++] = field.getName();
            }
            return names;
        }

        private Class<?> getCompositeKeyClass() {
            Class<?> tclazz = clazz;
            while (!tclazz.equals(Object.class)) {
                // Only consider mapped types
                if (tclazz.isAnnotationPresent(Entity.class)
                        || tclazz.isAnnotationPresent(MappedSuperclass.class)) {
                    IdClass idClass = tclazz.getAnnotation(IdClass.class);
                    if (idClass != null)
                        return idClass.value();
                }
                tclazz = tclazz.getSuperclass();
            }
            throw new UnexpectedException("Invalid mapping for class " + clazz + ": multiple IDs with no @IdClass annotation");
        }


        private void initProperties() {
            synchronized(this){
                if(properties != null)
                    return;
                properties = new HashMap<String, Model.Property>();
                Set<Field> fields = getModelFields(clazz);
                for (Field f : fields) {
                    if (Modifier.isTransient(f.getModifiers())) {
                        continue;
                    }
                    if (f.isAnnotationPresent(Transient.class)) {
                        continue;
                    }
                    play.db.Model.Property mp = buildProperty(f);
                    if (mp != null) {
                        properties.put(mp.name, mp);
                    }
                }
            }
        }

        private Object makeCompositeKey(play.db.Model model) throws Exception {
            initProperties();
            Class<?> idClass = getCompositeKeyClass();
            Object id = idClass.newInstance();
            PropertyDescriptor[] idProperties = PropertyUtils.getPropertyDescriptors(idClass);
            if(idProperties == null || idProperties.length == 0)
                throw new UnexpectedException("Composite id has no properties: "+idClass.getName());
            for (PropertyDescriptor idProperty : idProperties) {
                // do we have a field for this?
                String idPropertyName = idProperty.getName();
                // skip the "class" property...
                if(idPropertyName.equals("class"))
                    continue;
                play.db.Model.Property modelProperty = this.properties.get(idPropertyName);
                if(modelProperty == null)
                    throw new UnexpectedException("Composite id property missing: "+clazz.getName()+"."+idPropertyName
                            +" (defined in IdClass "+idClass.getName()+")");
                // sanity check
                Object value = modelProperty.field.get(model);

                if(modelProperty.isMultiple)
                    throw new UnexpectedException("Composite id property cannot be multiple: "+clazz.getName()+"."+idPropertyName);
                // now is this property a relation? if yes then we must use its ID in the key (as per specs)
                if(modelProperty.isRelation){
                    // get its id
                    if(!play.db.Model.class.isAssignableFrom(modelProperty.type))
                        throw new UnexpectedException("Composite id property entity has to be a subclass of Model: "
                                +clazz.getName()+"."+idPropertyName);
                    // we already checked that cast above
                    @SuppressWarnings("unchecked")
                    play.db.Model.Factory factory = play.db.Model.Manager.factoryFor((Class<? extends play.db.Model>) modelProperty.type);
                    if(factory == null)
                        throw new UnexpectedException("Failed to find factory for Composite id property entity: "
                                +clazz.getName()+"."+idPropertyName);
                    // we already checked that cast above
                    if(value != null)
                        value = factory.keyValue((play.db.Model) value);
                }
                // now affect the composite id with this id
                PropertyUtils.setSimpleProperty(id, idPropertyName, value);
            }
            return id;
        }



        public Object keyValue(play.db.Model m) {
            try {
                if (m == null) {
                    return null;
                }

                // Do we have a @IdClass or @Embeddable?
                if (m.getClass().isAnnotationPresent(IdClass.class)) {
                    return makeCompositeKey(m);
                }

                // Is it a composite key? If yes we need to return the matching PK
                final Field[] fields = keyFields();
                final Object[] values = new Object[fields.length];
                int i = 0;
                for (Field f : fields) {
                    final Object o = f.get(m);
                    if (o != null) {
                        values[i++] = o;
                    }
                }

                // If we have only one id return it
                if (values.length == 1) {
                    return values[0];
                }

                return values;
            } catch (Exception ex) {
                throw new UnexpectedException(ex);
            }
        }

        public static Set<Field> getModelFields(Class<?> clazz){
            Set<Field> fields = new LinkedHashSet<Field>();
            Class<?> tclazz = clazz;
            while (!tclazz.equals(Object.class)) {
                // Only add fields for mapped types
                if(tclazz.isAnnotationPresent(Entity.class)
                        || tclazz.isAnnotationPresent(MappedSuperclass.class))
                    Collections.addAll(fields, tclazz.getDeclaredFields());
                tclazz = tclazz.getSuperclass();
            }
            return fields;
        }

        //
        Field keyField() {
            Class c = clazz;
            try {
                while (!c.equals(Object.class)) {
                    for (Field field : c.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                            field.setAccessible(true);
                            return field;
                        }
                    }
                    c = c.getSuperclass();
                }
            } catch (Exception e) {
                throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz);
            }
            throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
        }

        Field[] keyFields() {
            Class c = clazz;
            try {
                List<Field> fields = new ArrayList<Field>();
                while (!c.equals(Object.class)) {
                    for (Field field : c.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                            field.setAccessible(true);
                            fields.add(field);
                        }
                    }
                    c = c.getSuperclass();
                }
                final Field[] f = fields.toArray(new Field[fields.size()]);
                if (f.length == 0) {
                    throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
                }
                return f;
            } catch (Exception e) {
                throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz);
            }
        }

        String getSearchQuery(List<String> searchFields) {
            String q = "";
            for (play.db.Model.Property property : listProperties()) {
                if (property.isSearchable && (searchFields == null || searchFields.isEmpty() ? true : searchFields.contains(property.name))) {
                    if (!q.equals("")) {
                        q += " or ";
                    }
                    q += "lower(" + property.name + ") like ?1";
                }
            }
            return q;
        }

        play.db.Model.Property buildProperty(final Field field) {
            play.db.Model.Property modelProperty = new play.db.Model.Property();
            modelProperty.type = field.getType();
            modelProperty.field = field;
            if (play.db.Model.class.isAssignableFrom(field.getType())) {
                if (field.isAnnotationPresent(OneToOne.class)) {
                    if (field.getAnnotation(OneToOne.class).mappedBy().equals("")) {
                        modelProperty.isRelation = true;
                        modelProperty.relationType = field.getType();
                        modelProperty.choices = new play.db.Model.Choices() {

                            @SuppressWarnings("unchecked")
                            public List<Object> list() {
                                return JPA.em().createQuery("from " + field.getType().getName()).getResultList();
                            }
                        };
                    }
                }
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    modelProperty.isRelation = true;
                    modelProperty.relationType = field.getType();
                    modelProperty.choices = new play.db.Model.Choices() {

                        @SuppressWarnings("unchecked")
                        public List<Object> list() {
                            return JPA.em().createQuery("from " + field.getType().getName()).getResultList();
                        }
                    };
                }
            }
            if (Collection.class.isAssignableFrom(field.getType())) {
                final Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                if (field.isAnnotationPresent(OneToMany.class)) {
                    if (field.getAnnotation(OneToMany.class).mappedBy().equals("")) {
                        modelProperty.isRelation = true;
                        modelProperty.isMultiple = true;
                        modelProperty.relationType = fieldType;
                        modelProperty.choices = new play.db.Model.Choices() {

                            @SuppressWarnings("unchecked")
                            public List<Object> list() {
                                return JPA.em().createQuery("from " + fieldType.getName()).getResultList();
                            }
                        };
                    }
                }
                if (field.isAnnotationPresent(ManyToMany.class)) {
                    if (field.getAnnotation(ManyToMany.class).mappedBy().equals("")) {
                        modelProperty.isRelation = true;
                        modelProperty.isMultiple = true;
                        modelProperty.relationType = fieldType;
                        modelProperty.choices = new play.db.Model.Choices() {

                            @SuppressWarnings("unchecked")
                            public List<Object> list() {
                                return JPA.em().createQuery("from " + fieldType.getName()).getResultList();
                            }
                        };
                    }
                }
            }
            if (field.getType().isEnum()) {
                modelProperty.choices = new play.db.Model.Choices() {

                    @SuppressWarnings("unchecked")
                    public List<Object> list() {
                        return (List<Object>) Arrays.asList(field.getType().getEnumConstants());
                    }
                };
            }
            modelProperty.name = field.getName();
            if (field.getType().equals(String.class)) {
                modelProperty.isSearchable = true;
            }
            if (field.isAnnotationPresent(GeneratedValue.class)) {
                modelProperty.isGenerated = true;
            }
            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                // Look if the target is an embeddable class
                if (field.getType().isAnnotationPresent(Embeddable.class) || field.getType().isAnnotationPresent(IdClass.class) ) {
                    modelProperty.isRelation = true;
                    modelProperty.relationType =  field.getType();
                }
            }
            return modelProperty;
        }
    }

}
