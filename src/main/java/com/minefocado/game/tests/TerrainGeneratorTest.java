package main.java.com.minefocado.game.tests;

import main.java.com.minefocado.game.world.World;
import main.java.com.minefocado.game.world.blocks.Block;
import main.java.com.minefocado.game.world.blocks.BlockRegistry;
import main.java.com.minefocado.game.world.chunk.Chunk;
import main.java.com.minefocado.game.world.generation.TerrainGenerator;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Clase para probar el generador de terreno (sin renderizado)
 */
public class TerrainGeneratorTest {

    /**
     * Método principal para ejecutar las pruebas
     */
    public static void main(String[] args) {
        System.out.println("Iniciando prueba del generador de terreno...");

        // Inicializar registro de bloques
        BlockRegistry blockRegistry = BlockRegistry.getInstance();
        System.out.println("Registro de bloques inicializado con " + blockRegistry.getBlockCount() + " bloques");

        try {
            // Semilla fija para pruebas consistentes
            long testSeed = 12345;
    
            // Crear el generador de terreno
            TerrainGenerator terrainGenerator = new TerrainGenerator(testSeed);
            
            // Crear un mundo de prueba sin inicializar OpenGL
            TestWorld testWorld = new TestWorld(testSeed);
            
            System.out.println("Generando chunks de prueba...");
            
            // Generar varios chunks para análisis
            final int SIZE = 5; // Tamaño del área de prueba (5x5 chunks)
            int totalBlocks = 0;
            int[] blockCounts = new int[blockRegistry.getBlockCount()];
            
            // Contar bloques generados por tipo en varios chunks
            for (int chunkX = -SIZE/2; chunkX <= SIZE/2; chunkX++) {
                for (int chunkZ = -SIZE/2; chunkZ <= SIZE/2; chunkZ++) {
                    // Crear chunk y generar terreno
                    Chunk chunk = new Chunk(testWorld, chunkX, chunkZ);
                    terrainGenerator.generateTerrain(chunk);
                    terrainGenerator.populateChunk(chunk);
                    
                    // Contar bloques por tipo
                    for (int x = 0; x < Chunk.WIDTH; x++) {
                        for (int y = 0; y < Chunk.HEIGHT; y++) {
                            for (int z = 0; z < Chunk.DEPTH; z++) {
                                byte blockId = chunk.getBlockId(x, y, z);
                                if (blockId >= 0 && blockId < blockCounts.length) {
                                    blockCounts[blockId]++;
                                    totalBlocks++;
                                }
                            }
                        }
                    }
                    
                    // Para inspección visual, imprimir un muestreo de alturas del terreno
                    if (chunkX == 0 && chunkZ == 0) {
                        System.out.println("Muestra de alturas del terreno (chunk 0,0):");
                        for (int z = 0; z < Chunk.DEPTH; z+=2) {
                            for (int x = 0; x < Chunk.WIDTH; x+=2) {
                                int highestBlock = 0;
                                for (int y = Chunk.HEIGHT-1; y >= 0; y--) {
                                    if (chunk.getBlockId(x, y, z) != BlockRegistry.AIR_ID) {
                                        highestBlock = y;
                                        break;
                                    }
                                }
                                System.out.print(String.format("%3d ", highestBlock));
                            }
                            System.out.println();
                        }
                    }
                    
                    // Analizar biomas en el chunk
                    if (chunkX == 0 && chunkZ == 0) {
                        System.out.println("\nDistribución de biomas en el chunk central:");
                        int[] biomeCounts = new int[4]; // Corresponde a los tipos de biomas definidos
                        for (int x = 0; x < Chunk.WIDTH; x++) {
                            for (int z = 0; z < Chunk.DEPTH; z++) {
                                int worldX = chunkX * Chunk.WIDTH + x;
                                int worldZ = chunkZ * Chunk.DEPTH + z;
                                int biome = terrainGenerator.getBiomeAt(worldX, worldZ);
                                biomeCounts[biome]++;
                            }
                        }
                        
                        String[] biomeNames = {"Llanuras", "Bosque", "Desierto", "Montañas"};
                        for (int i = 0; i < biomeCounts.length; i++) {
                            System.out.println(biomeNames[i] + ": " + biomeCounts[i] + " bloques");
                        }
                    }
                }
            }
            
            // Mostrar estadísticas de bloques
            System.out.println("\nEstadísticas de bloques en " + (SIZE*SIZE) + " chunks:");
            System.out.println("Total de bloques analizados: " + totalBlocks);
            
            // Mostrar porcentajes por tipo de bloque
            for (int id = 0; id < blockCounts.length; id++) {
                if (blockCounts[id] > 0) {
                    String blockName = blockRegistry.getBlock(id).getName();
                    double percentage = (blockCounts[id] * 100.0) / totalBlocks;
                    System.out.printf("%s (ID:%d): %.2f%% (%d bloques)\n", blockName, id, percentage, blockCounts[id]);
                }
            }
            
            System.out.println("\nPrueba completada.");
            
            // Comprobar si los valores son razonables (distribución esperada)
            boolean validAirPercentage = blockCounts[BlockRegistry.AIR_ID] > totalBlocks * 0.5; // >50% aire es normal
            boolean validStonePercentage = blockCounts[BlockRegistry.STONE_ID] > totalBlocks * 0.2; // >20% piedra es normal
            boolean hasTrees = blockCounts[BlockRegistry.WOOD_ID] > 0 && blockCounts[BlockRegistry.LEAVES_ID] > 0;
            
            System.out.println("\nResultados de validación:");
            System.out.println("- Distribución de aire adecuada: " + (validAirPercentage ? "SÍ" : "NO"));
            System.out.println("- Distribución de piedra adecuada: " + (validStonePercentage ? "SÍ" : "NO"));
            System.out.println("- Generación de árboles presente: " + (hasTrees ? "SÍ" : "NO"));
        
        } catch (Exception e) {
            System.err.println("Error en la prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Versión especial de World para pruebas, que no inicializa OpenGL
     */
    private static class TestWorld extends World {
        
        public TestWorld(long seed) {
            super(seed);
        }
        
        /**
         * Sobrescribe el método initRendering para evitar la inicialización de OpenGL
         */
        @Override
        protected void initRendering() {
            System.out.println("Renderizado desactivado para pruebas");
            // No inicializar shaders ni texturas
        }
        
        /**
         * Sobrescribe el método render para no hacer nada
         */
        @Override
        public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix, Vector3f playerPos) {
            // No hacer nada
        }
    }
}