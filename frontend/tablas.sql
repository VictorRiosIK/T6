CREATE TABLE articulos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(255) NOT NULL,
    precio INT NOT NULL,
    cantidad_en_almacen INT NOT NULL
);
CREATE TABLE carrito_compra(
	idCompra INT AUTO_INCREMENT PRIMARY KEY,
	id INT ,
    cantidad INT
)