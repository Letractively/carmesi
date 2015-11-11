#Tutorial en NetBeans
# Carmesí en NetBeans #

Para este tutorial se elaborará una aplicación de manejo de pendientes (Todo's). La aplicación permitirá agregar, eliminar y listar las actividades pendientes desde una misma pantalla. Para simplificar el desarrollo y enfocarnos en Carmesí, la lista de pendientes estará funcionando en memoria por lo que al detener el servidor, los datos serán limpiados.

Siguiendo los principios del patrón Modelo Vista Controlador, iniciaremos con la capa de modelo que  contiene la lógica de negocio, a continuación con la vista que estará conformada de una página jsp y  archivos de recursos necesarios (CSS); y se concluirá con la capa de control en la que utilizaremos Carmesí.

El enlace al archivo del proyecto se incluye al final de este tutorial.

## Requisitos ##
  * Java SE 6.
  * Un contenedor de servlet o servidor de aplicaciones con soporte de Servlet 3.0 (Glassfish 3.x, Tomcat 7.x, JBoss 6) ya que Carmesí está hecha para aprovechar las nuevas funciones de esta especificación.
  * Archivo jar de Carmesí (http://code.google.com/p/carmesi/downloads/list,  el archivo jar se encuentra dentro del archivo zip).
  * Última versión estable de NetBeans.

## Creación de proyecto ##
Por conveniencia,  incluiremos las clases de modelo junto con la vista y el controlador en el mismo proyecto pero en diferentes paquetes. En proyectos reales, se podría tener un proyecto exclusivamente para el modelo; y otro para la vista y el controlador.

Crear el proyecto en NetBeans.
  1. Seleccionar File > New Project y elegir tipo de proyecto Web.
> > ![http://wiki.carmesi.googlecode.com/hg/images/new-project.png](http://wiki.carmesi.googlecode.com/hg/images/new-project.png)

  1. Definir el nombre de proyecto como ActividadesPendientes y dar click en Next.
> > ![http://wiki.carmesi.googlecode.com/hg/images/name-project.png](http://wiki.carmesi.googlecode.com/hg/images/name-project.png)

  1. Ahora debemos definir que la versión de JEE que utilizaremos es la 6 y seleccionar uno de los servidores que soporten Servlet 3.0.
> > ![http://wiki.carmesi.googlecode.com/hg/images/server-jee-version.png](http://wiki.carmesi.googlecode.com/hg/images/server-jee-version.png)

  1. Hacer click en Finish.

## Modelo ##
El modelo es la parte central de la aplicación. Contiene la lógica de negocio y es independiente de la interfaz de usuario.

Será necesaria una clase que representará a una actividad pendiente. Además de una propiedad para la descripción de la actividad, agregaremos una propiedad de tipo java.util.Date donde colocaremos la fecha de creación de la actividad y un id (ambos serán asignados automáticamente al momento de agregar una actividad).

Para crear la clase hacer click en el nodo Source Packages > New > Java Class. El nombre de la clase es ActividadPendiente y el paquete es modelo.

```
package modelo;

import java.util.Date;

public class ActividadPendiente {
    private String id;
    private String descripcion;
    private Date fechaCreacion;
    
    /* Getters y setters de la clase */
    
    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
}
```

La lista de actividades se implementará en otra clase que tendrá métodos para agregar, recuperar y eliminar actividades. Nombraremos esta clase como RegistroActividades. Si bien podríamos usar un mapa directamente, la ventaja que nos representa esta clase es que encapsulamos la forma en que se guardan los objetos (en una aplicación real suele ser una BD, aunque podría también ser un archivo xml o incluso un archivo binario usando serialización). Para nuestro caso, internamente utilizaremos un mapa para el almacenamiento cuya llave sea el id de la actividad y el valor sea la actividad misma. Como deseamos utilizar la misma instancia para todas las operaciones la haremos un singleton (si no fuera un Singleton, tendríamos que crear una instancia en cada controlador y cada instancia tendría sus propios datos, otra opción podría ser definir atributo de contexto para que el objeto sea compartido; por razones de simplicidad optaremos por el singleton).

```
package modelo;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegistroActividades {
    private Map<String, ActividadPendiente> mapaActividades=new 
            HashMap<String, ActividadPendiente>();
    private int siguienteId=0;
    
    private RegistroActividades(){}
    
    public void agregar(ActividadPendiente actividad){
        /* Para el id de la actividad usamos un contador que se incrementa
         * cada vez q se agrega una actividad (cuando se remueve una actividad
         * el contador no se modifica). Este contador es convertido a una cadena
         * y se asigna al id */
        actividad.setId(String.valueOf(siguienteId));
        siguienteId++;
        
        /* Se establece la fecha cuando se agrega el objeto. */
        actividad.setFechaCreacion(new Date());
        mapaActividades.put(actividad.getId(), actividad);
    }
    
    public boolean remover(String idActividad){
        /* Indicar si el objeto existia antes de intentar removerse. */
        return mapaActividades.remove(idActividad) != null;
    }
    
    public ActividadPendiente[] getActividades(){
        return mapaActividades.values().toArray(new ActividadPendiente[0]);
    }
    
    
    /* Variable y método para implementar el singleton. */
    private static RegistroActividades registro=new RegistroActividades();
    
    public static RegistroActividades getRegistroActividades(){
        return registro;
    }

}
```

## Vista ##

Ahora procedemos a implementar la vista que se conforma de una página desde la que el usuario podrá realizar el manejo de las actividades pendientes.
Este es el diseño inicial del que partiremos para la vista:

> ![http://wiki.carmesi.googlecode.com/hg/images/proposed-design.png](http://wiki.carmesi.googlecode.com/hg/images/proposed-design.png)


La página contiene un formulario para la creación de actividades pendientes y una lista para mostrar las actividades que ya han sido agregadas. Para cada actividad listada,  colocaremos una enlace que permita eliminarla.

El nombre del jsp será actividades y se encontrará dentro del directorio WEB-INF en el nodo Web Pages. El código del jsp es el siguiente:

```
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Actividades Pendientes</title>
        <link rel="stylesheet" type="text/css" href="css/actividades.css"/>
    </head>
    <body>
        <h1>Activades pendientes</h1>
        <form action="agregarActividad" method="post">
            <label for="descripcion">Descripci&oacute;n</label>
            <input type="text" name="descripcion" size="80"/>
            <button type="submit">Agregar actividad</button>
        </form>
        <h2>Actuales:</h2>
        <ul>
            <c:forEach var="actividad" 
                       items="${requestScope.actividadesPendientes}" 
                       varStatus="status">
                <li class="${status.index mod 2 eq 0 ? 'impar': 'par'}">
                    ${actividad.descripcion} 
                    <a href="eliminarActividad?id=${actividad.id}">Eliminar</a>
                </li>
            </c:forEach>
        </ul>
    </body>
</html>
```


La definición visual de la página (layout y colores) se encuentra en el archivo css/actividades.css y se encuentra enlazado en nuestro jsp.  Sobre el diseño inicial, se han agregado declaraciones al archivo de estilos para volverlo un poco más atractivo (el código se encuentra en el archivo del proyecto).

> ![http://wiki.carmesi.googlecode.com/hg/images/web-files.png](http://wiki.carmesi.googlecode.com/hg/images/web-files.png)

## Controlador ##

Ahora implementaremos los controladores que unirán el modelo con la vista. Es en esta parte que utilizaremos Carmesí.

Primero necesitamos agregar el jar de Carmesí a nuestro proyecto.

  1. Hacer click derecho sobre el nodo libraries de nuestro proyecto y seleccionar Add JAR/Folder.
> > ![http://wiki.carmesi.googlecode.com/hg/images/libraries.png](http://wiki.carmesi.googlecode.com/hg/images/libraries.png)

  1. A continuación seleccionar el jar de Carmesí previamente descargado (y extraido del zip).
> > ![http://wiki.carmesi.googlecode.com/hg/images/select-jar.png](http://wiki.carmesi.googlecode.com/hg/images/select-jar.png)

  1. Debemos asegurarnos que el procesamiento de anotaciones durante la compilación está habilitado (hacer click derecho en el proyecto, seleccionar Properties, en el cuadro de dialogo seleccionar el modo Build/Compiling) y asegurarse se que está marcada la casilla Enable Annotation Processing.
> > ![http://wiki.carmesi.googlecode.com/hg/images/project-annotations.png](http://wiki.carmesi.googlecode.com/hg/images/project-annotations.png)




Comenzaremos con el controlador para recuperar la lista de actividades. Esta lista se colocará en un atributo de la solicitud para que la vista pueda accederla. El url del controlador será index.html, la vista es la página que realizamos (WEB-INF/actividades.jsp) y el atributo de solicitud lo llamaremos actividadesPendientes.

```
package controlador;
  
import carmesi.ForwardTo;
import carmesi.RequestAttribute;
import carmesi.URL;
import modelo.ActividadPendiente;
import modelo.RegistroActividades;
 
@URL("/index.html")
@ForwardTo("/WEB-INF/actividades.jsp")
public class ControladorPendientes {
    
    //Las actividades se establecen en este atributo.
    @RequestAttribute("actividadesPendientes") 
    public ActividadPendiente[] getActividades(){
        return RegistroActividades.getRegistroActividades().getActividades();
    }

}
```

Debemos notar que la página jsp no puede ser solicitada directamente desde el navegador por estar en el directorio WEB-INF, será a través de la url de este controlador que el usuario desplegará esta pantalla.

El siguiente controlador será encargado de agregar una actividad.

```
package controlador;

import carmesi.RedirectTo;  
import carmesi.RequestBean;
import carmesi.URL;
import modelo.ActividadPendiente;
import modelo.RegistroActividades;

@URL("/agregarActividad")
@RedirectTo("/index.html")
public class ControladorAgregarActividad {
    
    /* El formulario envia un parametro con la descripcion el cual es 
       establecido en este bean. El nombre del parametro debe coincidir con un
       nombre de propiedad del bean, en caso contrario es ignorado. */
    public void agregarActividad(@RequestBean ActividadPendiente actividad){
        RegistroActividades.getRegistroActividades().agregar(actividad);
    }

}
```


El URL es el declarado en el atributo action del formulario en la página actividades.jsp.  El método recibe un objeto de tipo ActividadPendiente mapeado de los parámetros del formulario.

Después de agregar, redirigimos a la página index.html (la ruta del anterior controlador). Cuando la lista de actividades se despliegue, contendrá la actividad recién agregada.

Nota: Para la anotación RedirectTo, si el valor inicia con una diagonal, Carmesí agrega la ruta del contexto.

Por último, procedamos a crear el controlador para eliminar una actividad.

```
package controlador;

import carmesi.RedirectTo;   
import carmesi.RequestParameter;  
import carmesi.URL; 
import modelo.RegistroActividades; 

@URL("/eliminarActividad")
@RedirectTo("/index.html")
public class ControladorEliminarActividad {
    
    //El id se recupera del parametro de la solicitud con el mismo nombre.
    public void eliminarActividad(@RequestParameter("id") String id){
        RegistroActividades.getRegistroActividades().remover(id);
    }

}
```

Notemos que el id es mapeado del parámetro de solicitud que definimos en el enlace para eliminar una actividad.

## Compilación y ejecución de la aplicación ##

Ya que tenemos todo listo, procedamos a compilar la aplicación. Cuando se hace la compilación de los controladores, el procesador de anotaciones definido en Carmesí elabora una lista de  las clases controlador que hemos creado. Esta lista es necesaria para registrar automáticamente los controladores al iniciar la aplicación, por lo que es necesario que verifiquemos que se haya generado correctamente.

En la barra de herramientas de NetBeans hacer click en el botón Clean & Build (Shift + F11, en mi computadora). En la vista por archivos del proyecto desplegar el archivo indicado en la siguiente pantalla y verificar que contenga los nombres de las tres clases de los controladores.


> ![http://wiki.carmesi.googlecode.com/hg/images/controller-list.png](http://wiki.carmesi.googlecode.com/hg/images/controller-list.png)


Una vez comprobada la lista, ejecutemos la aplicación (en el menu contextual del proyecto, seleccionar Run). Desde una ventana o pestaña del navegador de internet acceder a  htpp://localhost:8080/ActividadesPendientes/index.html (El puerto puede variar de acuerdo a la configuración local del contenedor de servlets o servidor de aplicaciones, por lo regular es 8080 u 8084).

En el navegador aparecerá la vista.  En este momento, como no hemos dado de alta ninguna actividad, la lista estará vacía.

> <img src='http://wiki.carmesi.googlecode.com/hg/images/pendientes-vacio.png' alt='' />


### Agregar actividades ###

En el campo de descripción del formulario introducimos Hacer respaldo de datos y hacemos click en Agregar.  A continuación el formulario será enviado a la aplicación y nos redireccionará nuevamente a nuestra página en la cual se mostrará la lista que ahora contiene una actividad.

<img src='http://wiki.carmesi.googlecode.com/hg/images/pendientes-agregar.png' alt='' />

Agregar ahora dos actividades más: Depurar archivos xml y Realizar pruebas de unidad.

<img src='http://wiki.carmesi.googlecode.com/hg/images/pendientes-agregar2.png' alt='' />


### Eliminar una actividad ###

Para dar por terminado el ejercicio, hagamos click en el enlace Eliminar de cualquiera de las actividades listadas.

<img src='http://wiki.carmesi.googlecode.com/hg/images/pendientes-eliminar.png' alt='' />

## Conclusión ##

Hemos utilizado Carmesí para implementar la parte de control de una  aplicación web. Hemos visto cómo definir la url de acceso de un controlador, cómo enlazar un controlador con una vista, el mapeo de valores y el mapeo de beans.

## Recursos ##

  * El archivo del proyecto en NetBeans puede ser descargado [aquí](http://carmesi.googlecode.com/files/ActividadesPendientes.zip).
  * Para más información de Carmesí, consultar http://code.google.com/p/carmesi/.