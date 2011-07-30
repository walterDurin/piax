package org.piax.ov.common;

import java.util.ArrayList;
import java.util.List;

import org.piax.ov.common.KeyComparator;

public class Range {
	public Comparable<?> min;
	public Comparable<?> max;
	public boolean includeMin;
	public boolean includeMax;
	public Range(Comparable<?> min, Comparable<?> max) {
		this.min = min;
		this.max = max;
		this.includeMin = true;
		this.includeMax = true;
	}
	int compare(Comparable<?> a, Comparable<?>b) {
        return KeyComparator.getInstance().compare(a, b);
    }
	public boolean includes(Comparable<?> k) {
	    if (includeMin && includeMax) {
	        return compare(min, k) <= 0 && compare(k, max) <= 0;
	    }
	    if (includeMin && !includeMax) {
	        return compare(min, k) <= 0 && compare(k, max) < 0;
	    }
	    if (!includeMin && includeMax) {
	        return compare(min, k) < 0 && compare(k, max) <= 0;
	    }
	    else {
	        return compare(min, k) < 0 && compare(k, max) < 0;
	    }
    }
	public List<Range> divideL(Comparable<?> k) {
        ArrayList<Range> ret = new ArrayList<Range>();
        Range left = new Range(min, k);
        left.includeMin = includeMin;
        ret.add(left);
        Range right = new Range(k, max);
        right.includeMin = false;
        right.includeMax = includeMax;
        ret.add(right);
        return ret;
    }
	public List<Range> divideR(Comparable<?> k) {
        ArrayList<Range> ret = new ArrayList<Range>();
        Range left = new Range(min, k);
        left.includeMin = includeMin;
        left.includeMax = false;        
        ret.add(left);
        Range right = new Range(k, max);
        right.includeMax = includeMax;
        ret.add(right);
        return ret;
    }
	public List<Range> divideC(Comparable<?> k) {
        ArrayList<Range> ret = new ArrayList<Range>();
        Range left = new Range(min, k);
        left.includeMin = includeMin;
        left.includeMax = false;
        ret.add(left);
        Range right = new Range(k, max);
        right.includeMin = false;
        right.includeMax = includeMax;
        ret.add(right);
        return ret;
    }
	
	public String toString() {
	    return (includeMin? "[" : "<") + min + "," + max + (includeMax? "]" : ">");
	}
}
