package main.java.com.minefocado.game.render;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Clase de utilidad para cargar código de shader desde recursos
 */
public class ShaderLoader {
    
    /**
     * Carga código de shader desde un archivo de recursos
     * 
     * @param resourcePath Ruta al archivo de shader en recursos
     * @return String que contiene el código del shader
     * @throws Exception Si el recurso no puede ser cargado
     */
    public static String loadShader(String resourcePath) throws Exception {
        try (InputStream in = ShaderLoader.class.getClassLoader().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            // Si el shader no puede ser cargado desde recursos, usar shaders por defecto embebidos
            return getDefaultShader(resourcePath);
        }
    }
    
    /**
     * Proporciona código de shader por defecto si la carga del recurso falla
     * 
     * @param resourcePath Ruta para identificar qué shader por defecto devolver
     * @return Código del shader por defecto como string
     */
    private static String getDefaultShader(String resourcePath) {
        if (resourcePath.contains("vertex")) {
            return getDefaultVertexShader();
        } else {
            return getDefaultFragmentShader();
        }
    }
    
    /**
     * Devuelve un vertex shader por defecto para renderizar chunks
     * 
     * @return Código del vertex shader por defecto
     */
    public static String getDefaultVertexShader() {
        return "#version 330 core\n" +
               "layout (location = 0) in vec3 position;\n" +
               "layout (location = 1) in vec2 texCoord;\n" +  // Cambiado de color a texCoord en ubicación 1
               "layout (location = 2) in vec3 normal;\n" +     // Cambiado normal a ubicación 2
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
     * Devuelve un fragment shader por defecto para renderizar chunks
     * 
     * @return Código del fragment shader por defecto
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
               "    // Iluminación ambiental - aumentada para evitar partes muy oscuras\n" +
               "    vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);\n" +
               "    \n" +
               "    // Iluminación difusa\n" +
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
               "    // Combinar luces\n" +
               "    vec3 lightResult = ambient + diffuse + vec3(topLight);\n" +
               "    \n" +
               "    // Muestrear textura\n" +
               "    vec4 texColor = texture(textureSampler, fragTexCoord);\n" +
               "    if (texColor.a < 0.1) discard; // Descartar píxeles transparentes\n" +
               "    \n" +
               "    // Color final con mapeo tonal para prevenir áreas demasiado brillantes\n" +
               "    outColor = vec4(lightResult, 1.0) * texColor;\n" +
               "}";
    }
}