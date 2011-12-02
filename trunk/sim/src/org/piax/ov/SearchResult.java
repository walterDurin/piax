package org.piax.ov;

import java.util.List;

import org.piax.trans.Node;
import org.piax.trans.common.Id;

public class SearchResult {
    public Node node;
    public List<Id> via;
    public SearchResult(Node node, List<Id> via) {
        this.node = node;
        this.via = via;
    }
}
