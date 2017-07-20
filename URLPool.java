import java.util.ArrayList;
import java.util.LinkedList;

/** 
 * Этот класс сохраняет пары <URL, глубина> для дальнейшего поиска.
 * @author sergeychaika
 */
public class URLPool {
    
    /**
     * Список для пар <URL, глубина>, которые ещё не просмотрены.
     */
    private final LinkedList<URLDepthPair> pendingURLs;
    
    /** 
     * Список для пар <URL, глубина>, которые уже просмотрены. 
     */
    public LinkedList<URLDepthPair> processedURLs;
    
    /** 
     * Список для URL, которые уже просмотрены.
     */
    private final ArrayList<String> seenURLs = new ArrayList<>();
    
    /**
     * Количество потоков, которые ожидают обработки.
     */
    public int waitingThreads;
    
    int maxDepth;
    
    /**
     * Конструктор для инициализации waitingThreads, processedURLs и 
     * pendingURLs.
     * @param maxDepthPair максимальная глубина поиска
     */
    public URLPool(int maxDepthPair) {
        
        maxDepth = maxDepthPair;
        waitingThreads = 0;
        pendingURLs = new LinkedList<>();
        processedURLs = new LinkedList<>();
    }
    
    /**
     * Метод для доступа к waitingThreads.
     * @return waitingThreads
     */
    public synchronized int getWaitThreads() {
        
        return waitingThreads;
    }
    
    /**
     * Метод для получения размера pendingURLs.
     * @return размер pendingURLs
     */
    public synchronized int size() {
        
        return pendingURLs.size();
    }
    
    /** 
     * Метод для добавление новой пары <URL, глубина>.
     * @param depthPair новая пара <URL, глубина>
     */
    public synchronized void put(URLDepthPair depthPair) {
        
        /**
         * Если был вызван put и есть потоки "в ожидании", то надо вызвать эти
         * потоки и декрементировать счётчик потоков "в ожидании".
         */
        if (waitingThreads != 0) {
                
            --waitingThreads;
            this.notify();
         }

        if (!seenURLs.contains(depthPair.getURL()) &
                !pendingURLs.contains(depthPair)) {
            
            if (depthPair.getDepth() < maxDepth) {
                
                pendingURLs.add(depthPair);
            }
            
            else {
                
                processedURLs.add(depthPair);
                seenURLs.add(depthPair.getURL());
            }
        }
    }

    /**
     * Метод для получения следующей пары из пула.
     * @return следующая пара из пула
     */
    public synchronized URLDepthPair get() {
        
        URLDepthPair myDepthPair;
        
        while (pendingURLs.isEmpty()) {
            
            ++waitingThreads;
            
            try {
                
                this.wait();
            }
            
            catch (InterruptedException e) {
                
                System.err.println("MalformedURLException: " + e.getMessage());
                return null;
            }
        }
        
        /**
         * Удаление первой пары и добавление её в seenURLs и processedURLs.
         */
        myDepthPair = pendingURLs.pop();
        
        while (seenURLs.contains(myDepthPair.getURL())) {
                
            myDepthPair = pendingURLs.pop();
        }
        
        processedURLs.add(myDepthPair);
        seenURLs.add(myDepthPair.getURL());
        
        return myDepthPair;
    }
}