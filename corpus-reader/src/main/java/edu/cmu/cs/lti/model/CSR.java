package edu.cmu.cs.lti.model;

import com.google.gson.annotations.SerializedName;

/**
 * A POJO for the AIDA CSR format.
 * Date: 4/5/18
 * Time: 2:28 PM
 *
 * @author Zhengzhong Liu
 */
public class CSR {
    @SerializedName("@context")
    public String[] context;
    @SerializedName("@type")
    public String objectType;
    public Meta meta;
    public Frame[] frames;

    public class Meta {
        @SerializedName("@type")
        public String objectType;
        public String component;
        public String organization;
        @SerializedName("document_id")
        public String documentId;
    }

    public class Frame {
        @SerializedName("@id")
        public String id;
        @SerializedName("@type")
        public String objectType;
        public String parent_scope;
    }

    public class TextSpan {
        @SerializedName("@type")
        public String objectType;
        public String reference;
        public int start;
        public int length;
    }

    public class Interp {
        @SerializedName("@type")
        public String objectType;
        public String type;
    }

    public class EntityMentionInterp extends Interp {

    }

    public class EventMentionInterp extends Interp {
        public EventMentionArg[] args;
    }

    public class DocFrame extends Frame {
        public String type;
        public String media_type;
    }

    public class SentenceFrame extends Frame {
        public String text;
        public TextSpan extent;
    }

    public class EntityMentionFrame extends Frame {
        public String text;
        public String reference;
        public int start;
        public int length;
        public EntityMentionInterp interp;
    }

    public class EventMentionFrame extends Frame {
        public TextSpan trigger;
        public TextSpan extent;
        public String text;
        public EventMentionInterp interp;
    }

    public class EventMentionArg {
        @SerializedName("@type")
        public String objectType;
        public String type;
        public String reference;
        public String text;
        public String arg;
    }
}
