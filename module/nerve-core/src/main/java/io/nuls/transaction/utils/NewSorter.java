package io.nuls.transaction.utils;

import java.util.*;

/**
 * @author Eva
 */
public class NewSorter {


    public static <T extends Comparable<? super T>> List<T> sort(List<T> tlist) {
        if (tlist.size() <= 1) {
            return tlist;
        }
        Map<Integer, SortItem<T>> resultMap = new HashMap<>();

        tlist.forEach(po -> {
            doRank(resultMap, new SortItem<>(po));
        });
        List<SortItem<T>> list = new ArrayList<>();
        list.addAll(resultMap.values());

        Collections.sort(list);

        List<T> resultList = new ArrayList<>();
        for (SortItem<T> item : list) {
            resultList.add(item.getT());
        }
        return resultList;
//        int index = 0;
//        for (SortItem po : resultMap.values()) {
//            po.setOrderBy(po.getOrderBy() + (index++));
//        }
//        return tlist;
    }

    private static <T extends Comparable<? super T>> void doRank(Map<Integer, SortItem<T>> resultMap, SortItem<T> item) {
        for (SortItem<T> po : resultMap.values()) {
//            System.out.println("  po :" + po.getT().toString() + ", level : " + po.getLevel());
//            System.out.println("item :" + item.getT().toString() + ", level : " + item.getLevel());
            int val = item.getT().compareTo(po.getT());
            if (val == 0) {
                continue;
            }
            if (val == -1) {
                //新元素在老元素前面
                //允许多次
                item.addFollower(po.hashCode());
                po.addPreHash(item.hashCode());
                if (item.getLevel() == 0 || item.getLevel() <= po.getLevel()) {
                    int count = po.getLevel() - item.level + 1;
                    item.setLevel(item.getLevel() + count);
                    addPreItemLevel(resultMap, item.getPreHash(), count);
                }
            }
            if (val == 1) {
                //新元素在老元素后面
                //允许多次
                if (po.getLevel() > item.level) {
                } else {
                    //逐级增加level值
                    int count = item.getLevel() - po.getLevel() + 1;
                    po.setLevel(po.getLevel() + count);
                    addPreItemLevel(resultMap, po.getPreHash(), count);

                }
                po.addFollower(item.hashCode());
                item.addPreHash(po.hashCode());
            }
        }
        resultMap.put(item.hashCode(), item);


    }

    private static <T extends Comparable<? super T>> void addPreItemLevel(Map<Integer, SortItem<T>> resultMap, List<Integer> preHash, int addLevel) {

        if (null == preHash || preHash.isEmpty()) {
            return;
        }
        for (Integer hash : preHash) {
            SortItem<T> item = resultMap.get(hash);
            if(null == item){
                break;
            }
            item.setLevel(item.getLevel() + addLevel);
            addPreItemLevel(resultMap, item.getPreHash(), addLevel);
        }
    }


    static class SortItem<T extends Comparable<? super T>> implements Comparable<SortItem<T>> {
        public SortItem(T t) {
            this.t = t;
        }

        private T t;

        //确定在这个元素之前的元素hash
        private List<Integer> preHash;
        //确定在这个元素之后的元素hash
        private List<Integer> followHash;
        //        本元素的层级
        private int level;

        private long orderBy;

        public String toString() {
            String follow = "";
            if (null != followHash) {
                for (Integer val : followHash) {
                    follow += "," + val;
                }
                follow = follow.substring(1);
            }
            String pre = "";
            if (null != preHash) {
                for (Integer val : preHash) {
                    pre += "," + val;
                }
                pre = pre.substring(1);
            }
            return t.toString() + " , level:" + level + ",pre: " + pre + ", follow: " + follow;
        }

        public T getT() {
            return t;
        }

        public void setT(T t) {
            this.t = t;
        }

        public List<Integer> getPreHash() {
            return preHash;
        }

        public List<Integer> getFollowHash() {
            return followHash;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public long getOrderBy() {
            return orderBy;
        }

        public void setOrderBy(long orderBy) {
            this.orderBy = orderBy;
        }

        public void addFollower(int hash) {
            if (this.followHash == null) {
                this.followHash = new ArrayList<>();
            }
            this.followHash.add(hash);
        }

        public void addPreHash(int hash) {
            if (this.preHash == null) {
                this.preHash = new ArrayList<>();
            }
            this.preHash.add(hash);
        }

        @Override
        public int compareTo(SortItem<T> o) {
            if (this.getLevel() > o.getLevel()) {
                return -1;
            }
            if (this.getLevel() < o.getLevel()) {
                return 1;
            }

            return 0;
        }
    }


}
