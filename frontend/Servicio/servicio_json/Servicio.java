/*
  Servicio.java
  Servicio web tipo REST
  Recibe parámetros utilizando JSON
  Carlos Pineda Guerrero, octubre 2023
*/

package servicio_json;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

import java.sql.*;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

import servicio_url.Usuario;

// la URL del servicio web es http://localhost:8080/Servicio/rest/ws
// donde:
//	"Servicio" es el dominio del servicio web (es decir, el nombre de archivo Servicio.war)
//	"rest" se define en la etiqueta <url-pattern> de <servlet-mapping> en el archivo WEB-INF\web.xml
//	"ws" se define en la siguiente anotación @Path de la clase Servicio

@Path("tienda")
public class Servicio
{
  static DataSource pool = null;
  static
  {		
    try
    {
      Context ctx = new InitialContext();
      pool = (DataSource)ctx.lookup("java:comp/env/jdbc/datasource_Servicio");
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  static Gson j = new GsonBuilder().registerTypeAdapter(byte[].class,new AdaptadorGsonBase64()).setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();

  @POST
  @Path("alta_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response alta(String json) throws Exception
  {
    ParamAltaArticulo p = (ParamAltaArticulo) j.fromJson(json,ParamAltaArticulo.class);
    Articulo articulo = p.articulo;

    Connection conexion = pool.getConnection();

    if (articulo.descripcion == null || articulo.descripcion.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar una descripcion valida"))).build();

    if (articulo.precio == null || articulo.precio<=0)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar un precio valido mayor que 0"))).build();

    if (articulo.cantidad == null || articulo.cantidad<=0)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar una cantidad valido mayor que 0"))).build();
    
    try
    {
      conexion.setAutoCommit(false);

      PreparedStatement stmt_1 = conexion.prepareStatement("INSERT INTO articulos(descripcion,precio,cantidad_en_almacen) VALUES (?,?,?)");
 
      try
      {
        stmt_1.setString(1,articulo.descripcion);
        stmt_1.setInt(2,articulo.precio);
        stmt_1.setInt(3,articulo.cantidad);

        stmt_1.executeUpdate();
      }
      finally
      {
        stmt_1.close();
      }

      
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

  @POST
  @Path("elimina_articulo")
  public Response elimina_articulo() throws Exception{
    
  }
  @POST
  @Path("consulta_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response consulta(String json) throws Exception{
    ParamConsultaArticulo p = (ParamConsultaArticulo) j.fromJson(json,ParamConsultaArticulo.class);
    String descripcion = p.descripcion;
    List<Articulo> listaArticulos=new ArrayList<>();
  
    

    Connection conexion = pool.getConnection();
    try {
      
      PreparedStatement st=conexion.prepareStatement("SELECT * from articulos where descripcion LIKE ?");
      st.setString(1,"%"+descripcion+"%");
      
      ResultSet rs=st.executeQuery();
      try {
        
        while(rs.next()){
          Articulo b=new Articulo();

          b.idA=rs.getInt(1);
          b.descripcion=rs.getString(2).toString();
          b.precio=rs.getInt(3);
          b.cantidad=rs.getInt(4);
          listaArticulos.add(b);
          

        }
        return Response.ok().entity(j.toJson(listaArticulos)).build();
      } catch (Exception e) {
        return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
        // TODO: handle exception
      }
      finally{
        rs.close();
      }
    } catch (Exception e) {
      // TODO: handle exception
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally{
      conexion.close();
    }
  }
  @POST
  @Path("trae_carrito")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response trae_carrito(String json) throws Exception{
    List<Carrito> listaArticulos=new ArrayList<>();
  
    

    Connection conexion = pool.getConnection();
    try {
      
      PreparedStatement st=conexion.prepareStatement("SELECT * from articulos a RIGHT OUTER JOIN carrito_compra c ON c.id=a.id");
      
      
      ResultSet rs=st.executeQuery();
      try {
        
        while(rs.next()){
          Carrito b=new Carrito();

          b.idA=rs.getInt(1);
          b.descripcion=rs.getString(2).toString();
          b.precio=rs.getInt(3);
          b.cantidad=rs.getInt(4);
          b.idCompra=rs.getInt(5);
          b.cantidadCarrito=rs.getInt(7);
          listaArticulos.add(b);
          

        }
        return Response.ok().entity(j.toJson(listaArticulos)).build();
      } catch (Exception e) {
        return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
        // TODO: handle exception
      }
      finally{
        rs.close();
      }
    } catch (Exception e) {
      // TODO: handle exception
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally{
      conexion.close();
    }
  }
  @POST
  @Path("inserta_compra")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response inserta_compra(String json) throws Exception {
    ParamInsertaCompraA p = (ParamInsertaCompraA) j.fromJson(json,ParamInsertaCompraA.class);
    
    Integer stock=p.stock;
    Integer id=p.id;
    Integer cantidad=p.cantidad;
    
    
    //iniciamos la transaccion
      Connection conexion = pool.getConnection();
    try
    {
     
      conexion.setAutoCommit(false);

      PreparedStatement stmt_1 = conexion.prepareStatement("INSERT INTO carrito_compra(id,cantidad) VALUES (?,?)");
      PreparedStatement stmt2=conexion.prepareStatement("UPDATE articulos SET cantidad_en_almacen=? WHERE id=?");
      try
      {
        stmt_1.setInt(1,id);
        stmt_1.setInt(2,cantidad);

        stmt_1.executeUpdate();

        stmt2.setInt(1, (stock-cantidad));
        stmt2.setInt(2, id);
        stmt2.executeUpdate();
      }
      finally
      {
        stmt_1.close();
        stmt2.close();
      }

      
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();

    
  }
}
