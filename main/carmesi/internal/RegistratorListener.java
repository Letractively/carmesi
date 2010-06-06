/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package carmesi.internal;

import carmesi.BeforeView;
import carmesi.Controller;
import carmesi.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.annotation.WebListener;
//import org.scannotation.AnnotationDB;
//import org.scannotation.WarUrlFinder;

/**
 *
 * @author Victor
 */
@WebListener
public class RegistratorListener implements ServletContextListener {
    public static final String CONFIG_FILE_PATH="META-INF/controllers.list";

    private ServletContext context;
    private CarmesiServlet carmesiServlet;
    private Dynamic dymanicServlet;
    private CarmesiFilter carmesiFilter;

    public void contextInitialized(ServletContextEvent sce) {
        try {
            //Controller with view
            context = sce.getServletContext();
            carmesiServlet = new CarmesiServlet();
            dymanicServlet = context.addServlet("Umbrella Servlet", carmesiServlet);

            //Controller before page
            carmesiFilter = new CarmesiFilter();
            FilterRegistration.Dynamic dynamicFilter = sce.getServletContext().addFilter("Umbrella Filter", carmesiFilter);
            EnumSet<DispatcherType> set = EnumSet.of(DispatcherType.REQUEST);
            dynamicFilter.addMappingForUrlPatterns(set, false, "/*");


//            AnnotationDB annotationDB = new AnnotationDB();
//            annotationDB.scanArchives(WarUrlFinder.findWebInfClassesPath(context));
//            Map<String, Set<String>> mapAnnotatedClasses = annotationDB.getAnnotationIndex();
//            if(mapAnnotatedClasses.containsKey(BeforeView.class.getName())){
//                for (String classname : mapAnnotatedClasses.get(BeforeView.class.getName())) {
//                    Class<?> klass = Class.forName(classname);
//                    try{
//                        Class<? extends Controller> subclass = klass.asSubclass(Controller.class);
//                        addControllerClass(subclass);
//                    }catch(ClassCastException ex){
//
//                    }
//                }
//            }
//            if(mapAnnotatedClasses.containsKey(RequestReceiver.class.getName())){
//                for (String classname : mapAnnotatedClasses.get(RequestReceiver.class.getName())) {
//                    Class<?> klass = Class.forName(classname);
//                    try{
//                        Class<? extends Controller> subclass = klass.asSubclass(Controller.class);
//                        addControllerClass(subclass);
//                    }catch(ClassCastException ex){
//
//                    }
//                }
//            }

//            System.out.println("map: "+mapAnnotatedClasses);
            scanForClasses();
            addClassesFromConfigFile();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void scanForClasses(){
        
    }

    private void addClassesFromConfigFile() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException{
        InputStream input;// = context.getResourceAsStream("META-INF/controllers.list");
        input=getClass().getResourceAsStream("/"+CONFIG_FILE_PATH);
        System.out.println("input: "+input);
        if (input != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("line: " + line);
                if (line.trim().startsWith("#")) {
                    continue;
                }
                Class<?> klass = Class.forName(line);
                try {
                    Class<? extends Controller> subclass = klass.asSubclass(Controller.class);
                    addControllerClass(subclass);
                } catch (ClassCastException ex) {
                }
            }
            reader.close();
        }
    }

    private void addControllerClass(Class<? extends Controller> klass) throws InstantiationException, IllegalAccessException {
        if (klass.isAnnotationPresent(BeforeView.class)) {
            addControlleBeforeRequest(klass);
        } else if (klass.isAnnotationPresent(URL.class)){
            addControllerToView(dymanicServlet, klass);
        }
    }

    private void addControllerToView(Dynamic dynamic, Class<? extends Controller> klass) throws IllegalAccessException, InstantiationException {
        System.out.println(klass);
        URL url = klass.getAnnotation(URL.class);
        System.out.println("info: " + url);
        carmesiServlet.addController(url.value(), klass.newInstance());
        dynamic.addMapping(url.value());
    }

    private void addControlleBeforeRequest(Class<? extends Controller> klass) throws InstantiationException, IllegalAccessException {
        BeforeView beforeRequest = klass.getAnnotation(BeforeView.class);
        carmesiFilter.addController(beforeRequest.value(), klass.newInstance());
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}
