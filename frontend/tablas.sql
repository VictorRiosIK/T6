CREATE TABLE artículos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripción VARCHAR(255) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    cantidad_en_almacén INT NOT NULL,
    fotografía VARCHAR(255) -- Puedes ajustar el tipo de datos para la imagen
);