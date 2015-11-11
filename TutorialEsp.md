# Tutorial #

En este pequeño tutorial se presentan dos ejemplos del uso de Carmesí. En el primer ejemplo se elabora un calculador de raíz cuadrada donde se muestra cómo realizar un controlador y cómo mapear un parámetro de solicitud a un parámetro del modelo. En el segundo ejemplo, un módulo de registro de usuarios, se muestra como mapear un conjunto de parámetros a un objeto bean.

## Calculador de raíz cuadrada ##

Esta es la clase con la funcionalidad de cálculo del resultado. No necesitamos implementar ninguna interfaz ni heredar de una clase en especial. La clase cuenta con un único método el cual no necesita tener un nombre en especial ni una declaración específica.

```
public class SqrtCalculator {
    
    public double sqrt(int number){
        return Math.sqrt(number);
    }

}
```

Para que la clase de nuestro ejemplo pueda ser utilizada como un controlador debemos agregar la información de los siguientes cuatro puntos: cuál será el URL exacta para invocar al controlador mediante la anotación `carmesi.URL`, cuál será la vista del controlador (utilizando `carmesi.ForwardTo`), cómo se expondrá el resultado al JSP; y por último indicar con la anotación `carmesi.RequestParameter` que el parámetro de nuestro método será mapeado a partir de un parámetro de solicitud  (la conversión de `String` a `int` la realiza automáticamente Carmesi).

He aquí la clase anterior con la información necesaria para que funcione como un controlador.

```
import carmesi.ForwardTo;
import carmesi.RequestAttribute;
import carmesi.RequestParameter;
import carmesi.URL;

@URL("/sqrt")
@ForwardTo("/sqrt.jsp")
public class SqrtCalculator {
    
    @RequestAttribute("resultado")
    public double sqrt(@RequestParameter("numero") int numero){
        return Math.sqrt(number);
    }

}
```

La vista (sqrt.jsp) despliega el atríbuto de resultado:

```
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!doctype html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Sqrt</title>
    </head>
    <body>
        <h1>La raiz cuadrada es ${requestScope.resultado}</h1>
    </body>
</html>
```

## Registro de Usuarios ##

Este es un ejemplo de un módulo donde un usuario se registra para poder tener acceso al sistema y le envía sus datos de acceso mediante correo. Los datos del usuario son su alias, su nombre y sus apellidos. El modelo consiste de una clase dummy para el registro de los usuarios y notificación vía correo. La vista es una página con un formulario de registro y una página para informar el resultado del proceso de registro. El controlador será implementado con carmesi utilizando mapeo automático de parámetros de solicitud a beans.

Primero, crearemos la clase bean Usuario:

```
public class Usuario {
    private String alias;
    private String email;
    private String nombre;
    private String apellidos;

    //Se omiten los setters y getters de cada propiedad.
    
}
```

Aquí el código de la clase dummy para el registro:

```
public class SistemaRegistro {
    
    public void registrar(Usuario usuario){
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
```


Ahora necesitamos definir la página para introducir los datos del usuario a registrar. Los nombres de los campos de entrada de los datos serán iguales a los nombre de la propiedades de la clase Usuario para poder utilizar el mapeo automático de beans.

```
<%-- Fragmento de la página de registro --%>
<form action="registrarUsuario">
    <p>
        <label for="alias">Alias</label>
        <input type="text" name="alias"/>
    </p>
    <p>
        <label for="nombre">Nombre</label>
        <input type="text" name="nombre"/>
    </p>
    <p>
        <label for="apellidos">Apellidos</label>
        <input type="text" name="apellidos"/>
    </p>
    <p>
        <label for="email">Email</label>
        <input type="text" name="email"/>
    </p>
    <button type="submit">Registrar</button>
</form>
```

Una vez que tenemos la página de registro y las clases del modelo definididas, procedemos a implementar el Controlador.

```
import carmesi.RedirectTo;
import carmesi.RequestBean;
import carmesi.URL;

@URL("/registrarUsuario")
@RedirectTo("/resultadoRegistro.jsp")
public class ControladorRegistro {
    
    public void registrar(@RequestBean Usuario usuario) throws Exception{
        SistemaRegistro sistemaRegistro=new SistemaRegistro();
        sistemaRegistro.registrar(usuario);
    }

}
```

El controlador invoca al método registrar de la clase modelo. Hemos definido que el url para invocar el controlador es registrarUsuario que es el que se encuentra definido en el formulario. La página de la vista del controlador muestra el resultado del proceso del registro realizando una redirección. El método no devuelve ningún valor.

Aquí vemos el uso de la anotación RequestBean que permite llenar los atributos de un bean en base a los parámetros de la solicitud. Aquellos parámetros cuyo nombre coincida con el nombre de algún atributo del bean serán colocados por el framework(realizando las conversiones necesarias).


## Notas ##

El código debe compilarse con proceso de anotaciones habilitado. Este procesador es necesario ya que carmesi elabora a partir de él una lista de las clases controlador contenidas en la aplicación.