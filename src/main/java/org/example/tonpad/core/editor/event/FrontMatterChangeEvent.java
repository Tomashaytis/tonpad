package org.example.tonpad.core.editor.event;

public record FrontMatterChangeEvent(String action, String oldKey, String oldValue, String newKey, String newValue) {

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