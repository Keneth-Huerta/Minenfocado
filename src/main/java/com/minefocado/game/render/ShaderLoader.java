package main.java.com.minefocado.game.render;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Utility class for loading shader code from resources
 */
public class ShaderLoader {
    
    /**
     * Loads shader code from a resource file
     * 
     * @param resourcePath Path to the shader file in resources
     * @return String containing the shader code
     * @throws Exception If the resource cannot be loaded
     */
    public static String loadShader(String resourcePath) throws Exception {
        try (InputStream in = ShaderLoader.class.getClassLoader().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            // If shader cannot be loaded from resources, use embedded default shaders
            return getDefaultShader(resourcePath);
        }
    }
    
    /**
     * Provides default shader code if resource loading fails
     * 
     * @param resourcePath Path to identify which default shader to return
     * @return Default shader code as string
     */
    private static String getDefaultShader(String resourcePath) {
        if (resourcePath.contains("vertex")) {
            return getDefaultVertexShader();
        } else {
            return getDefaultFragmentShader();
        }
    }
    
    /**
     * Returns a default vertex shader for rendering chunks
     * 
     * @return Default vertex shader code
     */
    public static String getDefaultVertexShader() {
        return "#version 330 core\n" +
               "layout (location = 0) in vec3 position;\n" +
               "layout (location = 1) in vec3 color;\n" +
               "layout (location = 2) in vec2 texCoord;\n" +
               "layout (location = 3) in vec3 normal;\n" +
               "\n" +
               "out vec3 fragColor;\n" +
               "out vec2 fragTexCoord;\n" +
               "out vec3 fragNormal;\n" +
               "out vec3 fragPos;\n" +
               "\n" +
               "uniform mat4 projectionMatrix;\n" +
               "uniform mat4 viewMatrix;\n" +
               "uniform mat4 modelMatrix;\n" +
               "\n" +
               "void main() {\n" +
               "    fragColor = color;\n" +
               "    fragTexCoord = texCoord;\n" +
               "    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;\n" +
               "    fragPos = vec3(modelMatrix * vec4(position, 1.0));\n" +
               "    \n" +
               "    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);\n" +
               "}";
    }
    
    /**
     * Returns a default fragment shader for rendering chunks
     * 
     * @return Default fragment shader code
     */
    public static String getDefaultFragmentShader() {
        return "#version 330 core\n" +
               "in vec3 fragColor;\n" +
               "in vec2 fragTexCoord;\n" +
               "in vec3 fragNormal;\n" +
               "in vec3 fragPos;\n" +
               "\n" +
               "out vec4 outColor;\n" +
               "\n" +
               "uniform sampler2D textureSampler;\n" +
               "uniform vec3 lightPosition;\n" +
               "uniform vec3 viewPosition;\n" +  // Aseguramos que este uniforme se llama igual que en World.java
               "uniform float ambientStrength;\n" +
               "\n" +
               "void main() {\n" +
               "    // Ambient lighting\n" +
               "    vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);\n" +
               "    \n" +
               "    // Diffuse lighting\n" +
               "    vec3 norm = normalize(fragNormal);\n" +
               "    vec3 lightDir = normalize(lightPosition - fragPos);\n" +
               "    float diff = max(dot(norm, lightDir), 0.0);\n" +
               "    vec3 diffuse = diff * vec3(1.0, 1.0, 1.0);\n" +
               "    \n" +
               "    // Combine lights\n" +
               "    vec3 lightResult = (ambient + diffuse) * fragColor;\n" +
               "    \n" +
               "    // Sample texture\n" +
               "    vec4 texColor = texture(textureSampler, fragTexCoord);\n" +
               "    \n" +
               "    // Final color\n" +
               "    outColor = vec4(lightResult, 1.0) * texColor;\n" +
               "}";
    }
}