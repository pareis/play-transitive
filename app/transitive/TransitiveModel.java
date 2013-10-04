package transitive;


import play.Logger;
import play.Play;
import play.data.binding.BeanWrapper;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@MappedSuperclass
public abstract class TransitiveModel extends GenericTransitiveModel {

    @Id
	@GeneratedValue()
	public Long id;


    public String toString() {
        return getClass().getSimpleName() + "#" + this.id + "";
    }


    @Override
    public void _save() {
        persist();
    }

    @Override
    public void _delete() {
        JPA.em().remove(this);
    }

    @Override
    public Object _key() {
        return id;
    }



    /**
     * This method is deprecated. Use this instead:
     *
     *  public static <T extends TransitiveModel> T create(ParamNode rootParamNode, String name, Class<?> type, Annotation[] annotations)
     */
    @Deprecated
    public static <T extends TransitiveModel> T create(Class<?> type, String name, Map<String, String[]> params, Annotation[] annotations) {
        ParamNode rootParamNode = ParamNode.convert(params);
        return (T)create(rootParamNode, name, type, annotations);
    }

    public static <T extends TransitiveModel> T create(ParamNode rootParamNode, String name, Class<?> type, Annotation[] annotations) {
        try {
            Constructor c = type.getDeclaredConstructor();
            c.setAccessible(true);
            TransitiveModel model = (TransitiveModel) c.newInstance();
            return (T) edit(rootParamNode, name, model, annotations);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method is deprecated. Use this instead:
     *
     *  public static <T extends TransitiveModel> T edit(ParamNode rootParamNode, String name, Object o, Annotation[] annotations)
     *
     * @return
     */
    @Deprecated
    public static <T extends TransitiveModel> T edit(Object o, String name, Map<String, String[]> params, Annotation[] annotations) {
        ParamNode rootParamNode = ParamNode.convert(params);
        return (T)edit( rootParamNode, name, o, annotations);
    }

    @SuppressWarnings("deprecation")
    public static <T extends TransitiveModel> T edit(ParamNode rootParamNode, String name, Object o, Annotation[] annotations) {
        ParamNode paramNode = rootParamNode.getChild(name, true);
        // #1195 - Needs to keep track of whick keys we remove so that we can restore it before
        // returning from this method.
        List<ParamNode.RemovedNode> removedNodesList = new ArrayList<ParamNode.RemovedNode>();
        try {
            BeanWrapper bw = new BeanWrapper(o.getClass());
            // Start with relations
            Set<Field> fields = new HashSet<Field>();
            Class clazz = o.getClass();
            Set<String> fieldNames = new HashSet<String>();
            while (!clazz.equals(Object.class)) {
                for(Field aField : clazz.getDeclaredFields()) {
                    if(!fieldNames.contains(aField.getName())) {
                        fields.add(aField);
                        fieldNames.add(aField.getName());
                    }
                }
                clazz = clazz.getSuperclass();
            }
            for (Field field : fields) {
                boolean isEntity = false;
                String relation = null;
                boolean multiple = false;
                String mappedBy = null;
                //
                if (field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(ManyToOne.class)) {
                    isEntity = true;
                    relation = field.getType().getName();
                }
                if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                    Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    isEntity = true;
                    relation = fieldType.getName();
                    multiple = true;
                }

                if(field.isAnnotationPresent(OneToMany.class)) {
                    mappedBy = field.getAnnotation(OneToMany.class).mappedBy();
                } else if(field.isAnnotationPresent(OneToOne.class)) {
                    mappedBy = field.getAnnotation(OneToOne.class).mappedBy();
                }

                mappedBy = mappedBy!=null && !mappedBy.trim().isEmpty() ? mappedBy.trim() : null;


                ParamNode fieldParamNode = paramNode.getChild(field.getName(), false);
                if (isEntity && fieldParamNode!=null) {
                    Class<TransitiveModel> c = (Class<TransitiveModel>) Play.classloader.loadClass(relation);
                    if (GenericTransitiveModel.class.isAssignableFrom(c)) {
                        String keyName = "id";
                        if (multiple && Collection.class.isAssignableFrom(field.getType())) {
                            Collection loaded = (Collection) field.get(o);
                            Collection elements = null;
                            boolean isInsertionOrder = false;

                            boolean needsInit = loaded==null;
                            if (SortedSet.class.isAssignableFrom(field.getType())) {
                                elements = new TreeSet();
                                if(needsInit) loaded = new TreeSet();
                            } else if (Set.class.isAssignableFrom(field.getType())) {
                                elements = new HashSet();
                                if(needsInit) loaded = new HashSet();
                            } else {
                                isInsertionOrder = true;
                                elements = new ArrayList();
                                if(needsInit) loaded = new ArrayList();
                            }
                            List<String> indexes = new ArrayList<String>(fieldParamNode.getAllChildrenKeys());
                            if(isInsertionOrder) {
                                Collections.sort(indexes);
                            }

                            for(String index : indexes) {
                                ParamNode indexParamNode = fieldParamNode.getChild(index, true);
                                String[] ids = indexParamNode.getChild(keyName, true).getValues();

                                if(ids!=null) {
                                    for (String _id : ids) {
                                        if (_id.equals("")) {
                                            continue;
                                        }
                                        Long idkey = (Long) Binder.directBind(rootParamNode.getOriginalKey(), annotations, _id, Long.class, null);
                                        if(idkey>0L) {
                                            Query q = JPA.em().createQuery("from " + relation + " where " + keyName + " = ?1");
                                            q.setParameter(1, idkey);
                                            try {
                                                TransitiveModel target = (TransitiveModel) q.getSingleResult();
                                                TransitiveModel.edit(fieldParamNode, index, target, field.getAnnotations());
                                                elements.add(target);
                                                if(mappedBy!=null) {
                                                    new BeanWrapper(target.getClass()).set(mappedBy, target, o);
                                                }

                                            } catch (NoResultException e) {
                                                Validation.addError(name + "." + field.getName(), "validation.notFound", _id);
                                            }
                                        } else {
                                            Logger.info("delete %s:%s", field.getName(), -idkey);
                                        }
                                    }
                                } else {
                                    TransitiveModel target = TransitiveModel.create(fieldParamNode, index, c, field.getAnnotations());
                                    Logger.info("created %s[%s] = %s", field.getName(), index, target);
                                    elements.add(target);
                                    if(mappedBy!=null) {
                                        new BeanWrapper(target.getClass()).set(mappedBy, target, o);
                                    }
                                }
                                // Remove it to prevent us from finding it again later
                                fieldParamNode.removeChild(index, removedNodesList);
                            }
                            if(needsInit) {
                                bw.set(field.getName(), o, elements);
                            } else {
                                List<Object> removables = new ArrayList<Object>();
                                removables.addAll(loaded);
                                if(isInsertionOrder) {
                                    loaded.clear();
                                    loaded.addAll(elements);
                                } else {
                                    loaded.addAll(elements);
                                    loaded.retainAll(elements);
                                }
                                removables.removeAll(loaded);
                            }
                        } else {

                            String[] ids = fieldParamNode.getChild(keyName, true).getValues();
                            if (ids != null && ids.length > 0 && !ids[0].equals("")) {

                                Query q = JPA.em().createQuery("from " + relation + " where " + keyName + " = ?1");
                                q.setParameter(1, Binder.directBind(rootParamNode.getOriginalKey(), annotations, ids[0], Long.class, null));
                                try {
                                    Object to = q.getSingleResult();
                                    edit(paramNode, field.getName(), to, field.getAnnotations());
                                    // Remove it to prevent us from finding it again later
                                    paramNode.removeChild( field.getName(), removedNodesList);
                                    bw.set(field.getName(), o, to);
                                } catch (NoResultException e) {
                                    Validation.addError(fieldParamNode.getOriginalKey(), "validation.notFound", ids[0]);
                                    // Remove only the key to prevent us from finding it again later
                                    // This how the old impl does it..
                                    fieldParamNode.removeChild(keyName, removedNodesList);
                                    if (fieldParamNode.getAllChildren().size()==0) {
                                        // remove the whole node..
                                        paramNode.removeChild( field.getName(), removedNodesList);
                                    }

                                }

                            } else if (ids != null && ids.length > 0 && ids[0].equals("")) {
                                bw.set(field.getName(), o, null);
                                // Remove the key to prevent us from finding it again later
                                fieldParamNode.removeChild(keyName, removedNodesList);
                            }
                        }
                    }
                }
            }
            ParamNode beanNode = rootParamNode.getChild(name, true);
            Binder.bindBean(beanNode, o, annotations);
            return (T) o;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        } finally {
            // restoring changes to paramNode
            ParamNode.restoreRemovedChildren( removedNodesList );
        }
    }



    public <T extends GenericTransitiveModel> T edit(ParamNode rootParamNode, String name) {
        edit(rootParamNode, name, this, null);
        return (T) this;
    }
}
