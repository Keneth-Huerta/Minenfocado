package main.java.com.minefocado.game.render;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Represents an OpenGL texture that can be used for rendering
 */
public class Texture {
    // OpenGL texture ID
    private final int id;
    
    // Texture dimensions
    private final int width;
    private final int height;
    
    /**
     * Creates a texture from raw pixel data
     * 
     * @param width Texture width in pixels
     * @param height Texture height in pixels
     * @param buffer Byte buffer containing pixel data (RGBA format)
     * @throws Exception If texture creation fails
     */
    public Texture(int width, int height, ByteBuffer buffer) throws Exception {
        this.width = width;
        this.height = height;
        
        // Create a new OpenGL texture
        id = glGenTextures();
        
        // Bind the texture
        glBindTexture(GL_TEXTURE_2D, id);
        
        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        // Upload the texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        
        // Generate mipmaps
        glGenerateMipmap(GL_TEXTURE_2D);
        
        // Unbind the texture
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Loads a texture from a resource
     * 
     * @param resourcePath Path to the texture resource
     * @return Texture object
     * @throws Exception If loading fails
     */
    public static Texture loadTexture(String resourcePath) throws Exception {
        // If resource loading fails, use a default texture
        BufferedImage image;
        try (InputStream in = Texture.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                // Resource not found, create default pattern
                return createDefaultTexture();
            }
            image = ImageIO.read(in);
        } catch (Exception e) {
            // Loading failed, create default pattern
            return createDefaultTexture();
        }
        
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Convert the image to an RGBA byte buffer
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();
        
        // Create the texture
        Texture texture = new Texture(width, height, buffer);
        
        // Free the buffer memory
        MemoryUtil.memFree(buffer);
        
        return texture;
    }
    
    /**
     * Creates a default checkerboard texture when loading fails
     * 
     * @return Default texture
     * @throws Exception If creation fails
     */
    private static Texture createDefaultTexture() throws Exception {
        // Create simple texture atlas with colored squares
        int width = 256;
        int height = 256;
        int tileSize = 16; // 16x16 tiles
        
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Calculate tile coordinates
                int tileX = x / tileSize;
                int tileY = y / tileSize;
                int tileIndex = tileY * (width / tileSize) + tileX;
                
                // Get color based on tile index
                byte r, g, b;
                switch (tileIndex % 16) {
                    case 0: // Air (transparent)
                        r = g = b = (byte) 0;
                        buffer.put(r).put(g).put(b).put((byte) 0);
                        break;
                    case 1: // Stone (gray)
                        r = g = b = (byte) 128;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 2: // Dirt (brown)
                        r = (byte) 150; g = (byte) 100; b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 3: // Grass side (brown/green)
                        r = (byte) 150; g = (byte) 100; b = (byte) 50;
                        if (y % tileSize < tileSize / 4) {
                            g = (byte) 180;
                        }
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 4: // Grass top (green)
                        r = (byte) 100; g = (byte) 180; b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 5: // Sand (yellow)
                        r = (byte) 220; g = (byte) 200; b = (byte) 100;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 6: // Water (blue, semi-transparent)
                        r = (byte) 50; g = (byte) 100; b = (byte) 200;
                        buffer.put(r).put(g).put(b).put((byte) 180);
                        break;
                    case 7: // Bedrock (dark gray)
                        r = g = b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 8: // Wood side (brown)
                        r = (byte) 150; g = (byte) 100; b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 9: // Wood end (light brown)
                        r = (byte) 170; g = (byte) 120; b = (byte) 70;
                        buffer.put(r).put(g).put(b).put((byte) 255);
                        break;
                    case 10: // Leaves (green, semi-transparent)
                        r = (byte) 50; g = (byte) 150; b = (byte) 50;
                        buffer.put(r).put(g).put(b).put((byte) 200);
                        break;
                    default: // Checkerboard pattern for remaining tiles
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
        
        // Create the texture
        Texture texture = new Texture(width, height, buffer);
        
        // Free the buffer memory
        MemoryUtil.memFree(buffer);
        
        return texture;
    }
    
    /**
     * Gets the OpenGL texture ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Gets the texture width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the texture height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Binds the texture to the current OpenGL context
     */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }
    
    /**
     * Unbinds the texture from the current OpenGL context
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Cleans up the texture
     */
    public void cleanup() {
        glDeleteTextures(id);
    }
}