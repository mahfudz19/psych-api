package com.psycorp.psychapi.feature.test.models;

import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "kraepelin_records")
public class KraepelinRecord extends PanacheMongoEntity {
    private ObjectId attemptId;
    private Map<String, Object> configSnapshot;
    private List<Map<String, Object>> columns;
    private Map<String, Object> totals;

    public ObjectId getAttemptId() { return attemptId; }
    public void setAttemptId(ObjectId attemptId) { this.attemptId = attemptId; }
    public Map<String, Object> getConfigSnapshot() { return configSnapshot; }
    public void setConfigSnapshot(Map<String, Object> configSnapshot) { this.configSnapshot = configSnapshot; }
    public List<Map<String, Object>> getColumns() { return columns; }
    public void setColumns(List<Map<String, Object>> columns) { this.columns = columns; }
    public Map<String, Object> getTotals() { return totals; }
    public void setTotals(Map<String, Object> totals) { this.totals = totals; }
}