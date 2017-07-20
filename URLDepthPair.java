import java.net.MalformedURLException;
import java.net.URL;

/**
 * Класс, хранящий пары <URL, глубина> для класса Crawler.
 * @author sergeychaika
 */
public class URLDepthPair {
    
    /**
     * Поля для хранения текущего URL и текущей глубины.
     */
    private final String currentURL;
    private final int currentDepth;
    
    /**
     * Конструктор сохраняет пару <URL, глубина> в соответствии с аргументом.
     * @param URL Текущий URL.
     * @param depth Текущая глубина.
     */
    public URLDepthPair(String URL, int depth) {
        
        currentURL = URL;
        currentDepth = depth;
    }
    
    /**
     * Метод для доступа к текущему URL.
     * @return Текущий URL.
     */
    public String getURL() {
        
        return currentURL;
    }
    
    /** 
     * Метод для доступа к текущей глубине.
     * @return Текущая глубина.
     */
    public int getDepth() {
        
        return currentDepth;
    }
    
    /**
     * Метод для доступа к строке вида <глубина, URL>.
     * @return 
     */
    @Override
    public String toString() {
        
        String stringDepth = Integer.toString(currentDepth);
        return stringDepth + '\t' + currentURL;
    }
    
    /** 
     * Метод, который преобразует текущий URL в путь к файлу. 
     * @return Путь к файлу.
     */
    public String getDocPath() {
        
        try {
            
            URL url = new URL(currentURL);
            return url.getPath();
        }
        
        catch (MalformedURLException e) {
            
            System.err.println("MalformedURLException: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Метод возращает хост текущего URL.
     * @return Хост текущего URL.
     */
    public String getWebHost() {
        
        try {
            
            URL url = new URL(currentURL);
            return url.getHost();
        }
        
        catch (MalformedURLException e) {
            
            System.err.println("MalformedURLException: " + e.getMessage());
            return null;
        }
    }   
}