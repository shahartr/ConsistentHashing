import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.stage.Stage;

import java.util.*;

public class Main extends Application {
    static final int KEYS = 10000;
    static final int SERVERS = 10;
    static final long MAX_VALUE = (long) Math.pow(2, 32) - 1;
    static final int VIRTUAL_SERVERS = 400;

    static List<Double> loadsResults;
    static List<Double> loadsWithCopiesResults;


    public static void main(String[] args) {
        //a:
        long[] keys = createRandomArray(KEYS);

        //b:
        long[] servers = createRandomArray(SERVERS);

        //c:
        Map<Long, Long> keyToServerMap = assignKeysToServers(keys, servers);

        //d:
        Map<Long, Integer> loads = getLoads(keyToServerMap); // loads is a server to num of assigned keys map

        double median = computeMedian(loads);
        double average = computeAverage(loads);
        double minimum = computeMinimum(loads);
        double maximum = computeMaximum(loads);
        double percentile25 = computePercentile(loads, 0.25);
        double percentile75 = computePercentile(loads, 0.75);
        addResultsToLoadsResults(median, average, minimum, maximum, percentile25, percentile75);

        //e:
        long[] serversWithCopies = createServersWithVirtualCopies(servers);
        Map<Long, List<Long>> serverToVirtualCopies = createServersVirtualCopies(servers, serversWithCopies);

        //f:
        Map<Long, Long> keyToServerMapWithCopies = assignKeysToServers(keys, serversWithCopies);
        Map<Long, Integer> loadsWithCopies = getLoads(keyToServerMapWithCopies);

        double medianWithCopies = computeMedian(loadsWithCopies);
        double averageWithCopies = computeAverage(loadsWithCopies);
        double minimumWithCopies = computeMinimum(loadsWithCopies);
        double maximumWithCopies = computeMaximum(loadsWithCopies);
        double percentile25WithCopies = computePercentile(loadsWithCopies, 0.25);
        double percentile75WithCopies = computePercentile(loadsWithCopies, 0.75);
        addResultsToLoadsWithCopiesResults(medianWithCopies, averageWithCopies, minimumWithCopies, maximumWithCopies, percentile25WithCopies, percentile75WithCopies);

        //g:
        launch(args);
    }

    private static long[] createRandomArray(int arraySize) {
        long[] array = new long[arraySize];
        Set<Long> uniqueNumbers = new HashSet<>();
        Random random = new Random();

        for (int i = 0; i < arraySize; i++) {
            long randomNumber;
            do {
                randomNumber = random.nextLong() & MAX_VALUE;
            }
            while (!uniqueNumbers.add(randomNumber));
            array[i] = randomNumber;
        }

        return array;
    }

    private static Map<Long, Long> assignKeysToServers(long[] keys, long[] servers) {
        Map<Long, Long> keyToServerMap = new HashMap<>();

        Arrays.sort(servers);

        for (long key: keys) {
            long assignedServer = assignServerForKey(key, servers);
            keyToServerMap.put(key, assignedServer);
        }
        return keyToServerMap;
    }

    private static long assignServerForKey(long key, long[] servers) {
        for (long server : servers) {
            if (server > key) {
                return server;
            }
        }
        // If no server is larger than the key, wrap around and assign to the smallest server
        return servers[0];
    }

    private static Map<Long, Integer> getLoads(Map<Long, Long> keyToServerMap) {
        Map<Long, Integer> loads = new HashMap<>();

        for (long key : keyToServerMap.keySet()) {
            long server = keyToServerMap.get(key);

            if(loads.containsKey(server)){
                loads.put(server, loads.get(server)+1);
            }
            else{
                loads.put(server, 1);
            }
        }
        return loads;
    }

    private static double computeMedian(Map<Long, Integer> loads) {
        List<Integer> loadValues = new ArrayList<>(loads.values());
        Collections.sort(loadValues);

        double median;
        if (loadValues.size() % 2 == 0) {
            median = (loadValues.get(loadValues.size() / 2 - 1) + loadValues.get(loadValues.size() / 2)) / 2.0;
        }
        else {
            median = loadValues.get(loadValues.size() / 2);
        }
        return median;
    }

    private static double computeAverage(Map<Long, Integer> loads) {
        List<Integer> loadValues = new ArrayList<>(loads.values());
        int sum = 0;

        for (int keys : loadValues) {
            sum += keys;
        }

        double average = sum / SERVERS;
        return average;
    }

    private static double computeMinimum(Map<Long, Integer> loads) {
        List<Integer> loadValues = new ArrayList<>(loads.values());

        int minimum = Integer.MAX_VALUE;

        for (int keys : loadValues) {
            minimum = Math.min(minimum, keys);
        }

        return minimum;
    }

