package alterbase;

import org.apache.commons.beanutils.PropertyUtils;
import play.Logger;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.data.binding.Binder;
import play.data.binding.NoBinding;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.db.Model;
import play.db.jpa.GenericModel;
import play.db.jpa.JPA;
import play.db.jpa.JPABase;
import play.exceptions.UnexpectedException;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlterBasePlugin extends PlayPlugin {

    private AlterBaseEnhancer findersEnhancer = new AlterBaseEnhancer();

    public void enhance(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
        findersEnhancer.enhanceThisClass(applicationClass);
    }

    @Override
    public Object bind(RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        if (Base.class.isAssignableFrom(clazz)) {
            ParamNode node = rootParamNode.getChild(name, true);
            ParamNode idNode = node.getChild("id");
            if(idNode!=null) {
                Base base = JPA.em().find((Class<Base>) clazz, Long.valueOf(idNode.getFirstValue(String.class)));
                // TODO: bind other param values to attributes of base object
                return base;
            }
        }
        return null;
    }


}
