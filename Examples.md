

# Carmesi Examples #

## Injection by type ##

Parameter injected by type.

### Controller ###
```
@URL("/lucky") // The url that is used to invoke this controller. 
@ForwardTo("/lucky.jsp") // The view
public class LuckyNumberGenerator {
    private Random random=new Random();
    
    /**
     * Automatic injected parameter. Automatic injected types are HttpServletRequest, 
     * HttpServletResponse, ServletContext, HttpSession.
     */
    public void sayHello(HttpServletRequest request){
        /* Request attribute that is accesed in the JSP view. */
        request.setAttribute("luckyNumber", random.nextInt(100));
    }

}
```

### View ###
```
<%-- lucky.jsp --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Lucky number</title>
    </head>
    <body>
        <h1>Lucky number is ${luckyNumber}</h1>
    </body>
</html>
```

## Injecting request parameters ##

In this example a request parameter is injected. If present, the value of parameter `number` is converted to `int` and passed to the method.

Here, the annotation `RequestParameter` is used.

### Controller ###
```
@URL("/sqrt")
@ForwardTo("/sqrt.jsp")
public class SqrtController {
    
    public void calculate(@RequestParameter("number") int number, HttpServletRequest request){
        request.setAttribute("result", Math.sqrt(number));
    }

}
```

### View ###
```
<%--- sqrt.jsp ---%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Sqrt</title>
    </head>
    <body>
        <h1>Square root is ${result}</h1>
    </body>
</html>
```

## Injecting attributes ##

This examples shows the injection of an attribute in one of the scopes (Context scope in this case).

### Setting the application attribute ###
```
@WebListener
public class SimpleContextListener implements ServletContextListener{
    

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("startTime", new Date());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        
    }

}
```

### Controller ###

To inject an attribute we can use `ApplicationAttribute`, `SessionAttribute` or `RequestAttribute`.

```
@URL("/uptime")
@ForwardTo("/uptime.jsp")
public class UptimeController {
    
    public void getUptime(@ApplicationAttribute("startTime") Date startTime, HttpServletRequest request){
        long time=System.currentTimeMillis()-startTime.getTime();
        int minutes=(int)(time/60000);
        request.setAttribute("uptime", minutes);
    }

}
```

### View ###
```
<%-- uptime.jsp --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Uptime</title>
    </head>
    <body>
        <h1>Uptime in minutes: ${uptime}</h1>
    </body>
</html>
```

## Injecting cookies ##

A cookie can be injected with the annotation `CookieValue`. In this example, a cookie is used to display a custom theme to the user.

### Controller ###
```
@BeforeURL("/welcome.jsp")
public class ThemeController {
    
    @SessionAttribute("theme")
    public String getCssResource(@CookieValue("theme") String themeName){
        return "themes/"+(themeName!= null ? themeName: "default")+"/theme.css";
    }
    
}
```

### View ###
```
<!-- welcome.jsp -->
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Welcome</title>
        <link rel="stylesheet" type="text/css" href="${sessionScope.theme}"/>
    </head>
    <body>
        <h1>Hi! how are you?</h1>
    </body>
</html>
```

## Injecting a bean from request parameters ##

Example of mapping parameters to an object. When the `RequestBean` annotation is used, the parameters of the request are set
> in the bean if a property with the same parameter name exists.

### Bean ###
```
public class Person {
    private String firstName;
    private String lastName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return "Person{" + "firstName=" + firstName + ", lastName=" + lastName + '}';
    }
    
}
```

### Controller ###
```
@URL("/register")
@RedirectTo("/registered.jsp")
public class RegistrationController {
    
    public void registerPerson(@RequestBean Person person){
        System.out.println("person: "+person);
        /* person would be registered*/
    }

}
```

### View ###
```
<%-- registered.jsp --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Registration</title>
    </head>
    <body>
        <h1>User was registered. Thank you!</h1>
    </body>
</html>
```

## Setting the return value into an attribute ##

You can use one of the scope attribute annotations (`ApplicationAttribute`, `SessionAttribute`, `RequestAttribute`) in the method and the return value will be set on the specified scope.

This controller is an alternative implementation of the previous sqrt calculator, the view is the same page.

### Controller ###
```
@URL("/sqrt2")
@ForwardTo("/sqrt.jsp")
public class SqrtController2 {
    
    @RequestAttribute("result")
    public double getSqrt(@RequestParameter("number") int number){
        return Math.sqrt(number);
    }

}
```

