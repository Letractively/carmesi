/*
 */

package carmesi.internal;

import carmesi.Controller;
import carmesi.internal.dynamic.DynamicController;
import carmesi.HttpMethod;
import carmesi.URL;
import carmesi.RedirectTo;
import carmesi.internal.dynamic.DynamicControllerServlet;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author Victor
 */
public class TestRedirect {
    @Rule
    public RequestResponseMocker mocker=new RequestResponseMocker();
    
    @Test
    public void shouldRedirect() throws Exception{
        AbstractControllerServlet servlet=new DynamicControllerServlet(new SimpleRedirectController());
        ServletConfig servletConfig = mock(ServletConfig.class);
        servlet.init(servletConfig);
        when(mocker.getRequest().getMethod()).thenReturn("GET");
        when(mocker.getRequest().getContextPath()).thenReturn("/MyPath");
        servlet.service(mocker.getRequest(), mocker.getResponse());
        verify(mocker.getResponse()).sendRedirect("/MyPath/viewRedirect.jsp");
    }
    
    @Test
    public void shouldRedirectToo() throws Exception{
        AbstractControllerServlet servlet=new TypeSafeControllerServlet(new TypesafeRedirectController());
        ServletConfig servletConfig = mock(ServletConfig.class);
        servlet.init(servletConfig);
        when(mocker.getRequest().getMethod()).thenReturn("GET");
        when(mocker.getRequest().getContextPath()).thenReturn("/MyPath");
        servlet.service(mocker.getRequest(), mocker.getResponse());
        verify(mocker.getResponse()).sendRedirect("/MyPath/viewRedirect.jsp");
    }
    
    @URL("/any")
    @RedirectTo("/viewRedirect.jsp")
    public static class SimpleRedirectController{
        
        public void doAction(){
            
        }
        
    }
    
    @URL("/any")
    @RedirectTo("/viewRedirect.jsp")
    public static class TypesafeRedirectController implements  Controller{
        
        public void execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
         
        }
        
    }

}
