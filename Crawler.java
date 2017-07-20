import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Этот класс реализует основную функциональность искателя веб-страниц. В нём
 * есть метод getSites для хранения всех ссылок на данной веб-странице в
 * дополнение к основному методу, который отслеживает важные переменные.
 * @author sergeychaika
 */
public class Crawler {
    
    /**
     * Программа принимает на вход начальный URL, максимальную глубину поиска и
     * количество потоков.
     * @param args {URL, глубина, количество потоков}
     * @throws java.io.IOException Стандартные исключения.
     */
    public static void main(String[] args) throws IOException {
        
        /**
         * Максимальная глубина.
         */
        int maxDepthPair = 0;
        
        int numThreads = 0;
        
        if (args.length != 3) {
            
            System.out.println("usage: java Crawler <URL> <depth> "
                    + "<number of crawler threads>");
            System.exit(1);
        }
        
        /**
         * Если пользователь ввёл правильное количество параметров, то
         * продолжаем.
         */
        try {
                
            /**
             * Перевод аргумента командной строки из string в integer.
             */
            maxDepthPair = Integer.parseInt(args[1]);
            numThreads = Integer.parseInt(args[2]);
        }
            
        catch (NumberFormatException nfe) {
            
            /**
             * Если второй и/или третий аргумент командной строки был введён не
             * числом, то остановка.
             */
            System.out.println("usage: java Crawler <URL> <depth> "
                    + "<number of crawler threads>");
            System.exit(1);
        }
        
        URLDepthPair currentDepthPair = new URLDepthPair(args[0], 0);
        
        /**
         * Содание нового пула.
         */
        URLPool pool = new URLPool(maxDepthPair);
        pool.put(currentDepthPair);
        
        /**
         * Поле для начального количества потоков.
         */
        int initialActiveThreads = Thread.activeCount();
        
        /**
         * Если количество ожидающих потоки не равно запрошенному числу
         * потоков и если количество всех потоков меньше запрошенного количества
         * потоков, то создаём больше потоков и запускаем их на CrawlerTask.
         * Иначе ждём.
         */
        while (pool.getWaitThreads() != numThreads) {
            
            if (Thread.activeCount() - initialActiveThreads < numThreads) {
                
                CrawlerTask crawler = new CrawlerTask(pool);
                new Thread(crawler).start();
            }
            
            else {
                
                try {
                    
                    Thread.sleep(500);
                }
                
                catch (InterruptedException ie) {
                    
                    System.out.println("Caught unexpected InterruptedException,"
                            + " ignoring...");
                }
            }
        }
            
        /**
         * Если все потоки ждут вывод найденных пар <глубина, URL> на экран.
         */
        for (int counter = 0; counter < pool.processedURLs.size(); ++counter){
            
            System.out.println(pool.processedURLs.get(counter));
        }
        
        System.exit(0);
    }
    
