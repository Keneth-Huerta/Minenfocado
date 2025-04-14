package main.java.com.minefocado.game.world.generation;

import java.util.Random;

/**
 * Implementación de ruido Perlin para generación de terreno.
 * Genera ruido coherente que puede ser usado para altura de terreno, mapas de biomas, etc.
 */
public class PerlinNoise {
    private final long seed;
    private final Random random;
    
    // Tabla de permutación
    private final int[] permutation = new int[512];
    
    /**
     * Crea un nuevo generador de ruido Perlin con la semilla especificada
     * 
     * @param seed La semilla para el generador de números aleatorios
     */
    public PerlinNoise(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        
        // Inicializar la tabla de permutación
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        
        // Mezclar basado en la semilla
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        
        // Extender la permutación para evitar desbordamiento
        for (int i = 0; i < 256; i++) {
            permutation[i + 256] = permutation[i];
        }
    }
    
    /**
     * Calcula el producto punto de un vector gradiente y un vector distancia
     * 
     * @param hash Valor hash para determinar el vector gradiente
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     * @return Resultado del producto punto
     */
    private static double grad(int hash, double x, double y, double z) {
        // Convertir los 4 bits inferiores del hash en 12 direcciones de gradiente
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    
    /**
     * Función de interpolación suave (curva de atenuación)
     * Mapea t a 6t^5 - 15t^4 + 10t^3
     */
    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    /**
     * Interpolación lineal entre a y b por t
     */
    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
    
    /**
     * Calcula el ruido Perlin 3D en las coordenadas dadas
     * 
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     * @return Valor de ruido entre -1 y 1
     */
    public double noise(double x, double y, double z) {
        // Encontrar el cubo unitario que contiene el punto
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;
        
        // Encontrar x, y, z relativas del punto en el cubo
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        
        // Calcular curvas de atenuación para cada x, y, z
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        
        // Hash de las coordenadas de las 8 esquinas del cubo
        int A = permutation[X] + Y;
        int AA = permutation[A] + Z;
        int AB = permutation[A + 1] + Z;
        int B = permutation[X + 1] + Y;
        int BA = permutation[B] + Z;
        int BB = permutation[B + 1] + Z;
        
        // Sumar los resultados mezclados de las 8 esquinas del cubo
        return lerp(w, lerp(v, lerp(u, grad(permutation[AA], x, y, z),
                                       grad(permutation[BA], x - 1, y, z)),
                               lerp(u, grad(permutation[AB], x, y - 1, z),
                                       grad(permutation[BB], x - 1, y - 1, z))),
                       lerp(v, lerp(u, grad(permutation[AA + 1], x, y, z - 1),
                                       grad(permutation[BA + 1], x - 1, y, z - 1)),
                               lerp(u, grad(permutation[AB + 1], x, y - 1, z - 1),
                                       grad(permutation[BB + 1], x - 1, y - 1, z - 1))));
    }
    
    /**
     * Genera ruido Perlin en diferentes octavas (frecuencias) y las combina
     * Las octavas más altas añaden más detalle pero tienen menos influencia
     * 
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     * @param octaves Número de octavas a calcular
     * @param persistence Cuánto contribuye cada octava al resultado final
     * @return Valor de ruido combinado entre -1 y 1
     */
    public double octaveNoise(double x, double y, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;  // Usado para normalizar el resultado
        
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency, z * frequency) * amplitude;
            
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        
        // Normalizar el resultado
        return total / maxValue;
    }
    
    /**
     * Devuelve un mapa de alturas 2D usando ruido Perlin
     * 
     * @param worldX Coordenada X en el espacio del mundo
     * @param worldZ Coordenada Z en el espacio del mundo
     * @param octaves Número de octavas para el ruido
     * @param scale Escala del ruido (valores más pequeños = características más grandes)
     * @param height Altura máxima del terreno
     * @return Valor de altura entre 0 y height
     */
    public double getHeightAt(int worldX, int worldZ, int octaves, double scale, double height) {
        double x = worldX / scale;
        double z = worldZ / scale;
        
        // Mapear ruido de [-1,1] a [0,1]
        double noise = (octaveNoise(x, 0, z, octaves, 0.5) + 1) / 2;
        
        // Escalar a la altura deseada
        return noise * height;
    }
    
    /**
     * Obtiene la semilla usada para este generador de ruido
     */
    public long getSeed() {
        return seed;
    }
}