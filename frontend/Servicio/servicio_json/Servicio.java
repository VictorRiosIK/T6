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

      
        PreparedStatement stmt_2=conexion.prepareStatement("INSERT INTO fotos_articulos(foto,id_usuario) VALUES(?,(SELECT id FROM articulos WHERE descripcion=?))");
        try {
          stmt_2.setBytes(1, articulo.foto);
          stmt_2.setString(2, articulo.descripcion);
          stmt_2.executeUpdate();
        } catch (Exception e) {
          // TODO: handle exception
        }finally{
          stmt_2.close();
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
  public Response elimina_articulo(String json) throws Exception{
     ParamEliminaArticulo p = (ParamEliminaArticulo) j.fromJson(json,ParamEliminaArticulo.class);

     Integer idArticulo=p.idArticulo;
     Integer idCompra=p.idCompra;
     //iniciamos la transaccion
     
      Connection conexion = pool.getConnection();
           Integer cantidadAuxAlmacen=0,cantidad=0;

    try
    {
      conexion.setAutoCommit(false);

      PreparedStatement st=conexion.prepareStatement("SELECT cantidad from carrito_compra where id=? and idCompra=?");
      st.setInt(1, idArticulo);
      st.setInt(2, idCompra);

      PreparedStatement st2=conexion.prepareStatement("SELECT cantidad_en_almacen from articulos where id=?");
      st2.setInt(1, idArticulo);

      PreparedStatement stmt2=conexion.prepareStatement("UPDATE articulos SET cantidad_en_almacen=? WHERE id=?");
      try
      {
        
        ResultSet rs=st.executeQuery();
        if(rs.next()){
           cantidad=rs.getInt(1);
        }
        ResultSet rs2=st2.executeQuery();
        if(rs2.next()){
           cantidadAuxAlmacen=rs.getInt(1);
        }

        
        //inicia la actualizacion de carrito
        stmt2.setInt(1, (cantidad+cantidadAuxAlmacen));
        stmt2.setInt(2, idArticulo);
        stmt2.executeUpdate();
        
        
      }
      finally
      {
        st.close();
        st2.close();
        //stmt2.close();
      }

      PreparedStatement stmt3=conexion.prepareStatement("DELETE FROM carrito_compra WHERE idCompra=?");
      try {
        stmt3.setInt(1, idCompra);
        stmt3.executeUpdate();
      } catch (Exception e) {
        // TODO: handle exception
      }finally{
        stmt3.close();
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
    //return Response.status(400).entity(j.toJson(new Error(idArticulo + "- " + idCompra+"/" +cantidadAuxAlmacen +"-" + cantidad))).build();
    
  }

  @POST
    @Path("elimina_carrito_completo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response elimina_carrito_completo(String json) throws Exception {
        
        // Inicia la transacción
        Connection conexion = pool.getConnection();
        try {
            conexion.setAutoCommit(false);

            PreparedStatement stmt1 = conexion.prepareStatement("SELECT * FROM carrito_compra ");
            PreparedStatement stmt2 = conexion.prepareStatement("UPDATE articulos SET cantidad_en_almacen = cantidad_en_almacen + ? WHERE id=?");
            PreparedStatement stmt3 = conexion.prepareStatement("DELETE FROM carrito_compra");

            try {
                
                ResultSet rs = stmt1.executeQuery();//traemos todo de la tabla carrito_compra

                while (rs.next()) {
                    int idArticulo = rs.getInt("id");
                    int cantidadCarrito = rs.getInt("cantidad");

                    // Actualiza la cantidad_en_almacen en la tabla de artículos
                    stmt2.setInt(1, cantidadCarrito);
                    stmt2.setInt(2, idArticulo);
                    stmt2.executeUpdate();
                }

                // Elimina todos los registros del carrito de compra asociados a la compra
                
                stmt3.executeUpdate();
            } finally {
                stmt1.close();
                stmt2.close();
                stmt3.close();
            }

            conexion.commit();
        } catch (Exception e) {
            conexion.rollback();
            return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
        } finally {
            conexion.setAutoCommit(true);
            conexion.close();
        }

        return Response.ok().build();
    }

    @POST
    @Path("incrementa_cantidad_carrito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response incrementa_cantidad_carrito(String json) throws Exception {
        ParamEliminaArticulo p = (ParamEliminaArticulo) j.fromJson(json, ParamEliminaArticulo.class);

        Integer idArticulo = p.idArticulo;
        Integer idCompra = p.idCompra;

        // Inicia la transacción
        Connection conexion = pool.getConnection();
        try {
            conexion.setAutoCommit(false);

            // Verifica la disponibilidad en la tabla de artículos
            PreparedStatement stmt1 = conexion.prepareStatement("SELECT cantidad_en_almacen FROM articulos WHERE id=?");
            stmt1.setInt(1, idArticulo);
            ResultSet rs1 = stmt1.executeQuery();

            if (rs1.next()) {
                int cantidadDisponible = rs1.getInt("cantidad_en_almacen");

                if (cantidadDisponible > 0) {
                    // Incrementa la cantidad en el carrito
                    PreparedStatement stmt2 = conexion.prepareStatement("UPDATE carrito_compra SET cantidad = cantidad + 1 WHERE id=? AND idCompra=?");
                    stmt2.setInt(1, idArticulo);
                    stmt2.setInt(2, idCompra);
                    stmt2.executeUpdate();

                    // Actualiza la cantidad_en_almacen en la tabla de artículos
                    PreparedStatement stmt3 = conexion.prepareStatement("UPDATE articulos SET cantidad_en_almacen = cantidad_en_almacen - 1 WHERE id=?");
                    stmt3.setInt(1, idArticulo);
                    stmt3.executeUpdate();

                    stmt2.close();
                    stmt3.close();
                } else {
                    return Response.status(400).entity(j.toJson(new Error("No hay suficientes existencias disponibles para agregar al carrito."))).build();
                }
            } else {
                return Response.status(400).entity(j.toJson(new Error("Artículo no encontrado."))).build();
            }

            stmt1.close();
            conexion.commit();
        } catch (Exception e) {
            conexion.rollback();
            return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
        } finally {
            conexion.setAutoCommit(true);
            conexion.close();
        }

        return Response.ok().build();
    }
    @POST
    @Path("decrementa_cantidad_carrito")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response decrementa_cantidad_carrito(String json) throws Exception {
        ParamEliminaArticulo p = (ParamEliminaArticulo) j.fromJson(json, ParamEliminaArticulo.class);

        Integer idArticulo = p.idArticulo;
        Integer idCompra = p.idCompra;

        // Inicia la transacción
        Connection conexion = pool.getConnection();
        try {
            conexion.setAutoCommit(false);

            // Verifica la existencia del artículo en el carrito
            PreparedStatement stmt1 = conexion.prepareStatement("SELECT cantidad FROM carrito_compra WHERE id=? AND idCompra=?");
            stmt1.setInt(1, idArticulo);
            stmt1.setInt(2, idCompra);
            ResultSet rs1 = stmt1.executeQuery();

            if (rs1.next()) {
                int cantidadCarrito = rs1.getInt("cantidad");

                if (cantidadCarrito > 1) {
                    // Decrementa la cantidad en el carrito
                    PreparedStatement stmt2 = conexion.prepareStatement("UPDATE carrito_compra SET cantidad = cantidad - 1 WHERE id=? AND idCompra=?");
                    stmt2.setInt(1, idArticulo);
                    stmt2.setInt(2, idCompra);
                    stmt2.executeUpdate();

                    // Actualiza la cantidad_en_almacen en la tabla de artículos
                    PreparedStatement stmt3 = conexion.prepareStatement("UPDATE articulos SET cantidad_en_almacen = cantidad_en_almacen + 1 WHERE id=?");
                    stmt3.setInt(1, idArticulo);
                    stmt3.executeUpdate();

                    stmt2.close();
                    stmt3.close();
                } else {
                    // Si la cantidad en el carrito es 1, elimina el artículo del carrito
                    PreparedStatement stmt4 = conexion.prepareStatement("DELETE FROM carrito_compra WHERE id=? AND idCompra=?");
                    stmt4.setInt(1, idArticulo);
                    stmt4.setInt(2, idCompra);
                    stmt4.executeUpdate();

                    // Incrementa la cantidad_en_almacen en la tabla de artículos
                    PreparedStatement stmt5 = conexion.prepareStatement("UPDATE articulos SET cantidad_en_almacen = cantidad_en_almacen + 1 WHERE id=?");
                    stmt5.setInt(1, idArticulo);
                    stmt5.executeUpdate();

                    stmt4.close();
                    stmt5.close();
                }
            } else {
                return Response.status(400).entity(j.toJson(new Error("El artículo no está en el carrito."))).build();
            }

            stmt1.close();
            conexion.commit();
        } catch (Exception e) {
            conexion.rollback();
            return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
        } finally {
            conexion.setAutoCommit(true);
            conexion.close();
        }

        return Response.ok().build();
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
      
      PreparedStatement st=conexion.prepareStatement("SELECT * from articulos INNER JOIN fotos_articulos ON articulos.id=fotos_articulos.id_usuario where descripcion LIKE ?");
      st.setString(1,"%"+descripcion+"%");
      
      ResultSet rs=st.executeQuery();
      try {
        
        while(rs.next()){
          Articulo b=new Articulo();

          b.idA=rs.getInt(1);
          b.descripcion=rs.getString(2).toString();
          b.precio=rs.getInt(3);
          b.cantidad=rs.getInt(4);
          b.foto=rs.getBytes(6);
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