    /**
     * Поиск всех ссылок на рассматриваемом сайте и сохранение их в
     * список LinkedList<String>.
     */
    static LinkedList<String> getSites(URLDepthPair myDepthPair)
            throws IOException {
        
        /**
         * Создание списка LinkedList<String>, списка всех ссылок на
         * рассматриваемом сайте.
         */
        LinkedList<String> URLs = new LinkedList<>();
        
        /**
         * Создаём новый сокет.
         */
        Socket sock;
        
        /**
         * Инициализируем новый сокет из строки String, содержащей имя хоста, и
         * из номера порта, равный 80 (http).
         */
        try {
            
            sock = new Socket(myDepthPair.getWebHost(), 80);   
        }
        
        catch (UnknownHostException e) {
            
            System.err.println("UnknownHostException: " + e.getMessage());
            return URLs;
        }

        catch (IOException ex) {
            
            System.err.println("IOException: " + ex.getMessage()); 
            return URLs;
        }
        
        /**
         * Устанавливает таймаут сокета на 3 секунды.
         */
        try {
            
            sock.setSoTimeout(3000);
        }
        
        catch (SocketException exc) {
            
            System.err.println("SocketException: " + exc.getMessage());
            return URLs;
        }
        
        /**
         * Поля для хранения пути к файлу и хоста.
         */
        String docPath = myDepthPair.getDocPath();
        String webHost = myDepthPair.getWebHost();
        
        OutputStream outStream;
        
        /**
         * Возвращает OutputStream связанный с сокетом используемый для передачи
         * данных.
         */
        try {
            
            outStream = sock.getOutputStream(); 
        }
        
        catch (IOException exce) {
            
            System.err.println("IOException: " + exce.getMessage());
            return URLs;
        }
        
        PrintWriter myWriter = new PrintWriter(outStream, true);
        
        /**
         * Составление запроса на сервер сайта.
         */
        if (docPath.length() == 0) {
            
            myWriter.println("GET / HTTP/1.1");
            myWriter.println("Host: " + webHost);
            myWriter.println("Connection: close");
            myWriter.println();
        }
        else {
            
            myWriter.println("GET " + docPath + " HTTP/1.1");
            myWriter.println("Host: " + webHost);
            myWriter.println("Connection: close");
            myWriter.println();  
        }
        
        InputStream inStream;
        
        /**
         * Возвращает InputStream связанный с объектом Socket используемый для
         * приема данных.
         */
        try {
            
            inStream = sock.getInputStream(); 
        }

        catch (IOException excep){
            
            System.err.println("IOException: " + excep.getMessage());
            return URLs;
        }
        
        InputStreamReader inStreamReader = new InputStreamReader(inStream);
        BufferedReader BuffReader = new BufferedReader(inStreamReader);
        
        /**
         * Проверям код ответа сервера.
         */
        int serverCode = 0;
        String lineCode;
        
        try {
            
            lineCode = BuffReader.readLine();
        }
        
        catch (IOException except) {
                
            System.err.println("IOException: " + except.getMessage());
            return URLs;
        }
        
        /**
         * Если сервер вернул пустой ответ, то заканчиваем обработку ответа.
         */
        if (lineCode == null) {
            
            System.out.println("Ошибка: сайт \"" + myDepthPair.getURL() + 
                    "\" вернул пустой ответ");
                         
            /**
             * Возвращаем пустой список ссылок.
             */
            return URLs;
        }
        
        /**
         * Паттерн для кодов html: 2xx, 3xx, 4xx.
         */
        Pattern patternCode = Pattern.compile("(2|3|4)[0-9]{2}");
        Matcher matcherCode = patternCode.matcher(lineCode);
        
        /**
         * Поиск кода html: 2xx, 3xx, 4xx.
         */
        while (matcherCode.find()) {
                
            serverCode = Integer.valueOf(lineCode.substring(matcherCode.start(),
                    matcherCode.end() - 2));
        }
        
        /**
         * Обработка для кодов html, равных 2xx.
         */
        if (serverCode == 2) {
            
            /**
             * Читаем строчка за строчкой ответ сервера.
             */
            while (true) {
                
                String line;
                        
                try {
                
                    line = BuffReader.readLine();
                }            
                        
                catch (IOException except) {
                
                    System.err.println("IOException: " + except.getMessage());
                    
                    /**
                     * Возвращаем пустой список ссылок.
                     */
                    return URLs;
                }
                
                /**
                 * Конец чтения документа.
                 */
                if (line == null) {
                
                    break;
                }
            
                /**
                 * Паттерн для поиска URL.
                 */
                Pattern patternURL = Pattern.compile(
                    "[\"]"              /**
                                         * Перед ссылкой должно быть ".
                                         */
                    + "[https?://]{7,8}"/**
                                         * Может быть http://, а может быть
                                         * https://.
                                         */
                    + "([w]{3})?"       /**
                                         * www может быть, а может не быть.
                                         */
                    + "[\\w\\.\\-]+"    /** 
                                         * Хост сайта без домена 1-ого уровня.
                                         */
                    + "\\."             /**
                                         * Точка перед доменом 1-ого уровня.
                                         */
                    + "[A-Za-z]{2,6}"   /**
                                         * Домен 1-ого уровня.
                                         */
                    + "[\\w\\.-/]*"     /**
                                         * Путь к странице.
                                         */
                    + "[\"]");          /**
                                         * После ссылки должно быть ".
                                         */
            
                Matcher matcherURL = patternURL.matcher(line);
            
                /**
                 * Поиск URL в строке с помощью паттерна.
                 */
                while (matcherURL.find()) {
                
                    String newLink = line.substring(matcherURL.start() + 1,
                            matcherURL.end() - 1);
                
                    /**
                     * Добавление ссылки в список URLs.
                     */
                    URLs.add(newLink);
                }
            }
        
            sock.close();
        
            /**
             * Возвращаем все ссылки на рассматриваемом сайте.
             */
            return URLs;
        }
        
        /**
         * Обработка для кодов html, равных 3xx.
         */
        if (serverCode == 3) {
            
            /**
             * Поле для исправленного URL.
             */
            String newURL = "";
            String tempLine;
            
            while (true) {
                
                try {
                
                    tempLine = BuffReader.readLine();
                }            
                        
                catch (IOException except) {
                
                    System.err.println("IOException: " + except.getMessage());
                    
                    /**
                     * Возвращаем пустой список ссылок.
                     */
                    return URLs;
                }
                
                /**
                 * Конец чтения документа.
                 */
                if (tempLine == null) {
                
                    break;
                }
                
                /**
                 * Паттерн для поиска URL для перенаправления.
                 */
                Pattern patternNewURL = Pattern.compile(
                        "(Location: ){1}[\\S]+");
                Matcher matcherNewURL = patternNewURL.matcher(tempLine);
        
                while (matcherNewURL.find()) {
                
                    newURL = tempLine.substring(matcherNewURL.start() + 10,
                            matcherNewURL.end());
                }
            }
            
            if (newURL.equals(myDepthPair.getURL())) {
                
                System.out.println("Ошибка: сайт \"" + myDepthPair.getURL() +
                            "\" перенаправляет на самого себя"
                                + " (код ответа HTML 3xx)");

                sock.close();
                
                /**
                 * Возвращаем пустой список ссылок.
                 */
                return URLs;
            }
            
            URLDepthPair newDepthPair;
            newDepthPair = new URLDepthPair(newURL, myDepthPair.getDepth());
            
            /**
             * Вызываем этот метод с исправленным URL.
             */
            return getSites(newDepthPair);
        }
        
        /**
         * Обработка для кодов html, равных 4xx.
         */
        else {
            
            System.out.println("Ошибка: сайт \"" + myDepthPair.getURL() +
                            "\" недоступен (код ответа HTML 4xx)");
            
            sock.close();
            
            /**
             * Возвращаем пустой список ссылок.
             */
            return URLs;
        }
    }   
}