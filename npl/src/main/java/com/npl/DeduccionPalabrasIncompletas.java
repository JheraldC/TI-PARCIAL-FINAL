package com.npl;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class DeduccionPalabrasIncompletas {

    private static final String DICCIONARIO_PATH = "dic.txt";
    private static final int UMBRAL_LONGITUD_PALABRA = 3;
    private static final int UMBRAL_DISTANCIA = 2; // Reduce umbral de distancia

    public static void main(String[] args) {
        // 1. Inicializar Stanford CoreNLP
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos");
        props.setProperty("tokenize.language", "es");
        props.setProperty("pos.model", "edu/stanford/nlp/models/pos-tagger/spanish-ud.tagger");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // 2. Cargar diccionario
        Set<String> diccionario = cargarDiccionario(DICCIONARIO_PATH);
        String textoHistorico = "La ciuda de Arequip fundda en 1540.";

        // 4. Procesar el texto
        Annotation documento = new Annotation(textoHistorico);
        pipeline.annotate(documento);

        List<CoreMap> oraciones = documento.get(SentencesAnnotation.class);
        for (CoreMap oracion : oraciones) {
            for (CoreLabel token : oracion.get(TokensAnnotation.class)) {
                String palabraOriginal = token.originalText();

                if (palabraOriginal.length() >= UMBRAL_LONGITUD_PALABRA &&
                        !diccionario.contains(palabraOriginal.toLowerCase()) &&
                        !esNumericaOPuntuacion(palabraOriginal)) {

                    String palabraCorregida = corregirPalabra(palabraOriginal, diccionario);
                    System.out.println(
                            "Palabra original: " + palabraOriginal + " -> Palabra corregida: " + palabraCorregida);
                } else {
                    System.out.println(
                            "Palabra original: " + palabraOriginal + " -> Palabra corregida: " + palabraOriginal);
                }
            }
        }
    }

    // Método para cargar el diccionario desde un archivo
    private static Set<String> cargarDiccionario(String path) {
        Set<String> palabras = new HashSet<>();
        try (InputStream is = DeduccionPalabrasIncompletas.class.getClassLoader().getResourceAsStream(path);
                Scanner scanner = new Scanner(is)) {
            while (scanner.hasNext()) {
                palabras.add(scanner.nextLine().toLowerCase());
            }
        } catch (Exception e) {
            System.err.println("Error al cargar el diccionario: " + e.getMessage());
        }
        return palabras;
    }

    // Función para corregir una palabra usando distancia de Levenshtein
    private static String corregirPalabra(String palabra, Set<String> diccionario) {
        String mejorCandidata = palabra;
        int menorDistancia = Integer.MAX_VALUE;

        for (String candidata : diccionario) {
            int distancia = calcularDistanciaLevenshtein(palabra.toLowerCase(), candidata);
            if (distancia < menorDistancia && distancia <= UMBRAL_DISTANCIA) {
                menorDistancia = distancia;
                mejorCandidata = candidata;
            }
        }
        return mejorCandidata;
    }

    // Implementación de la distancia de Levenshtein
    private static int calcularDistanciaLevenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
                }
            }
        }

        return dp[a.length()][b.length()];
    }

    // Método para validar si la palabra es numérica o puntuación
    private static boolean esNumericaOPuntuacion(String palabra) {
        return palabra.chars().allMatch(Character::isDigit) || Pattern.matches("\\p{Punct}", palabra);
    }
}
