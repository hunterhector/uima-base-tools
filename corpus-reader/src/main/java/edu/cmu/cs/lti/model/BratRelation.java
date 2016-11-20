package edu.cmu.cs.lti.model;

public class BratRelation {
        public String relationId;
        public String relationName;
        public String arg1Name;
        public String arg1Id;
        public String arg2Name;
        public String arg2Id;

        public BratRelation(String attributeLine) {
            parseRelation(attributeLine);
        }

        private void parseRelation(String attributeLine) {
            String[] parts = attributeLine.split("\t");
            relationId = parts[0];
            String[] attributeFields = parts[1].split(" ");
            relationName = attributeFields[0];
            String[] arg1 = attributeFields[1].split(":");
            String[] arg2 = attributeFields[2].split(":");

            arg1Name = arg1[0];
            arg1Id = arg1[1];

            arg2Name = arg2[0];
            arg2Id = arg2[1];
        }
    }