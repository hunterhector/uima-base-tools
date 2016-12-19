package edu.cmu.cs.lti.model;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/6/16
 * Time: 6:01 PM
 *
 * @author Zhengzhong Liu
 */
public class MultiSpan implements Comparable<MultiSpan>, Iterable<Span> {
    private List<Span> spans;

    public MultiSpan(List<Span> spans) {
        spans.sort(Comparator.reverseOrder());
        this.spans = new ArrayList<>();
        this.spans.addAll(spans);
    }

    public boolean equals(Object object) {
        if (object instanceof MultiSpan) {
            MultiSpan that = (MultiSpan) object;
            return new EqualsBuilder().append(spans, that.spans).isEquals();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return Joiner.on("; ").join(spans);
    }

    public int hashCode() {
        return new HashCodeBuilder().append(spans).toHashCode();
    }

    @Override
    public int compareTo(MultiSpan thatSpan) {
        return new CompareToBuilder().append(spans, thatSpan.spans).toComparison();
    }

    @Override
    public Iterator<Span> iterator() {
        return spans.iterator();
    }

    @Override
    public Spliterator<Span> spliterator() {
        return spans.spliterator();
    }

    public int size() {
        return spans.size();
    }

    public Span get(int i) {
        return spans.get(i);
    }

    public Span getRange() {
        return Span.of(spans.get(0).getBegin(), spans.get(spans.size() - 1).getEnd());
    }
}
