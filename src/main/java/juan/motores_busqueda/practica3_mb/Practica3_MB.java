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
import java.util.Collections;
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

    private static String extractWordsFromLine(BufferedReader reader, StringBuilder queryTextBuilder) throws IOException
    {
        Pattern wordPattern = Pattern.compile("\\w+");
        String line;
        while ((line = reader.readLine()) != null && !line.startsWith("."))
        {
            Matcher matcher = wordPattern.matcher(line);
            while (matcher.find() && queryTextBuilder.length() < 5)
            {
                queryTextBuilder.append(matcher.group()).append(" ");
            }
        }
        return queryTextBuilder.toString().trim();
    }

    private static int extractQueryIdFromLine(String line)
    {
        if (line.startsWith(".I"))
        {
            String[] parts = line.split("\\s+");
            if (parts.length >= 2)
            {
                try
                {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException e)
                {
                    // Manejar la excepción si no se puede convertir a entero
                    e.printStackTrace();
                }
            }
        }
        // Retornar un valor predeterminado si no se puede leer la ID de consulta
        return -1;
    }

    private static List<String> getResponseFromServer(Path pathToDocument, HttpSolrClient solrClient) throws SolrServerException, IOException
    {

        List<String> lineasTREC = new ArrayList<>();
        StringBuilder queryTextBuilder = new StringBuilder();
        ArrayList<Integer> numerosConsulta = new ArrayList<>();

        String line;
        SolrDocumentList docs = null;
        int ranking = 0; // Inicializar el ranking

        BufferedReader br = Files.newBufferedReader(pathToDocument.toAbsolutePath());
        while ((line = br.readLine()) != null)
        {
            if (line.startsWith(".I"))
            {
                /*String[] parts = line.split("\\s+");
                int idConsulta = Integer.parseInt(parts[1]);
                numerosConsulta.add(idConsulta);*/

                int queryId = extractQueryIdFromLine(line);
                if (queryId != -1)
                {
                    Collections.sort(numerosConsulta);
                    numerosConsulta.add(queryId);
                }

            } else if (line.startsWith(".W"))
            {
                String queryText = extractWordsFromLine(br, queryTextBuilder);

                // Construir la consulta a Solr
                SolrQuery solrQuery = new SolrQuery();
                solrQuery.setQuery("content:" + queryText);
                solrQuery.set("fl", "id,score");

                // Realizar la consulta a Solr y procesar los resultados
                QueryResponse response = solrClient.query(solrQuery);

                // Procesar los resultados de Solr
                docs = response.getResults();

                for (int i = 0; i < docs.size(); i++)
                {
                    SolrDocument doc = docs.get(i);
                    String idDocumento = (String) doc.getFieldValue("id");
                    Float score = (Float) doc.getFieldValue("score");

                    /*System.out.println("Numero de Consulta: " + numerosConsulta.get(i));
                    System.out.println("ID Documento: " + idDocumento);
                    System.out.println("Ranking: " + ranking);
                    System.out.println("Score: " + score);*/
                    for (int j = 0; j < numerosConsulta.size(); j++)
                    {
                        // Construir la línea en formato TREC
                        StringBuilder lineaTREC = new StringBuilder();
                        lineaTREC.append(numerosConsulta.get(j))
                                .append(" Q0 ")
                                .append(idDocumento)
                                .append(" ")
                                .append(ranking)
                                .append(" ")
                                .append(score)
                                .append(" ETSI");
                        lineasTREC.add(lineaTREC.toString());
                        // Incrementar el ranking para el próximo documento
                        ranking++;
                    }
                }
                // Limpiar el StringBuilder para la próxima consulta
                queryTextBuilder.setLength(0);
            }

        }
        System.out.println(numerosConsulta.size());
        return lineasTREC;
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

        try
        {
            String rutaArchivo = "trec_solr_file.trec";
            try
            {
                BufferedWriter writer = new BufferedWriter(new FileWriter(rutaArchivo));
                List<String> lineasTREC = getResponseFromServer(pathToDocument, solrClient);

                // Escribir la línea en el archivo
                for (int i = 0; i < lineasTREC.size(); i++)
                {
                    writer.write(lineasTREC.get(i));
                    writer.newLine();
                }
                System.out.println("Archivo TREC generado con éxito: " + rutaArchivo);
                writer.close();

            } catch (IOException ex)
            {
                Logger.getLogger(Practica3_MB.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
            scanner.close();
            solrClient.close();

        } catch (IOException | SolrServerException ex)
        {
            Logger.getLogger(Practica3_MB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
