/* Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0) */

package carmesi.internal.simplecontrollers;

import carmesi.convert.TargetInfo;
import carmesi.convert.Converter;
import carmesi.convert.DateConverter;
import carmesi.RequestParameter;
import carmesi.RequestAttribute;
import carmesi.ContextParameter;
import carmesi.RequestBean;
import carmesi.ApplicationAttribute;
import carmesi.Controller;
import carmesi.SessionAttribute;
import carmesi.CookieValue;
import carmesi.ToJSON;
import carmesi.convert.ConverterException;
import carmesi.jsonserializers.JSONSerializer;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Wraps simple annotated POJOS in order to be used as a Controller. Its function is injecting parameters and processing the return value .
 * 
 * @author Victor Hugo Herrera Maldonado
 */
public class SimpleControllerWrapper implements Controller{
    private Object simpleController;
    private Method method;
    private Map<Class, Converter> converters=new ConcurrentHashMap<Class, Converter>();
    private boolean autoRequestAttribute=true;
    private int defaultCookieMaxAge=-1;
    private JSONSerializer jsonSerializer;
    
    private SimpleControllerWrapper(Object simpleController, Method m){
        assert simpleController != null;
        assert m != null;
        this.simpleController=simpleController;
        method=m;
        addConverter(Date.class, new DateConverter());
    }
    
    /**
     * 
     * @param <T>
     * @param klass
     * @param converter
     * @throws NullPointerException if class or converter is null
     */
    public final <T> void addConverter(Class<T> klass, Converter<T> converter) throws NullPointerException {
        if(klass == null){
            throw new NullPointerException("class is null");
        }
        if(converter == null){
            throw new NullPointerException("converter is null");
        }
        converters.put(klass, converter);
    }
    
    /**
     * 
     * @param <T>
     * @param klass
     * @throws NullPointerException if klass is null
     * @return 
     */
    public <T> Converter<T> getConverter(Class<T> klass) throws NullPointerException {
        return converters.get(klass);
    }
    
    public Map<Class, Converter> getConverters(){
        return new HashMap<Class, Converter>(converters);
    }

    public boolean isAutoRequestAttribute() {
        return autoRequestAttribute;
    }

    public void setAutoRequestAttribute(boolean autoRequestAttribute) {
        this.autoRequestAttribute = autoRequestAttribute;
    }

    public int getDefaultCookieMaxAge() {
        return defaultCookieMaxAge;
    }

    /**
     * 
     * @param defaultCookieMaxAge 
     * @see Cookie.setMaxAge
     */
    public void setDefaultCookieMaxAge(int defaultCookieMaxAge) {
        this.defaultCookieMaxAge = defaultCookieMaxAge;
    }

    public JSONSerializer getJSONSerializer() {
        return jsonSerializer;
    }

    /**
     * 
     * @param jsonSerializer
     * @throws NullPointerException if jsonSerializer is null
     */
    public void setJSONSerializer(JSONSerializer jsonSerializer) throws  NullPointerException{
        if (jsonSerializer == null) {
            throw new NullPointerException("jsonSerializer is null");
        }
        this.jsonSerializer = jsonSerializer;
    }
    
    /**
     * Executes the controller.
     * 
     * @param request
     * @param response
     * @throws NullPointerException if request or response is null
     * @throws Exception is an exception occurs when invoking the pojo controller.
     */
    public void execute(HttpServletRequest request, HttpServletResponse response) throws NullPointerException, Exception {
        executeAndGetResult(request, response);
    }
    
    /**
     * Executes the controller.
     * 
     * @param request
     * @param response
     * @throws NullPointerException if request or response is null
     * @throws Exception is an exception occurs when invoking the pojo controller
     * @return Result the result of the execution.
     */
    public Result executeAndGetResult(HttpServletRequest request, HttpServletResponse response) throws NullPointerException, Exception {
        if(request == null){
            throw new NullPointerException("request is null");
        }
        if(response == null){
            throw new NullPointerException("response is null");
        }
        Result result=execute(new ExecutionContext(request, response));
        result.process();
        return result;
    }

    
    private Result execute(ExecutionContext context) throws IllegalAccessException, InvocationTargetException, InstantiationException, IntrospectionException {
        assert context != null;
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] actualParameters=new Object[parameterTypes.length];
        
