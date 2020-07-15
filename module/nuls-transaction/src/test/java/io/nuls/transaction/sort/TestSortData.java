package io.nuls.transaction.sort;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eva
 */
public class TestSortData implements Comparable<TestSortData> {

    private String id;

    private List<String> preId;

    public TestSortData(String id, String... preId) {
        this.id = id;
        this.preId = new ArrayList<>();
        for (String pre : preId) {
            this.preId.add(pre);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getPreId() {
        return preId;
    }

    public void setPreId(List<String> preId) {
        this.preId = preId;
    }

    @Override
    public int compareTo(TestSortData o) {
        int val = 0;
        if (o.getPreId().contains(this.getId())) {
            val = -1;
        } else if (this.getPreId().contains(o.getId())) {
            val = 1;
        }
//        System.out.println(this.getId() + " and " + o.getId() + " ==== " + val);
        return val;
    }

    public String toString(){
        return "id: "+this.getId();
    }
}
