package org.example.tonpad.core.editor.event;

public class FrontMatterChangeEvent {
    private final String action; // "add", "updateValue", "updateKey", "delete"
    private final String oldKey;
    private final String oldValue;
    private final String newKey;
    private final String newValue;

    public FrontMatterChangeEvent(String action, String oldKey, String oldValue, String newKey, String newValue) {
        this.action = action;
        this.oldKey = oldKey;
        this.oldValue = oldValue;
        this.newKey = newKey;
        this.newValue = newValue;
    }

    // Getters
    public String getAction() { return action; }
    public String getOldKey() { return oldKey; }
    public String getOldValue() { return oldValue; }
    public String getNewKey() { return newKey; }
    public String getNewValue() { return newValue; }

    public String getAffectedKey() {
        return switch (action) {
            case "add", "updateKey" -> newKey;
            case "updateValue", "delete" -> oldKey;
            default -> null;
        };
    }

    public String getCurrentValue() {
        return switch (action) {
            case "add", "updateValue", "updateKey" -> newValue;
            case "delete" -> oldValue;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return "FrontMatterChangeEvent{" +
                "action='" + action + '\'' +
                ", oldKey='" + oldKey + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newKey='" + newKey + '\'' +
                ", newValue='" + newValue + '\'' +
                '}';
    }
}