        /* Iterates each parameter */
        for(int i=0; i < parameterTypes.length; i++){
            actualParameters[i]=getActualParameter(new TargetInfo(parameterTypes[i], parameterAnnotations[i]), context);
        }
        return new Result(method.invoke(simpleController, actualParameters), method.getReturnType().equals(Void.TYPE), context);
    }

    private Object getActualParameter(TargetInfo parameterInfo, ExecutionContext context) throws InstantiationException, IllegalAccessException, IntrospectionException, InvocationTargetException {
        assert parameterInfo != null;
        assert context != null;
        /* Definir por tipos */
        if(parameterInfo.getType().equals(ServletContext.class)){
            return context.getServletContext();
        }else if(ServletRequest.class.isAssignableFrom(parameterInfo.getType())){
            return context.getRequest();
        }else if(ServletResponse.class.isAssignableFrom(parameterInfo.getType())){
            return context.getResponse();
        }else if(HttpSession.class.isAssignableFrom(parameterInfo.getType())){
            return context.getRequest().getSession();
        }else{
            /* Definir por anotaciones */
            if(parameterInfo.isAnnotationPresent(RequestBean.class)){
                return fillBeanWithParameters(parameterInfo.getType().newInstance(), context.getRequest());
            }
            if(parameterInfo.isAnnotationPresent(RequestParameter.class)){
                RequestParameter requestParameter=parameterInfo.getAnnotation(RequestParameter.class);
                if(parameterInfo.getType().isArray()){
                    String[] parameterValues = context.getRequest().getParameterValues(requestParameter.value());
                    Object array=null;
                    if(parameterValues != null){
                        array=asArray(parameterValues, parameterInfo);
                    }
                    return array;
                }else{
                    return convertStringToType(context.getRequest().getParameter(requestParameter.value()), parameterInfo);
                }
            }
            if(parameterInfo.isAnnotationPresent(RequestAttribute.class)){
                return context.getRequest().getAttribute(parameterInfo.getAnnotation(RequestAttribute.class).value());
            }
            if(parameterInfo.isAnnotationPresent(SessionAttribute.class)){
                return context.getRequest().getSession().getAttribute(parameterInfo.getAnnotation(SessionAttribute.class).value());
            }
            if(parameterInfo.isAnnotationPresent(ApplicationAttribute.class)){
                return context.getServletContext().getAttribute(parameterInfo.getAnnotation(ApplicationAttribute.class).value());
            }
            if(parameterInfo.isAnnotationPresent(ContextParameter.class)){
                return convertStringToType(context.getServletContext().getInitParameter(parameterInfo.getAnnotation(ContextParameter.class).value()), parameterInfo);
            }
            if(parameterInfo.isAnnotationPresent(CookieValue.class)){
                Cookie[] cookies = context.getRequest().getCookies();
                String string=null;
                if(cookies != null){
                    for(Cookie c:cookies){
                        if(c.getName().equals(parameterInfo.getAnnotation(CookieValue.class).value())){
                            string=c.getValue();
                        }
                    }
                }
                return convertStringToType(string, parameterInfo);
            }
            return null;
        }
    }
    
    private Object asArray(String[] stringValues, TargetInfo parameterInfo){
        assert stringValues != null;
        assert parameterInfo != null;
        Object array = Array.newInstance(parameterInfo.getType().getComponentType(), stringValues.length);
        for(int i=0; i < stringValues.length; i++){
            Array.set(array, i, convertStringToType(stringValues[i], new TargetInfo(parameterInfo.getType().getComponentType(), parameterInfo.getAnnotations())));
        }
        return array;
    }
    
    private Object convertStringToType(String string, TargetInfo parameterInfo){
        assert string != null;
        assert parameterInfo != null;
        Class<?> targetType=parameterInfo.getType();
        Converter<?> converter = converters.get(parameterInfo.getType());
        if(string == null){
            return null;
        }else if(targetType.equals(byte.class) || targetType.equals(Byte.class)){
            return Byte.valueOf(string);
        }else if(targetType.equals(short.class) || targetType.equals(Short.class)){
            return Short.valueOf(string);
        }else if(targetType.equals(int.class) || targetType.equals(Integer.class)){
            return Integer.valueOf(string);
        }else if(targetType.equals(long.class) || targetType.equals(Long.class)){
            return Long.valueOf(string);
        }else if(targetType.equals(char.class) || targetType.equals(Character.class)){
            if(string.length() != -1){
                throw new IllegalArgumentException("Invalid number of characters: "+string.length());
            }else{
                return Character.valueOf(string.charAt(0));
            }
        }else if(targetType.equals(BigDecimal.class)){
            return new BigDecimal(string);
        }else if(targetType.equals(boolean.class) || targetType.equals(Boolean.class)){
            return Boolean.valueOf(string);
        }else if(targetType.equals(String.class)){
            return string;
        }else if(converter != null){
            try{
                return converter.convertToObject(string, parameterInfo);
            }catch(ConverterException ex){
                throw new RuntimeException(ex);
            }
        }else{
            Method[] methods = targetType.getDeclaredMethods();
            for(Method m: methods){
                if(m.getName().equals("valueOf") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0].equals(String.class)
                        && m.getReturnType().equals(targetType) && Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())){
                    try {
                        return m.invoke(null, string);
                    } catch (IllegalAccessException ex) {
                        throw new AssertionError(ex);
                    } catch (IllegalArgumentException ex) {
                        throw new AssertionError(ex);
                    } catch (InvocationTargetException ex) {
                        throw new AssertionError(ex);
                    }
                }
            }
        }
        return null;
    }
    
    private Object fillBeanWithParameters(Object object, HttpServletRequest request) throws IntrospectionException, IllegalAccessException, InvocationTargetException{
        assert object != null;
        assert request != null;
        BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        if(propertyDescriptors != null){
            for(PropertyDescriptor descriptor: propertyDescriptors){
               String[] parameterValues = request.getParameterValues(descriptor.getName());
               if(parameterValues != null){
                   Object propertyValue;
                   if(descriptor.getPropertyType().isArray()){
                       propertyValue=asArray(parameterValues, new TargetInfo(descriptor.getPropertyType(), descriptor.getPropertyType().getAnnotations()));
                   }else{
                       propertyValue=convertStringToType(parameterValues[0], new TargetInfo(descriptor.getPropertyType(), descriptor.getPropertyType().getAnnotations()));
                   }
                   descriptor.getWriteMethod().invoke(object, propertyValue);
               }
            }
        }
        return object;
    }
    
    /**
     * Create an instance of SimpleControllerWrapper with the specified POJO controller.
     * 
     * @param <T>
     * @param simpleController A POJO for using it as a Controller.
     * @return
     * @throws NullPointerException if object is null.
     */
    public static <T> SimpleControllerWrapper createInstance(Object simpleController) throws  NullPointerException{
        if(simpleController == null){
            throw new NullPointerException("controller object is null");
        }
        List<Method> methods=new LinkedList<Method>();
        for(Method method:simpleController.getClass().getDeclaredMethods()){
            if(Modifier.isPublic(method.getModifiers()) && !method.isAnnotationPresent(PostConstruct.class) && !method.isAnnotationPresent(PreDestroy.class)){
                methods.add(method);
            }
        }
        if(methods.size() != 1){
            throw new IllegalArgumentException("Controller must have one and only one public method.");
        }
        return new SimpleControllerWrapper(simpleController, methods.get(0));
    }

    public class Result{
        private Object value;
        private boolean isVoid;
        private ExecutionContext executionContext;

        private Result(Object v, boolean b, ExecutionContext context) {
            assert context != null;
            value=v;
            isVoid=b;
            executionContext=context;
        }

        /**
         * The value of the result.
         * 
         * @return
         */
        public Object getValue() {
            return value;
        }
        
        private Pattern getterPattern=Pattern.compile("get(.+)");

        private void process() throws IOException {
            if(isVoid){
                return;
            }else{
                if(method.isAnnotationPresent(RequestAttribute.class)){
                    executionContext.getRequest().setAttribute(method.getAnnotation(RequestAttribute.class).value(), value);
                }else if(method.isAnnotationPresent(SessionAttribute.class)){
                    executionContext.getRequest().getSession().setAttribute(method.getAnnotation(SessionAttribute.class).value(), value);
                }else if(method.isAnnotationPresent(ApplicationAttribute.class)){
                    executionContext.getServletContext().setAttribute(method.getAnnotation(ApplicationAttribute.class).value(), value);
                }else if(method.isAnnotationPresent(CookieValue.class)){
                    Converter<Object> converter=value != null? getConverter((Class<Object>)value.getClass()): null;
                    String stringValue;
                    if(converter != null){
                        try{
                            TargetInfo targetInfo = new TargetInfo(method.getReturnType(), method.getAnnotations());
                            stringValue=converter.convertToString(value, targetInfo);
                        }catch(ConverterException ex){
                            throw new RuntimeException(ex);
                        }
                    }else{
                        stringValue=String.valueOf(value);
                    }
                    Cookie cookie=new Cookie(method.getAnnotation(CookieValue.class).value(), stringValue);
                    cookie.setMaxAge(defaultCookieMaxAge);
                    executionContext.getResponse().addCookie(cookie);
                }else if(value instanceof Cookie){
                    executionContext.getResponse().addCookie((Cookie) value);
                }else if(method.isAnnotationPresent(ToJSON.class)){
                    if(jsonSerializer != null){
                        executionContext.getResponse().getWriter().println(jsonSerializer.serialize(value));
                    }
                }else{
                    if(autoRequestAttribute){
                        Matcher matcher = getterPattern.matcher(method.getName());
                        if(matcher.matches()){
                            StringBuilder builder=new StringBuilder(matcher.group(1));
                            builder.setCharAt(0, Character.toLowerCase(builder.charAt(0)));
                            executionContext.getRequest().setAttribute(builder.toString(), value);
                        }
                    }
                }
            }
        }
        
    }

}