package transitive;

import javassist.CtClass;
import javassist.CtMethod;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.enhancers.Enhancer;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.db.jpa.JPA;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class TransitivePlugin extends PlayPlugin {

    private Enhancer findersEnhancer = new Enhancer() {
        public void enhanceThisClass(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
            CtClass ctClass = makeClass(applicationClass);

            if (!ctClass.subtypeOf(classPool.get("alterbase.GenericTransitiveModel"))) {
                return;
            }

            // Enhance only JPA entities
            if (!hasAnnotation(ctClass, "javax.persistence.Entity")) {
                return;
            }

            String entityName = ctClass.getName();

            // count
            CtMethod count = CtMethod.make("public static long count() { return alterbase.jpa.JPQL.instance.count(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(count);

            // count2
            CtMethod count2 = CtMethod.make("public static long count(String query, Object[] params) { return alterbase.jpa.JPQL.instance.count(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(count2);

            // findAll
            CtMethod findAll = CtMethod.make("public static java.util.List findAll() { return alterbase.jpa.JPQL.instance.findAll(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(findAll);

            // findById
            CtMethod findById = CtMethod.make("public static alterbase.GenericTransitiveModel findById(Object id) { return alterbase.jpa.JPQL.instance.findById(\"" + entityName + "\", id); }", ctClass);
            ctClass.addMethod(findById);

            // find
            CtMethod find = CtMethod.make("public static alterbase.GenericTransitiveModel.JPAQuery find(String query, Object[] params) { return alterbase.jpa.JPQL.instance.find(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(find);

            // find
            CtMethod find2 = CtMethod.make("public static alterbase.GenericTransitiveModel.JPAQuery find() { return alterbase.jpa.JPQL.instance.find(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(find2);

            // all
            CtMethod all = CtMethod.make("public static alterbase.GenericTransitiveModel.JPAQuery all() { return alterbase.jpa.JPQL.instance.all(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(all);

            // delete
            CtMethod delete = CtMethod.make("public static int delete(String query, Object[] params) { return alterbase.jpa.JPQL.instance.delete(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(delete);

            // deleteAll
            CtMethod deleteAll = CtMethod.make("public static int deleteAll() { return alterbase.jpa.JPQL.instance.deleteAll(\"" + entityName + "\"); }", ctClass);
            ctClass.addMethod(deleteAll);

            // findOneBy
            CtMethod findOneBy = CtMethod.make("public static alterbase.GenericTransitiveModel findOneBy(String query, Object[] params) { return alterbase.jpa.JPQL.instance.findOneBy(\"" + entityName + "\", query, params); }", ctClass);
            ctClass.addMethod(findOneBy);


            // Done.
            applicationClass.enhancedByteCode = ctClass.toBytecode();
            ctClass.defrost();
        }
    };

    public void enhance(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
        findersEnhancer.enhanceThisClass(applicationClass);
    }

    @Override
    public Object bind(RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        if (TransitiveModel.class.isAssignableFrom(clazz)) {
            ParamNode node = rootParamNode.getChild(name, true);
            ParamNode idNode = node.getChild("id");
            if(idNode!=null) {
                TransitiveModel transitiveModel = JPA.em().find((Class<TransitiveModel>) clazz, Long.valueOf(idNode.getFirstValue(String.class)));
                // TODO: bind other param values to attributes of transitiveModel object
                return transitiveModel;
            }
        }
        return null;
    }


}
