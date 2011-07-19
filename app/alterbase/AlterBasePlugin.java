package alterbase;

import play.PlayPlugin;
import play.classloading.ApplicationClasses;

public class AlterBasePlugin extends PlayPlugin {

    private AlterBaseEnhancer findersEnhancer = new AlterBaseEnhancer();

    public void enhance(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
        findersEnhancer.enhanceThisClass(applicationClass);
    }
}
