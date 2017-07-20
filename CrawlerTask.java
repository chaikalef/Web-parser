import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CrawlerTask реализует интерфейс Runnable. Каждый экземпляр имеет ссылку на
 * экземпляр класса URLPool. Получает пару <URL, глубина> из пула (ждёт если
 * недоступно), извлекает веб-страницу, получает все URL-адреса со страницы и
 * добавляет новую пару URLDepth в пул URL для каждого найденного URL.
 * @author sergeychaika
 */
public class CrawlerTask implements Runnable {
    
    public URLDepthPair depthPair;    
    public URLPool pool;
    
    /**
     * Конструктор для инициализации пула.
     * @param newPool
     */
    public CrawlerTask (URLPool newPool) {
        
        pool = newPool;
    }
    
    /**
     * Метод для запуска задач в CrawlerTask.
     */
    @Override
    public void run() {

        /**
         * Достаём из пула следующую пару.
         */
        depthPair = pool.get();
        
        int depth = depthPair.getDepth();
        
        /**
         * Поиск всех ссылок на рассматриваемом сайте и сохранение их в
         * список linksList.
         */
        LinkedList<String> linksList = null;
        
        try {
            
            linksList = Crawler.getSites(depthPair);
        }
        
        catch (IOException ex) {
            
            Logger.getLogger(CrawlerTask.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
        
        for (int counter = 0; counter < linksList.size(); ++counter) {
            
            String newURL = linksList.get(counter);
            
            /**
             * Создание новой пары для каждой ссылки и добавление её в пул.
             */
            URLDepthPair newDepthPair = new URLDepthPair(newURL, depth + 1);
                
            pool.put(newDepthPair);
        }
    }
}