### View ###
```
<%--- sqrt.jsp ---%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Sqrt</title>
    </head>
    <body>
        <h1>Square root is ${result}</h1>
    </body>
</html>
```

## Add a cookie to the response ##

If the return value of the controller is an instance of javax.servlet.http.Cookie, is added automatically to the response.

This is a complement to the functionality of the example in [Injecting cookies](Examples#Injecting_cookies.md).

### Controller ###
```
@URL("/changeTheme")
@RedirectTo("/welcome.jsp")
public class ChangeTheme {
    
    public Cookie change(@RequestAttribute("theme") String themeName){
        Cookie cookie=new Cookie("theme", themeName);
        cookie.setMaxAge(3600*24*30);
        return cookie;
    }
    
}
```

Please see [Adding cookies to the response](CarmesiGuide#Adding_cookies_to_the_response.md), for more information about adding cookies to the response.

## Generating a JSON response ##

We can annotate the method with `ToJSON` to return a JSON response to the client.

### Controller ###
```
@URL("/person")
public class GetPersonController {
    
    @ToJSON
    public Person getPerson(@RequestParameter("id") int id){
        Person person=new Person();
        person.setFirstName("Victor");
        person.setLastName("Herrera");
        return person;
    }

}
```

See [Generating a JSON response](CarmesiGuide#Generating_a_JSON_response.md), for more information.

## Using BeforeURL ##

This is an example of BeforeURL. When the user request _mails.jsp_, this controller is executed before the page is served to set the mails in a request attribute.
If we'd remove this controller, the _mails.jsp_ would still be served but the mails attribute would be empty.

### Controller ###
```
@BeforeURL("/mails.jsp")
public class MailRetriever {
    
    @RequestAttribute("mails")
    public Mail[] getMails(@SessionAttribute("user") String user){
        /* Sample mails for the specified user*/
        return new Mail[]{new Mail("admin@mail.example.com", "Welcome!", new Date()), 
                          new Mail("user22@mail.example.com", "Issue #23", new Date()),
                          new Mail("friend@mail.example.com", "Invitation", new Date())};
    }
    
    public static class Mail{
        private String from;
        private String subject;
        private Date date;

        public Mail(String from, String subject, Date date) {
            this.from = from;
            this.subject = subject;
            this.date = date;
        }

        public Date getDate() {
            return date;
        }

        public String getFrom() {
            return from;
        }

        public String getSubject() {
            return subject;
        }
        
    }

}
```

### View ###
```
<%-- mails.jsp --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Mails</title>
    </head>
    <body>
        <table>
            <thead>
                <tr>
                    <th>From</th>
                    <th>Subject</th>
                    <th>Date</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="mail" items="${mails}">
                    <tr>
                        <td>${mail.from}</td>
                        <td>${mail.subject}</td>
                        <td>${mail.date}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </body>
</html>
```

## Restricting HTTP methods ##

We can restrict the HTTP methods allowed for invoking a controller with the annotation `AllowedHttpMethods`.

The example is a variation of register controller used in [Injecting a bean from request parameters](Examples#Injecting_a_bean_from_request_parameters.md)

### Controller ###
```
@URL("/registerWithPost")
@AllowedHttpMethods(HttpMethod.POST)
@RedirectTo("/registered.jsp")
public class RestrictedRegistrationController {
    
    public void registerPerson(@RequestBean Person person){
        System.out.println("person: "+person);
        /* person would be registered*/
    }
    
}
```

## CDI ##

In this example, we use CDI to inject a bean into the controller. Notice that the bean has an application scope because
there is just one instance of the controller. Controller instances are created when the servlet context is started and are disposed
when the context is destroyed.

### Named Bean to inject ###
```
@Named
@ApplicationScoped
public class NotificationService {
    
    public void sendNotification(){
        //make notification
    }
    
}
```

### Controller ###
```
@URL("/notify")
@RedirectTo("/notificationResult.jsp")
public class NotificationController {
    @Inject //We use CDI.
    private NotificationService service;
    
    public void sendNotification(){
        service.sendNotification();
    }
    
}
```

### View ###
```
<!-- notificationResult.jsp -->
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Notification</title>
    </head>
    <body>
        <h1>Success!</h1>
    </body>
</html>
```