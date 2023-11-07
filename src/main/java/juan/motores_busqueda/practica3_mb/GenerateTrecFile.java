package juan.motores_busqueda.practica3_mb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 *
 * @author juald
 */
public class GenerateTrecFile
{

    public static void main(String[] args)
    {
        String solrUrl = "http://localhost:8983/solr/CORPUS2"; 
        HttpSolrClient solrClient = new HttpSolrClient.Builder(solrUrl).build();

        String cisiQueryFilePath = "src\\main\\java\\resources\\CISI.QRY";
        String trecTopFilePath = "trec_solr_file.trec";

        try
        {
            Map<Integer, String> queries = SolrQueryParser.readQueriesFromFile(cisiQueryFilePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(trecTopFilePath));

            for (Map.Entry<Integer, String> entry : queries.entrySet())
            {
                int queryId = entry.getKey();
                System.out.println(queryId);
                String queryString = entry.getValue();
                System.out.println(queryString);

                SolrQuery solrQuery = new SolrQuery();
                solrQuery.setQuery("content:" + queryString);
                solrQuery.set("fl", "id,score");

                List<String> relevantDocuments = performSolrSearch(solrClient, solrQuery);

                // Escribe los resultados en el archivo trec_top_file
                for (int i = 0; i < relevantDocuments.size(); i++)
                {
                    writer.write(queryId + " ");
                    writer.write(relevantDocuments.get(i) + "\n");
                }
            }
            writer.close();
            solrClient.close();
            System.out.println("Los resultados se han escrito en " + trecTopFilePath);
        } catch (IOException | SolrServerException e)
        {
            Logger.getLogger(GenerateTrecFile.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static List<String> performSolrSearch(SolrClient solr, SolrQuery query)
            throws SolrServerException, IOException
    {
        // Realiza la búsqueda en Solr y devuelve una lista de documentos relevantes
        QueryResponse response = solr.query(query);
        SolrDocumentList results = response.getResults();
        List<String> relevantDocumentIds = new ArrayList<>();
        int ranking = 1;
        for (SolrDocument document : results)
        {
            String trecLine = "QO" + " " + (String) document.getFieldValue("id") + " " + ranking + " " + document.getFieldValue("score").toString() + " " + "ETSI";
            relevantDocumentIds.add(trecLine);
            ranking++;
        }
        return relevantDocumentIds;
    }
}
