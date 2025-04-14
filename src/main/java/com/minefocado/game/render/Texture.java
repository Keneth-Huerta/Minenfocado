package main.java.com.minefocado.game.render;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Representa una textura OpenGL que puede ser usada para renderizado
 */
public class Texture {
    // ID de textura OpenGL
    private final int id;
    
    // Dimensiones de la textura
    private final int width;
    private final int height;
    
    /**
     * Crea una textura a partir de datos de píxeles sin procesar
     * 
     * @param width Ancho de la textura en píxeles
     * @param height Alto de la textura en píxeles
     * @param buffer Buffer de bytes que contiene los datos de píxeles (formato RGBA)
     * @throws Exception Si la creación de la textura falla
     */
    public Texture(int width, int height, ByteBuffer buffer) throws Exception {
        this.width = width;
        this.height = height;
        
        // Crear una nueva textura OpenGL
        id = glGenTextures();
        
        // Vincular la textura
        glBindTexture(GL_TEXTURE_2D, id);
        
        // Establecer parámetros de textura
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        // Subir los datos de la textura
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        
        // Generar mipmaps
        glGenerateMipmap(GL_TEXTURE_2D);
        
        // Desvincular la textura
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Carga una textura desde un recurso
     * 
     * @param resourcePath Ruta al recurso de la textura
     * @return Objeto Texture
     * @throws Exception Si la carga falla
     */
    public static Texture loadTexture(String resourcePath) throws Exception {
        // Si la carga del recurso falla, usar una textura por defecto
        BufferedImage image;
        try (InputStream in = Texture.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                // Recurso no encontrado, crear patrón por defecto
                return createDefaultTexture();
            }
            image = ImageIO.read(in);
        } catch (Exception e) {
            // La carga falló, crear patrón por defecto
            return createDefaultTexture();
        }
        
        // Obtener dimensiones de la imagen
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Convertir la imagen a un buffer de bytes RGBA
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Rojo
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Verde
                buffer.put((byte) (pixel & 0xFF));         // Azul
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alfa
            }
        }
        buffer.flip();
        
        // Crear la textura
        Texture texture = new Texture(width, height, buffer);
        
        // Liberar la memoria del buffer
        MemoryUtil.memFree(buffer);
        
        return texture;
    }
    
    /**
     * Crea una textura de tablero de ajedrez por defecto cuando la carga falla
     * 
     * @return Textura por defecto
     * @throws Exception Si la creación falla
     */
    private static Texture createDefaultTexture() throws Exception {
        // Crear un atlas de texturas simple con cuadrados de colores
        int width = 256;
        int height = 256;
        int tileSize = 16; // 16x16 tiles
        
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Calcular coordenadas del tile
                int tileX = x / tileSize;
                int tileY = y / tileSize;
                int tileIndex = tileY * (width / tileSize) + tileX;
                
                // Obtener color basado en el índice del tile
                byte r, g, b;
                switch (tileIndex % 16) {
                    case 0: // Aire (transparente)
                        r = g = b = (byte) 0;
                        buffer.put(r).put(g).put(b).put((byte) 0);
                        break;
                    case 1: // Piedra (gris)
                        r = g = b = (byte) 128;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 2: // Tierra (marrón)
                        r = (byte) 150; g = (byte) 100; b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 3: // Lado de hierba (marrón/verde)
                        r = (byte) 150; g = (byte) 100; b = (byte) 50;
                        if (y % tileSize < tileSize / 4) {
                            g = (byte) 180;
                        }
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 4: // Parte superior de hierba (verde)
                        r = (byte) 100; g = (byte) 180; b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 5: // Arena (amarillo)
                        r = (byte) 220; g = (byte) 200; b = (byte) 100;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 6: // Agua (azul, semi-transparente)
                        r = (byte) 50; g = (byte) 100; b = (byte) 200;
                        buffer.put(r).put(g).put(b).put((byte) 180);
                        break;
                    case 7: // Roca madre (gris oscuro)
                        r = g = b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 8: // Lado de madera (marrón)
                        r = (byte) 150; g = (byte) 100; b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 9: // Extremo de madera (marrón claro)
                        r = (byte) 170; g = (byte) 120; b = (byte) 70;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 10: // Hojas (verde, semi-transparente)
                        r = (byte) 50; g = (byte) 150; b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 200);
                        break;
                    default: // Patrón de tablero de ajedrez para los tiles restantes
                        boolean isWhite = ((x / (tileSize/4)) + (y / (tileSize/4))) % 2 == 0;
                        if (isWhite) {
                            r = g = b = (byte) 200;
                        } else {
                            r = g = b = (byte) 100;
                        }
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                }
            }
        }
        buffer.flip();
        
        // Crear la textura
        Texture texture = new Texture(width, height, buffer);
        
        // Liberar la memoria del buffer
        MemoryUtil.memFree(buffer);
        
        return texture;
    }
    
    /**
     * Obtiene el ID de la textura OpenGL
     */
    public int getId() {
        return id;
    }
    
    /**
     * Obtiene el ancho de la textura
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Obtiene el alto de la textura
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Vincula la textura al contexto OpenGL actual
     */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }
    
    /**
     * Desvincula la textura del contexto OpenGL actual
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Limpia la textura
     */
    public void cleanup() {
        glDeleteTextures(id);
    }
}