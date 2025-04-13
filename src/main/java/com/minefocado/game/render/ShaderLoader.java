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
               "layout (location = 1) in vec2 texCoord;\n" +  // Changed from color to texCoord at location 1
               "layout (location = 2) in vec3 normal;\n" +     // Changed normal to location 2
               "\n" +
               "out vec2 fragTexCoord;\n" +
               "out vec3 fragNormal;\n" +
               "out vec3 fragPos;\n" +
               "\n" +
               "uniform mat4 projectionMatrix;\n" +
               "uniform mat4 viewMatrix;\n" +
               "uniform mat4 modelMatrix;\n" +
               "\n" +
               "void main() {\n" +
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
               "in vec2 fragTexCoord;\n" +
               "in vec3 fragNormal;\n" +
               "in vec3 fragPos;\n" +
               "\n" +
               "out vec4 outColor;\n" +
               "\n" +
               "uniform sampler2D textureSampler;\n" +
               "uniform vec3 lightPosition;\n" +
               "uniform vec3 viewPosition;\n" +
               "uniform float ambientStrength;\n" +
               "\n" +
               "void main() {\n" +
               "    // Ambient lighting - aumentada para evitar partes muy oscuras\n" +
               "    vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);\n" +
               "    \n" +
               "    // Diffuse lighting\n" +
               "    vec3 norm = normalize(fragNormal);\n" +
               "    \n" +
               "    // Dirección de luz desde arriba (como el sol) - no basada en posición\n" +
               "    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.5));\n" +
               "    \n" +
               "    // Cálculo de luz difusa usando valor absoluto para iluminar ambos lados\n" +
               "    float diff = max(dot(norm, lightDir), 0.4);\n" +
               "    vec3 diffuse = diff * vec3(1.0, 1.0, 0.9); // Luz ligeramente amarilla\n" +
               "    \n" +
               "    // Luz direccional adicional para la parte superior\n" +
               "    float topLight = max(dot(vec3(0.0, 1.0, 0.0), norm), 0.0) * 0.5;\n" +
               "    \n" +
               "    // Combine lights\n" +
               "    vec3 lightResult = ambient + diffuse + vec3(topLight);\n" +
               "    \n" +
               "    // Sample texture\n" +
               "    vec4 texColor = texture(textureSampler, fragTexCoord);\n" +
               "    if (texColor.a < 0.1) discard; // Descartar píxeles transparentes\n" +
               "    \n" +
               "    // Final color with tone mapping to prevent over-bright areas\n" +
               "    outColor = vec4(lightResult, 1.0) * texColor;\n" +
               "}";
    }
}