package main.java.com.minefocado.game.world.generation;

import java.util.Random;

/**
 * Implementation of Perlin noise for terrain generation.
 * Generates coherent noise that can be used for terrain height, biome maps, etc.
 */
public class PerlinNoise {
    private final long seed;
    private final Random random;
    
    // Permutation table
    private final int[] permutation = new int[512];
    
    /**
     * Creates a new Perlin noise generator with the specified seed
     * 
     * @param seed The seed for the random number generator
     */
    public PerlinNoise(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        
        // Initialize the permutation table
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        
        // Shuffle based on the seed
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        
        // Extend the permutation to avoid overflow
        for (int i = 0; i < 256; i++) {
            permutation[i + 256] = permutation[i];
        }
    }
    
    /**
     * Computes the dot product of a gradient vector and a distance vector
     * 
     * @param hash Hash value to determine gradient vector
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Dot product result
     */
    private static double grad(int hash, double x, double y, double z) {
        // Convert low 4 bits of hash into 12 gradient directions
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    
    /**
     * Smooth interpolation function (fade curve)
     * Maps t to 6t^5 - 15t^4 + 10t^3
     */
    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    /**
     * Linear interpolation between a and b by t
     */
    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
    
    /**
     * Computes 3D Perlin noise at the given coordinates
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Noise value between -1 and 1
     */
    public double noise(double x, double y, double z) {
        // Find unit cube that contains the point
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;
        
        // Find relative x, y, z of point in cube
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        
        // Compute fade curves for each of x, y, z
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        
        // Hash coordinates of the 8 cube corners
        int A = permutation[X] + Y;
        int AA = permutation[A] + Z;
        int AB = permutation[A + 1] + Z;
        int B = permutation[X + 1] + Y;
        int BA = permutation[B] + Z;
        int BB = permutation[B + 1] + Z;
        
        // And add blended results from the 8 corners of the cube
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
     * Generates Perlin noise at different octaves (frequencies) and combines them
     * Higher octaves add more detail but have less influence
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param octaves Number of octaves to compute
     * @param persistence How much each octave contributes to the final result
     * @return Combined noise value between -1 and 1
     */
    public double octaveNoise(double x, double y, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;  // Used for normalizing result
        
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency, z * frequency) * amplitude;
            
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        
        // Normalize the result
        return total / maxValue;
    }
    
    /**
     * Returns a 2D heightmap using Perlin noise
     * 
     * @param worldX X coordinate in world space
     * @param worldZ Z coordinate in world space
     * @param octaves Number of octaves for the noise
     * @param scale Scale of the noise (smaller values = larger features)
     * @param height Maximum height of the terrain
     * @return Height value between 0 and height
     */
    public double getHeightAt(int worldX, int worldZ, int octaves, double scale, double height) {
        double x = worldX / scale;
        double z = worldZ / scale;
        
        // Map noise from [-1,1] to [0,1]
        double noise = (octaveNoise(x, 0, z, octaves, 0.5) + 1) / 2;
        
        // Scale to desired height
        return noise * height;
    }
    
    /**
     * Gets the seed used for this noise generator
     */
    public long getSeed() {
        return seed;
    }
}