package juan.motores_busqueda.practica3_mb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author juald
 */
public class SolrQueryParser
{

    public static Map<Integer, String> readQueriesFromFile(String filePath) throws IOException
    {
        Map<Integer, String> queries = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        boolean readingContent = false;
        int currentQueryId = -1;
        StringBuilder currentQuery = new StringBuilder();

        while ((line = reader.readLine()) != null)
        {
            if (line.startsWith(".I"))
            {
                if (currentQueryId != -1)
                {
                    //queries.put(currentQueryId, currentQuery.toString().trim());
                    String encodedQuery = URLEncoder.encode(currentQuery.toString().trim(), "UTF-8");
                    //String encodedQuery = removeColon(currentQuery.toString().trim());
                    queries.put(currentQueryId, encodedQuery);
                }
                currentQueryId = Integer.parseInt(line.split("\\s+")[1]);
                currentQuery = new StringBuilder();
            } else if (line.startsWith(".A"))
            {
                readingContent = false;
            } else if (line.startsWith(".T"))
            {
                readingContent = false;
            } else if (line.startsWith(".W"))
            {
                readingContent = true;
                continue;
            } else if (line.startsWith(".B"))
            {
                readingContent = false;
            } else if (readingContent)
            {
                currentQuery.append(line).append(" ");
            }
        }

        // Add the last query
        if (currentQueryId != -1)
        {
            String encodedQuery = URLEncoder.encode(currentQuery.toString().trim(), "UTF-8");
            //queries.put(currentQueryId, currentQuery.toString().trim());
            encodedQuery = encodedQuery.replace(":", "");
            queries.put(currentQueryId, encodedQuery);
        }
        reader.close();
        return queries;
    }
    
    // Método para eliminar los dos puntos usando expresiones regulares
    private static String removeColon(String input) {
        Pattern pattern = Pattern.compile(":");
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll("");
    }
}
