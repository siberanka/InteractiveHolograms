package com.siberanka.interactiveholograms.display;

import com.siberanka.interactiveholograms.api.actions.Action;
import com.siberanka.interactiveholograms.api.actions.ClickType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A text hologram page with independent content, spacing and click actions. */
public final class TextDisplayPage {

    private final List<TextDisplayLine> lines = new ArrayList<>();
    private final Map<ClickType, List<Action>> actions = new EnumMap<>(ClickType.class);

    public TextDisplayPage() {
    }

    public TextDisplayPage(List<TextDisplayLine> lines, Map<ClickType, List<Action>> actions) {
        setLines(lines);
        setActions(actions);
    }

    public synchronized void setLines(List<TextDisplayLine> values) {
        lines.clear();
        if (values != null) {
            values.forEach(line -> lines.add(line.copy()));
        }
    }

    public synchronized List<TextDisplayLine> getLines() {
        List<TextDisplayLine> copy = new ArrayList<>(lines.size());
        lines.forEach(line -> copy.add(line.copy()));
        return Collections.unmodifiableList(copy);
    }

    synchronized List<TextDisplayLine> mutableLines() {
        return lines;
    }

    public synchronized void setActions(Map<ClickType, List<Action>> values) {
        actions.clear();
        if (values != null) {
            values.forEach((type, entries) -> actions.put(type, new ArrayList<>(entries)));
        }
    }

    public synchronized Map<ClickType, List<Action>> getActions() {
        Map<ClickType, List<Action>> copy = new EnumMap<>(ClickType.class);
        actions.forEach((type, entries) -> copy.put(type,
                Collections.unmodifiableList(new ArrayList<>(entries))));
        return Collections.unmodifiableMap(copy);
    }

    public synchronized List<Action> getActions(ClickType type) {
        List<Action> entries = actions.get(type);
        return entries == null ? Collections.emptyList() : new ArrayList<>(entries);
    }

    public synchronized boolean hasActions() {
        return actions.values().stream().anyMatch(entries -> entries != null && !entries.isEmpty());
    }

    public TextDisplayPage copy() {
        return new TextDisplayPage(getLines(), getActions());
    }
}