    private static double computeMaximum(Map<Long, Integer> loads) {
        List<Integer> loadValues = new ArrayList<>(loads.values());

        int maximum = 0;

        for (int keys : loadValues) {
            maximum = Math.max(maximum, keys);
        }

        return maximum;
    }

    private static double computePercentile(Map<Long, Integer> loads, double percent) {
        List<Integer> loadValues = new ArrayList<>(loads.values());

        Collections.sort(loadValues);

        double percentile = loadValues.get((int) Math.ceil(percent * loadValues.size()));

        return percentile;
    }

    private static void addResultsToLoadsResults(double median, double average, double minimum, double maximum, double percentile25, double percentile75) {
        loadsResults = new ArrayList<>();
        loadsResults.add(median);
        loadsResults.add(average);
        loadsResults.add(minimum);
        loadsResults.add(maximum);
        loadsResults.add(percentile25);
        loadsResults.add(percentile75);
    }

    private static void addResultsToLoadsWithCopiesResults(double medianWithCopies, double averageWithCopies, double minimumWithCopies, double maximumWithCopies, double percentile25WithCopies, double percentile75WithCopies) {
        loadsWithCopiesResults = new ArrayList<>();
        loadsWithCopiesResults.add(medianWithCopies);
        loadsWithCopiesResults.add(averageWithCopies);
        loadsWithCopiesResults.add(minimumWithCopies);
        loadsWithCopiesResults.add(maximumWithCopies);
        loadsWithCopiesResults.add(percentile25WithCopies);
        loadsWithCopiesResults.add(percentile75WithCopies);
    }

    private static Map<Long, List<Long>> createServersVirtualCopies(long[] servers, long[] virtualServers) {
        Map<Long, List<Long>> serverToVirtualCopies = new HashMap<>();

        int virtualServerIndex = 0;
        for(int i = 0; i < servers.length; i++){
            List<Long> copies = new ArrayList<>();

            copies.add(virtualServers[virtualServerIndex]);
            copies.add(virtualServers[virtualServerIndex+1]);
            copies.add(virtualServers[virtualServerIndex+2]);
            copies.add(virtualServers[virtualServerIndex+3]);

            serverToVirtualCopies.put(servers[i], copies);

            virtualServerIndex = virtualServerIndex + 4;
        }

        return serverToVirtualCopies;
    }

    private static long[] createServersWithVirtualCopies(long[] servers) {
        long[] ServersWithVirtualCopies = new long[VIRTUAL_SERVERS + SERVERS];
        Set<Long> uniqueNumbers = new HashSet<>();
        Random random = new Random();

        for (int i = 0; i < VIRTUAL_SERVERS; i++) {
            long randomNumber;
            do {
                randomNumber = random.nextLong() & MAX_VALUE;
            }
            while (!uniqueNumbers.add(randomNumber) || isContainInServers(randomNumber, servers));
            ServersWithVirtualCopies[i] = randomNumber;
        }

        int j = 0;
        for(int i = VIRTUAL_SERVERS; i < VIRTUAL_SERVERS + SERVERS; i++){
            ServersWithVirtualCopies[i] = servers[j];
            j++;
        }

        return ServersWithVirtualCopies;
    }

    private static boolean isContainInServers(long randomNumber, long[] servers) {
        for (long value : servers) {
            if (value == randomNumber) {
                return true;
            }
        }
        return false;
    }

    //g:
    @Override
    public void start(Stage primaryStage){
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);

        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName("Without Copies");
        series1.getData().add(new XYChart.Data<>("Median", loadsResults.get(0)));
        series1.getData().add(new XYChart.Data<>("Average", loadsResults.get(1)));
        series1.getData().add(new XYChart.Data<>("Minimum", loadsResults.get(2)));
        series1.getData().add(new XYChart.Data<>("Maximum", loadsResults.get(3)));
        series1.getData().add(new XYChart.Data<>("Percentile 25", loadsResults.get(4)));
        series1.getData().add(new XYChart.Data<>("Percentile 75", loadsResults.get(5)));

        XYChart.Series<String, Number> series2 = new XYChart.Series<>();
        series2.setName("With Copies");
        series2.getData().add(new XYChart.Data<>("Median", loadsWithCopiesResults.get(0)));
        series2.getData().add(new XYChart.Data<>("Average", loadsWithCopiesResults.get(1)));
        series2.getData().add(new XYChart.Data<>("Minimum", loadsWithCopiesResults.get(2)));
        series2.getData().add(new XYChart.Data<>("Maximum", loadsWithCopiesResults.get(3)));
        series2.getData().add(new XYChart.Data<>("Percentile 25", loadsWithCopiesResults.get(4)));
        series2.getData().add(new XYChart.Data<>("Percentile 75", loadsWithCopiesResults.get(5)));

        Scene scene = new Scene(barChart, 800, 600);
        barChart.getData().addAll(series1, series2);

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}