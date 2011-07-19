package alterbase;

import javassist.CtClass;
import javassist.CtMethod;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

/**
 * Enhance Base entities classes
 */
public class AlterBaseEnhancer extends Enhancer {

    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get("alterbase.AlterBase"))) {
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
        CtMethod findById = CtMethod.make("public static alterbase.AlterBase findById(Object id) { return alterbase.jpa.JPQL.instance.findById(\"" + entityName + "\", id); }", ctClass);
        ctClass.addMethod(findById);

        // find
        CtMethod find = CtMethod.make("public static alterbase.AlterBase.JPAQuery find(String query, Object[] params) { return alterbase.jpa.JPQL.instance.find(\"" + entityName + "\", query, params); }", ctClass);
        ctClass.addMethod(find);

        // find
        CtMethod find2 = CtMethod.make("public static alterbase.AlterBase.JPAQuery find() { return alterbase.jpa.JPQL.instance.find(\"" + entityName + "\"); }", ctClass);
        ctClass.addMethod(find2);

        // all
        CtMethod all = CtMethod.make("public static alterbase.AlterBase.JPAQuery all() { return alterbase.jpa.JPQL.instance.all(\"" + entityName + "\"); }", ctClass);
        ctClass.addMethod(all);

        // delete
        CtMethod delete = CtMethod.make("public static int delete(String query, Object[] params) { return alterbase.jpa.JPQL.instance.delete(\"" + entityName + "\", query, params); }", ctClass);
        ctClass.addMethod(delete);

        // deleteAll
        CtMethod deleteAll = CtMethod.make("public static int deleteAll() { return alterbase.jpa.JPQL.instance.deleteAll(\"" + entityName + "\"); }", ctClass);
        ctClass.addMethod(deleteAll);

        // findOneBy
        CtMethod findOneBy = CtMethod.make("public static alterbase.AlterBase findOneBy(String query, Object[] params) { return alterbase.jpa.JPQL.instance.findOneBy(\"" + entityName + "\", query, params); }", ctClass);
        ctClass.addMethod(findOneBy);


        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

}
