package juan.motores_busqueda.practica3_mb;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 *
 * @author juald
 */
public class Practica3_MB
{

    private static final String OUTPUT_FILE_PATH = "trec_solr_file.trec";

    private static void writeToFile(String content) throws IOException
    {
        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, true)))
        {
            writer.write(content);
            writer.newLine();
        }
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Por favor, proporciona la ruta del archivo CISI.QRY como argumento.");
        String filePath = scanner.nextLine();
        Path pathToDocument = Paths.get(filePath);

        // Conexión al servidor Solr
        String solrUrl = "http://localhost:8983/solr/CORPUS2";
        HttpSolrClient solrClient = new HttpSolrClient.Builder(solrUrl).build();

        List<String> querysId = new ArrayList();
        StringBuilder queryTextBuilder = new StringBuilder();
        String line;
        SolrDocumentList docs;
        int ranking = 1;
        int wordsCount = 0;

        try ( BufferedReader br = Files.newBufferedReader(pathToDocument.toAbsolutePath()))
        {
            BufferedReader br2 = Files.newBufferedReader(pathToDocument.toAbsolutePath());
            while ((line = br.readLine()) != null)
            {
                if (line.startsWith(".I"))
                {
                    Pattern wordPatternId = Pattern.compile("^\\.I\\s(\\d+)$");
                    while ((line = br2.readLine()) != null)
                    {
                        //System.out.println("Línea leída: " + line);
                        Matcher matcher = wordPatternId.matcher(line);
                        if (matcher.matches())
                        {
                            querysId.add(matcher.group(1));
                            //System.out.println(querysId);
                        }
                    }

                } else if (line.startsWith(".W"))
                {
                    queryTextBuilder.setLength(0); // Limpiar el StringBuilder antes de procesar la nueva consulta
                    wordsCount = 0; // Reiniciar el contador de palabras
                    Pattern wordPattern = Pattern.compile("\\w+");
                    while ((line = br.readLine()) != null && !line.startsWith("."))
                    {
                        Matcher matcher = wordPattern.matcher(line);
                        while (matcher.find() && wordsCount < 5)
                        {
                            queryTextBuilder.append(matcher.group()).append(" ");
                            wordsCount++;
                        }
                    }
                    String queryText = queryTextBuilder.toString().trim();
                    //System.out.println(queryText);

                    // Construir la consulta a Solr
                    SolrQuery solrQuery = new SolrQuery();
                    solrQuery.setQuery("content:" + queryText);
                    solrQuery.set("fl", "id,score");

                    // Realizar la consulta a Solr y procesar los resultados
                    QueryResponse response = solrClient.query(solrQuery);

                    // Procesar los resultados de Solr
                    docs = response.getResults();

                    /*for (int i = 0; i < docs.size(); i++)
                    {
                        SolrDocument doc = docs.get(i);
                        String id = (String) doc.getFieldValue("id");
                        Float score = (Float) doc.getFieldValue("score");
                        for (String queryId : querysId)
                        {
                            // Crea la línea de salida en el formato TREC
                            String outputLine = queryId + " Q0 " + id + " " + ranking + " " + score + " ETSI";

                            // Escribir la línea en el archivo de salida
                            writeToFile(outputLine);
                        }
                        ranking++;
                    }*/
                    for (String queryId : querysId)
                    {
                        // Itera sobre los documentos relevantes para la consulta actual
                        for (int i = 0; i < docs.size(); i++)
                        {
                            SolrDocument doc = docs.get(i);
                            String id = (String) doc.getFieldValue("id");
                            Float score = (Float) doc.getFieldValue("score");

                            // Crea la línea de salida en el formato TREC
                            String outputLine = queryId + " Q0 " + id + " " + ranking + " " + score + " ETSI";

                            // Escribir la línea en el archivo de salida
                            writeToFile(outputLine);
                            ranking++;
                        }
                        // Reinicia el ranking para la próxima consulta
                        ranking = 1;
                    }
                    // Limpiar el StringBuilder para la próxima consulta
                    queryTextBuilder.setLength(0);
                }
            }
            solrClient.close();
            br.close();
            br2.close();
            scanner.close();
        } catch (IOException | SolrServerException e)
        {
            Logger.getLogger(Practica3_MB.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
