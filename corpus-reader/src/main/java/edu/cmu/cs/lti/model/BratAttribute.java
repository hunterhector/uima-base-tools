package edu.cmu.cs.lti.model;

public class BratAttribute {
        public String attributeId;
        public String attributeName;
        public String attributeValue;
        public String attributeHost;

        public BratAttribute(String attributeLine) {
            parseAttribute(attributeLine);
        }

        private void parseAttribute(String attributeLine) {
            String[] parts = attributeLine.split("\t");
            attributeId = parts[0];
            String[] attributeFields = parts[1].split(" ");
            attributeName = attributeFields[0];
            attributeHost = attributeFields[1];
            attributeValue = attributeFields[2];
        }
    }