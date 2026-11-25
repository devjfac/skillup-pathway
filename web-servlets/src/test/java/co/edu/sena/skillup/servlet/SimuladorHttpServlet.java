package co.edu.sena.skillup.servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulador liviano de objetos HTTP ({@link HttpServletRequest}, {@link HttpServletResponse},
 * {@link HttpSession} y {@link RequestDispatcher}) basado en proxies dinámicos del JDK para
 * ejecutar pruebas unitarias determinísticas sobre los métodos {@code doGet} y {@code doPost}
 * de los Servlets sin requerir un contenedor externo en memoria.
 */
public final class SimuladorHttpServlet {

    private final Map<String, String> parametros = new HashMap<>();
    private final Map<String, Object> atributosRequest = new HashMap<>();
    private final Map<String, Object> atributosSesion = new HashMap<>();
    private final Map<String, Object> atributosContexto = new HashMap<>();
    private String metodoHttp = "GET";
    private String contextPath = "/skillup-web";
    private String vistaDespachada;
    private String urlRedirigida;
    private boolean sesionInvalidada;

    /**
     * Crea un nuevo simulador HTTP configurado con ruta de contexto {@code /skillup-web}.
     */
    public SimuladorHttpServlet() {
        // Estado limpio por cada caso de prueba.
    }

    /**
     * Define un parámetro HTTP simulado (de cadena de consulta {@code GET} o cuerpo {@code POST}).
     *
     * @param nombre nombre del parámetro del formulario
     * @param valor valor enviado
     * @return este mismo simulador para encadenamiento fluido
     */
    public SimuladorHttpServlet conParametro(String nombre, String valor) {
        parametros.put(nombre, valor);
        return this;
    }

    /**
     * Define el método HTTP de la petición ({@code GET} o {@code POST}).
     *
     * @param metodo verbo HTTP
     * @return este mismo simulador
     */
    public SimuladorHttpServlet conMetodo(String metodo) {
        this.metodoHttp = metodo;
        return this;
    }

    /**
     * Construye el proxy de {@link HttpServletRequest} conectado a este simulador.
     *
     * @return instancia simulada de petición HTTP
     */
    public HttpServletRequest crearRequest() {
        ServletContext contexto = crearContexto();
        HttpSession sesion = crearSesion(contexto);
        InvocationHandler manejador = (proxy, metodo, args) ->
                manejarRequest(metodo, args, contexto, sesion);
        return (HttpServletRequest) Proxy.newProxyInstance(
                SimuladorHttpServlet.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                manejador);
    }

    /**
     * Construye el proxy de {@link HttpServletResponse} conectado a este simulador.
     *
     * @return instancia simulada de respuesta HTTP
     */
    public HttpServletResponse crearResponse() {
        InvocationHandler manejador = (proxy, metodo, args) -> {
            if ("sendRedirect".equals(metodo.getName()) && args != null && args.length == 1) {
                urlRedirigida = (String) args[0];
                return null;
            }
            return valorPorDefecto(metodo.getReturnType());
        };
        return (HttpServletResponse) Proxy.newProxyInstance(
                SimuladorHttpServlet.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                manejador);
    }

    /**
     * Devuelve el atributo almacenado en la petición tras la ejecución del Servlet.
     *
     * @param nombre clave del atributo
     * @return valor almacenado o {@code null}
     */
    public Object getAtributoRequest(String nombre) {
        return atributosRequest.get(nombre);
    }

    /**
     * Devuelve el atributo almacenado en la sesión HTTP.
     *
     * @param nombre clave del atributo de sesión
     * @return valor almacenado o {@code null}
     */
    public Object getAtributoSesion(String nombre) {
        return atributosSesion.get(nombre);
    }

    public String getVistaDespachada() {
        return vistaDespachada;
    }

    public String getUrlRedirigida() {
        return urlRedirigida;
    }

    public boolean isSesionInvalidada() {
        return sesionInvalidada;
    }

    private Object manejarRequest(
            Method metodo, Object[] args, ServletContext contexto, HttpSession sesion) {
        String nombre = metodo.getName();
        return switch (nombre) {
            case "getParameter" -> parametros.get((String) args[0]);
            case "getAttribute" -> atributosRequest.get((String) args[0]);
            case "setAttribute" -> {
                atributosRequest.put((String) args[0], args[1]);
                yield null;
            }
            case "getMethod" -> metodoHttp;
            case "getContextPath" -> contextPath;
            case "getServletContext" -> contexto;
            case "getSession" -> sesion;
            case "getRequestDispatcher" -> crearDispatcher((String) args[0]);
            default -> valorPorDefecto(metodo.getReturnType());
        };
    }

    private RequestDispatcher crearDispatcher(String ruta) {
        InvocationHandler manejador = (proxy, metodo, args) -> {
            if ("forward".equals(metodo.getName()) || "include".equals(metodo.getName())) {
                vistaDespachada = ruta;
            }
            return null;
        };
        return (RequestDispatcher) Proxy.newProxyInstance(
                SimuladorHttpServlet.class.getClassLoader(),
                new Class<?>[]{RequestDispatcher.class},
                manejador);
    }

    private HttpSession crearSesion(ServletContext contexto) {
        InvocationHandler manejador = (proxy, metodo, args) -> switch (metodo.getName()) {
            case "getAttribute" -> atributosSesion.get((String) args[0]);
            case "setAttribute" -> {
                atributosSesion.put((String) args[0], args[1]);
                yield null;
            }
            case "invalidate" -> {
                sesionInvalidada = true;
                atributosSesion.clear();
                yield null;
            }
            case "getServletContext" -> contexto;
            default -> valorPorDefecto(metodo.getReturnType());
        };
        return (HttpSession) Proxy.newProxyInstance(
                SimuladorHttpServlet.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                manejador);
    }

    private ServletContext crearContexto() {
        InvocationHandler manejador = (proxy, metodo, args) -> switch (metodo.getName()) {
            case "getAttribute" -> atributosContexto.get((String) args[0]);
            case "setAttribute" -> {
                atributosContexto.put((String) args[0], args[1]);
                yield null;
            }
            case "getContextPath" -> contextPath;
            default -> valorPorDefecto(metodo.getReturnType());
        };
        return (ServletContext) Proxy.newProxyInstance(
                SimuladorHttpServlet.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                manejador);
    }

    private static Object valorPorDefecto(Class<?> tipoRetorno) {
        if (!tipoRetorno.isPrimitive()) {
            return null;
        }
        if (tipoRetorno == boolean.class) {
            return false;
        }
        if (tipoRetorno == int.class) {
            return 0;
        }
        if (tipoRetorno == long.class) {
            return 0L;
        }
        return null;
    }
}
