import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implement the two methods below. We expect this class to be stateless and thread safe.
 */
public class Census {
    /**
     * Number of cores in the current machine.
     */
    private static final int CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Output format expected by our tests.
     */
    public static final String OUTPUT_FORMAT = "%d:%d=%d"; // Position:Age=Total

    /**
     * Factory for iterators.
     */
    private final Function<String, Census.AgeInputIterator> iteratorFactory;

    /**
     * Creates a new Census calculator.
     *
     * @param iteratorFactory factory for the iterators.
     */
    public Census(Function<String, Census.AgeInputIterator> iteratorFactory) {
        this.iteratorFactory = iteratorFactory;
    }

    /**
     * Given one region name, call {@link #iteratorFactory} to get an iterator for this region and return
     * the 3 most common ages in the format specified by {@link #OUTPUT_FORMAT}.
     */
    public String[] top3Ages(String region) {

        Map<Integer,Integer> agesCounter = new HashMap<>(); //Key:Age - Value:Frequency

        this.iteratorFactory.apply(region).forEachRemaining(ageEntry -> {
            if(ageEntry>=0) //valid ages only
                agesCounter.put(ageEntry, agesCounter.getOrDefault(ageEntry, 0) + 1);
        });

        //done with iterator ==> close
        try {
            this.iteratorFactory.apply(region).close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this.getTop3Ages(agesCounter);
    }

    /**
     * Given a list of region names, call {@link #iteratorFactory} to get an iterator for each region and return
     * the 3 most common ages across all regions in the format specified by {@link #OUTPUT_FORMAT}.
     * We expect you to make use of all cores in the machine, specified by {@link #CORES).
     */
    public String[] top3Ages(List<String> regionNames) {

        Map<Integer,Integer> agesCounter = new HashMap<>();

        regionNames.forEach(region -> { //loop on regions to get all ages from all regions
            AgeInputIterator currentIterator;
            try{
                currentIterator = this.iteratorFactory.apply(region);
                while(currentIterator.hasNext()){
                    int ageEntry = currentIterator.next();
                    if(ageEntry>=0) //valid ages only
                        agesCounter.put(ageEntry, agesCounter.getOrDefault(ageEntry, 0) + 1);
                }
                //done with current iterator ==> close
                try {
                    currentIterator.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        });

        if(agesCounter.entrySet().size()>0)
            return this.getTop3Ages(agesCounter);
        return new String[]{};

    }

    private String[] getTop3Ages(Map<Integer,Integer> agesCounter) {

        //sort counter of ages descendingly
        Comparator<Map.Entry<Integer,Integer>> descComparator = Map.Entry.comparingByValue(Comparator.reverseOrder());  
        Map<Integer, Integer> sortedAges = agesCounter.entrySet().stream().sorted(descComparator)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (age1,age2)->age1, LinkedHashMap::new));

        //get top distinct common items
        List<String> topAgesList = new ArrayList<>();
        int rank = 0;
        int prevAge = -1;
        for(Map.Entry<Integer,Integer> entry:sortedAges.entrySet()){
            if(rank>=3)
                break;
            if(entry.getValue()!=prevAge) {
                prevAge = entry.getValue();
                rank++;
            }
            String age = String.format(OUTPUT_FORMAT,rank, entry.getKey(), entry.getValue());
            topAgesList.add(age); 
        }

        //copy final list into string array
        String[] topAges = new String[topAgesList.size()];
        topAges = topAgesList.toArray(topAges);
        
        return topAges;
    }


    /**
     * Implementations of this interface will return ages on call to {@link Iterator#next()}. They may open resources
     * when being instantiated created.
     */
    public interface AgeInputIterator extends Iterator<Integer>, Closeable {
    }
}
